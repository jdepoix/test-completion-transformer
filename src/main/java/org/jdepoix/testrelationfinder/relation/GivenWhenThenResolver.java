package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GivenWhenThenResolver {
    private static final Set<String> JUNIT_ASSERTIONS = Set.of(
        "assertSame",
        "assertNotNull",
        "assertFalse",
        "assertThrows",
        "assertTimeout",
        "assertArrayEquals",
        "assertTimeoutPreemptively",
        "assertEquals",
        "assertIterableEquals",
        "assertNotSame",
        "assertNull",
        "assertTrue",
        "assertThat",
        "assertNotEquals",
        "assertLinesMatch",
        "assertAll"
    );
    private static final List<String> JUNIT_BEFORE_CLASS_ANNOTATIONS = List.of(
        "BeforeClass",
        "BeforeAll"
    );
    private static final List<String> JUNIT_BEFORE_EACH_ANNOTATIONS = List.of(
        "Before",
        "BeforeEach"
    );
    private static final String WHEN_TOKEN = "<WHEN>";

    public GivenWhenThenRelation resolve(TestRelation testRelation) {
        if (testRelation.getRelatedMethod().isEmpty()) {
            return new GivenWhenThenRelation(testRelation, GivenWhenThenRelation.ResolutionStatus.NOT_RESOLVED);
        }

        final MethodDeclaration testMethod = testRelation.getTestMethod().clone();

        final String relatedMethodCallName = testRelation.getRelatedMethod().get().getNameAsString();
        final String relatedMethodCallString = testRelation.getRelatedMethod().get().toString();

        MethodCallExpr whenCall = null;
        List<Statement> thenCalls = new ArrayList<>();

        for (MethodCallExpr methodCall : testMethod.findAll(MethodCallExpr.class)) {
            final String methodCallName = methodCall.getNameAsString();
            if (methodCallName.equals(relatedMethodCallName)) {
                if (whenCall != null && !methodCall.toString().equals(relatedMethodCallString)) {
                    return new GivenWhenThenRelation(
                        testRelation,
                        GivenWhenThenRelation.ResolutionStatus.MULTIPLE_WHENS_FOUND
                    );
                }
                whenCall = methodCall;
            } else if (JUNIT_ASSERTIONS.contains(methodCallName)) {
                thenCalls.add(this.extractAssertionExpression(methodCall));
            }
        }

        if (thenCalls.isEmpty()) {
            return new GivenWhenThenRelation(testRelation, GivenWhenThenRelation.ResolutionStatus.NO_THEN_FOUND);
        }

        return new GivenWhenThenRelation(
            testRelation,
            this.parseStatementList(
                Stream.concat(
                    this.getSetupCode(testRelation.getTestMethod()),
                    testMethod.getBody().get().getStatements().stream()
                ).collect(Collectors.toList()),
                relatedMethodCallString
            ),
            relatedMethodCallString,
            this.parseStatementList(thenCalls, relatedMethodCallString)
        );
    }

    private Stream<Statement> getSetupCode(MethodDeclaration testMethod) {
        List<Statement> beforeClass = new ArrayList<>();
        List<Statement> beforeEach = new ArrayList<>();

        for (AnnotationExpr annotation : this.findParentClass(testMethod).findAll(AnnotationExpr.class)) {
            final String annotationName = annotation.getNameAsString();
            if (JUNIT_BEFORE_CLASS_ANNOTATIONS.contains(annotationName)) {
                beforeClass
                    .addAll(((MethodDeclaration) annotation.getParentNode().get()).getBody().get().getStatements());
            } else if (JUNIT_BEFORE_EACH_ANNOTATIONS.contains(annotationName)) {
                beforeEach
                    .addAll(((MethodDeclaration) annotation.getParentNode().get()).getBody().get().getStatements());
            }
        }

        return Stream.concat(beforeClass.stream(), beforeEach.stream());
    }

    private ExpressionStmt extractAssertionExpression(MethodCallExpr assertionCall) {
        final Node expression = assertionCall.getParentNode().get();
        expression.getParentNode().get().remove(expression);
        return (ExpressionStmt) expression;
    }

    private String parseASTToMaskedString(Node ast, String relatedMethodCallString) {
        return ast.toString().replace(relatedMethodCallString, WHEN_TOKEN);
    }

    private String parseStatementList(
        List<Statement> statementList,
        String relatedMethodCallString
    ) {
        return statementList
            .stream()
            .map(statement -> this.parseASTToMaskedString(statement, relatedMethodCallString))
            .collect(Collectors.joining("\n"));
    }

    private ClassOrInterfaceDeclaration findParentClass(Node node) {
        final Node parentNode = node.getParentNode().get();
        if (parentNode instanceof ClassOrInterfaceDeclaration) {
            return (ClassOrInterfaceDeclaration) parentNode;
        }
        return this.findParentClass(parentNode);
    }
}
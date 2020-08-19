package org.jdepoix.testrelationfinder.gwt;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import org.jdepoix.testrelationfinder.relation.ResolvedTestRelation;
import org.jdepoix.testrelationfinder.relation.TestRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GWTSectionResolver {
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

    public GWTTestRelation resolve(ResolvedTestRelation resolvedTestRelation) {
        final TestRelation testRelation = resolvedTestRelation.getTestRelation();

        if (testRelation.getRelatedMethod().isEmpty()) {
            return new GWTTestRelation(resolvedTestRelation, GWTTestRelation.ResolutionStatus.NOT_RESOLVED);
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
                    return new GWTTestRelation(
                        resolvedTestRelation,
                        GWTTestRelation.ResolutionStatus.MULTIPLE_WHENS_FOUND
                    );
                }
                whenCall = methodCall;
            } else if (JUNIT_ASSERTIONS.contains(methodCallName)) {
                this.extractAssertionExpression(methodCall).ifPresent(expressionStmt -> thenCalls.add(expressionStmt));
            }
        }

        if (thenCalls.isEmpty()) {
            return new GWTTestRelation(resolvedTestRelation, GWTTestRelation.ResolutionStatus.NO_THEN_FOUND);
        }

        return new GWTTestRelation(
            resolvedTestRelation,
            this.parseStatementList(
                Stream.concat(
                    this.getSetupCode(testRelation.getTestMethod()),
                    testMethod.getBody().get().getStatements().stream()
                ).collect(Collectors.toList()),
                relatedMethodCallString
            ),
            this.parseStatementList(thenCalls, relatedMethodCallString),
            List.of()
        );
    }


    // TODO
    public GWTTestRelation resolveTEST(ResolvedTestRelation resolvedTestRelation) {
        // TODO do not sub with <WHEN>
        if (resolvedTestRelation.getResolvedRelatedMethod().isEmpty()) {
            return new GWTTestRelation(resolvedTestRelation, GWTTestRelation.ResolutionStatus.NOT_RESOLVED);
        }

        final String relatedMethodName = resolvedTestRelation
            .getTestRelation()
            .getRelatedMethod()
            .get()
            .getNameAsString();

        boolean whenFound = false;
        // TODO get setup code
        final List<Statement> given = new ArrayList<>();
        final List<Statement> then = new ArrayList<>();
        // TODO get setup code context
        final List<MethodCallExpr> context = new ArrayList<>();

        for (Statement statement : resolvedTestRelation.getTestRelation().getTestMethod().findAll(Statement.class)) {
            boolean isAssertionStatement = false;
            boolean statementContainsWhenCall = false;
            for (MethodCallExpr methodCall : statement.findAll(MethodCallExpr.class)) {
                final String methodName = methodCall.getNameAsString();
                if (methodName.equals(relatedMethodName)) {
                    ResolvedMethodDeclaration resolvedMethodCall = null;
                    try {
                        resolvedMethodCall = methodCall.resolve();
                    } catch (Exception e) {}
                    if (
                        resolvedMethodCall == null
                        || !resolvedTestRelation.getResolvedRelatedMethod().get().equals(resolvedMethodCall)
                    ) {
                        return new GWTTestRelation(
                            resolvedTestRelation,
                            GWTTestRelation.ResolutionStatus.MULTIPLE_WHENS_FOUND
                        );
                    }
                    statementContainsWhenCall = true;
                } else if (JUNIT_ASSERTIONS.contains(methodName)) {
                    isAssertionStatement = true;
                } else {
                    context.add(methodCall);
                }
            }

            if (statementContainsWhenCall && !then.isEmpty()) {
                return new GWTTestRelation(
                    resolvedTestRelation,
                    GWTTestRelation.ResolutionStatus.VIOLATES_SAP
                );
            }

            if (isAssertionStatement && (whenFound || statementContainsWhenCall)) {
                then.add(statement);
            } else {
                given.add(statement);
            }

            if (statementContainsWhenCall) {
                whenFound = true;
            }
        }

        if (then.isEmpty()) {
            return new GWTTestRelation(
                resolvedTestRelation,
                GWTTestRelation.ResolutionStatus.NO_THEN_FOUND
            );
        }

        // TODO create final object
        // TODO resolve context
        return null;
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

    private Optional<ExpressionStmt> extractAssertionExpression(MethodCallExpr assertionCall) {
        final Node expression = assertionCall.getParentNode().get();
        expression.getParentNode().get().remove(expression);
        if (!(expression instanceof ExpressionStmt)) {
            return Optional.empty();
        }
        return Optional.of((ExpressionStmt) expression);
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
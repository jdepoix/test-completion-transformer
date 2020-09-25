package org.jdepoix.testrelationfinder.gwt;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import org.jdepoix.testrelationfinder.relation.TestRelation;

import java.util.ArrayList;
import java.util.List;
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

    public GWTTestRelation resolve(TestRelation testRelation) {
        if (!testRelation.isResolved() || testRelation.getRelatedMethod().isEmpty()) {
            return new GWTTestRelation(testRelation, GWTTestRelation.ResolutionStatus.NOT_RESOLVED);
        }

        final String relatedMethodName = testRelation
            .getRelatedMethod()
            .get()
            .getName();

        boolean whenFound = false;
        final MethodDeclaration testMethod = testRelation.getTestMethod();
        final List<Statement> given = this.getSetupCode(testMethod);
        final List<Statement> then = new ArrayList<>();
        final List<MethodCallExpr> context = given
            .stream()
            .flatMap(statement -> statement.findAll(MethodCallExpr.class).stream())
            .collect(Collectors.toList());
        List<Statement> cachedStatements = new ArrayList<>();
        Integer thenSectionStartIndex = null;

        final NodeList<Statement> statements = testMethod.getBody().get().getStatements();
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
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
                        || !testRelation
                            .getRelatedMethod()
                            .get()
                            .getQualifiedSignature()
                            .equals(resolvedMethodCall.getQualifiedSignature())
                    ) {
                        return new GWTTestRelation(
                            testRelation,
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

            if (!then.isEmpty()) {
                if (statementContainsWhenCall) {
                    return new GWTTestRelation(testRelation, GWTTestRelation.ResolutionStatus.VIOLATES_SAP);
                }

                then.add(statement);
                continue;
            }

            if (statementContainsWhenCall) {
                given.addAll(cachedStatements);
                if (isAssertionStatement) {
                    if (thenSectionStartIndex == null) {
                        thenSectionStartIndex = i;
                    }
                    then.add(statement);
                } else {
                    given.add(statement);
                }

                cachedStatements.clear();
                whenFound = true;
            } else if (isAssertionStatement && whenFound) {
                if (thenSectionStartIndex == null) {
                    thenSectionStartIndex = i;
                }
                then.addAll(cachedStatements);
                then.add(statement);
            } else {
                cachedStatements.add(statement);
            }
        }

        if (!whenFound) {
            return new GWTTestRelation(testRelation, GWTTestRelation.ResolutionStatus.NO_WHEN_FOUND);
        }

        if (then.isEmpty()) {
            return new GWTTestRelation(testRelation, GWTTestRelation.ResolutionStatus.NO_THEN_FOUND);
        }

        return new GWTTestRelation(
            testRelation,
            given.stream().map(Statement::toString).collect(Collectors.joining("\n")),
            then.stream().map(Statement::toString).collect(Collectors.joining("\n")),
            this.findWhenLocation(statements, thenSectionStartIndex, relatedMethodName),
            thenSectionStartIndex,
            context
        );
    }

    private GWTTestRelation.WhenLocation findWhenLocation(
        List<Statement> statements, int thenSectionStartIndex, String relatedMethodName
    ) {
        final boolean inThen = this.doStatementsContainRelatedMethodCall(
            statements.subList(0, thenSectionStartIndex), relatedMethodName
        );
        final boolean inGiven = this.doStatementsContainRelatedMethodCall(
            statements.subList(thenSectionStartIndex, statements.size()), relatedMethodName
        );

        if (inGiven && inGiven) {
            return GWTTestRelation.WhenLocation.BOTH;
        } else if (inGiven) {
            return GWTTestRelation.WhenLocation.GIVEN;
        } else if (inThen)  {
            return GWTTestRelation.WhenLocation.THEN;
        }

        throw new IllegalStateException("this method should only be used for methods which contain When calls");
    }

    private boolean doStatementsContainRelatedMethodCall(List<Statement> statements, String relatedMethodName) {
        return statements
            .stream()
            .anyMatch(statement ->
                statement
                    .findFirst(
                        MethodCallExpr.class,
                        methodCallExpr -> methodCallExpr.getNameAsString().equals(relatedMethodName)
                    ).isPresent()
            );
    }

    public static List<Statement> getSetupCode(MethodDeclaration testMethod) {
        List<Statement> beforeClass = new ArrayList<>();
        List<Statement> beforeEach = new ArrayList<>();

        for (AnnotationExpr annotation : findParentClass(testMethod).findAll(AnnotationExpr.class)) {
            final String annotationName = annotation.getNameAsString();
            if (JUNIT_BEFORE_CLASS_ANNOTATIONS.contains(annotationName)) {
                beforeClass
                    .addAll(((MethodDeclaration) annotation.getParentNode().get()).getBody().get().getStatements());
            } else if (JUNIT_BEFORE_EACH_ANNOTATIONS.contains(annotationName)) {
                beforeEach
                    .addAll(((MethodDeclaration) annotation.getParentNode().get()).getBody().get().getStatements());
            }
        }

        return Stream.concat(beforeClass.stream(), beforeEach.stream()).collect(Collectors.toList());
    }

    private static ClassOrInterfaceDeclaration findParentClass(Node node) {
        final Node parentNode = node.getParentNode().get();
        if (parentNode instanceof ClassOrInterfaceDeclaration) {
            return (ClassOrInterfaceDeclaration) parentNode;
        }
        return findParentClass(parentNode);
    }
}
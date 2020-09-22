package org.jdepoix.testrelationfinder.gwt;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import org.jdepoix.testrelationfinder.relation.ResolvedTestRelation;

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

    public GWTTestRelation resolve(ResolvedTestRelation resolvedTestRelation) {
        if (
            resolvedTestRelation.getResolutionStatus() != ResolvedTestRelation.ResolutionStatus.RESOLVED
            || resolvedTestRelation.getResolvedRelatedMethod().isEmpty()
        ) {
            return new GWTTestRelation(resolvedTestRelation, GWTTestRelation.ResolutionStatus.NOT_RESOLVED);
        }

        final String relatedMethodName = resolvedTestRelation
            .getTestRelation()
            .getRelatedMethod()
            .get()
            .getNameAsString();

        boolean whenFound = false;
        final MethodDeclaration testMethod = resolvedTestRelation.getTestRelation().getTestMethod();
        final List<Statement> given = this.getSetupCode(testMethod);
        final List<Statement> then = new ArrayList<>();
        final List<MethodCallExpr> context = given
            .stream()
            .flatMap(statement -> statement.findAll(MethodCallExpr.class).stream())
            .collect(Collectors.toList());
        List<Statement> cachedStatements = new ArrayList<>();

        // TODO save index where then starts in getStatements list
        for (Statement statement : testMethod.getBody().get().getStatements()) {
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
                        || !resolvedTestRelation
                            .getResolvedRelatedMethod()
                            .get()
                            .getQualifiedSignature()
                            .equals(resolvedMethodCall.getQualifiedSignature())
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

            if (!then.isEmpty()) {
                if (statementContainsWhenCall) {
                    return new GWTTestRelation(resolvedTestRelation, GWTTestRelation.ResolutionStatus.VIOLATES_SAP);
                }

                then.add(statement);
                continue;
            }

            if (statementContainsWhenCall) {
                given.addAll(cachedStatements);
                if (isAssertionStatement) {
                    then.add(statement);
                } else {
                    given.add(statement);
                }

                cachedStatements.clear();
                whenFound = true;
            } else if (isAssertionStatement && whenFound) {
                then.addAll(cachedStatements);
                then.add(statement);
            } else {
                cachedStatements.add(statement);
            }
        }

        if (!whenFound) {
            return new GWTTestRelation(resolvedTestRelation, GWTTestRelation.ResolutionStatus.NO_WHEN_FOUND);
        }

        if (then.isEmpty()) {
            return new GWTTestRelation(resolvedTestRelation, GWTTestRelation.ResolutionStatus.NO_THEN_FOUND);
        }

        return new GWTTestRelation(
            resolvedTestRelation,
            given.stream().map(Statement::toString).collect(Collectors.joining("\n")),
            then.stream().map(Statement::toString).collect(Collectors.joining("\n")),
            context
        );
    }

    private List<Statement> getSetupCode(MethodDeclaration testMethod) {
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

        return Stream.concat(beforeClass.stream(), beforeEach.stream()).collect(Collectors.toList());
    }

    private ClassOrInterfaceDeclaration findParentClass(Node node) {
        final Node parentNode = node.getParentNode().get();
        if (parentNode instanceof ClassOrInterfaceDeclaration) {
            return (ClassOrInterfaceDeclaration) parentNode;
        }
        return this.findParentClass(parentNode);
    }
}
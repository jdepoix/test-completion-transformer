package org.jdepoix.dataset.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import org.jdepoix.dataset.ast.node.*;
import org.jdepoix.dataset.testrelationfinder.gwt.GWTSectionResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestDeclarationCreator {
    public TestDeclaration create(MethodDeclaration testMethod, MethodDeclaration relatedMethod) {
        substituteGWTNodesInTestMethod(testMethod, relatedMethod);

        final String relatedMethodSignature = String.format(
            "%s.%s",
            relatedMethod.resolve().getQualifiedName(),
            relatedMethod.getDeclarationAsString()
        );
        final List<MethodDeclaration> testContextDeclarations = substituteTestMethodContext(
            testMethod,
            relatedMethodSignature
        );

        final List<MethodDeclaration> contextDeclarations = substituteRelatedMethodContext(
            relatedMethod,
            relatedMethodSignature
        );

        final TestDeclaration testDeclaration = buildTestDeclaration(
            testMethod,
            relatedMethod,
            testContextDeclarations,
            contextDeclarations
        );
        JavaParserFacade.clearInstances();
        return testDeclaration;
    }

    private void substituteGWTNodesInTestMethod(MethodDeclaration testMethod, MethodDeclaration relatedMethod) {
        testMethod.findAll(
            MethodCallExpr.class,
            methodCallExpr -> methodCallExpr.getNameAsString().equals(relatedMethod.getNameAsString())
        ).forEach(methodCallExpr -> methodCallExpr.replace(WhenMethodCallExpr.fromMethodCallExpr(methodCallExpr)));

        testMethod.getBody().get().getStatements().addAll(0, GWTSectionResolver.getSetupCode(testMethod));
        testMethod.getBody().get().getStatements().add(new ThenSection());
    }

    private List<MethodDeclaration> substituteTestMethodContext(
        MethodDeclaration testMethod, String relatedMethodSignature
    ) {
        final List<MethodDeclaration> testContextDeclarations = new ArrayList<>();
        this.substituteMethodCallExpressions(
            testMethod,
            testContextDeclarations,
            TestContextMethodCallExpr::fromMethodCallExpr,
            relatedMethodSignature
        );
        return testContextDeclarations;
    }

    private List<MethodDeclaration> substituteRelatedMethodContext(
        MethodDeclaration relatedMethod, String relatedMethodSignature
    ) {
        final List<MethodDeclaration> contextDeclarations = new ArrayList<>();
        this.substituteMethodCallExpressions(
            relatedMethod,
            contextDeclarations,
            ContextMethodCallExpr::fromMethodCallExpr,
            relatedMethodSignature
        );
        return contextDeclarations;
    }

    private void substituteMethodCallExpressions(
        MethodDeclaration method,
        List<MethodDeclaration> substitutedMethods,
        Function<MethodCallExpr, Node> substituteGenerator,
        String relatedMethodSignature
    ) {
        method.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
            ResolvedMethodDeclaration resolvedMethod = null;
            MethodDeclaration methodDeclaration = null;
            try {
                resolvedMethod = methodCallExpr.resolve();
                methodDeclaration = resolvedMethod.toAst().orElse(null);
            } catch (Exception e) {}
            if (resolvedMethod != null && methodDeclaration != null) {
                if (
                    String.format(
                        "%s.%s",
                        resolvedMethod.getQualifiedName(),
                        methodDeclaration.getDeclarationAsString()
                    ).equals(relatedMethodSignature)
                ) {
                    methodCallExpr.replace(WhenMethodCallExpr.fromMethodCallExpr(methodCallExpr));
                } else {
                    if (!substitutedMethods.contains(methodDeclaration)) {
                        substitutedMethods.add(methodDeclaration);
                        substituteMethodCallExpressions(
                            methodDeclaration, substitutedMethods, substituteGenerator, relatedMethodSignature
                        );
                    }
                    methodCallExpr.replace(substituteGenerator.apply(methodCallExpr));
                }
            }
        });
    }

    private TestDeclaration buildTestDeclaration(
        MethodDeclaration testMethod,
        MethodDeclaration relatedMethod,
        List<MethodDeclaration> testContextDeclarations,
        List<MethodDeclaration> contextDeclarations
    ) {
        return new TestDeclaration(
            new TestBody(testMethod.getBody().get().getStatements()),
            new NodeList<>(
                testContextDeclarations.stream()
                    .map(TestContextMethodDeclaration::fromMethodDeclaration)
                    .collect(Collectors.toList())
            ),
            WhenMethodDeclaration.fromMethodDeclaration(relatedMethod),
            new NodeList<>(
                contextDeclarations.stream()
                    .map(ContextMethodDeclaration::fromMethodDeclaration)
                    .collect(Collectors.toList())
            ),
            testMethod.getName()
        );
    }
}

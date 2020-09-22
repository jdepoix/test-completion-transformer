package org.jdepoix.datasetcreator.gwt;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.jdepoix.ast.node.*;
import org.jdepoix.ast.serialization.AST;
import org.jdepoix.config.ResultDirConfig;
import org.jdepoix.testrelationfinder.gwt.GWTSectionResolver;
import org.jdepoix.testrelationfinder.reporting.TestRelationReportEntry;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ASTResolver {
    public class CantResolve extends Exception {
        public CantResolve(String message) {
            super(message);
        }
    }

    private final ResultDirConfig config;

    public ASTResolver(ResultDirConfig config) {
        this.config = config;
        final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
    }

    // TODO build return type
    // TODO safe prediction target
    public ResolvedAST resolve(TestRelationReportEntry entry) throws IOException, CantResolve {
        final MethodDeclaration testMethod = parseTestMethod(entry);
        final MethodDeclaration relatedMethod = parseRelatedMethod(entry);

        final NodeList<Statement> thenSection = substituteGWTNodesInTestMethod(testMethod, entry);

        final String relatedMethodSignature = relatedMethod.resolve().getQualifiedSignature();
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

        try {
            final AST ast = AST.serialize(testDeclaration);
            System.out.println(ast.printTree());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO
        return null;
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

    private NodeList<Statement> substituteGWTNodesInTestMethod(
        MethodDeclaration testMethod, TestRelationReportEntry entry
    ) {
        testMethod.findAll(
            MethodCallExpr.class,
            methodCallExpr -> methodCallExpr.getNameAsString().equals(entry.getRelatedMethodName().get())
        ).forEach(methodCallExpr -> methodCallExpr.replace(WhenMethodCallExpr.fromMethodCallExpr(methodCallExpr)));

        final NodeList<Statement> thenSection = this.substituteThenSection(
            testMethod.getBody().get().getStatements(), entry.getThenSectionStartIndex().get()
        );
        testMethod.getBody().get().getStatements().addAll(0, GWTSectionResolver.getSetupCode(testMethod));
        return thenSection;
    }

    private MethodDeclaration parseRelatedMethod(TestRelationReportEntry entry) throws CantResolve, IOException {
        return findMethodDeclarationBySignature(
            StaticJavaParser.parse(config.resolveRepoFile(entry.getRepoName(), entry.getRelatedMethodPath().get())),
            entry.getRelatedMethodClassName().get(), entry.getRelatedMethodSignature().get()
        );
    }

    private MethodDeclaration parseTestMethod(TestRelationReportEntry entry) throws CantResolve, IOException {
        return findMethodDeclarationBySignature(
            StaticJavaParser.parse(config.resolveRepoFile(entry.getRepoName(), entry.getTestPath())),
            entry.getTestMethodClassName(), entry.getTestMethodSignature()
        );
    }

    private NodeList<Statement> substituteThenSection(NodeList<Statement> statements, int thenSectionStartIndex) {
        List<Statement> thenStatements = new ArrayList<>();
        while (statements.size() > thenSectionStartIndex) {
            thenStatements.add(statements.remove(thenSectionStartIndex));
        }

        statements.add(new ThenSection());

        return new NodeList<>(thenStatements);
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
                if (resolvedMethod.getQualifiedSignature().equals(relatedMethodSignature)) {
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

    private MethodDeclaration findMethodDeclarationBySignature(
        CompilationUnit compilationUnit,
        String className,
        String methodSignature
    ) throws CantResolve {
        return compilationUnit
            .findFirst(
                ClassOrInterfaceDeclaration.class,
                classOrInterfaceDeclaration -> classOrInterfaceDeclaration.getNameAsString().equals(className)
            )
            .orElseThrow(() -> new CantResolve(String.format("can't find class %s", className)))
            .findFirst(
                MethodDeclaration.class,
                methodDeclaration -> methodDeclaration.getDeclarationAsString().equals(methodSignature)
            )
            .orElseThrow(() -> new CantResolve(String.format("can't find method %s", methodSignature)));
    }
}

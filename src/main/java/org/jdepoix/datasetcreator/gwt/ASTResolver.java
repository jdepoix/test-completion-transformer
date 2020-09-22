package org.jdepoix.datasetcreator.gwt;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import org.jdepoix.ast.node.WhenMethodCallExpr;
import org.jdepoix.ast.serialization.AST;
import org.jdepoix.config.ResultDirConfig;
import org.jdepoix.testrelationfinder.reporting.TestRelationReportEntry;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ASTResolver {
    public class CantResolve extends Exception {
        public CantResolve(String message) {
            super(message);
        }
    }

    private final ResultDirConfig config;

    public ASTResolver(ResultDirConfig config) {
        this.config = config;
        StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(new CombinedTypeSolver()));
    }

    // TODO resolve when definition
    // TODO make sure to avoid circular graph when resolving method declarations
    // TODO recursively travers when method and resolve subcalls into ContextMethodDeclarations and substitute with ContextMethodCallExpr/WhenMethodCallExpr
    // TODO safe ThenSection start index in GWTResolver
    // TODO substitute then with ThenSection node
    // TODO build return type
    // TODO load setup
    // TODO safe prediction target
    public ResolvedAST resolve(TestRelationReportEntry entry) throws IOException, CantResolve {
        final MethodDeclaration testMethod = findMethodDeclarationBySignature(
            StaticJavaParser.parse(config.resolveRepoFile(entry.getRepoName(), entry.getTestPath())),
            entry.getTestMethodClassName(), entry.getTestMethodSignature()
        );

        testMethod.findAll(
            MethodCallExpr.class,
            methodCallExpr -> methodCallExpr.getNameAsString().equals(entry.getRelatedMethodName().get())
        ).forEach(methodCallExpr -> methodCallExpr.replace(
            new WhenMethodCallExpr(
                methodCallExpr.getScope().orElse(null),
                methodCallExpr.getTypeArguments().orElse(null),
                methodCallExpr.getName(),
                methodCallExpr.getArguments()
            )
        ));

        final MethodDeclaration relatedMethod = findMethodDeclarationBySignature(
            StaticJavaParser.parse(config.resolveRepoFile(entry.getRepoName(), entry.getRelatedMethodPath().get())),
            entry.getRelatedMethodClassName().get(), entry.getRelatedMethodSignature().get()
        );
        this.resolveMethodCallExpressions(relatedMethod, new HashSet<>());


        try {
            final AST ast = AST.serialize(relatedMethod);
            System.out.println(ast.printTree());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO
        return null;
    }

    private void resolveMethodCallExpressions(MethodDeclaration method, Set<String> visitedMethods) {
        // TODO
        final String methodDeclaration = method.getDeclarationAsString();
        if (visitedMethods.contains(methodDeclaration)) {
            return;
        }
        visitedMethods.add(methodDeclaration);

        method.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
            MethodDeclaration declaration = null;
            try {
                declaration = methodCallExpr.resolve().toAst().get();
            } catch (Exception e) {}
            if (declaration != null) {
                resolveMethodCallExpressions(declaration, visitedMethods);
                // TODO replace with call expr
//                methodCallExpr.replace(
//                    new DeclaredMethodCallExpr(
//                        declaration,
//                        methodCallExpr.getScope().orElse(null),
//                        methodCallExpr.getTypeArguments().orElse(null),
//                        methodCallExpr.getName(),
//                        methodCallExpr.getArguments()
//                    )
//                );
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

package org.jdepoix.datasetcreator.gwt;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.jdepoix.ast.node.*;
import org.jdepoix.ast.serialization.ASTSequentializer;
import org.jdepoix.ast.serialization.ASTSerializer;
import org.jdepoix.ast.serialization.ASTToken;
import org.jdepoix.config.ResultDirConfig;
import org.jdepoix.datasetcreator.Datapoint;
import org.jdepoix.datasetcreator.DatapointResolver;
import org.jdepoix.testrelationfinder.gwt.GWTSectionResolver;
import org.jdepoix.testrelationfinder.reporting.TestRelationReportEntry;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GWTDatapointResolver implements DatapointResolver {
    public class CantResolve extends Exception {
        public CantResolve(String message) {
            super(message);
        }
    }

    private final ResultDirConfig config;
    private final ASTSerializer astSerializer;
    private final ASTSequentializer astSequentializer;

    public GWTDatapointResolver(
        ResultDirConfig config, ASTSerializer astSerializer, ASTSequentializer astSequentializer
    ) {
        this.config = config;
        this.astSerializer = astSerializer;
        this.astSequentializer = astSequentializer;
        final CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
    }

    public Datapoint resolve(TestRelationReportEntry entry)
        throws IOException, CantResolve, NoSuchFieldException, IllegalAccessException 
    {
        final MethodDeclaration testMethod = parseTestMethod(entry);
        final MethodDeclaration relatedMethod = parseRelatedMethod(entry);

        final NodeList<Statement> thenSection = substituteGWTNodesInTestMethod(testMethod, entry);

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

        final GWTDatapoint datapoint = new GWTDatapoint(
            entry.getId(),
            astSequentializer.sequentialize(astSerializer.serialize(testDeclaration)),
            sequentializeThen(thenSection),
            thenSection.stream().map(Statement::toString).collect(Collectors.joining("\n")),
            testContextDeclarations.size(),
            contextDeclarations.size()
        );

        JavaParserFacade.clearInstances();

        return datapoint;
    }

    private List<ASTToken> sequentializeThen(List<Statement> statements)
        throws NoSuchFieldException, IllegalAccessException
    {
        List<ASTToken> sequentializedThen = new ArrayList<>();
        for (Statement statement : statements) {
            sequentializedThen.addAll(astSequentializer.sequentialize(astSerializer.serialize(statement)));
        }
        return sequentializedThen;
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
            entry.getRelatedMethodClassName().get(),
            entry.getRelatedMethodSignature().get()
        );
    }

    private MethodDeclaration parseTestMethod(TestRelationReportEntry entry) throws CantResolve, IOException {
        return findMethodDeclarationBySignature(
            StaticJavaParser.parse(config.resolveRepoFile(entry.getRepoName(), entry.getTestPath())),
            entry.getTestMethodClassName(),
            entry.getTestMethodSignature()
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

    private MethodDeclaration findMethodDeclarationBySignature(
        CompilationUnit compilationUnit,
        String className,
        String methodSignature
    ) throws CantResolve {
        Optional<? extends Node> parentDeclaration = compilationUnit
            .findFirst(
                ClassOrInterfaceDeclaration.class,
                classOrInterfaceDeclaration -> classOrInterfaceDeclaration.resolve().getClassName().equals(className)
            );
        if (parentDeclaration.isEmpty()) {
            parentDeclaration = compilationUnit
                .findFirst(
                    EnumDeclaration.class,
                    enumDeclaration -> enumDeclaration.resolve().getClassName().equals(className)
                );
        }
        return parentDeclaration
            .orElseThrow(() -> new CantResolve(String.format("can't find class/enum %s", className)))
            .findFirst(
                MethodDeclaration.class,
                methodDeclaration -> methodDeclaration.getDeclarationAsString().equals(methodSignature)
            )
            .orElseThrow(() -> new CantResolve(String.format("can't find method %s", methodSignature)));
    }
}

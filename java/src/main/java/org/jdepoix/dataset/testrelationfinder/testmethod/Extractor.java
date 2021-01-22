package org.jdepoix.dataset.testrelationfinder.testmethod;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Extractor {
    public Stream<MethodDeclaration> extractTestMethods(Path projectRootPath) {
        List<SourceRoot> sourceRoots = this.collectSourceRoots(projectRootPath);
        Stream<CompilationUnit> compilationUnitStream = this.parseCompilationUnits(sourceRoots);
        return this.extractTestMethodsFromCompilationUnits(compilationUnitStream);
    }

    private Stream<MethodDeclaration> extractTestMethodsFromCompilationUnits(
        Stream<CompilationUnit> compilationUnitStream
    ) {
       return compilationUnitStream
           .flatMap(
               compilationUnit -> compilationUnit
                   .findAll(MethodDeclaration.class)
                   .stream()
                   .filter(Extractor.isTestMethod())
           );
    }

    private Stream<CompilationUnit> parseCompilationUnits(List<SourceRoot> sourceRoots) {
        return sourceRoots
            .stream()
            .filter(sourceRoot -> sourceRoot.getRoot().toString().contains("test"))
            .map(SourceRoot::tryToParseParallelized)
            .flatMap(List::stream)
            .map(ParseResult::getResult)
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private List<SourceRoot> collectSourceRoots(Path projectRootPath) {
        ParserCollectionStrategy collectionStrategy = new ParserCollectionStrategy();
        ProjectRoot projectRoot = collectionStrategy.collect(projectRootPath);
        CombinedTypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
        projectRoot
            .getSourceRoots()
            .forEach(sourceRoot -> typeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot())));
        final ParserConfiguration parserConfiguration = collectionStrategy.getParserConfiguration();
        parserConfiguration.setSymbolResolver(new JavaSymbolSolver(typeSolver));
        parserConfiguration.setAttributeComments(false);
        parserConfiguration.setIgnoreAnnotationsWhenAttributingComments(true);
        return projectRoot.getSourceRoots();
    }

    static Predicate<MethodDeclaration> isTestMethod() {
        return methodDeclaration -> methodDeclaration
            .getAnnotations()
            .stream()
            .anyMatch(annotationExpr -> {
                String annotationName = annotationExpr
                    .getName()
                    .asString();
                String[] splittedName = annotationName
                    .split("\\.");
                return (
                    splittedName.length > 0
                        ? splittedName[splittedName.length - 1]
                        : annotationName
                ).equals("Test");
            });
    }
}

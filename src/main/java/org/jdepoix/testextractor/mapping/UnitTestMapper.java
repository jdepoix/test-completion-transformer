package org.jdepoix.testextractor.mapping;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.google.common.collect.Sets;
import org.jdepoix.testextractor.indexing.ClassIndexer;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class UnitTestMapper {
    private ClassIndexer classIndex;

    public UnitTestMapper(ClassIndexer classIndex) {
        this.classIndex = classIndex;
    }

    public void findMapping(String testClassContent) {

        ParserCollectionStrategy parserCollectionStrategy = new ParserCollectionStrategy();
        ProjectRoot projectRoot = parserCollectionStrategy.collect(Path.of("assets/staticcode/RxJava"));
        System.out.println(projectRoot.getSourceRoots());
        CombinedTypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
        projectRoot
            .getSourceRoots()
            .forEach(sourceRoot -> typeSolver.add(new JavaParserTypeSolver(sourceRoot.getRoot())));
        parserCollectionStrategy.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));

        // parse all test files
        List<CompilationUnit> compilationUnits = projectRoot
            .getSourceRoots()
            .stream()
            .filter(sourceRoot -> sourceRoot.getRoot().toString().contains("test"))
            .map(sourceRoot -> sourceRoot.tryToParseParallelized())
            .flatMap(List::stream)
            .map(parseResult -> ((ParseResult<CompilationUnit>) parseResult).getResult().get())
            .collect(Collectors.toList());

        // find tests
        List<MethodDeclaration> testMethods = compilationUnits
            .stream()
            .map(
                compilationUnit -> compilationUnit
                    .findAll(MethodDeclaration.class)
                    .stream()
                    .filter(
                        methodDeclaration ->
                            methodDeclaration
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
                                })
                    ).collect(Collectors.toList())
            )
            .flatMap(List::stream)
            .collect(Collectors.toList());

        System.out.println(testMethods.size());


//        MethodDeclaration testMethod = testMethods.get(100);
//        System.out.println(testMethod);

        int notFoundCounter = 0;
        int foundCounter = 0;
        int cantResolveCounter = 0;
        int identifiedByClassCounter = 0;
        int identifiedByMethodCounter = 0;

        for (MethodDeclaration testMethod : testMethods) {
            TestEntityNameTokenizer tokenizer = new TestEntityNameTokenizer();
            TestEntitiesNamingSimilaritiesRanker similarityRanker = new TestEntitiesNamingSimilaritiesRanker();

            Optional<RankingResult<MethodCallExpr>> highestRankedMethod = testMethod
                .findAll(MethodCallExpr.class)
                .stream()
                .map(
                    methodCallExpr ->
                        new RankingResult<>(
                            methodCallExpr,
                            similarityRanker.rank(
                                tokenizer.tokenize(testMethod.getNameAsString()),
                                tokenizer.tokenize(methodCallExpr.getNameAsString())
                            )
                        )
                )
                .filter(rankingResult -> rankingResult.getScore() > 0.05)
                .sorted(Comparator.reverseOrder())
                .findFirst();

            if (highestRankedMethod.isEmpty()) {
                highestRankedMethod = testMethod
                    .findAll(MethodCallExpr.class)
                    .stream()
                    .map(
                        methodCallExpr ->
                            new RankingResult<>(
                                methodCallExpr,
                                similarityRanker.rank(
                                    tokenizer.tokenize(testMethod.resolve().getClassName()),
                                    tokenizer.tokenize(methodCallExpr.getNameAsString())
                                )
                            )
                    )
                    .filter(rankingResult -> rankingResult.getScore() > 0.05)
                    .sorted(Comparator.reverseOrder())
                    .findFirst();

                if (highestRankedMethod.isPresent()) {
                    identifiedByClassCounter++;
                }
//
//                try {
//                    System.out.printf("%s.%s tests %s\n", testMethod.resolve().getClassName(), testMethod.getNameAsString(), highestRankedMethod.get().getEntity().resolve().getQualifiedName());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            } else {
                identifiedByMethodCounter++;
            }

            if (highestRankedMethod.isEmpty()) {
//                System.out.printf("no method found for %s\n", testMethod.getNameAsString());
                notFoundCounter++;
            } else {
                try {
//                    System.out.printf("%s tests %s\n", testMethod.getNameAsString(), highestRankedMethod.get().getEntity().resolve().getQualifiedName());
//                    System.out.println(highestRankedMethod.get().getEntity().findCompilationUnit().get().getStorage().get().getPath());
                    ResolvedMethodDeclaration resolved = highestRankedMethod.get().getEntity().resolve();
                    Path path = resolved.toAst().get().findCompilationUnit().get().getStorage().get().getPath();
//                    System.out.println(resolved.toAst().get().findCompilationUnit().get().getStorage().get().getPath());
//                    System.out.println(highestRankedMethod.get().getEntity().resolve().getQualifiedName());
//                    System.out.println(highestRankedMethod.get().getEntity().resolve().getQualifiedSignature());
                    foundCounter++;
                } catch (Exception e) {
//                    System.out.printf("cant't resolve tested method %s\n", highestRankedMethod.get().getEntity().getNameAsString());
                    cantResolveCounter++;
                }
            }
        }

        System.out.println("not found: " + notFoundCounter);
        System.out.println("found: " + foundCounter);
        System.out.println("unresolvable: " + cantResolveCounter);
        System.out.println("found by method: " + identifiedByMethodCounter);
        System.out.println("found by class: " + identifiedByClassCounter);
    }
}

class RankingResult<T> implements Comparable<RankingResult<T>> {
    private final T entity;
    private final double score;

    public RankingResult(T entity, double score) {
        this.entity = entity;
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public T getEntity() {
        return entity;
    }

    @Override
    public int compareTo(RankingResult<T> o) {
        return Double.compare(this.getScore(), o.getScore());
    }

    @Override
    public String toString() {
        return "RankingResult{" +
            "score=" + score +
            ", entity=" + entity +
            '}';
    }
}

class TestEntitiesNamingSimilaritiesRanker {
    double rank(
        String[] tokenizedTestEntityName,
        String[] tokenizedPotentiallyTestedMethodName
    ) {
        Set<String> tokenizedNameSet = new HashSet<>(Arrays.asList(tokenizedTestEntityName));
        if (tokenizedNameSet.size() == 0) {
            return 0;
        }
        return (double) Sets.intersection(
                tokenizedNameSet,
                new HashSet<>(Arrays.asList(tokenizedPotentiallyTestedMethodName))
            ).size()
            / (double) tokenizedNameSet.size();
    }
}

class TestEntityNameTokenizer {
    String[] tokenize(String name) {
        return Arrays
            .stream(name.split("(?=\\p{Lu})"))
            .map(String::toLowerCase)
            .filter(s -> !s.equals("test"))
            .toArray(String[]::new);
    }
}
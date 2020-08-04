package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Finder {
    private final TestEntitiesNamingSimilaritiesRanker similarityRanker;
    private final TestEntityNameTokenizer tokenizer;

    public Finder() {
        this.similarityRanker = new TestEntitiesNamingSimilaritiesRanker();
        this.tokenizer = new TestEntityNameTokenizer();
    }

    public List<TestRelation> findTestRelations(Stream<MethodDeclaration> testMethods) {
        return testMethods
            .map(this::findTestRelation)
            .collect(Collectors.toList());
    }

    private TestRelation findTestRelation(MethodDeclaration testMethod) {
        return Optional.ofNullable(
            this
                .findHighestRankingMethod(testMethod, () -> testMethod.getNameAsString())
                .orElseGet(
                    () -> this
                        .findHighestRankingMethod(testMethod, () -> testMethod.resolve().getClassName())
                        .orElse(null)
                )
        )
        .map(rankingResult -> TestRelation.resolveFromNodes(testMethod, Optional.of(rankingResult.getEntity())))
        .orElseGet(() -> TestRelation.resolveFromNodes(testMethod, Optional.empty()));
    }

    private Optional<RankingResult<MethodCallExpr>> findHighestRankingMethod(
        MethodDeclaration testMethod,
        Supplier<String> testEntityNameSupplier
    ) {
        return testMethod
            .findAll(MethodCallExpr.class)
            .stream()
            .map(
                methodCallExpr ->
                    new RankingResult<>(
                        methodCallExpr,
                        this.similarityRanker.rank(
                            this.tokenizer.tokenize(testEntityNameSupplier.get()),
                            this.tokenizer.tokenize(methodCallExpr.getNameAsString())
                        )
                    )
            )
            .filter(rankingResult -> rankingResult.getScore() > 0.05)
            .sorted(Comparator.reverseOrder())
            .findFirst();
    }
}

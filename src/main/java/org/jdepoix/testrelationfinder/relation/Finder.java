package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Finder {
    private final TestEntitiesNamingSimilaritiesRanker similarityRanker;
    private final TestEntityNameTokenizer tokenizer;

    public Finder() {
        this.similarityRanker = new TestEntitiesNamingSimilaritiesRanker();
        this.tokenizer = new TestEntityNameTokenizer();
    }

    public Stream<TestRelation> findTestRelations(Stream<MethodDeclaration> testMethods) {
        return testMethods.map(this::findTestRelation);
    }

    private TestRelation findTestRelation(MethodDeclaration testMethod) {
        return this
            .findHighestRankingMethod(testMethod, () -> testMethod.getNameAsString())
            .map(
                rankingResult ->
                    new TestRelation(
                        testMethod,
                        rankingResult.getEntity(),
                        TestRelation.Type.MAPPED_BY_TEST_METHOD_NAME
                    )
            )
            .orElseGet(
                () -> this
                    .findHighestRankingMethod(testMethod, () -> testMethod.resolve().getClassName())
                    .map(
                        rankingResult ->
                            new TestRelation(
                                testMethod,
                                rankingResult.getEntity(),
                                TestRelation.Type.MAPPED_BY_TEST_CLASS_NAME
                            )
                    )
                    .orElseGet(() -> new TestRelation(testMethod))
            );
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

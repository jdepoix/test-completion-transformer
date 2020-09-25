package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
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
        final List<MethodCallExpr> methodCalls = this.getDistinctMethodCalls(testMethod.findAll(MethodCallExpr.class));
        final TestRelation.Builder testRelationBuilder = new TestRelation.Builder().setTestMethod(testMethod);

        final ResolvedMethodDeclaration resolvedTestMethod = testMethod.resolve();
        this.findHighestRankingMethods(
            testRelationBuilder, methodCalls, testMethod.getNameAsString(), resolvedTestMethod.getPackageName()
        );
        if (testRelationBuilder.getType().isEmpty()) {
            return testRelationBuilder.setType(TestRelation.Type.MAPPED_BY_TEST_METHOD_NAME).build();
        }

        this.findHighestRankingMethods(
            testRelationBuilder, methodCalls, resolvedTestMethod.getClassName(), resolvedTestMethod.getPackageName()
        );
        if (testRelationBuilder.getType().isEmpty()) {
            testRelationBuilder.setType(TestRelation.Type.MAPPED_BY_TEST_CLASS_NAME);
        }
        return testRelationBuilder.build();
    }

    private List<MethodCallExpr> getDistinctMethodCalls(List<MethodCallExpr> methodCalls) {
        final Set<String> uniqueMethodNames = new HashSet<>();
        return methodCalls
            .stream()
            .filter(methodCallExpr -> uniqueMethodNames.add(methodCallExpr.getNameAsString()))
            .collect(Collectors.toList());
    }

    private void findHighestRankingMethods(
        TestRelation.Builder testRelationBuilder,
        List<MethodCallExpr> methodCalls,
        String testEntityName,
        String testPackage
    ) {
        final List<RankingResult<ResolvedMethodDeclaration>> rankingResults = resolveRelevantRankingResults(
            createRankingResults(methodCalls, testEntityName), testPackage
        );
        TestRelation.Type newType = null;

        if (rankingResults.isEmpty()) {
            if (testRelationBuilder.getType().orElse(null) == TestRelation.Type.AMBIGUOUS_RELATIONS) {
                newType = TestRelation.Type.AMBIGUOUS_RELATIONS;
            } else {
                newType = TestRelation.Type.NO_RELATION_FOUND;
            }
        } else if (rankingResults.size() >= 2 && rankingResults.get(0).getScore() == rankingResults.get(1).getScore()) {
            newType = TestRelation.Type.AMBIGUOUS_RELATIONS;
        }

        if (newType == null) {
            testRelationBuilder.setRelatedMethod(rankingResults.get(0).getEntity());
        }
        testRelationBuilder.setType(newType);
    }

    private List<RankingResult<MethodCallExpr>> createRankingResults(
        List<MethodCallExpr> methodCalls,
        String testEntityName
    ) {
        return methodCalls
            .stream()
            .map(
                methodCallExpr ->
                    new RankingResult<>(
                        methodCallExpr,
                        this.similarityRanker.rank(
                            this.tokenizer.tokenize(testEntityName),
                            this.tokenizer.tokenize(methodCallExpr.getNameAsString())
                        )
                    )
            )
            .filter(rankingResult -> rankingResult.getScore() > 0.1)
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());
    }

    private List<RankingResult<ResolvedMethodDeclaration>> resolveRelevantRankingResults(
        List<RankingResult<MethodCallExpr>> rankingResults, String testPackage
    ) {
        List<RankingResult<ResolvedMethodDeclaration>> relevantRankingResults = new ArrayList<>();

        for (RankingResult<MethodCallExpr> rankingResult : rankingResults) {
            ResolvedMethodDeclaration resolvedRelatedMethod = null;
            try {
                resolvedRelatedMethod = rankingResult.getEntity().resolve();
            } catch (Exception e) {}

            if (
                resolvedRelatedMethod != null
                && !(resolvedRelatedMethod instanceof ReflectionMethodDeclaration)
                && !resolvedRelatedMethod.getPackageName().equals(testPackage)
            ) {
                relevantRankingResults.add(new RankingResult<>(resolvedRelatedMethod, rankingResult.getScore()));
                if (relevantRankingResults.size() >= 2) {
                    return relevantRankingResults;
                }
            }
        }

        return relevantRankingResults;
    }
}

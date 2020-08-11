package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        this.findHighestRankingMethods(testRelationBuilder, methodCalls, testMethod.getNameAsString());
        if (testRelationBuilder.getType().isEmpty()) {
            return testRelationBuilder.setType(TestRelation.Type.MAPPED_BY_TEST_METHOD_NAME).build();
        }

        this.findHighestRankingMethods(testRelationBuilder, methodCalls, testMethod.resolve().getClassName());
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
        String testEntityName
    ) {
        final List<RankingResult<MethodCallExpr>> methodRanking = methodCalls
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
            .filter(rankingResult -> rankingResult.getScore() > 0.05)
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());

        TestRelation.Type newType = null;
        if (methodRanking.isEmpty()) {
            if (testRelationBuilder.getType().orElse(null) == TestRelation.Type.AMBIGUOUS_RELATIONS) {
                newType = TestRelation.Type.AMBIGUOUS_RELATIONS;
            } else {
                newType = TestRelation.Type.NO_RELATION_FOUND;
            }
        } else if (methodRanking.size() >= 2 && methodRanking.get(0).getScore() == methodRanking.get(1).getScore()) {
            newType = TestRelation.Type.AMBIGUOUS_RELATIONS;
        }
        if (newType == null) {
            testRelationBuilder.setRelatedMethod(methodRanking.get(0).getEntity());
        }
        testRelationBuilder.setType(newType);
    }
}

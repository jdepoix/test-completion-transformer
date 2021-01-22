package org.jdepoix.dataset.testrelationfinder.relation;

import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

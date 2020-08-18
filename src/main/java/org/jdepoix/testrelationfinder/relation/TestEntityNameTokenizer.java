package org.jdepoix.testrelationfinder.relation;

import java.util.Arrays;
import java.util.regex.Pattern;

class TestEntityNameTokenizer {
    private static final Pattern MATCH_LOWERCASE = Pattern.compile("[^A-Z]+");
    private static final Pattern MATCH_SURROUNDED_UNDERSCORES = Pattern.compile(".+_.+");

    String[] tokenize(String name) {
        return Arrays
            .stream(this.splitMethodName(name))
            .map(String::toLowerCase)
            .filter(s -> !(s.equals("") || s.equals("test")))
            .toArray(String[]::new);
    }

    private String[] splitMethodName(String name) {
        if (name.length() == 0) {
            return new String[]{};
        }

        if (MATCH_LOWERCASE.matcher(name).matches() && MATCH_SURROUNDED_UNDERSCORES.matcher(name).matches()) {
            return name.split("_");
        }
        return name.split("(?=\\p{Lu})");
    }
}

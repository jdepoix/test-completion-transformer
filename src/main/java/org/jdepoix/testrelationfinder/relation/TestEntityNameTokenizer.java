package org.jdepoix.testrelationfinder.relation;

import java.util.Arrays;

class TestEntityNameTokenizer {
    String[] tokenize(String name) {
        return Arrays
            .stream(name.split("(?=\\p{Lu})"))
            .map(String::toLowerCase)
            .filter(s -> !s.equals("test"))
            .toArray(String[]::new);
    }
}

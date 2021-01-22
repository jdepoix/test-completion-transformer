package org.jdepoix.dataset.testrelationfinder.relation;

import java.util.ArrayList;
import java.util.List;

class TestEntityNameTokenizer {
    String[] tokenize(String name) {
        return this.splitMethodName(name)
            .stream()
            .map(String::toLowerCase)
            .filter(s -> !(s.equals("") || s.equals("test")))
            .toArray(String[]::new);
    }

    private List<String> splitMethodName(String name) {
        final ArrayList<String> splittedNames = new ArrayList<>();
        String currentName = "";

        for (char currentChar : name.toCharArray()) {
            if (currentChar == '_') {
                if (currentName.length() > 0) {
                    splittedNames.add(currentName);
                    currentName = "";
                }
                continue;
            }

            if (
                Character.isUpperCase(currentChar)
                && currentName.length() > 0
                && Character.isLowerCase(currentName.charAt(currentName.length() - 1))
            ) {
                splittedNames.add(currentName);
                currentName = "";
            }

            currentName += currentChar;
        }
        if (currentName.length() > 0) {
            splittedNames.add(currentName);
        }

        return splittedNames;
    }
}

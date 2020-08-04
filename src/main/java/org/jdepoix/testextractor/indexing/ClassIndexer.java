package org.jdepoix.testextractor.indexing;

import java.nio.file.Path;

public class ClassIndexer extends Indexer {
    private static final String CLASS_KEYWORD = "class ";

    @Override
    public void index(Path path, String fileContent) throws AmbiguousEntryKey, AmbiguousPackageName {
        String packageName = this.getPackageName(fileContent);

        int classKeywordStartIndex = fileContent.indexOf(CLASS_KEYWORD);
        while (classKeywordStartIndex >= 0) {
            if (this.checkKeywordOccurrence(fileContent, classKeywordStartIndex)) {
                this.addIndexEntry(
                    packageName,
                    extractClassName(fileContent, classKeywordStartIndex),
                    new IndexEntry(path, new LazyLoadedFileContent(path))
                );
            }
            classKeywordStartIndex = fileContent.indexOf(CLASS_KEYWORD, classKeywordStartIndex + 1);
        }
        // TODO start handling multiple package.class identifiers (can't identify inner classes)

        // TODO remove debug stuff
//        this.indexedEntries.forEach((s, indexEntry) -> System.out.println("\"" + s + "\" " + indexEntry.getPath()));
    }

    private String extractClassName(String fileContent, int classKeywordStartIndex) {
        int classKeywordEndIndex = classKeywordStartIndex + CLASS_KEYWORD.length();
        int classNameEndIndex = fileContent.indexOf(" ", classKeywordEndIndex);
        String className = fileContent.substring(classKeywordEndIndex, classNameEndIndex);
        int genericTypeStartIndex = className.indexOf('<');
        if (genericTypeStartIndex != -1) {
            className = className.substring(0, genericTypeStartIndex);
        }
        return className;
    }

    private boolean checkKeywordOccurrence(String fileContent, int index) {
        // file starts with class
        if (index == 0) {
            return true;
        }

        // new line start with class
        char previousChar = fileContent.charAt(index - 1);
        if (previousChar == '\n') {
            return true;
        }

        // check if code before class keyword is commented out
        String codeBeforeClassKeyword = fileContent.substring(0, index);
        int lineBeforeClassKeywordIndex = codeBeforeClassKeyword.lastIndexOf("\n");
        String lineBeforeClassKeyword = lineBeforeClassKeywordIndex != -1
            ? codeBeforeClassKeyword.substring(lineBeforeClassKeywordIndex + 1) : codeBeforeClassKeyword;

        return !lineBeforeClassKeyword.contains("\"")
            && !lineBeforeClassKeyword.contains("*")
            && !lineBeforeClassKeyword.contains("private")
            && !lineBeforeClassKeyword.contains("//")
            && previousChar == ' ';
    }
}

package org.jdepoix.testextractor.indexing;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class AmbiguousPackageName extends Exception {
    public AmbiguousPackageName(String fileContent) {
        super(fileContent);
    }
}

class AmbiguousEntryKey extends Exception {
    public AmbiguousEntryKey(IndexKey key) {
        super(key.toString());
    }
}

public abstract class Indexer {
    private static final String PACKAGE_KEYWORD = "package ";

    private final Map<IndexKey, IndexEntry> indexedEntries;

    public Indexer() {
        this.indexedEntries = new HashMap<>();
    }

    public abstract void index(Path path, String fileContent) throws AmbiguousEntryKey, AmbiguousPackageName;

    public IndexEntry lookup(IndexKey key) {
        return this.indexedEntries.get(key);
    }

    void addIndexEntry(String packageName, String name, IndexEntry entry) throws AmbiguousEntryKey {
        IndexKey key = new IndexKey(packageName, name);

        if (this.indexedEntries.get(key) != null) {
            System.out.println(this.indexedEntries.get(key).getPath());
            System.out.println(entry.getPath());
            throw new AmbiguousEntryKey(key);
        }

        this.indexedEntries.put(key, entry);
    }

    String getPackageName(String fileContent) throws AmbiguousPackageName {
        int packageKeywordStartIndex = fileContent.indexOf(PACKAGE_KEYWORD);
        if (packageKeywordStartIndex == -1) {
            throw new AmbiguousPackageName(fileContent);
        }

        int packageKeywordEndIndex = packageKeywordStartIndex + PACKAGE_KEYWORD.length();
        return fileContent.substring(packageKeywordEndIndex, fileContent.indexOf(";", packageKeywordEndIndex));
    }
}

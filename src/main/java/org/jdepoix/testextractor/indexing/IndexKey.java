package org.jdepoix.testextractor.indexing;

import java.util.Objects;

public class IndexKey {
    private final String packageName;
    private final String name;

    public IndexKey(String packageName, String name) {
        this.packageName = packageName;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexKey indexKey = (IndexKey) o;
        return Objects.equals(packageName, indexKey.packageName) &&
                Objects.equals(name, indexKey.name);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s.%s", this.packageName, this.name);
    }
}

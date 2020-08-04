package org.jdepoix.testextractor.indexing;

import java.nio.file.Path;

public class IndexEntry {
    private final Path path;
    private final FileContent fileContent;

    public IndexEntry(Path path, FileContent fileContent) {
        this.path = path;
        this.fileContent = fileContent;
    }

    public Path getPath() {
        return path;
    }

    public FileContent getFileContent() {
        return fileContent;
    }
}

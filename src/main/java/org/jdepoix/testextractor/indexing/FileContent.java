package org.jdepoix.testextractor.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface FileContent {
    String get() throws IOException;
}

class LazyLoadedFileContent implements FileContent {
    private final Path path;

    public LazyLoadedFileContent(Path path) {
        this.path = path;
    }

    @Override
    public String get() throws IOException {
        return Files.readString(this.path);
    }
}

class CachedFileContent implements FileContent {
    private final String fileContent;

    public CachedFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    @Override
    public String get() throws IOException {
        return this.fileContent;
    }
}

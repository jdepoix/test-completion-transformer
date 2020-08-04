package org.jdepoix.testextractor.indexing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JavaFileIndexCreator {
    private final Path rootDirectory;

    public JavaFileIndexCreator(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public List<Indexer> createIndices(Indexer... indexers) throws IOException {
        Files
            .walk(this.rootDirectory)
            .filter(path -> path.toString().endsWith(".java"))
            .forEach(path -> this.createIndicesForFile(path, indexers));

        return List.of(indexers);
    }

    private void createIndicesForFile(Path path, Indexer[] indexers) {
        // TODO consider optimizing this
        String fileContent = null;
        try {
            fileContent = Files.readString(path);

            for (Indexer indexer : indexers) {
                indexer.index(path, fileContent);
            }
        } catch (Exception e) {
            // TODO improve error handling
            e.printStackTrace();
            System.exit(1);
        }
    }
}

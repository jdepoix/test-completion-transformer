package org.jdepoix.config;

import java.nio.file.Path;

public class ResultDirConfig {
    private final Path resultDir;
    private final Path tempDir;
    private final Path repoDir;
    private final Path datasetDir;
    private final Path dbFile;

    public ResultDirConfig(Path resultDir) {
        this.resultDir = resultDir;
        this.tempDir = this.resultDir.resolve("temp");
        this.repoDir = this.resultDir.resolve("repos");
        this.datasetDir = this.resultDir.resolve("datasets");
        this.dbFile = this.resultDir.resolve("test_relations_index.sqlite");
    }

    public Path getResultDir() {
        return resultDir;
    }

    public Path getTempDir() {
        return tempDir;
    }

    public Path getRepoDir() {
        return repoDir;
    }

    public Path getDatasetDir() {
        return datasetDir;
    }

    public Path getDbFile() {
        return dbFile;
    }

    public Path resolveRepoFile(String repoName, Path filePath) {
        return this.repoDir.resolve(repoName).resolve(filePath);
    }
}

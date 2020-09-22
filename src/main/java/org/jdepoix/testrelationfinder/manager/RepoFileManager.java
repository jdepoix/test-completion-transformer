package org.jdepoix.testrelationfinder.manager;

import org.jdepoix.config.ResultDirConfig;
import org.jdepoix.testrelationfinder.reporting.TestRelationContextReportEntry;
import org.jdepoix.testrelationfinder.reporting.TestRelationReportEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RepoFileManager {
    private final Path basePath;

    public RepoFileManager(ResultDirConfig config) {
        this.basePath = config.getRepoDir();
    }

    public void saveFiles(String repoName, Path repoBasePath, TestRelationReportEntry testRelationReportEntry) {
        testRelationReportEntry.getRelatedMethodPath().ifPresent(relatedMethodPath -> {
            try {
                this.copyFile(repoName, repoBasePath, testRelationReportEntry.getTestPath());
                this.copyFile(repoName, repoBasePath, relatedMethodPath);
                for (TestRelationContextReportEntry contextReportEntry : testRelationReportEntry.getContext()) {
                    if (contextReportEntry.getPath().isPresent()) {
                        this.copyFile(repoName, repoBasePath, contextReportEntry.getPath().get());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void copyFile(String repoName, Path repoBasePath, Path filePath) throws IOException {
        final Path fileCopyPath = this.basePath.resolve(repoName).resolve(filePath);
        if (!Files.exists(fileCopyPath)) {
            Files.createDirectories(fileCopyPath.getParent());
            Files.copy(repoBasePath.resolve(filePath), fileCopyPath);
        }
    }
}

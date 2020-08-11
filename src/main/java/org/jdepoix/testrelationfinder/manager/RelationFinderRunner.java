package org.jdepoix.testrelationfinder.manager;

import org.jdepoix.testrelationfinder.archive.ArchiveHandler;
import org.jdepoix.testrelationfinder.relation.Finder;
import org.jdepoix.testrelationfinder.relation.GivenWhenThenResolver;
import org.jdepoix.testrelationfinder.relation.ResolvedTestRelation;
import org.jdepoix.testrelationfinder.relation.TestRelationResolver;
import org.jdepoix.testrelationfinder.reporting.SQLiteReporter;
import org.jdepoix.testrelationfinder.testmethod.Extractor;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RelationFinderRunner {
    private final Extractor testExtractor;
    private final Finder relationFinder;
    private final ArchiveHandler archiveHandler;
    private final GivenWhenThenResolver givenWhenThenResolver;
    private final TestRelationResolver testRelationResolver;
    private final RepoFileManager fileManager;
    private final SQLiteReporter reporter;

    public RelationFinderRunner(
        Extractor testExtractor,
        Finder relationFinder,
        ArchiveHandler archiveHandler,
        GivenWhenThenResolver givenWhenThenResolver,
        TestRelationResolver testRelationResolver,
        RepoFileManager fileManager,
        SQLiteReporter reporter
    ) {
        this.testExtractor = testExtractor;
        this.relationFinder = relationFinder;
        this.archiveHandler = archiveHandler;
        this.givenWhenThenResolver = givenWhenThenResolver;
        this.testRelationResolver = testRelationResolver;
        this.fileManager = fileManager;
        this.reporter = reporter;
    }

    public void run(Path repoBasePath) throws IOException, SQLException {
        final Path unpackedArchivePath = this.archiveHandler.unpackArchive(repoBasePath);
        try {
            this.runRelationDetection(this.getRepoName(repoBasePath), unpackedArchivePath);
        } finally {
            this.archiveHandler.deleteTempUnpackedArchive(unpackedArchivePath);
        }
    }

    private String getRepoName(Path repoBasePath) {
        return Arrays
            .stream(repoBasePath.getFileName().toString().split("\\.tar\\.gz")[0].split("#"))
            .collect(Collectors.joining("/"));
    }

    private void runRelationDetection(String repoName, Path path) throws SQLException {
        System.out.println("start " + repoName);
        final List<ResolvedTestRelation> resolvedTestRelations = this.relationFinder
            .findTestRelations(this.testExtractor.extractTestMethods(path))
            .map(testRelation -> this.givenWhenThenResolver.resolve(testRelation))
            .map(testRelation -> this.testRelationResolver.resolve(repoName, path, testRelation))
            .peek(testRelation -> this.fileManager.saveFiles(repoName, path, testRelation))
            .collect(Collectors.toList());

        System.out.println(repoName + ": " + resolvedTestRelations.size());
        System.out.println("finished start reporting: " + Instant.now());
        this.reporter.reportResults(repoName, resolvedTestRelations);
    }
}

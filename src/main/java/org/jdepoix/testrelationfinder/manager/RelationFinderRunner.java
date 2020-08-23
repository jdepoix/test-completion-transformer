package org.jdepoix.testrelationfinder.manager;

import org.jdepoix.testrelationfinder.archive.ArchiveHandler;
import org.jdepoix.testrelationfinder.gwt.GWTContextResolver;
import org.jdepoix.testrelationfinder.gwt.GWTSectionResolver;
import org.jdepoix.testrelationfinder.relation.Finder;
import org.jdepoix.testrelationfinder.relation.TestRelationResolver;
import org.jdepoix.testrelationfinder.reporting.ReportCreator;
import org.jdepoix.testrelationfinder.reporting.SQLiteReportStore;
import org.jdepoix.testrelationfinder.reporting.TestRelationReportEntry;
import org.jdepoix.testrelationfinder.testmethod.Extractor;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RelationFinderRunner {
    private final Extractor testExtractor;
    private final Finder relationFinder;
    private final ArchiveHandler archiveHandler;
    private final TestRelationResolver testRelationResolver;
    private final GWTSectionResolver gwtSectionResolver;
    private final GWTContextResolver gwtContextResolver;
    private final ReportCreator reportCreator;
    private final RepoFileManager fileManager;
    private final SQLiteReportStore reportStore;

    public RelationFinderRunner(
        Extractor testExtractor,
        Finder relationFinder,
        ArchiveHandler archiveHandler,
        TestRelationResolver testRelationResolver,
        GWTSectionResolver gwtSectionResolver,
        GWTContextResolver gwtContextResolver,
        ReportCreator reportCreator,
        RepoFileManager fileManager,
        SQLiteReportStore reportStore
    ) {
        this.testExtractor = testExtractor;
        this.relationFinder = relationFinder;
        this.archiveHandler = archiveHandler;
        this.testRelationResolver = testRelationResolver;
        this.gwtSectionResolver = gwtSectionResolver;
        this.gwtContextResolver = gwtContextResolver;
        this.reportCreator = reportCreator;
        this.fileManager = fileManager;
        this.reportStore = reportStore;
    }

    public void run(Path repoBasePath) throws Exception {
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

    private void runRelationDetection(String repoName, Path path) throws Exception {
        final List<TestRelationReportEntry> resolvedTestRelations = this.relationFinder
            .findTestRelations(this.testExtractor.extractTestMethods(path))
            .map(this.testRelationResolver::resolve)
            .map(this.gwtSectionResolver::resolve)
            .map(this.gwtContextResolver::resolve)
            .map(testRelation -> this.reportCreator.createReportEntry(repoName, path, testRelation))
            .peek(testRelation -> this.fileManager.saveFiles(repoName, path, testRelation))
            .collect(Collectors.toList());
        this.reportStore.storeReport(resolvedTestRelations);
    }
}

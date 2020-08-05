package org.jdepoix.testrelationfinder.manager;

import org.jdepoix.testrelationfinder.archive.ArchiveHandler;
import org.jdepoix.testrelationfinder.relation.Finder;
import org.jdepoix.testrelationfinder.relation.ResolvedTestRelation;
import org.jdepoix.testrelationfinder.relation.TestRelationResolver;
import org.jdepoix.testrelationfinder.testmethod.Extractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// TODO make non public when used by manager
public class RelationFinderRunner {
    private final Extractor testExtractor;
    private final Finder relationFinder;
    private final ArchiveHandler archiveHandler;

    public RelationFinderRunner(Extractor testExtractor, Finder relationFinder, ArchiveHandler archiveHandler) {
        this.testExtractor = testExtractor;
        this.relationFinder = relationFinder;
        this.archiveHandler = archiveHandler;
    }

    public void run(Path repoBasePath) throws IOException {
        this.archiveHandler.runOnArchiveContent(
            repoBasePath,
            path -> this.runRelationDetection(this.getRepoName(repoBasePath), path)
        );
    }

    private String getRepoName(Path repoBasePath) {
        return Arrays
            .stream(
                repoBasePath.getFileName().toString().split("-master\\.tar\\.gz")[0].split("#")
            )
            .collect(Collectors.joining("/"));
    }

    private void runRelationDetection(String repoName, Path path) {
        // TODO think about error handling
        final TestRelationResolver testRelationResolver = new TestRelationResolver();
        final List<ResolvedTestRelation> collect = this.relationFinder
            .findTestRelations(this.testExtractor.extractTestMethods(path))
            .map(testRelation -> testRelationResolver.resolve(repoName, path, testRelation))
            .collect(Collectors.toList());
        System.out.println(collect.size());
    }
}

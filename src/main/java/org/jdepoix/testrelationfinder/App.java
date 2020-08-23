package org.jdepoix.testrelationfinder;

import org.jdepoix.testrelationfinder.archive.ArchiveHandler;
import org.jdepoix.testrelationfinder.gwt.GWTContextResolver;
import org.jdepoix.testrelationfinder.gwt.GWTSectionResolver;
import org.jdepoix.testrelationfinder.manager.RelationFinderRunner;
import org.jdepoix.testrelationfinder.manager.RepoFileManager;
import org.jdepoix.testrelationfinder.relation.Finder;
import org.jdepoix.testrelationfinder.relation.TestRelationResolver;
import org.jdepoix.testrelationfinder.reporting.ReportCreator;
import org.jdepoix.testrelationfinder.reporting.SQLiteReportStore;
import org.jdepoix.testrelationfinder.testmethod.Extractor;
import org.jdepoix.testrelationfinder.sqlite.ConnectionHandler;

import java.nio.file.Path;

public class App {
    public static void main(String[] args) throws Exception {
        final Path workingDir = Path.of(args[1]);
        new RelationFinderRunner(
            new Extractor(),
            new Finder(),
            new ArchiveHandler(workingDir.resolve("temp")),
            new TestRelationResolver(),
            new GWTSectionResolver(),
            new GWTContextResolver(),
            new ReportCreator(),
            new RepoFileManager(workingDir.resolve("repos")),
            new SQLiteReportStore(new ConnectionHandler(workingDir.resolve("test_relations_index.sqlite")))
        ).run(Path.of(args[0]));
    }
}

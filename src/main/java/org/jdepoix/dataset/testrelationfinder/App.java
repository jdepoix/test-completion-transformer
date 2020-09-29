package org.jdepoix.dataset.testrelationfinder;

import org.jdepoix.dataset.config.ResultDirConfig;
import org.jdepoix.dataset.testrelationfinder.archive.ArchiveHandler;
import org.jdepoix.dataset.testrelationfinder.gwt.GWTContextResolver;
import org.jdepoix.dataset.testrelationfinder.gwt.GWTSectionResolver;
import org.jdepoix.dataset.testrelationfinder.manager.RelationFinderRunner;
import org.jdepoix.dataset.testrelationfinder.manager.RepoFileManager;
import org.jdepoix.dataset.testrelationfinder.relation.Finder;
import org.jdepoix.dataset.testrelationfinder.reporting.ReportCreator;
import org.jdepoix.dataset.testrelationfinder.reporting.SQLiteReportStore;
import org.jdepoix.dataset.testrelationfinder.testmethod.Extractor;
import org.jdepoix.dataset.sqlite.ConnectionHandler;

import java.nio.file.Path;

public class App {
    public static void main(String[] args) throws Exception {
        final ResultDirConfig config = new ResultDirConfig(Path.of(args[1]));
        new RelationFinderRunner(
            new Extractor(),
            new Finder(),
            new ArchiveHandler(config),
            new GWTSectionResolver(),
            new GWTContextResolver(),
            new ReportCreator(),
            new RepoFileManager(config),
            new SQLiteReportStore(new ConnectionHandler(config.getDbFile()))
        ).run(Path.of(args[0]));
    }
}

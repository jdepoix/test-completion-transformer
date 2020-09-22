package org.jdepoix.datasetcreator.gwt;

import org.jdepoix.config.ResultDirConfig;
import org.jdepoix.sqlite.ConnectionHandler;
import org.jdepoix.testrelationfinder.reporting.TestRelationReportEntry;

import java.nio.file.Path;
import java.util.List;

public class App {
    public static void main(String[] args) {
        try {
            final ResultDirConfig config = new ResultDirConfig(Path.of(args[0]));
            final ReportRetriever reportRetriever = new ReportRetriever(new ConnectionHandler(config.getDbFile()));
            final ASTResolver astResolver = new ASTResolver(config);
            List<TestRelationReportEntry> testRelationReportEntries = null;
            testRelationReportEntries = reportRetriever.retrieve();
            for (TestRelationReportEntry testRelationReportEntry : testRelationReportEntries) {
                astResolver.resolve(testRelationReportEntry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

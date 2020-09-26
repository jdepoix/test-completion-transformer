package org.jdepoix.datasetcreator.gwt;

import org.jdepoix.ast.serialization.ASTSerializer;
import org.jdepoix.config.ResultDirConfig;
import org.jdepoix.sqlite.ConnectionHandler;
import org.jdepoix.testrelationfinder.reporting.TestRelationReportEntry;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class App {
    public static void main(String[] args) {
        final Instant start = Instant.now();
        try {
            final ResultDirConfig config = new ResultDirConfig(Path.of(args[0]));
            final ReportRetriever reportRetriever = new ReportRetriever(new ConnectionHandler(config.getDbFile()));
            final DatapointResolver datapointResolver = new DatapointResolver(config, new ASTSerializer());
            List<TestRelationReportEntry> testRelationReportEntries = reportRetriever.retrieve();
            for (TestRelationReportEntry testRelationReportEntry : testRelationReportEntries) {
                datapointResolver.resolve(testRelationReportEntry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(Duration.between(start, Instant.now()));
    }

}

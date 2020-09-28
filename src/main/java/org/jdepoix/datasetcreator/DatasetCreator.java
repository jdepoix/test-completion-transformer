package org.jdepoix.datasetcreator;

import org.jdepoix.config.ResultDirConfig;
import org.jdepoix.testrelationfinder.reporting.TestRelationReportEntry;

import java.util.List;
import java.util.logging.Logger;

public class DatasetCreator {
    private final ResultDirConfig config;
    private final ReportRetriever reportRetriever;
    private final DatapointResolver datapointResolver;
    private final DatasetStore datasetStore;
    private final Logger logger;

    public DatasetCreator(
        ResultDirConfig config,
        ReportRetriever reportRetriever,
        DatapointResolver datapointResolver,
        DatasetStore datasetStore,
        Logger logger
    ) {
        this.config = config;
        this.reportRetriever = reportRetriever;
        this.datapointResolver = datapointResolver;
        this.datasetStore = datasetStore;
        this.logger = logger;
    }

    public void create() throws Exception {
        List<TestRelationReportEntry> testRelationReportEntries = reportRetriever.retrieve();
        try (final StoreWriter storeWriter = this.datasetStore.getStoreWriter()) {
            for (TestRelationReportEntry testRelationReportEntry : testRelationReportEntries) {
                try {
                    storeWriter.store(datapointResolver.resolve(testRelationReportEntry));
                    this.logger.info(testRelationReportEntry.getId());
                } catch (Exception e) {
                    this.logger.warning(
                        String.format("%s failed with %s", testRelationReportEntry.getId(), e.toString())
                    );
                }
            }
        }
    }
}

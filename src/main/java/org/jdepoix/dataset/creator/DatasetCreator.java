package org.jdepoix.dataset.creator;

import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import org.jdepoix.dataset.config.ResultDirConfig;
import org.jdepoix.dataset.testrelationfinder.reporting.TestRelationReportEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DatasetCreator <T extends Datapoint> {
    private final ResultDirConfig config;
    private final ReportRetriever reportRetriever;
    private final DatapointResolver<T> datapointResolver;
    private final List<DatasetStore<T>> datasetStores;
    private final Logger logger;

    public DatasetCreator(
        ResultDirConfig config,
        ReportRetriever reportRetriever,
        DatapointResolver<T> datapointResolver,
        List<DatasetStore<T>> datasetStores,
        Logger logger
    ) {
        this.config = config;
        this.reportRetriever = reportRetriever;
        this.datapointResolver = datapointResolver;
        this.datasetStores = datasetStores;
        this.logger = logger;
    }

    public void create() throws Exception {
        List<TestRelationReportEntry> testRelationReportEntries = reportRetriever.retrieve();
        List<StoreWriter<T>> storeWriters = new ArrayList<>();
        try {
            for (DatasetStore<T> datasetStore : this.datasetStores) {
                storeWriters.add(datasetStore.getStoreWriter());
            }
            for (TestRelationReportEntry testRelationReportEntry : testRelationReportEntries) {
                try {
                    for (StoreWriter<T> storeWriter : storeWriters) {
                        storeWriter.store(datapointResolver.resolve(testRelationReportEntry));
                    }
                    this.logger.info(testRelationReportEntry.getId());
                } catch (Exception | Error e) {
                    JavaParserFacade.clearInstances();
                    System.gc();
                    this.logger.warning(
                        String.format("%s failed with %s", testRelationReportEntry.getId(), e.toString())
                    );
                }
            }
        } finally {
            for (StoreWriter<T> storeWriter : storeWriters) {
                storeWriter.close();
            }
        }
    }
}

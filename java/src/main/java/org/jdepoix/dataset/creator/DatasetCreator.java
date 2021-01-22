package org.jdepoix.dataset.creator;

import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import org.jdepoix.dataset.testrelationfinder.reporting.TestRelationReportEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DatasetCreator <T extends Datapoint> {
    private final ReportRetriever reportRetriever;
    private final DatapointCreator<T> datapointCreator;
    private final List<DatasetStore<T>> datasetStores;
    private final Logger logger;

    public DatasetCreator(
        ReportRetriever reportRetriever,
        DatapointCreator<T> datapointCreator,
        List<DatasetStore<T>> datasetStores,
        Logger logger
    ) {
        this.reportRetriever = reportRetriever;
        this.datapointCreator = datapointCreator;
        this.datasetStores = datasetStores;
        this.logger = logger;
    }

    public void create() throws Exception {
        final List<TestRelationReportEntry> testRelationReportEntries = reportRetriever.retrieve();
        final int entriesSize = testRelationReportEntries.size();
        int completedCounter = 0;
        List<StoreWriter<T>> storeWriters = new ArrayList<>();
        try {
            for (DatasetStore<T> datasetStore : this.datasetStores) {
                storeWriters.add(datasetStore.getStoreWriter());
            }
            for (TestRelationReportEntry testRelationReportEntry : testRelationReportEntries) {
                try {
                    final T datapoint = datapointCreator.create(testRelationReportEntry);
                    for (StoreWriter<T> storeWriter : storeWriters) {
                        storeWriter.store(datapoint);
                    }
                    completedCounter++;
                    this.logger.info(
                        String.format(
                            "%d/%d: %s",
                            completedCounter,
                            entriesSize,
                            testRelationReportEntry.getId()
                        )
                    );
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

package org.jdepoix.datasetcreator;

import org.jdepoix.testrelationfinder.reporting.TestRelationReportEntry;

public interface DatapointResolver {
    Datapoint resolve(TestRelationReportEntry entry) throws Exception;
}

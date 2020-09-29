package org.jdepoix.dataset.creator;

import org.jdepoix.dataset.testrelationfinder.reporting.TestRelationReportEntry;

public interface DatapointResolver {
    Datapoint resolve(TestRelationReportEntry entry) throws Exception;
}

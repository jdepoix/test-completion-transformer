package org.jdepoix.dataset.creator;

import org.jdepoix.dataset.testrelationfinder.reporting.TestRelationReportEntry;

public interface DatapointCreator<T extends Datapoint> {
    T create(TestRelationReportEntry entry) throws Exception;
}

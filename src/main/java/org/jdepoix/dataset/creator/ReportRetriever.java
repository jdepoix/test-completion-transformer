package org.jdepoix.dataset.creator;

import org.jdepoix.dataset.testrelationfinder.reporting.TestRelationReportEntry;

import java.util.List;

public interface ReportRetriever {
    List<TestRelationReportEntry> retrieve() throws Exception;
}

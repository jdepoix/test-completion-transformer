package org.jdepoix.datasetcreator;

import org.jdepoix.testrelationfinder.reporting.TestRelationReportEntry;

import java.util.List;

public interface ReportRetriever {
    List<TestRelationReportEntry> retrieve() throws Exception;
}

package org.jdepoix.dataset.creator.gwt;

import org.jdepoix.dataset.creator.ReportRetriever;
import org.jdepoix.dataset.sqlite.ConnectionHandler;
import org.jdepoix.dataset.testrelationfinder.gwt.GWTTestRelation;
import org.jdepoix.dataset.testrelationfinder.relation.TestRelation;
import org.jdepoix.dataset.testrelationfinder.reporting.TestRelationReportEntry;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GWTReportRetriever implements ReportRetriever {
    private final String QUERY_GWT_RESOLVED_ENTRIES =
        "SELECT * from test_relations where gwt_resolution_status = 'RESOLVED'";

    private final ConnectionHandler connectionHandler;

    public GWTReportRetriever(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    public List<TestRelationReportEntry> retrieve() throws Exception {
        try (this.connectionHandler) {
            final List<TestRelationReportEntry> entries = new ArrayList<>();
            Connection connection = this.connectionHandler.getConnection();
            final ResultSet resultSet = connection.createStatement().executeQuery(this.QUERY_GWT_RESOLVED_ENTRIES);
            while (resultSet.next()) {
                entries.add(new TestRelationReportEntry(
                    resultSet.getString("id"),
                    resultSet.getString("repo_name"),
                    TestRelation.Type.valueOf(resultSet.getString("relation_type")),
                    GWTTestRelation.ResolutionStatus.valueOf(resultSet.getString("gwt_resolution_status")),
                    resultSet.getString("test_package"),
                    resultSet.getString("test_class"),
                    resultSet.getString("test_method"),
                    resultSet.getString("test_method_signature"),
                    resultSet.getString("test_method_token_range"),
                    Path.of(resultSet.getString("test_file_path")),
                    Optional.ofNullable(resultSet.getString("related_package")),
                    Optional.ofNullable(resultSet.getString("related_class")),
                    Optional.ofNullable(resultSet.getString("related_method")),
                    Optional.ofNullable(resultSet.getString("related_method_signature")),
                    Optional.ofNullable(resultSet.getString("related_method_token_range")),
                    Optional.ofNullable(Path.of(resultSet.getString("related_file_path"))),
                    Optional.ofNullable(resultSet.getString("given_section")),
                    Optional.ofNullable(resultSet.getString("then_section")),
                    Optional.ofNullable(GWTTestRelation.WhenLocation.valueOf(resultSet.getString("when_location"))),
                    Optional.ofNullable(Integer.parseInt(resultSet.getString("then_section_start_index"))),
                    List.of()
                ));
            }
            return entries;
        }
    }
}

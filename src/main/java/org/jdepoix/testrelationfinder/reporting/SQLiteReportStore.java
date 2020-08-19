package org.jdepoix.testrelationfinder.reporting;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class SQLiteReportStore {
    // TODO update query for context
    // TODO update create table statement
    private static final String INSERT_TEST_RELATION_SQL =
        "INSERT INTO test_relations VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final int BATCH_SIZE = 1000;

    private final Path dbFile;

    public SQLiteReportStore(Path dbFile) {
        this.dbFile = dbFile;
    }

    public void storeReport(String repoName, List<TestRelationReportEntry> reportEntries) throws SQLException {
        try (
            final Connection connection = DriverManager.getConnection(
                String.format("jdbc:sqlite:%s", this.dbFile.toString())
            )
        ) {
            connection.setAutoCommit(false);
            insertTestRelations(connection, repoName, reportEntries);
            connection.commit();
        }
    }

    private void insertTestRelations(
        Connection connection,
        String repoName,
        List<TestRelationReportEntry> reportEntries
    ) throws SQLException
    {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_TEST_RELATION_SQL);
        final int size = reportEntries.size();
        int fullBatches = size / BATCH_SIZE;

        for (int i = 0; i < fullBatches; i++) {
            for (int j = i * BATCH_SIZE; j < (i + 1) * BATCH_SIZE; j++) {
                this.setupPreparedStatement(preparedStatement, repoName, reportEntries.get(j));
            }
            preparedStatement.executeBatch();
        }
        for (int i = fullBatches * BATCH_SIZE; i < fullBatches * BATCH_SIZE + size % BATCH_SIZE; i++) {
            this.setupPreparedStatement(preparedStatement, repoName, reportEntries.get(i));
        }
        preparedStatement.executeBatch();
    }

    private void setupPreparedStatement(
        PreparedStatement preparedStatement,
        String repoName,
        TestRelationReportEntry testRelationReportEntry
    ) throws SQLException {
        preparedStatement.setString(1, repoName);
        preparedStatement.setString(2, testRelationReportEntry.getRelationType().toString());
        preparedStatement.setString(3, testRelationReportEntry.getResolutionStatus().toString());
        preparedStatement.setString(4, testRelationReportEntry.getGwtResolutionStatus().toString());
        preparedStatement.setString(5, testRelationReportEntry.getTestMethodPackageName());
        preparedStatement.setString(6, testRelationReportEntry.getTestMethodClassName());
        preparedStatement.setString(7, testRelationReportEntry.getTestMethodName());
        preparedStatement.setString(8, testRelationReportEntry.getTestMethodSignature());
        preparedStatement.setString(9, testRelationReportEntry.getTestPath().toString());
        preparedStatement.setString(10, testRelationReportEntry.getRelatedMethodPackageName().orElse(null));
        preparedStatement.setString(11, testRelationReportEntry.getRelatedMethodClassName().orElse(null));
        preparedStatement.setString(12, testRelationReportEntry.getRelatedMethodName().orElse(null));
        preparedStatement.setString(13, testRelationReportEntry.getRelatedMethodSignature().orElse(null));
        preparedStatement.setString(14, testRelationReportEntry.getRelatedMethodPath().map(Path::toString).orElse(null));
        preparedStatement.setString(15, testRelationReportEntry.getGiven().orElse(null));
        preparedStatement.setString(16, testRelationReportEntry.getThen().orElse(null));

        preparedStatement.addBatch();
    }
}

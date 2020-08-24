package org.jdepoix.testrelationfinder.reporting;

import org.jdepoix.testrelationfinder.sqlite.ConnectionHandler;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.List;
import java.util.stream.Collectors;

interface StatementHandler<T> {
    void setupPreparedStatement(PreparedStatement statement, T data) throws SQLException;
}

public class SQLiteReportStore {
    private static final String INSERT_TEST_RELATION_SQL =
        "INSERT INTO test_relations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String INSERT_TEST_CONTEXT_SQL = "INSERT INTO test_context VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
    private static final int BATCH_SIZE = 1000;

    private final ConnectionHandler connectionHandler;

    public SQLiteReportStore(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    public void storeReport(List<TestRelationReportEntry> reportEntries) throws Exception {
        Connection connection = this.connectionHandler.getConnection();
        try (this.connectionHandler) {
            connection.setAutoCommit(false);
            this.insertTestRelations(connection, reportEntries);
            this.insertTestContext(connection, reportEntries
                .stream()
                .flatMap(testRelationReportEntry ->
                    testRelationReportEntry
                        .getContext()
                        .stream()
                        .map(testRelationContextReportEntry ->
                            new AbstractMap.SimpleEntry<>(
                                testRelationReportEntry.getId(),
                                testRelationContextReportEntry
                            )
                        )
                )
                .collect(Collectors.toList())
            );
            connection.commit();
        }
    }

    private void insertTestRelations(
        Connection connection,
        List<TestRelationReportEntry> reportEntries
    ) throws SQLException
    {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(INSERT_TEST_RELATION_SQL);
            this.executePreparedStatementInBatches(
                preparedStatement,
                reportEntries,
                (statement, data) -> {
                    statement.setString(1, data.getId());
                    statement.setString(2, data.getRepoName());
                    statement.setString(3, data.getRelationType().toString());
                    statement.setString(4, data.getResolutionStatus().toString());
                    statement.setString(5, data.getGwtResolutionStatus().toString());
                    statement.setString(6, data.getTestMethodPackageName());
                    statement.setString(7, data.getTestMethodClassName());
                    statement.setString(8, data.getTestMethodName());
                    statement.setString(9, data.getTestMethodSignature());
                    statement.setString(10, data.getTestPath().toString());
                    statement.setString(11, data.getRelatedMethodPackageName().orElse(null));
                    statement.setString(12, data.getRelatedMethodClassName().orElse(null));
                    statement.setString(13, data.getRelatedMethodName().orElse(null));
                    statement.setString(14, data.getRelatedMethodSignature().orElse(null));
                    statement.setString(15, data.getRelatedMethodPath().map(Path::toString)
                        .orElse(null));
                    statement.setString(16, data.getGiven().orElse(null));
                    statement.setString(17, data.getThen().orElse(null));

                    statement.addBatch();
                }
            );
        } finally {
            if (preparedStatement != null && !preparedStatement.isClosed()) {
                preparedStatement.close();
            }
        }
    }

    private void insertTestContext(
        Connection connection,
        List<AbstractMap.SimpleEntry<String, TestRelationContextReportEntry>> reportEntries
    ) throws SQLException
    {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(INSERT_TEST_CONTEXT_SQL);
            this.executePreparedStatementInBatches(
                preparedStatement,
                reportEntries,
                (statement, data) -> {
                    final TestRelationContextReportEntry reportEntry = data.getValue();
                    statement.setString(1, data.getKey());
                    statement.setString(2, reportEntry.getResolutionStatus().toString());
                    statement.setString(3, reportEntry.getMethodCall());
                    statement.setString(4, reportEntry.getPackageName().orElse(null));
                    statement.setString(5, reportEntry.getClassName().orElse(null));
                    statement.setString(6, reportEntry.getMethodName().orElse(null));
                    statement.setString(7, reportEntry.getMethodSignature().orElse(null));
                    statement.setString(8, reportEntry.getPath().map(Path::toString).orElse(null));

                    statement.addBatch();
                }
            );
        } finally {
            if (preparedStatement != null && !preparedStatement.isClosed()) {
                preparedStatement.close();
            }
        }
    }

    private <T> void executePreparedStatementInBatches(
        PreparedStatement preparedStatement,
        List<T> entries,
        StatementHandler<T> statementHandler
    ) throws SQLException {
        final int size = entries.size();
        int fullBatches = size / BATCH_SIZE;

        for (int i = 0; i < fullBatches; i++) {
            for (int j = i * BATCH_SIZE; j < (i + 1) * BATCH_SIZE; j++) {
                statementHandler.setupPreparedStatement(preparedStatement, entries.get(j));
            }
            preparedStatement.executeBatch();
        }
        for (int i = fullBatches * BATCH_SIZE; i < fullBatches * BATCH_SIZE + size % BATCH_SIZE; i++) {
            statementHandler.setupPreparedStatement(preparedStatement, entries.get(i));
        }
        preparedStatement.executeBatch();
        preparedStatement.close();
    }
}

package org.jdepoix.dataset.testrelationfinder.reporting;

import org.jdepoix.dataset.sqlite.ConnectionHandler;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SQLiteReportStore {
    private static final String INSERT_TEST_RELATION_SQL =
        "INSERT INTO test_relations " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String INSERT_TEST_CONTEXT_SQL =
        "INSERT INTO test_context VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final int BATCH_SIZE = 1000;

    private final ConnectionHandler connectionHandler;

    public SQLiteReportStore(ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    public void storeReport(List<TestRelationReportEntry> reportEntries) throws Exception {
        try (this.connectionHandler) {
            Connection connection = this.connectionHandler.getConnection();
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
                data ->
                    Arrays.asList(
                        data.getId(),
                        data.getRepoName(),
                        data.getRelationType().toString(),
                        data.getGwtResolutionStatus().toString(),
                        data.getTestMethodPackageName(),
                        data.getTestMethodClassName(),
                        data.getTestMethodName(),
                        data.getTestMethodSignature(),
                        data.getTestMethodTokenRange(),
                        data.getTestPath().toString(),
                        data.getRelatedMethodPackageName().orElse(null),
                        data.getRelatedMethodClassName().orElse(null),
                        data.getRelatedMethodName().orElse(null),
                        data.getRelatedMethodSignature().orElse(null),
                        data.getRelatedMethodTokenRange().orElse(null),
                        data.getRelatedMethodPath().map(Path::toString).orElse(null),
                        data.getGiven().orElse(null),
                        data.getThen().orElse(null),
                        data.getWhenLocation().map(Enum::toString).orElse(null),
                        data.getThenSectionStartIndex().map(integer -> Integer.toString(integer)).orElse(null)
                    )
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
                data -> {
                    final TestRelationContextReportEntry reportEntry = data.getValue();
                    return Arrays.asList(
                        data.getKey(),
                        reportEntry.getResolutionStatus().toString(),
                        reportEntry.getMethodCall(),
                        reportEntry.getMethodCallTokenRange(),
                        reportEntry.getPackageName().orElse(null),
                        reportEntry.getClassName().orElse(null),
                        reportEntry.getMethodName().orElse(null),
                        reportEntry.getMethodSignature().orElse(null),
                        reportEntry.getMethodTokenRange().orElse(null),
                        reportEntry.getPath().map(Path::toString).orElse(null)
                    );
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
        Function<T, List<String>> getBatchData
    ) throws SQLException {
        final int size = entries.size();
        int fullBatches = size / BATCH_SIZE;

        for (int i = 0; i < fullBatches; i++) {
            for (int j = i * BATCH_SIZE; j < (i + 1) * BATCH_SIZE; j++) {
                final List<String> batchData = getBatchData.apply(entries.get(j));
                for (int k = 0; k < batchData.size(); k++) {
                    preparedStatement.setString(k + 1, batchData.get(k));
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
        for (int i = fullBatches * BATCH_SIZE; i < fullBatches * BATCH_SIZE + size % BATCH_SIZE; i++) {
            final List<String> batchData = getBatchData.apply(entries.get(i));
            for (int j = 0; j < batchData.size(); j++) {
                preparedStatement.setString(j + 1, batchData.get(j));
            }
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
        preparedStatement.close();
    }
}

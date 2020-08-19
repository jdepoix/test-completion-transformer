package org.jdepoix.testrelationfinder.reporting;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class SQLiteReportStore {
    private static final String INSERT_TEST_RELATION_SQL =
        "INSERT INTO test_relations VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String INSERT_TEST_CONTEXT_SQL = "INSERT INTO test_context VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
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
            this.insertTestRelations(connection, repoName, reportEntries);
            this.insertTestContext(connection, reportEntries
                .stream()
                .flatMap(testRelationReportEntry ->
                    testRelationReportEntry
                        .getContext()
                        .stream()
                        .map(testRelationContextReportEntry ->
                            new AbstractMap.SimpleEntry<>(
                                testRelationReportEntry.hashCode(),
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
        String repoName,
        List<TestRelationReportEntry> reportEntries
    ) throws SQLException
    {
        this.executePreparedStatementInBatches(
            connection.prepareStatement(INSERT_TEST_RELATION_SQL),
            reportEntries,
            (preparedStatement, testRelationReportEntry) -> {
                try {
                    preparedStatement.setInt(1, testRelationReportEntry.hashCode());
                    preparedStatement.setString(2, repoName);
                    preparedStatement.setString(3, testRelationReportEntry.getRelationType().toString());
                    preparedStatement.setString(4, testRelationReportEntry.getResolutionStatus().toString());
                    preparedStatement.setString(5, testRelationReportEntry.getGwtResolutionStatus().toString());
                    preparedStatement.setString(6, testRelationReportEntry.getTestMethodPackageName());
                    preparedStatement.setString(7, testRelationReportEntry.getTestMethodClassName());
                    preparedStatement.setString(8, testRelationReportEntry.getTestMethodName());
                    preparedStatement.setString(9, testRelationReportEntry.getTestMethodSignature());
                    preparedStatement.setString(10, testRelationReportEntry.getTestPath().toString());
                    preparedStatement.setString(11, testRelationReportEntry.getRelatedMethodPackageName().orElse(null));
                    preparedStatement.setString(12, testRelationReportEntry.getRelatedMethodClassName().orElse(null));
                    preparedStatement.setString(13, testRelationReportEntry.getRelatedMethodName().orElse(null));
                    preparedStatement.setString(14, testRelationReportEntry.getRelatedMethodSignature().orElse(null));
                    preparedStatement.setString(15, testRelationReportEntry.getRelatedMethodPath().map(Path::toString)
                        .orElse(null));
                    preparedStatement.setString(16, testRelationReportEntry.getGiven().orElse(null));
                    preparedStatement.setString(17, testRelationReportEntry.getThen().orElse(null));

                    preparedStatement.addBatch();
                } catch (SQLException exception) {
                    throw new RuntimeException(exception);
                }
            }
        );
    }

    private void insertTestContext(
        Connection connection,
        List<AbstractMap.SimpleEntry<Integer, TestRelationContextReportEntry>> reportEntries
    ) throws SQLException
    {
        this.executePreparedStatementInBatches(
            connection.prepareStatement(INSERT_TEST_CONTEXT_SQL),
            reportEntries,
            (preparedStatement, entry) -> {
                try {
                    final TestRelationContextReportEntry reportEntry = entry.getValue();
                    preparedStatement.setInt(1, entry.getKey());
                    preparedStatement.setString(2, reportEntry.getResolutionStatus().toString());
                    preparedStatement.setString(3, reportEntry.getMethodCall());
                    preparedStatement.setString(4, reportEntry.getPackageName().orElse(null));
                    preparedStatement.setString(5, reportEntry.getClassName().orElse(null));
                    preparedStatement.setString(6, reportEntry.getMethodName().orElse(null));
                    preparedStatement.setString(7, reportEntry.getMethodSignature().orElse(null));
                    preparedStatement.setString(8, reportEntry.getPath().map(Path::toString).orElse(null));

                    preparedStatement.addBatch();
                } catch (SQLException exception) {
                    throw new RuntimeException(exception);
                }
            }
        );
    }

    private <T> void executePreparedStatementInBatches(
        PreparedStatement preparedStatement,
        List<T> entries,
        BiConsumer<PreparedStatement, T> setupPreparedStatement
    ) throws SQLException {
        final int size = entries.size();
        int fullBatches = size / BATCH_SIZE;

        for (int i = 0; i < fullBatches; i++) {
            for (int j = i * BATCH_SIZE; j < (i + 1) * BATCH_SIZE; j++) {
                setupPreparedStatement.accept(preparedStatement, entries.get(j));
            }
            preparedStatement.executeBatch();
        }
        for (int i = fullBatches * BATCH_SIZE; i < fullBatches * BATCH_SIZE + size % BATCH_SIZE; i++) {
            setupPreparedStatement.accept(preparedStatement, entries.get(i));
        }
        preparedStatement.executeBatch();
    }
}

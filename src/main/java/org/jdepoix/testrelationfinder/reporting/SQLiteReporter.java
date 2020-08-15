package org.jdepoix.testrelationfinder.reporting;

import org.jdepoix.testrelationfinder.relation.ResolvedTestRelation;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class SQLiteReporter {
    private static final String INSERT_TEST_RELATION_SQL =
        "INSERT INTO test_relations VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final int BATCH_SIZE = 1000;

    private final Path dbFile;

    public SQLiteReporter(Path dbFile) {
        this.dbFile = dbFile;
    }

    public void reportResults(String repoName, List<ResolvedTestRelation> testRelations)
        throws SQLException
    {
        try (
            final Connection connection = DriverManager.getConnection(
                String.format("jdbc:sqlite:%s", this.dbFile.toString())
            )
        ) {
            connection.setAutoCommit(false);
            insertTestRelations(connection, repoName, testRelations);
            connection.commit();
        }
    }

    private void insertTestRelations(Connection connection, String repoName, List<ResolvedTestRelation> testRelations)
        throws SQLException
    {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_TEST_RELATION_SQL);
        final int size = testRelations.size();
        int fullBatches = size / BATCH_SIZE;

        for (int i = 0; i < fullBatches; i++) {
            for (int j = i * BATCH_SIZE; j < (i + 1) * BATCH_SIZE; j++) {
                this.setupPreparedStatement(preparedStatement, repoName, testRelations.get(j));
            }
            preparedStatement.executeBatch();
        }
        for (int i = fullBatches * BATCH_SIZE; i < fullBatches * BATCH_SIZE + size % BATCH_SIZE; i++) {
            this.setupPreparedStatement(preparedStatement, repoName, testRelations.get(i));
        }
        preparedStatement.executeBatch();
    }

    private void setupPreparedStatement(
        PreparedStatement preparedStatement,
        String repoName,
        ResolvedTestRelation testRelation
    ) throws SQLException {
        preparedStatement.setString(1, repoName);
        preparedStatement.setString(2, testRelation.getRelationType().toString());
        preparedStatement.setString(3, testRelation.getResolutionStatus().toString());
        preparedStatement.setString(4, testRelation.getGwtResolutionStatus().toString());
        preparedStatement.setString(5, testRelation.getTestMethodPackageName());
        preparedStatement.setString(6, testRelation.getTestMethodClassName());
        preparedStatement.setString(7, testRelation.getTestMethodName());
        preparedStatement.setString(8, testRelation.getTestMethodSignature());
        preparedStatement.setString(9, testRelation.getTestPath().toString());
        preparedStatement.setString(10, testRelation.getRelatedMethodPackageName().orElse(null));
        preparedStatement.setString(11, testRelation.getRelatedMethodClassName().orElse(null));
        preparedStatement.setString(12, testRelation.getRelatedMethodName().orElse(null));
        preparedStatement.setString(13, testRelation.getRelatedMethodSignature().orElse(null));
        preparedStatement.setString(14, testRelation.getRelatedMethodPath().map(Path::toString).orElse(null));
        preparedStatement.setString(15, testRelation.getGiven().orElse(null));
        preparedStatement.setString(16, testRelation.getWhen().orElse(null));
        preparedStatement.setString(17, testRelation.getThen().orElse(null));

        preparedStatement.addBatch();
    }
}

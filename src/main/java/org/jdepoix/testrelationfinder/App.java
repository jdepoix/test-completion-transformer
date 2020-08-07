package org.jdepoix.testrelationfinder;

import org.jdepoix.testrelationfinder.archive.ArchiveHandler;
import org.jdepoix.testrelationfinder.manager.RelationFinderRunner;
import org.jdepoix.testrelationfinder.manager.RepoFileManager;
import org.jdepoix.testrelationfinder.relation.Finder;
import org.jdepoix.testrelationfinder.relation.ResolvedTestRelation;
import org.jdepoix.testrelationfinder.relation.TestRelation;
import org.jdepoix.testrelationfinder.reporting.SQLiteReporter;
import org.jdepoix.testrelationfinder.testmethod.Extractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
    public static void main(String[] args) throws ClassNotFoundException {

//        try {
//            final Channel<TestRelation> reportingChannel = new Channel<>();
//            final SQLiteReporter sqLiteReporter = SQLiteReporter.create(reportingChannel, "test_relations.sqlite");
//
//            Thread reporterThread = new Thread(sqLiteReporter);
//            reporterThread.start();
//
////            reportingChannel.send(new TestRelation());
//
//            reportingChannel.close();
//            reporterThread.join();
//            sqLiteReporter.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }



//        Class.forName("org.sqlite.JDBC");
//        Connection connection = null;
//        try
//        {
//            // create a database connection
//            connection = DriverManager.getConnection("jdbc:sqlite:sample.db");
//            Statement statement = connection.createStatement();
//            statement.setQueryTimeout(30);  // set timeout to 30 sec.
//
//            statement.executeUpdate("drop table if exists person");
//            statement.executeUpdate("create table person (id integer, name string)");
//            statement.executeUpdate("insert into person values(1, 'leo')");
//            statement.executeUpdate("insert into person values(2, 'yui')");
//            ResultSet rs = statement.executeQuery("select * from person");
//            while(rs.next())
//            {
//                // read the result set
//                System.out.println("name = " + rs.getString("name"));
//                System.out.println("id = " + rs.getInt("id"));
//            }
//        }
//        catch(SQLException e)
//        {
//            // if the error message is "out of memory",
//            // it probably means no database file is found
//            System.err.println(e.getMessage());
//        }
//        finally
//        {
//            try
//            {
//                if(connection != null)
//                    connection.close();
//            }
//            catch(SQLException e)
//            {
//                // connection close failed.
//                System.err.println(e);
//            }
//        }






        Instant start = Instant.now();
        System.out.println("START: " + start);

//        ArchiveHandler archiveHandler = new ArchiveHandler(Path.of("assets/temp"));
//        Extractor extractor = new Extractor();
//        Finder finder = new Finder();
//        Channel<ResolvedTestRelation> reportingChannel = new Channel<>();
//        RelationFinderRunner relationFinderRunner = new RelationFinderRunner(extractor, finder, archiveHandler, reportingChannel);

//        List<Thread> threadPool = new ArrayList<>();
//        try {
//            try (Stream<Path> pathStream = Files.list(Path.of("assets/tars"))) {
//                pathStream.forEach(path -> {
//                    final Thread t = new Thread(() -> {
//                        try {
//                            System.out.println("start thread");
//                            relationFinderRunner.run(path);
//                            System.out.println("ended thread: " + Duration.between(start, Instant.now()));
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    });
//                    threadPool.add(t);
//                    t.start();
//                });
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        for (Thread thread : threadPool) {
//            try {
//                thread.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

        // TODO test without filtering test source roots

//        List<Path> files = new ArrayList<>();
//        files.add(Path.of("assets/tars/ReactiveX#RxJava.tar.gz"));
//        files.add(Path.of("assets/tars/spring-projects#spring-boot.tar.gz"));
//        try (Stream<Path> pathStream = Files.list(Path.of("assets/tars"))) {
//            files = pathStream.collect(Collectors.toList());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        files.forEach(path -> {
            Path workingDir = Path.of("../temp");

            try {
                ArchiveHandler archiveHandler = new ArchiveHandler(workingDir.resolve("temp"));
                Extractor extractor = new Extractor();
                Finder finder = new Finder();
                RepoFileManager fileManager = new RepoFileManager(workingDir.resolve("repos"));
                SQLiteReporter reporter = new SQLiteReporter(workingDir.resolve("test_relations_index.sqlite"));
                RelationFinderRunner relationFinderRunner = new RelationFinderRunner(extractor, finder, archiveHandler, fileManager, reporter);
                final Instant runStart = Instant.now();
                relationFinderRunner.run(Path.of("assets/tars/ReactiveX#RxJava.tar.gz"));
//                relationFinderRunner.run(Path.of(args[0]));
                System.out.println("ended run: " + Duration.between(runStart, Instant.now()));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
//        });

        Instant end = Instant.now();
        System.out.println("END: " + end);
        System.out.println("DURATION: " + Duration.between(start, end));



//        ArchiveHandler archiveHandler = new ArchiveHandler(Path.of("assets/temp"));
//        try {
//            archiveHandler.runOnArchiveContent(Path.of("assets/tars/spring-projects#spring-boot-master.tar.gz"), path -> {
//                Extractor extractor = new Extractor();
//                Finder finder = new Finder();
//                List<TestRelation> testRelations = finder.findTestRelations(extractor.extractTestMethods(path)).collect(Collectors.toList());
//                System.out.println(testRelations.size());
//            });
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//

//
//        int notFoundCounter = 0;
//        int foundCounter = 0;
//        int resolvedCounter = 0;
//        int cantResolveCounter = 0;
//        int identifiedByClassCounter = 0;
//        int identifiedByMethodCounter = 0;
//
//        for (TestRelation testRelation : testRelations) {
//            foundCounter++;
//
//            if (testRelation.getType() == TestRelation.Type.MAPPED_BY_TEST_CLASS_NAME) {
//                identifiedByClassCounter++;
//            }
//            else if (testRelation.getType() == TestRelation.Type.MAPPED_BY_TEST_METHOD_NAME) {
//                identifiedByMethodCounter++;
//            } else {
//                notFoundCounter++;
//            }
//
//            if (testRelation.getResolutionStatus() == TestRelation.ResolutionStatus.UNRESOLVABLE) {
//                cantResolveCounter++;
//            } else if (testRelation.getResolutionStatus() == TestRelation.ResolutionStatus.RESOLVED) {
//                resolvedCounter++;
//            }
//        }
//
//        System.out.println("notFoundCounter: " + notFoundCounter);
//        System.out.println("foundCounter: " + foundCounter);
//        System.out.println("resolvedCounter: " + resolvedCounter);
//        System.out.println("cantResolveCounter: " + cantResolveCounter);
//        System.out.println("identifiedByClassCounter: " + identifiedByClassCounter);
//        System.out.println("identifiedByMethodCounter: " + identifiedByMethodCounter);
    }
}

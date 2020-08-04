package org.jdepoix.testextractor;

import org.jdepoix.testextractor.indexing.ClassIndexer;
import org.jdepoix.testextractor.indexing.JavaFileIndexCreator;
import org.jdepoix.testextractor.mapping.UnitTestMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class App {
    public static void main(String[] args) {
        Instant start = Instant.now();
        System.out.println("START: " + start);

        JavaFileIndexCreator indicesCreator = new JavaFileIndexCreator(Path.of("assets/staticcode/RxJava"));

        ClassIndexer classIndexer = new ClassIndexer();

//        try {
//            indicesCreator.createIndices(classIndexer);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        try {
            String testFileContent = Files.readString(Path.of("assets/staticcode/RxJava/src/test/java/io/reactivex/rxjava3/flowable/FlowableBackpressureTests.java"));
            UnitTestMapper unitTestMapper = new UnitTestMapper(classIndexer);
            unitTestMapper.findMapping(testFileContent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Instant end = Instant.now();
        System.out.println("END: " + start);
        System.out.println("DURATION: " + Duration.between(start, end));
    }
}

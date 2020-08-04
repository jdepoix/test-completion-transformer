package org.jdepoix.testrelationfinder;

import org.jdepoix.testrelationfinder.relation.Finder;
import org.jdepoix.testrelationfinder.relation.TestRelation;
import org.jdepoix.testrelationfinder.testmethod.Extractor;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class App {
    public static void main(String[] args) {
        Instant start = Instant.now();
        System.out.println("START: " + start);



        Extractor extractor = new Extractor();
        Finder finder = new Finder();

        List<TestRelation> testRelations = finder.findTestRelations(extractor.extractTestMethods(Path.of("assets/staticcode/RxJava")));
        System.out.println(testRelations.size());


        Instant end = Instant.now();
        System.out.println("END: " + start);
        System.out.println("DURATION: " + Duration.between(start, end));
    }
}

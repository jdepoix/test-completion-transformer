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

        int notFoundCounter = 0;
        int foundCounter = 0;
        int resolvedCounter = 0;
        int cantResolveCounter = 0;
        int identifiedByClassCounter = 0;
        int identifiedByMethodCounter = 0;

        for (TestRelation testRelation : testRelations) {
            foundCounter++;

            if (testRelation.getType() == TestRelation.Type.MAPPED_BY_TEST_CLASS_NAME) {
                identifiedByClassCounter++;
            }
            else if (testRelation.getType() == TestRelation.Type.MAPPED_BY_TEST_METHOD_NAME) {
                identifiedByMethodCounter++;
            } else {
                notFoundCounter++;
            }

            if (testRelation.getResolutionStatus() == TestRelation.ResolutionStatus.UNRESOLVABLE) {
                cantResolveCounter++;
            } else if (testRelation.getResolutionStatus() == TestRelation.ResolutionStatus.RESOLVED) {
                resolvedCounter++;
            }
        }

        System.out.println("notFoundCounter: " + notFoundCounter);
        System.out.println("foundCounter: " + foundCounter);
        System.out.println("resolvedCounter: " + resolvedCounter);
        System.out.println("cantResolveCounter: " + cantResolveCounter);
        System.out.println("identifiedByClassCounter: " + identifiedByClassCounter);
        System.out.println("identifiedByMethodCounter: " + identifiedByMethodCounter);
    }
}

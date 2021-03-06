package org.jdepoix.dataset.testrelationfinder.reporting;

import org.jdepoix.dataset.testrelationfinder.gwt.GWTTestRelation;
import org.jdepoix.dataset.testrelationfinder.relation.TestRelation;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestRelationReportEntry {
    private final String id;
    private final String repoName;
    private final TestRelation.Type relationType;
    private final GWTTestRelation.ResolutionStatus gwtResolutionStatus;
    private final String testMethodPackageName;
    private final String testMethodClassName;
    private final String testMethodName;
    private final String testMethodSignature;
    private final String testMethodTokenRange;
    private final Path testPath;
    private final Optional<String> relatedMethodPackageName;
    private final Optional<String> relatedMethodClassName;
    private final Optional<String> relatedMethodName;
    private final Optional<String> relatedMethodSignature;
    private final Optional<String> relatedMethodTokenRange;
    private final Optional<Path> relatedMethodPath;
    private final Optional<String> given;
    private final Optional<String> then;
    private final Optional<GWTTestRelation.WhenLocation> whenLocation;
    private final Optional<Integer> thenSectionStartIndex;
    private final List<TestRelationContextReportEntry> context;

    public TestRelationReportEntry(
        String id,
        String repoName,
        TestRelation.Type relationType,
        GWTTestRelation.ResolutionStatus gwtResolutionStatus,
        String testMethodPackageName,
        String testMethodClassName,
        String testMethodName,
        String testMethodSignature,
        String testMethodTokenRange,
        Path testPath,
        Optional<String> relatedMethodPackageName,
        Optional<String> relatedMethodClassName,
        Optional<String> relatedMethodName,
        Optional<String> relatedMethodSignature,
        Optional<String> relatedMethodTokenRange,
        Optional<Path> relatedMethodPath,
        Optional<String> given,
        Optional<String> then,
        Optional<GWTTestRelation.WhenLocation> whenLocation,
        Optional<Integer> thenSectionStartIndex,
        List<TestRelationContextReportEntry> context
    ) {
        this.id = id;
        this.repoName = repoName;
        this.relationType = relationType;
        this.gwtResolutionStatus = gwtResolutionStatus;
        this.testMethodPackageName = testMethodPackageName;
        this.testMethodClassName = testMethodClassName;
        this.testMethodName = testMethodName;
        this.testMethodSignature = testMethodSignature;
        this.testMethodTokenRange = testMethodTokenRange;
        this.testPath = testPath;
        this.relatedMethodPackageName = relatedMethodPackageName;
        this.relatedMethodClassName = relatedMethodClassName;
        this.relatedMethodName = relatedMethodName;
        this.relatedMethodSignature = relatedMethodSignature;
        this.relatedMethodTokenRange = relatedMethodTokenRange;
        this.relatedMethodPath = relatedMethodPath;
        this.given = given;
        this.then = then;
        this.whenLocation = whenLocation;
        this.thenSectionStartIndex = thenSectionStartIndex;
        this.context = context;
    }

    TestRelationReportEntry(
        String repoName,
        TestRelation.Type relationType,
        GWTTestRelation.ResolutionStatus gwtResolutionStatus,
        String testMethodPackageName,
        String testMethodClassName,
        String testMethodName,
        String testMethodSignature,
        String testMethodTokenRange,
        Path testPath,
        Optional<String> relatedMethodPackageName,
        Optional<String> relatedMethodClassName,
        Optional<String> relatedMethodName,
        Optional<String> relatedMethodSignature,
        Optional<String> relatedMethodTokenRange,
        Optional<Path> relatedMethodPath,
        Optional<String> given,
        Optional<String> then,
        Optional<GWTTestRelation.WhenLocation> whenLocation,
        Optional<Integer> thenSectionStartIndex,
        List<TestRelationContextReportEntry> context
    ) {
        this(
            UUID.randomUUID().toString(),
            repoName,
            relationType,
            gwtResolutionStatus,
            testMethodPackageName,
            testMethodClassName,
            testMethodName,
            testMethodSignature,
            testMethodTokenRange,
            testPath,
            relatedMethodPackageName,
            relatedMethodClassName,
            relatedMethodName,
            relatedMethodSignature,
            relatedMethodTokenRange,
            relatedMethodPath,
            given,
            then,
            whenLocation,
            thenSectionStartIndex,
            context
        );
    }

    public String getId() {
        return id;
    }

    public String getRepoName() {
        return repoName;
    }

    public TestRelation.Type getRelationType() {
        return relationType;
    }

    public GWTTestRelation.ResolutionStatus getGwtResolutionStatus() {
        return gwtResolutionStatus;
    }

    public String getTestMethodPackageName() {
        return testMethodPackageName;
    }

    public String getTestMethodClassName() {
        return testMethodClassName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public String getTestMethodSignature() {
        return testMethodSignature;
    }

    public Path getTestPath() {
        return testPath;
    }

    public Optional<String> getRelatedMethodPackageName() {
        return relatedMethodPackageName;
    }

    public Optional<String> getRelatedMethodClassName() {
        return relatedMethodClassName;
    }

    public Optional<String> getRelatedMethodName() {
        return relatedMethodName;
    }

    public Optional<String> getRelatedMethodSignature() {
        return relatedMethodSignature;
    }

    public Optional<Path> getRelatedMethodPath() {
        return relatedMethodPath;
    }

    public Optional<String> getGiven() {
        return given;
    }

    public Optional<String> getThen() {
        return then;
    }

    public List<TestRelationContextReportEntry> getContext() {
        return context;
    }

    public String getTestMethodTokenRange() {
        return testMethodTokenRange;
    }

    public Optional<String> getRelatedMethodTokenRange() {
        return relatedMethodTokenRange;
    }

    public Optional<GWTTestRelation.WhenLocation> getWhenLocation() {
        return whenLocation;
    }

    public Optional<Integer> getThenSectionStartIndex() {
        return thenSectionStartIndex;
    }
}

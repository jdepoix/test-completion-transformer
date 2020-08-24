package org.jdepoix.testrelationfinder.reporting;

import org.jdepoix.testrelationfinder.gwt.GWTTestRelation;
import org.jdepoix.testrelationfinder.relation.ResolvedTestRelation;
import org.jdepoix.testrelationfinder.relation.TestRelation;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TestRelationReportEntry {
    private final String id;
    private final String repoName;
    private final TestRelation.Type relationType;
    private final ResolvedTestRelation.ResolutionStatus resolutionStatus;
    private final GWTTestRelation.ResolutionStatus gwtResolutionStatus;
    private final String testMethodPackageName;
    private final String testMethodClassName;
    private final String testMethodName;
    private final String testMethodSignature;
    private final Path testPath;
    private final Optional<String> relatedMethodPackageName;
    private final Optional<String> relatedMethodClassName;
    private final Optional<String> relatedMethodName;
    private final Optional<String> relatedMethodSignature;
    private final Optional<Path> relatedMethodPath;
    private final Optional<String> given;
    private final Optional<String> then;
    private final List<TestRelationContextReportEntry> context;

    TestRelationReportEntry(
        String repoName,
        TestRelation.Type relationType,
        ResolvedTestRelation.ResolutionStatus resolutionStatus,
        GWTTestRelation.ResolutionStatus gwtResolutionStatus,
        String testMethodPackageName,
        String testMethodClassName,
        String testMethodName,
        String testMethodSignature,
        Path testPath,
        Optional<String> relatedMethodPackageName,
        Optional<String> relatedMethodClassName,
        Optional<String> relatedMethodName,
        Optional<String> relatedMethodSignature,
        Optional<Path> relatedMethodPath,
        Optional<String> given,
        Optional<String> then,
        List<TestRelationContextReportEntry> context
    ) {
        this.id = UUID.randomUUID().toString();
        this.repoName = repoName;
        this.relationType = relationType;
        this.resolutionStatus = resolutionStatus;
        this.gwtResolutionStatus = gwtResolutionStatus;
        this.testMethodPackageName = testMethodPackageName;
        this.testMethodClassName = testMethodClassName;
        this.testMethodName = testMethodName;
        this.testMethodSignature = testMethodSignature;
        this.testPath = testPath;
        this.relatedMethodPackageName = relatedMethodPackageName;
        this.relatedMethodClassName = relatedMethodClassName;
        this.relatedMethodName = relatedMethodName;
        this.relatedMethodSignature = relatedMethodSignature;
        this.relatedMethodPath = relatedMethodPath;
        this.given = given;
        this.then = then;
        this.context = context;
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

    public ResolvedTestRelation.ResolutionStatus getResolutionStatus() {
        return resolutionStatus;
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
}

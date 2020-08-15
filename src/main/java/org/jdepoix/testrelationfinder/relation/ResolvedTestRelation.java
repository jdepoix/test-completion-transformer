package org.jdepoix.testrelationfinder.relation;

import java.nio.file.Path;
import java.util.Optional;

public class ResolvedTestRelation {
    public enum ResolutionStatus {
        NOT_RESOLVED,
        RESOLVED,
        UNRESOLVABLE
    }

    private final String repoName;
    private final TestRelation.Type relationType;
    private final ResolvedTestRelation.ResolutionStatus resolutionStatus;
    private final GivenWhenThenRelation.ResolutionStatus gwtResolutionStatus;
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
    private final Optional<String> when;
    private final Optional<String> then;

    public ResolvedTestRelation(
        String repoName,
        TestRelation.Type relationType,
        ResolvedTestRelation.ResolutionStatus resolutionStatus,
        GivenWhenThenRelation.ResolutionStatus gwtResolutionStatus,
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
        Optional<String> when,
        Optional<String> then
    ) {
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
        this.when = when;
        this.then = then;
    }

    public String getRepoName() {
        return repoName;
    }

    public TestRelation.Type getRelationType() {
        return relationType;
    }

    public ResolutionStatus getResolutionStatus() {
        return resolutionStatus;
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

    public Optional<Path> getRelatedMethodPath() {
        return relatedMethodPath;
    }

    public GivenWhenThenRelation.ResolutionStatus getGwtResolutionStatus() {
        return gwtResolutionStatus;
    }

    public Optional<String> getGiven() {
        return given;
    }

    public Optional<String> getWhen() {
        return when;
    }

    public Optional<String> getThen() {
        return then;
    }

    public String getTestMethodSignature() {
        return testMethodSignature;
    }

    public Optional<String> getRelatedMethodSignature() {
        return relatedMethodSignature;
    }
}

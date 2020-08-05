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
    private final String testMethodPackageName;
    private final String testMethodClassName;
    private final String testMethodName;
    private final Path testPath;
    private final Optional<String> testFileContent;
    private final Optional<String> relatedMethodPackageName;
    private final Optional<String> relatedMethodClassName;
    private final Optional<String> relatedMethodName;
    private final Optional<Path> relatedMethodPath;
    private final Optional<String> relatedMethodFileContent;

    public ResolvedTestRelation(
        String repoName,
        TestRelation.Type relationType,
        ResolutionStatus resolutionStatus,
        String testMethodPackageName,
        String testMethodClassName,
        String testMethodName,
        Path testPath,
        Optional<String> testFileContent,
        Optional<String> relatedMethodPackageName,
        Optional<String> relatedMethodClassName,
        Optional<String> relatedMethodName,
        Optional<Path> relatedMethodPath,
        Optional<String> relatedMethodFileContent
    ) {
        this.repoName = repoName;
        this.relationType = relationType;
        this.resolutionStatus = resolutionStatus;
        this.testMethodPackageName = testMethodPackageName;
        this.testMethodClassName = testMethodClassName;
        this.testMethodName = testMethodName;
        this.testPath = testPath;
        this.testFileContent = testFileContent;
        this.relatedMethodPackageName = relatedMethodPackageName;
        this.relatedMethodClassName = relatedMethodClassName;
        this.relatedMethodName = relatedMethodName;
        this.relatedMethodPath = relatedMethodPath;
        this.relatedMethodFileContent = relatedMethodFileContent;
    }
}

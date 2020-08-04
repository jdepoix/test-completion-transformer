package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.nio.file.Path;
import java.util.Optional;

public class TestRelation {
    public enum Type {
        NO_RELATION_FOUND,
        MAPPED_BY_TEST_METHOD_NAME,
        MAPPED_BY_TEST_CLASS_NAME
    }

    public enum ResolutionStatus {
        NOT_RESOLVED,
        RESOLVED,
        UNRESOLVABLE
    }

    private final Type type;
    private final ResolutionStatus resolutionStatus;
    private final String testMethodPackageName;
    private final String testMethodClassName;
    private final String testMethodName;
    private final Path testPath;
    private final Optional<String> relatedMethodPackageName;
    private final Optional<String> relatedMethodClassName;
    private final Optional<String> relatedMethodName;
    private final Optional<Path> relatedMethodPath;

    private TestRelation(
        Type type,
        ResolutionStatus resolutionStatus,
        String testMethodPackageName,
        String testMethodClassName,
        String testMethodName,
        Path testPath,
        Optional<String> relatedMethodPackageName,
        Optional<String> relatedMethodClassName,
        Optional<String> relatedMethodName,
        Optional<Path> relatedMethodPath
    ) {
        this.type = type;
        this.resolutionStatus = resolutionStatus;
        this.testMethodPackageName = testMethodPackageName;
        this.testMethodClassName = testMethodClassName;
        this.testMethodName = testMethodName;
        this.testPath = testPath;
        this.relatedMethodPackageName = relatedMethodPackageName;
        this.relatedMethodClassName = relatedMethodClassName;
        this.relatedMethodName = relatedMethodName;
        this.relatedMethodPath = relatedMethodPath;
    }

    private static TestRelation create(
        Type type,
        ResolutionStatus resolutionStatus,
        MethodDeclaration testMethod
    ) {
        return create(
            type,
            resolutionStatus,
            testMethod,
            null,
            null,
            null,
            null
        );
    }

    private static TestRelation create(
        Type type,
        ResolutionStatus resolutionStatus,
        MethodDeclaration testMethod,
        String relatedMethodPackageName,
        String relatedMethodClassName,
        String relatedMethodName,
        Path relatedMethodPath
    ) {
        ResolvedMethodDeclaration resolvedTestMethod = testMethod.resolve();
        String testMethodPackageName = resolvedTestMethod.getPackageName();
        String testMethodClassName = resolvedTestMethod.getClassName();
        String testMethodName = resolvedTestMethod.getName();
        Path testPath = testMethod.findCompilationUnit().get().getStorage().get().getPath();
        return new TestRelation(
            type,
            resolutionStatus,
            testMethodPackageName,
            testMethodClassName,
            testMethodName,
            testPath,
            Optional.ofNullable(relatedMethodPackageName),
            Optional.ofNullable(relatedMethodClassName),
            Optional.ofNullable(relatedMethodName),
            Optional.ofNullable(relatedMethodPath)
        );
    }

    static TestRelation resolveFromNodes(MethodDeclaration testMethod) {
        return create(Type.NO_RELATION_FOUND, ResolutionStatus.NOT_RESOLVED, testMethod);
    }

    static TestRelation resolveFromNodes(
        MethodDeclaration testMethod,
        MethodCallExpr relatedMethod,
        Type type
    ) {
        try {
            final ResolvedMethodDeclaration resolvedRelatedMethod = relatedMethod.resolve();
            return create(
                type,
                ResolutionStatus.RESOLVED,
                testMethod,
                resolvedRelatedMethod.getPackageName(),
                resolvedRelatedMethod.getClassName(),
                resolvedRelatedMethod.getName(),
                resolvedRelatedMethod.toAst().get().findCompilationUnit().get().getStorage().get().getPath()
            );
        } catch (Exception e) {
            return create(type, ResolutionStatus.UNRESOLVABLE, testMethod);
        }
    }

    public Type getType() {
        return type;
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
}

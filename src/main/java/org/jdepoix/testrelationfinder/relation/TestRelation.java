package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.nio.file.Path;
import java.util.Optional;

public class TestRelation {
    public enum Type {
        RESOLVED,
        UNRESOLVABLE,
        RELATION_NOT_FOUND
    }

    private final Type type;
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
        String testMethodPackageName,
        String testMethodClassName,
        String testMethodName,
        Path testPath,
        String relatedMethodPackageName,
        String relatedMethodClassName,
        String relatedMethodName,
        Path relatedMethodPath
    ) {
        this.type = type;
        this.testMethodPackageName = testMethodPackageName;
        this.testMethodClassName = testMethodClassName;
        this.testMethodName = testMethodName;
        this.testPath = testPath;
        this.relatedMethodPackageName = Optional.of(relatedMethodPackageName);
        this.relatedMethodClassName = Optional.of(relatedMethodClassName);
        this.relatedMethodName = Optional.of(relatedMethodName);
        this.relatedMethodPath = Optional.of(relatedMethodPath);
    }

    private TestRelation(
        Type type,
        String testMethodPackageName,
        String testMethodClassName,
        String testMethodName,
        Path testPath
    ) {
        this.type = type;
        this.testMethodPackageName = testMethodPackageName;
        this.testMethodClassName = testMethodClassName;
        this.testMethodName = testMethodName;
        this.testPath = testPath;
        this.relatedMethodPackageName = Optional.empty();
        this.relatedMethodClassName = Optional.empty();
        this.relatedMethodName = Optional.empty();
        this.relatedMethodPath = Optional.empty();
    }

    static TestRelation resolveFromNodes(MethodDeclaration testMethod, Optional<MethodCallExpr> relatedMethod) {
        ResolvedMethodDeclaration resolvedTestMethod = testMethod.resolve();
        String testMethodPackageName = resolvedTestMethod.getPackageName();
        String testMethodClassName = resolvedTestMethod.getClassName();
        String testMethodName = resolvedTestMethod.getName();
        Path testPath = testMethod.findCompilationUnit().get().getStorage().get().getPath();

        if (relatedMethod.isEmpty()) {
            return new TestRelation(
                Type.RELATION_NOT_FOUND,
                testMethodPackageName,
                testMethodClassName,
                testMethodName,
                testPath
            );
        }

        try {
            final ResolvedMethodDeclaration resolvedRelatedMethod = relatedMethod.get().resolve();
            return new TestRelation(
                Type.RELATION_NOT_FOUND,
                testMethodPackageName,
                testMethodClassName,
                testMethodName,
                testPath,
                resolvedRelatedMethod.getPackageName(),
                resolvedRelatedMethod.getClassName(),
                resolvedRelatedMethod.getName(),
                resolvedRelatedMethod.toAst().get().findCompilationUnit().get().getStorage().get().getPath()
            );
        } catch (Exception e) {
            return new TestRelation(
                Type.UNRESOLVABLE,
                testMethodPackageName,
                testMethodClassName,
                testMethodName,
                testPath
            );
        }
    }
}

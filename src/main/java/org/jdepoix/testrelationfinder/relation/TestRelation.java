package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

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
    private final Optional<String> testMethodCode;
    private final Optional<String> relatedMethodPackageName;
    private final Optional<String> relatedMethodClassName;
    private final Optional<String> relatedMethodName;
    private final Optional<String> relatedMethodCode;

    private TestRelation(
        Type type,
        String testMethodPackageName,
        String testMethodClassName,
        String testMethodName,
        String testMethodCode,
        String relatedMethodPackageName,
        String relatedMethodClassName,
        String relatedMethodName,
        String relatedMethodCode
    ) {
        this.type = type;
        this.testMethodPackageName = testMethodPackageName;
        this.testMethodClassName = testMethodClassName;
        this.testMethodName = testMethodName;
        this.testMethodCode = Optional.of(testMethodCode);
        this.relatedMethodPackageName = Optional.of(relatedMethodPackageName);
        this.relatedMethodClassName = Optional.of(relatedMethodClassName);
        this.relatedMethodName = Optional.of(relatedMethodName);
        this.relatedMethodCode = Optional.of(relatedMethodCode);
    }

    private TestRelation(
        Type type,
        String testMethodPackageName,
        String testMethodClassName,
        String testMethodName
    ) {
        this.type = type;
        this.testMethodPackageName = testMethodPackageName;
        this.testMethodClassName = testMethodClassName;
        this.testMethodName = testMethodName;
        this.testMethodCode = Optional.empty();
        this.relatedMethodPackageName = Optional.empty();
        this.relatedMethodClassName = Optional.empty();
        this.relatedMethodName = Optional.empty();
        this.relatedMethodCode = Optional.empty();
    }

    static TestRelation resolveFromNodes(MethodDeclaration testMethod, Optional<MethodCallExpr> relatedMethod) {
        ResolvedMethodDeclaration resolvedTestMethod = testMethod.resolve();
        String testMethodPackageName = resolvedTestMethod.getPackageName();
        String testMethodClassName = resolvedTestMethod.getClassName();
        String testMethodName = resolvedTestMethod.getName();

        if (relatedMethod.isEmpty()) {
            return new TestRelation(
                Type.RELATION_NOT_FOUND,
                testMethodPackageName,
                testMethodClassName,
                testMethodName
            );
        }

        try {
            final ResolvedMethodDeclaration resolvedRelatedMethod = relatedMethod.get().resolve();
            return new TestRelation(
                Type.RELATION_NOT_FOUND,
                testMethodPackageName,
                testMethodClassName,
                testMethodName,
                // TODO full file
                "testMethod.toString()",
                resolvedRelatedMethod.getPackageName(),
                resolvedRelatedMethod.getClassName(),
                resolvedRelatedMethod.getName(),
                // TODO full file
                "resolvedRelatedMethod.g()"
            );
        } catch (Exception e) {
            return new TestRelation(
                Type.UNRESOLVABLE,
                testMethodPackageName,
                testMethodClassName,
                testMethodName
            );
        }
    }
}

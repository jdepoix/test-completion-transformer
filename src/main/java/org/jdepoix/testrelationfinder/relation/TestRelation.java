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

    private final MethodDeclaration testMethod;
    private final Optional<MethodCallExpr> relatedMethod;
    private final Type type;

    public TestRelation(MethodDeclaration testMethod, MethodCallExpr relatedMethod, Type type) {
        this.testMethod = testMethod;
        this.relatedMethod = Optional.of(relatedMethod);
        this.type = type;
    }

    public TestRelation(MethodDeclaration testMethod) {
        this.testMethod = testMethod;
        this.relatedMethod = Optional.empty();
        this.type = Type.NO_RELATION_FOUND;
    }

    Type getType() {
        return type;
    }

    Optional<MethodCallExpr> getRelatedMethod() {
        return relatedMethod;
    }

    MethodDeclaration getTestMethod() {
        return testMethod;
    }
}

package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Optional;

public class TestRelation {
    public static class Builder {
        private Optional<MethodDeclaration> testMethod = Optional.empty();
        private Optional<MethodCallExpr> relatedMethod = Optional.empty();
        private Optional<Type> type = Optional.empty();

        public Builder setTestMethod(MethodDeclaration testMethod) {
            this.testMethod = Optional.of(testMethod);
            return this;
        }

        public Builder setRelatedMethod(MethodCallExpr relatedMethod) {
            this.relatedMethod = Optional.of(relatedMethod);
            return this;
        }

        public Builder setType(Type type) {
            this.type = Optional.ofNullable(type);
            return this;
        }

        public Optional<Type> getType() {
            return this.type;
        }

        public TestRelation build() {
            return new TestRelation(this.testMethod.get(), this.relatedMethod, this.type.get());
        }
    }

    public enum Type {
        NO_RELATION_FOUND,
        MAPPED_BY_TEST_METHOD_NAME,
        MAPPED_BY_TEST_CLASS_NAME,
        AMBIGUOUS_RELATIONS
    }

    private final MethodDeclaration testMethod;
    private final Optional<MethodCallExpr> relatedMethod;
    private final Type type;

    public TestRelation(MethodDeclaration testMethod, Optional<MethodCallExpr> relatedMethod, Type type) {
        this.testMethod = testMethod;
        this.relatedMethod = relatedMethod;
        this.type = type;
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

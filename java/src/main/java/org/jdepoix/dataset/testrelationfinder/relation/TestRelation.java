package org.jdepoix.dataset.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.util.Optional;

public class TestRelation {
    static class Builder {
        private Optional<MethodDeclaration> testMethod = Optional.empty();
        private Optional<ResolvedMethodDeclaration> relatedMethod = Optional.empty();
        private Optional<Type> type = Optional.empty();

        Builder setTestMethod(MethodDeclaration testMethod) {
            this.testMethod = Optional.of(testMethod);
            return this;
        }

        Builder setRelatedMethod(ResolvedMethodDeclaration relatedMethod) {
            this.relatedMethod = Optional.of(relatedMethod);
            return this;
        }

        Builder setType(Type type) {
            this.type = Optional.ofNullable(type);
            return this;
        }

        Optional<Type> getType() {
            return this.type;
        }

        TestRelation build() {
            return new TestRelation(this.testMethod.get(), this.relatedMethod, this.type.get());
        }
    }

    public enum Type {
        MAPPED_BY_TEST_METHOD_NAME,
        MAPPED_BY_TEST_CLASS_NAME,
        NO_RELATION_FOUND,
        NO_IMPLEMENTATION_FOUND,
        AMBIGUOUS_RELATIONS
    }

    private final MethodDeclaration testMethod;
    private final Optional<ResolvedMethodDeclaration> relatedMethod;
    private Type type;

    TestRelation(MethodDeclaration testMethod, Optional<ResolvedMethodDeclaration> relatedMethod, Type type) {
        this.testMethod = testMethod;
        this.relatedMethod = relatedMethod;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Optional<ResolvedMethodDeclaration> getRelatedMethod() {
        return relatedMethod;
    }

    public MethodDeclaration getTestMethod() {
        return testMethod;
    }

    public boolean isResolved() {
        return this.getType() == Type.MAPPED_BY_TEST_CLASS_NAME || this.getType() == Type.MAPPED_BY_TEST_METHOD_NAME;
    }
}

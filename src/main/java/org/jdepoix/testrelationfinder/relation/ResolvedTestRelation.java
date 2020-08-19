package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.util.Optional;

public class ResolvedTestRelation {
    public enum ResolutionStatus {
        NOT_RESOLVED,
        RESOLVED,
        UNRESOLVABLE
    }

    final private TestRelation testRelation;
    final private ResolutionStatus resolutionStatus;
    final private Optional<ResolvedMethodDeclaration> resolvedRelatedMethod;

    ResolvedTestRelation(TestRelation testRelation, ResolvedMethodDeclaration resolvedRelatedMethod) {
        this.testRelation = testRelation;
        this.resolutionStatus = ResolutionStatus.RESOLVED;
        this.resolvedRelatedMethod = Optional.of(resolvedRelatedMethod);
    }

    ResolvedTestRelation(TestRelation testRelation, ResolutionStatus resolutionStatus) {
        this.testRelation = testRelation;
        this.resolutionStatus = resolutionStatus;
        this.resolvedRelatedMethod = Optional.empty();
    }

    public TestRelation getTestRelation() {
        return testRelation;
    }

    public ResolutionStatus getResolutionStatus() {
        return resolutionStatus;
    }

    public Optional<ResolvedMethodDeclaration> getResolvedRelatedMethod() {
        return resolvedRelatedMethod;
    }
}

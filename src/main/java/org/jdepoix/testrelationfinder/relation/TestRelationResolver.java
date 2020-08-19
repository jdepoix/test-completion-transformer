package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.util.Optional;

public class TestRelationResolver {
    public ResolvedTestRelation resolve(TestRelation testRelation) {
        final Optional<MethodCallExpr> relatedMethod = testRelation.getRelatedMethod();
        if (relatedMethod.isEmpty()) {
            return new ResolvedTestRelation(testRelation, ResolvedTestRelation.ResolutionStatus.NOT_RESOLVED);
        }

        ResolvedMethodDeclaration resolvedRelatedMethod = null;
        try {
            resolvedRelatedMethod = relatedMethod.get().resolve();
        } catch (Exception e) {
            return new ResolvedTestRelation(testRelation, ResolvedTestRelation.ResolutionStatus.UNRESOLVABLE);
        }

        return new ResolvedTestRelation(testRelation, resolvedRelatedMethod);
    }
}

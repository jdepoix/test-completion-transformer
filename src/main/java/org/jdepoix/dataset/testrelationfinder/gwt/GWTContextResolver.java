package org.jdepoix.dataset.testrelationfinder.gwt;

import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.stream.Collectors;

public class GWTContextResolver {
    public ResolvedGWTTestRelation resolve(GWTTestRelation testRelation) {
        if (
            testRelation.getResolutionStatus() != GWTTestRelation.ResolutionStatus.RESOLVED
            || testRelation.getContext().get().isEmpty()
        ) {
            return new ResolvedGWTTestRelation(testRelation);
        }

        return new ResolvedGWTTestRelation(
            testRelation,
            testRelation.getContext().get().stream().distinct().map(this::resolveContext).collect(Collectors.toList())
        );
    }

    private GWTContext resolveContext(MethodCallExpr methodCall) {
        try {
            return new GWTContext(methodCall, methodCall.resolve());
        } catch (Exception e) {
            return new GWTContext(methodCall);
        }
    }
}

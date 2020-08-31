package org.jdepoix.testrelationfinder.gwt;

import com.github.javaparser.ast.expr.MethodCallExpr;
import org.jdepoix.testrelationfinder.relation.ResolvedTestRelation;

import java.util.List;
import java.util.Optional;

public class GWTTestRelation {
    public enum ResolutionStatus {
        NOT_RESOLVED,
        RESOLVED,
        MULTIPLE_WHENS_FOUND,
        NO_THEN_FOUND,
        NO_WHEN_FOUND,
        VIOLATES_SAP
    }

    private final ResolvedTestRelation resolvedTestRelation;
    private final ResolutionStatus resolutionStatus;
    private final Optional<String> given;
    private final Optional<String> then;
    private final Optional<List<MethodCallExpr>> context;

    GWTTestRelation(
        ResolvedTestRelation resolvedTestRelation,
        String given,
        String then,
        List<MethodCallExpr> context
    ) {
        this.resolvedTestRelation = resolvedTestRelation;
        this.resolutionStatus = ResolutionStatus.RESOLVED;
        this.given = Optional.of(given);
        this.then = Optional.of(then);
        this.context = Optional.of(context);
    }

    GWTTestRelation(ResolvedTestRelation resolvedTestRelation, ResolutionStatus resolutionStatus) {
        this.resolvedTestRelation = resolvedTestRelation;
        this.resolutionStatus = resolutionStatus;
        this.given = Optional.empty();
        this.then = Optional.empty();
        this.context = Optional.empty();
    }

    public ResolvedTestRelation getResolvedTestRelation() {
        return resolvedTestRelation;
    }

    public ResolutionStatus getResolutionStatus() {
        return resolutionStatus;
    }

    public Optional<String> getGiven() {
        return given;
    }

    public Optional<String> getThen() {
        return then;
    }

    Optional<List<MethodCallExpr>> getContext() {
        return context;
    }
}

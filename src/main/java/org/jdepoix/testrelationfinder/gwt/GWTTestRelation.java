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

    public enum WhenLocation {
        GIVEN,
        THEN,
        BOTH
    }

    private final ResolvedTestRelation resolvedTestRelation;
    private final ResolutionStatus resolutionStatus;
    private final Optional<String> given;
    private final Optional<String> then;
    private final Optional<WhenLocation> whenLocation;
    private final Optional<Integer> thenSectionStartIndex;
    private final Optional<List<MethodCallExpr>> context;

    GWTTestRelation(
        ResolvedTestRelation resolvedTestRelation,
        String given,
        String then,
        WhenLocation whenLocation,
        int thenSectionStartIndex,
        List<MethodCallExpr> context
    ) {
        this.resolvedTestRelation = resolvedTestRelation;
        this.resolutionStatus = ResolutionStatus.RESOLVED;
        this.given = Optional.of(given);
        this.then = Optional.of(then);
        this.whenLocation = Optional.of(whenLocation);
        this.thenSectionStartIndex = Optional.of(thenSectionStartIndex);
        this.context = Optional.of(context);
    }

    GWTTestRelation(ResolvedTestRelation resolvedTestRelation, ResolutionStatus resolutionStatus) {
        this.resolvedTestRelation = resolvedTestRelation;
        this.resolutionStatus = resolutionStatus;
        this.given = Optional.empty();
        this.then = Optional.empty();
        this.context = Optional.empty();
        this.whenLocation = Optional.empty();
        this.thenSectionStartIndex = Optional.empty();
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

    public Optional<WhenLocation> getWhenLocation() {
        return whenLocation;
    }

    public Optional<Integer> getThenSectionStartIndex() {
        return thenSectionStartIndex;
    }

    Optional<List<MethodCallExpr>> getContext() {
        return context;
    }


}

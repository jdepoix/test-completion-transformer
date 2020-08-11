package org.jdepoix.testrelationfinder.relation;

import java.util.Optional;

public class GivenWhenThenRelation {
    public enum ResolutionStatus {
        NOT_RESOLVED,
        RESOLVED,
        MULTIPLE_WHENS_FOUND,
        NO_THEN_FOUND
    }

    private final TestRelation testRelation;
    private final ResolutionStatus resolutionStatus;
    private final Optional<String> given;
    private final Optional<String> when;
    private final Optional<String> then;

    public GivenWhenThenRelation(TestRelation testRelation, String given, String when, String then) {
        this.testRelation = testRelation;
        this.resolutionStatus = ResolutionStatus.RESOLVED;
        this.given = Optional.of(given);
        this.when = Optional.of(when);
        this.then = Optional.of(then);
    }

    public GivenWhenThenRelation(TestRelation testRelation, ResolutionStatus resolutionStatus) {
        this.testRelation = testRelation;
        this.resolutionStatus = resolutionStatus;
        this.given = Optional.empty();
        this.when = Optional.empty();
        this.then = Optional.empty();
    }

    public TestRelation getTestRelation() {
        return testRelation;
    }

    public ResolutionStatus getResolutionStatus() {
        return resolutionStatus;
    }

    public Optional<String> getGiven() {
        return given;
    }

    public Optional<String> getWhen() {
        return when;
    }

    public Optional<String> getThen() {
        return then;
    }
}

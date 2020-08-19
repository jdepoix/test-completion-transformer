package org.jdepoix.testrelationfinder.gwt;

import java.util.List;

public class ResolvedGWTTestRelation {
    private final GWTTestRelation gwtTestRelation;
    private final List<GWTContext> context;

    public ResolvedGWTTestRelation(GWTTestRelation gwtTestRelation) {
        this(gwtTestRelation, List.of());
    }

    public ResolvedGWTTestRelation(GWTTestRelation gwtTestRelation, List<GWTContext> context) {
        this.gwtTestRelation = gwtTestRelation;
        this.context = context;
    }

    public GWTTestRelation getGwtTestRelation() {
        return gwtTestRelation;
    }

    public List<GWTContext> getContext() {
        return context;
    }
}

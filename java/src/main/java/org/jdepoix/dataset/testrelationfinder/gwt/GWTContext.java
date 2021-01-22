package org.jdepoix.dataset.testrelationfinder.gwt;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.util.Optional;

public class GWTContext {
    public enum ResolutionStatus {
        RESOLVED,
        UNRESOLVABLE
    }

    private final MethodCallExpr methodCall;
    private final Optional<ResolvedMethodDeclaration> resolvedMethod;
    private final ResolutionStatus resolutionStatus;

    GWTContext(MethodCallExpr methodCall) {
        this.methodCall = methodCall;
        this.resolvedMethod = Optional.empty();
        this.resolutionStatus = ResolutionStatus.UNRESOLVABLE;
    }

    GWTContext(MethodCallExpr methodCall, ResolvedMethodDeclaration resolvedMethod) {
        this.methodCall = methodCall;
        this.resolvedMethod = Optional.of(resolvedMethod);
        this.resolutionStatus = ResolutionStatus.RESOLVED;
    }

    public MethodCallExpr getMethodCall() {
        return methodCall;
    }

    public Optional<ResolvedMethodDeclaration> getResolvedMethod() {
        return resolvedMethod;
    }

    public ResolutionStatus getResolutionStatus() {
        return resolutionStatus;
    }
}

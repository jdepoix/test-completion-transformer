package org.jdepoix.testrelationfinder.reporting;

import org.jdepoix.testrelationfinder.gwt.GWTContext;

import java.nio.file.Path;
import java.util.Optional;

public class TestRelationContextReportEntry {
    private final GWTContext.ResolutionStatus resolutionStatus;
    private final String methodCall;
    private final String methodCallTokenRange;
    private final Optional<String> packageName;
    private final Optional<String> className;
    private final Optional<String> methodName;
    private final Optional<String> methodSignature;
    private final Optional<String> methodTokenRange;
    private final Optional<Path> path;

    public TestRelationContextReportEntry(
        String methodCall,
        String methodCallTokenRange,
        String packageName,
        String className,
        String methodName,
        String methodSignature,
        Optional<String> methodTokenRange,
        Optional<Path> path
    ) {
        this.resolutionStatus = GWTContext.ResolutionStatus.RESOLVED;
        this.methodCall = methodCall;
        this.methodCallTokenRange = methodCallTokenRange;
        this.packageName = Optional.of(packageName);
        this.className = Optional.of(className);
        this.methodName = Optional.of(methodName);
        this.methodSignature = Optional.of(methodSignature);
        this.methodTokenRange = methodTokenRange;
        this.path = path;
    }

    public TestRelationContextReportEntry(
        String methodCall,
        String methodCallTokenRange
    ) {
        this.resolutionStatus = GWTContext.ResolutionStatus.UNRESOLVABLE;
        this.methodCall = methodCall;
        this.methodCallTokenRange = methodCallTokenRange;
        this.packageName = Optional.empty();
        this.className = Optional.empty();
        this.methodName = Optional.empty();
        this.methodSignature = Optional.empty();
        this.methodTokenRange = Optional.empty();
        this.path = Optional.empty();
    }

    public GWTContext.ResolutionStatus getResolutionStatus() {
        return resolutionStatus;
    }

    public String getMethodCall() {
        return methodCall;
    }

    public Optional<String> getPackageName() {
        return packageName;
    }

    public Optional<String> getClassName() {
        return className;
    }

    public Optional<String> getMethodName() {
        return methodName;
    }

    public Optional<String> getMethodSignature() {
        return methodSignature;
    }

    public Optional<Path> getPath() {
        return path;
    }

    public String getMethodCallTokenRange() {
        return methodCallTokenRange;
    }

    public Optional<String> getMethodTokenRange() {
        return methodTokenRange;
    }
}

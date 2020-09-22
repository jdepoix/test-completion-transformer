package org.jdepoix.testrelationfinder.reporting;

import com.github.javaparser.Range;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import org.jdepoix.testrelationfinder.gwt.GWTContext;
import org.jdepoix.testrelationfinder.gwt.GWTTestRelation;
import org.jdepoix.testrelationfinder.gwt.ResolvedGWTTestRelation;
import org.jdepoix.testrelationfinder.relation.ResolvedTestRelation;
import org.jdepoix.testrelationfinder.relation.TestRelation;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReportCreator {
    public TestRelationReportEntry createReportEntry(
        String repoName,
        Path basePath,
        ResolvedGWTTestRelation resolvedGWTTestRelation
    ) {
        final GWTTestRelation gwtTestRelation = resolvedGWTTestRelation.getGwtTestRelation();
        final ResolvedTestRelation resolvedTestRelation = gwtTestRelation.getResolvedTestRelation();
        final TestRelation testRelation = resolvedTestRelation.getTestRelation();
        final MethodDeclaration testMethod = testRelation.getTestMethod();
        ResolvedMethodDeclaration resolvedTestMethod = testMethod.resolve();
        final Range testMethodTokenRange = testMethod.getRange().get();
        final Optional<ResolvedMethodDeclaration> resolvedRelatedMethod = resolvedTestRelation
            .getResolvedRelatedMethod();
        final Optional<MethodDeclaration> resolvedRelatedAst = resolvedRelatedMethod
            .map(resolvedMethodDeclaration -> resolvedMethodDeclaration.toAst().orElse(null));
        final Optional<Range> relatedMethodTokenRange = resolvedRelatedAst.map(
            methodDeclaration -> methodDeclaration.getRange().get()
        );

        return new TestRelationReportEntry(
            repoName,
            testRelation.getType(),
            resolvedTestRelation.getResolutionStatus(),
            gwtTestRelation.getResolutionStatus(),
            resolvedTestMethod.getPackageName(),
            resolvedTestMethod.getClassName(),
            resolvedTestMethod.getName(),
            testMethod.getDeclarationAsString(),
            this.parseTokenRange(testMethodTokenRange),
            this.resolveRelativeFilePath(basePath, testMethod.findCompilationUnit().get().getStorage().get().getPath()),
            resolvedRelatedMethod.map(ResolvedMethodLikeDeclaration::getPackageName),
            resolvedRelatedMethod.map(ResolvedMethodLikeDeclaration::getClassName),
            resolvedRelatedMethod.map(ResolvedMethodLikeDeclaration::getName),
            resolvedRelatedAst.map(MethodDeclaration::getDeclarationAsString),
            relatedMethodTokenRange.map(this::parseTokenRange),
            resolvedRelatedAst.map(
                methodDeclaration ->
                    this.resolveRelativeFilePath(
                        basePath,
                        methodDeclaration.findCompilationUnit().get().getStorage().get().getPath()
                    )
            ),
            gwtTestRelation.getGiven(),
            gwtTestRelation.getThen(),
            gwtTestRelation.getWhenLocation(),
            gwtTestRelation.getThenSectionStartIndex(),
            resolvedGWTTestRelation
                .getContext()
                .stream()
                .map(gwtContext -> this.createContextReportEntry(gwtContext, basePath))
                .collect(Collectors.toList())
        );
    }

    private Path resolveRelativeFilePath(Path basePath, Path filePath) {
        return basePath.toAbsolutePath().relativize(filePath.toAbsolutePath());
    }

    private TestRelationContextReportEntry createContextReportEntry(GWTContext context, Path basePath) {
        final Range methodCallRange = context.getMethodCall().getRange().get();
        if (context.getResolvedMethod().isEmpty()) {
            return new TestRelationContextReportEntry(
                context.getMethodCall().toString(),
                this.parseTokenRange(methodCallRange)
            );
        }

        final ResolvedMethodDeclaration resolvedMethod = context.getResolvedMethod().get();
        final Optional<MethodDeclaration> methodDeclarationOptional = resolvedMethod.toAst();
        if (methodDeclarationOptional.isEmpty() && !(resolvedMethod instanceof ReflectionMethodDeclaration)) {
            return new TestRelationContextReportEntry(
                context.getMethodCall().toString(),
                this.parseTokenRange(methodCallRange)
            );
        }

        final Optional<Range> methodDeclarationRange = methodDeclarationOptional.map(
            methodDeclaration -> methodDeclaration.getRange().get()
        );

        return new TestRelationContextReportEntry(
            context.getMethodCall().toString(),
            this.parseTokenRange(methodCallRange),
            resolvedMethod.getPackageName(),
            resolvedMethod.getClassName(),
            resolvedMethod.getName(),
            methodDeclarationOptional.map(MethodDeclaration::getDeclarationAsString).orElseGet(() -> {
                final String methodSignature = resolvedMethod
                    .toString()
                    .split("ReflectionMethodDeclaration\\{method=")[1];
                return methodSignature.substring(0, methodSignature.length() - 1);
            }),
            methodDeclarationRange.map(this::parseTokenRange),
            methodDeclarationOptional.map(methodDeclaration ->
                this.resolveRelativeFilePath(
                    basePath,
                    methodDeclaration.findCompilationUnit().get().getStorage().get().getPath()
                )
            )
        );
    }

    private String parseTokenRange(Range range) {
        return String.format(
            "%s:%s;%s:%s",
            range.begin.line,
            range.begin.column,
            range.end.line,
            range.end.column
        );
    }
}

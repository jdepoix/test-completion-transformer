package org.jdepoix.testrelationfinder.reporting;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
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
        final Optional<ResolvedMethodDeclaration> resolvedRelatedMethod = resolvedTestRelation
            .getResolvedRelatedMethod();
        final Optional<MethodDeclaration> resolvedRelatedAst = resolvedRelatedMethod
            .map(resolvedMethodDeclaration -> resolvedMethodDeclaration.toAst().get());

        return new TestRelationReportEntry(
            repoName,
            testRelation.getType(),
            resolvedTestRelation.getResolutionStatus(),
            gwtTestRelation.getResolutionStatus(),
            resolvedTestMethod.getPackageName(),
            resolvedTestMethod.getClassName(),
            resolvedTestMethod.getName(),
            testMethod.getDeclarationAsString(),
            this.resolveRelativeFilePath(basePath, testMethod.findCompilationUnit().get().getStorage().get().getPath()),
            resolvedRelatedMethod.map(ResolvedMethodLikeDeclaration::getPackageName),
            resolvedRelatedMethod.map(ResolvedMethodLikeDeclaration::getClassName),
            resolvedRelatedMethod.map(ResolvedMethodLikeDeclaration::getName),
            resolvedRelatedAst.map(MethodDeclaration::getDeclarationAsString),
            resolvedRelatedAst.map(
                methodDeclaration ->
                    this.resolveRelativeFilePath(
                        basePath,
                        methodDeclaration.findCompilationUnit().get().getStorage().get().getPath()
                    )
            ),
            gwtTestRelation.getGiven(),
            gwtTestRelation.getThen(),
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
        if (context.getResolvedMethod().isEmpty()) {
            return new TestRelationContextReportEntry(context.getMethodCall().toString());
        }

        final ResolvedMethodDeclaration resolvedMethod = context.getResolvedMethod().get();
        final MethodDeclaration methodDeclaration = resolvedMethod.toAst().get();
        return new TestRelationContextReportEntry(
            context.getMethodCall().toString(),
            resolvedMethod.getPackageName(),
            resolvedMethod.getClassName(),
            resolvedMethod.getName(),
            methodDeclaration.getDeclarationAsString(),
            this.resolveRelativeFilePath(
                basePath,
                methodDeclaration.findCompilationUnit().get().getStorage().get().getPath()
            )
        );
    }
}

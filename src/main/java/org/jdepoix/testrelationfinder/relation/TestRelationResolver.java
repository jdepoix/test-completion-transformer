package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.nio.file.Path;
import java.util.Optional;

public class TestRelationResolver {
    public ResolvedTestRelation resolve(String repoName, Path basePath, GivenWhenThenRelation gwtRelation) {
        final TestRelation testRelation = gwtRelation.getTestRelation();
        final Optional<MethodCallExpr> relatedMethod = testRelation.getRelatedMethod();
        if (relatedMethod.isPresent()) {
            try {
                final ResolvedMethodDeclaration resolvedRelatedMethod = relatedMethod.get().resolve();
                final MethodDeclaration resolveRelatedMethodAst = resolvedRelatedMethod.toAst().get();
                return build(
                    repoName,
                    basePath,
                    testRelation.getType(),
                    ResolvedTestRelation.ResolutionStatus.RESOLVED,
                    gwtRelation.getResolutionStatus(),
                    testRelation.getTestMethod(),
                    Optional.of(resolvedRelatedMethod.getPackageName()),
                    Optional.of(resolvedRelatedMethod.getClassName()),
                    Optional.of(resolvedRelatedMethod.getName()),
                    Optional.of(resolveRelatedMethodAst.getDeclarationAsString()),
                    Optional.of(resolveRelatedMethodAst.findCompilationUnit().get().getStorage().get().getPath()),
                    gwtRelation.getGiven(),
                    gwtRelation.getWhen(),
                    gwtRelation.getThen()
                );
            } catch (Exception e) {
                return build(
                    repoName,
                    basePath,
                    testRelation.getType(),
                    ResolvedTestRelation.ResolutionStatus.UNRESOLVABLE,
                    gwtRelation.getResolutionStatus(),
                    testRelation.getTestMethod()
                );
            }
        } else {
            return build(
                repoName,
                basePath,
                testRelation.getType(),
                ResolvedTestRelation.ResolutionStatus.NOT_RESOLVED,
                gwtRelation.getResolutionStatus(),
                testRelation.getTestMethod()
            );
        }
    }

    private ResolvedTestRelation build(
        String repoName,
        Path basePath,
        TestRelation.Type type,
        ResolvedTestRelation.ResolutionStatus resolutionStatus,
        GivenWhenThenRelation.ResolutionStatus gwtResolutionStatus,
        MethodDeclaration testMethod
    ) {
        return build(
            repoName,
            basePath,
            type,
            resolutionStatus,
            gwtResolutionStatus,
            testMethod,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }

    private ResolvedTestRelation build(
        String repoName,
        Path basePath,
        TestRelation.Type type,
        ResolvedTestRelation.ResolutionStatus resolutionStatus,
        GivenWhenThenRelation.ResolutionStatus gwtResolutionStatus,
        MethodDeclaration testMethod,
        Optional<String> relatedMethodPackageName,
        Optional<String> relatedMethodClassName,
        Optional<String> relatedMethodName,
        Optional<String> relatedMethodSignature,
        Optional<Path> relatedMethodPath,
        Optional<String> given,
        Optional<String> when,
        Optional<String> then
    ) {
        ResolvedMethodDeclaration resolvedTestMethod = testMethod.resolve();
        return new ResolvedTestRelation(
            repoName,
            type,
            resolutionStatus,
            gwtResolutionStatus,
            resolvedTestMethod.getPackageName(),
            resolvedTestMethod.getClassName(),
            resolvedTestMethod.getName(),
            testMethod.getDeclarationAsString(),
            this.resolveRelativeFilePath(
                basePath,
                Optional.of(testMethod.findCompilationUnit().get().getStorage().get().getPath())
            ).get(),
            relatedMethodPackageName,
            relatedMethodClassName,
            relatedMethodName,
            relatedMethodSignature,
            this.resolveRelativeFilePath(basePath, relatedMethodPath),
            given,
            when,
            then
        );
    }

    private Optional<Path> resolveRelativeFilePath(Path basePath, Optional<Path> filePath) {
        if (filePath.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(basePath.toAbsolutePath().relativize(filePath.get().toAbsolutePath()));
    }
}

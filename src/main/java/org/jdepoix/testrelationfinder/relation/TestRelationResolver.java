package org.jdepoix.testrelationfinder.relation;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class TestRelationResolver {
    private final HashMap<Path, String> fileCache = new HashMap<>();

    public ResolvedTestRelation resolve(String repoName, Path basePath, TestRelation testRelation) {
        final Optional<MethodCallExpr> relatedMethod = testRelation.getRelatedMethod();

        if (relatedMethod.isPresent()) {
            try {
                final ResolvedMethodDeclaration resolvedRelatedMethod = relatedMethod.get().resolve();
                return build(
                    repoName,
                    basePath,

                    testRelation.getType(),
                    ResolvedTestRelation.ResolutionStatus.RESOLVED,
                    testRelation.getTestMethod(),
                    Optional.of(resolvedRelatedMethod.getPackageName()),
                    Optional.of(resolvedRelatedMethod.getClassName()),
                    Optional.of(resolvedRelatedMethod.getName()),
                    Optional.of(
                        resolvedRelatedMethod.toAst().get().findCompilationUnit().get().getStorage().get().getPath()
                    )
                );
            } catch (Exception e) {
                return build(
                    repoName,
                    basePath,
                    testRelation.getType(),
                    ResolvedTestRelation.ResolutionStatus.UNRESOLVABLE,
                    testRelation.getTestMethod()
                );
            }
        } else {
            return build(
                repoName,
                basePath,
                TestRelation.Type.NO_RELATION_FOUND,
                ResolvedTestRelation.ResolutionStatus.NOT_RESOLVED,
                testRelation.getTestMethod()
            );
        }
    }

    private ResolvedTestRelation build(
        String repoName,
        Path basePath,
        TestRelation.Type type,
        ResolvedTestRelation.ResolutionStatus resolutionStatus,
        MethodDeclaration testMethod
    ) {
        return build(
            repoName,
            basePath,
            type,
            resolutionStatus,
            testMethod,
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
        MethodDeclaration testMethod,
        Optional<String> relatedMethodPackageName,
        Optional<String> relatedMethodClassName,
        Optional<String> relatedMethodName,
        Optional<Path> relatedMethodPath
    ) {
        ResolvedMethodDeclaration resolvedTestMethod = testMethod.resolve();
        String testMethodPackageName = resolvedTestMethod.getPackageName();
        String testMethodClassName = resolvedTestMethod.getClassName();
        String testMethodName = resolvedTestMethod.getName();
        Path testPath = testMethod.findCompilationUnit().get().getStorage().get().getPath();
        String testFileContent = null;
        String relatedMethodFileContent = null;
        if (relatedMethodPath.isPresent()) {
            testFileContent = this.getFileContent(testPath);
            relatedMethodFileContent = this.getFileContent(relatedMethodPath.get());
        }
        return new ResolvedTestRelation(
            repoName,
            type,
            resolutionStatus,
            testMethodPackageName,
            testMethodClassName,
            testMethodName,
            this.resolveRelativeFilePath(basePath, Optional.of(testPath)).get(),
            Optional.ofNullable(testFileContent),
            relatedMethodPackageName,
            relatedMethodClassName,
            relatedMethodName,
            this.resolveRelativeFilePath(basePath, relatedMethodPath),
            Optional.ofNullable(relatedMethodFileContent)
        );
    }

    private Optional<Path> resolveRelativeFilePath(Path basePath, Optional<Path> filePath) {
        if (filePath.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(basePath.toAbsolutePath().relativize(filePath.get().toAbsolutePath()));
    }

    private String getFileContent(Path filePath) {
        String cacheEntry = this.fileCache.get(filePath);
        if (cacheEntry == null) {
            try {
                cacheEntry = Files.readString(filePath);
            } catch (IOException e) {
                cacheEntry = null;
            }
            this.fileCache.put(filePath, cacheEntry);
        }
        return cacheEntry;
    }
}

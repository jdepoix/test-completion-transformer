package org.jdepoix.dataset.creator.gwt;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;
import org.jdepoix.dataset.ast.AstUtils;
import org.jdepoix.dataset.ast.DefaultParser;
import org.jdepoix.dataset.ast.TestDeclarationCreator;
import org.jdepoix.dataset.ast.ThenSectionExtractor;
import org.jdepoix.dataset.ast.node.*;
import org.jdepoix.dataset.ast.serialization.ASTSequentializer;
import org.jdepoix.dataset.ast.serialization.ASTSerializer;
import org.jdepoix.dataset.ast.serialization.ASTToken;
import org.jdepoix.dataset.config.ResultDirConfig;
import org.jdepoix.dataset.creator.DatapointCreator;
import org.jdepoix.dataset.testrelationfinder.reporting.TestRelationReportEntry;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GWTDatapointCreator implements DatapointCreator<GWTDatapoint> {
    private final ResultDirConfig config;
    private final TestDeclarationCreator testDeclarationCreator;
    private final ThenSectionExtractor thenSectionExtractor;
    private final ASTSerializer astSerializer;
    private final ASTSequentializer astSequentializer;

    public GWTDatapointCreator(
        ResultDirConfig config,
        TestDeclarationCreator testDeclarationCreator,
        ThenSectionExtractor thenSectionExtractor,
        ASTSerializer astSerializer,
        ASTSequentializer astSequentializer
    ) {
        this.config = config;
        this.testDeclarationCreator = testDeclarationCreator;
        this.thenSectionExtractor = thenSectionExtractor;
        this.astSerializer = astSerializer;
        this.astSequentializer = astSequentializer;
    }

    public GWTDatapoint create(TestRelationReportEntry entry)
        throws IOException, NoSuchFieldException, IllegalAccessException, AstUtils.Unresolvable {
        final MethodDeclaration testMethod = parseTestMethod(entry);
        final MethodDeclaration relatedMethod = parseRelatedMethod(entry);

        final NodeList<Statement> thenSection = this.thenSectionExtractor.extract(
            testMethod,
            entry.getThenSectionStartIndex().get()
        );

        final TestDeclaration testDeclaration = this.testDeclarationCreator.create(testMethod, relatedMethod);

        final GWTDatapoint datapoint = new GWTDatapoint(
            entry.getId(),
            testDeclaration,
            astSequentializer.sequentialize(astSerializer.serialize(testDeclaration)),
            sequentializeThen(thenSection),
            thenSection.stream().map(Statement::toString).collect(Collectors.joining("\n")),
            testDeclaration.getTestContextMethodDeclarations().size(),
            testDeclaration.getContextMethodDeclarations().size(),
            entry.getWhenLocation().get()
        );

        return datapoint;
    }

    private List<ASTToken> sequentializeThen(List<Statement> statements)
        throws NoSuchFieldException, IllegalAccessException
    {
        List<ASTToken> sequentializedThen = new ArrayList<>();
        for (Statement statement : statements) {
            sequentializedThen.addAll(astSequentializer.sequentialize(astSerializer.serialize(statement)));
        }
        return sequentializedThen;
    }

    private MethodDeclaration parseRelatedMethod(
        TestRelationReportEntry entry
    ) throws IOException, AstUtils.Unresolvable {
        return AstUtils.findMethodDeclarationBySignature(
            DefaultParser.parse(config.resolveRepoFile(entry.getRepoName(), entry.getRelatedMethodPath().get())),
            entry.getRelatedMethodClassName().get(),
            entry.getRelatedMethodSignature().get()
        );
    }

    private MethodDeclaration parseTestMethod(
        TestRelationReportEntry entry
    ) throws IOException, AstUtils.Unresolvable {
        return AstUtils.findMethodDeclarationBySignature(
            DefaultParser.parse(config.resolveRepoFile(entry.getRepoName(), entry.getTestPath())),
            entry.getTestMethodClassName(),
            entry.getTestMethodSignature()
        );
    }
}

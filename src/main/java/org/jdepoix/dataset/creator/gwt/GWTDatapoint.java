package org.jdepoix.dataset.creator.gwt;

import com.github.javaparser.ast.body.MethodDeclaration;
import org.apache.commons.text.StringEscapeUtils;
import org.jdepoix.dataset.ast.node.TestDeclaration;
import org.jdepoix.dataset.ast.serialization.ASTToken;
import org.jdepoix.dataset.creator.Datapoint;
import org.jdepoix.dataset.testrelationfinder.gwt.GWTTestRelation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GWTDatapoint implements Datapoint {
    private final String testRelationId;
    private final TestDeclaration testDeclaration;
    private final List<ASTToken> sourceTokens;
    private final List<ASTToken> targetTokens;
    private final String targetCode;
    private final int testContextDeclarationCount;
    private final int contextDeclarationCount;
    private final GWTTestRelation.WhenLocation whenLocation;

    public GWTDatapoint(
        String testRelationId,
        TestDeclaration testDeclaration,
        List<ASTToken> sourceTokens,
        List<ASTToken> targetTokens,
        String targetCode,
        int testContextDeclarationCount,
        int contextDeclarationCount,
        GWTTestRelation.WhenLocation whenLocation
    ) {
        this.testRelationId = testRelationId;
        this.testDeclaration = testDeclaration;
        this.sourceTokens = sourceTokens;
        this.targetTokens = targetTokens;
        this.targetCode = targetCode;
        this.testContextDeclarationCount = testContextDeclarationCount;
        this.contextDeclarationCount = contextDeclarationCount;
        this.whenLocation = whenLocation;
    }

    @Override
    public String toJSON() {
        return String.format(
            "{" +
                "\"id\":\"%s\"," +
                "\"testDec\":%s," +
                "\"src\":%s," +
                "\"trgTok\":%s," +
                "\"trgCode\":\"%s\"," +
                "\"testCtxCount\":%d," +
                "\"ctxCount\":%d," +
                "\"whenLocation\":\"%s\"" +
            "}",
            this.testRelationId,
            this.testDeclarationToJSON(),
            this.createJSONArray(this.sourceTokens.stream().map(ASTToken::toJSON)),
            this.createJSONArray(this.targetTokens.stream().map(ASTToken::toJSON)),
            StringEscapeUtils.escapeJson(this.targetCode),
            this.testContextDeclarationCount,
            this.contextDeclarationCount,
            this.whenLocation.toString()
        );
    }

    private String testDeclarationToJSON() {
        return String.format(
            "{" +
                "\"name\":\"%s\"," +
                "\"body\":\"%s\"," +
                "\"testCtx\":%s," +
                "\"when\":\"%s\"," +
                "\"ctx\":%s" +
            "}",
            StringEscapeUtils.escapeJson(testDeclaration.getNameAsString()),
            StringEscapeUtils.escapeJson(testDeclaration.getTestBody().toString()),
            createJSONArray(stringSerializeMethodDeclarations(testDeclaration.getTestContextMethodDeclarations())),
            StringEscapeUtils.escapeJson(testDeclaration.getWhenMethodDeclaration().toString()),
            createJSONArray(stringSerializeMethodDeclarations(testDeclaration.getContextMethodDeclarations()))
        );
    }

    private Stream<String> stringSerializeMethodDeclarations(List<? extends MethodDeclaration> methodDeclarations) {
        return methodDeclarations
            .stream()
            .map(MethodDeclaration::toString)
            .map(StringEscapeUtils::escapeJson);
    }

    private String createJSONArray(Stream<String> stringStream) {
        final String joinedString = stringStream.collect(Collectors.joining("\",\""));
        if (joinedString.equals("")) {
            return "[]";
        }
        return String.format(
            "[\"%s\"]",
            joinedString
        );
    }

    public GWTTestRelation.WhenLocation getWhenLocation() {
        return whenLocation;
    }
}

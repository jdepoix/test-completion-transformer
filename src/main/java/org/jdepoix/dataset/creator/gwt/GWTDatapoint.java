package org.jdepoix.dataset.creator.gwt;

import org.apache.commons.text.StringEscapeUtils;
import org.jdepoix.dataset.ast.serialization.ASTToken;
import org.jdepoix.dataset.creator.Datapoint;
import org.jdepoix.dataset.testrelationfinder.gwt.GWTTestRelation;

import java.util.List;
import java.util.stream.Collectors;

public class GWTDatapoint implements Datapoint {
    private final String testRelationId;
    private final List<ASTToken> sourceTokens;
    private final List<ASTToken> targetTokens;
    private final String targetCode;
    private final int testContextDeclarationCount;
    private final int contextDeclarationCount;
    private final GWTTestRelation.WhenLocation whenLocation;

    public GWTDatapoint(
        String testRelationId,
        List<ASTToken> sourceTokens,
        List<ASTToken> targetTokens,
        String targetCode,
        int testContextDeclarationCount,
        int contextDeclarationCount,
        GWTTestRelation.WhenLocation whenLocation
    ) {
        this.testRelationId = testRelationId;
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
                "\"src\":%s," +
                "\"trgTok\":%s," +
                "\"trgCode\":\"%s\"," +
                "\"testCtxCount\":%d," +
                "\"ctxCount\":%d," +
                "\"whenLocation\":\"%s\"" +
            "}",
            this.testRelationId,
            this.createJSONArray(this.sourceTokens),
            this.createJSONArray(this.targetTokens),
            StringEscapeUtils.escapeJson(this.targetCode),
            this.testContextDeclarationCount,
            this.contextDeclarationCount,
            this.whenLocation.toString()
        );
    }

    private String createJSONArray(List<ASTToken> tokens) {
        if (tokens.isEmpty()) {
            return "[]";
        }
        return String.format(
            "[\"%s\"]",
            tokens.stream().map(ASTToken::toJSON).collect(Collectors.joining("\",\""))
        );
    }

    public GWTTestRelation.WhenLocation getWhenLocation() {
        return whenLocation;
    }
}

package org.jdepoix.datasetcreator.gwt;

import org.apache.commons.text.StringEscapeUtils;
import org.jdepoix.ast.serialization.ASTToken;
import org.jdepoix.datasetcreator.Datapoint;

import java.util.List;
import java.util.stream.Collectors;

public class GWTDatapoint implements Datapoint {
    private final String testRelationId;
    private final List<ASTToken> sourceTokens;
    private final List<ASTToken> targetTokens;
    private final String targetCode;
    private final int testContextDeclarationCount;
    private final int contextDeclarationCount;

    public GWTDatapoint(
        String testRelationId,
        List<ASTToken> sourceTokens,
        List<ASTToken> targetTokens,
        String targetCode,
        int testContextDeclarationCount,
        int contextDeclarationCount
    ) {
        this.testRelationId = testRelationId;
        this.sourceTokens = sourceTokens;
        this.targetTokens = targetTokens;
        this.targetCode = targetCode;
        this.testContextDeclarationCount = testContextDeclarationCount;
        this.contextDeclarationCount = contextDeclarationCount;
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
                "\"ctxCount\":%d" +
            "}",
            this.testRelationId,
            this.createJSONArray(this.sourceTokens),
            this.createJSONArray(this.targetTokens),
            StringEscapeUtils.escapeJson(this.targetCode),
            this.testContextDeclarationCount,
            this.contextDeclarationCount
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
}

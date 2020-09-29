package org.jdepoix.dataset.ast.serialization;

import org.apache.commons.text.StringEscapeUtils;

public class ASTToken {
    private final String token;
    private final boolean isValue;

    public ASTToken(String token, boolean isValue) {
        this.token = token;
        this.isValue = isValue;
    }

    public String getToken() {
        return token;
    }

    public boolean isValue() {
        return isValue;
    }

    @Override
    public String toString() {
        return this.token;
    }

    public String toJSON() {
        return this.isValue ? StringEscapeUtils.escapeJson(this.token) : this.token;
    }
}

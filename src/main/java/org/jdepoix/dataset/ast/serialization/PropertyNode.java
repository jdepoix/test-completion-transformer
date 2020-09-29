package org.jdepoix.dataset.ast.serialization;

import java.util.List;

class PropertyNode extends NodeWithChildren {
    private final String property;

    PropertyNode(String type, List<? extends SerializedASTNode> childNodes, String property) {
        super(type, childNodes);
        this.property = property;
    }

    @Override
    public String toString() {
        return String.format(".%s:%s", this.getProperty(), this.getType());
    }

    public String getProperty() {
        return property;
    }
}

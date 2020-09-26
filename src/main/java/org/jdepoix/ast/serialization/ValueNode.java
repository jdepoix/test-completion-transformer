package org.jdepoix.ast.serialization;

class ValueNode extends SerializedASTNode {
    private final String value;

    ValueNode(String type, String value) {
        super(type);
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("%s", this.getValue());
    }

    public String getValue() {
        return value;
    }
}

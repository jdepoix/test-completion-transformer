package org.jdepoix.ast.serialization;

abstract class SerializedASTNode {
    private final String type;

    SerializedASTNode(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }

    public String getType() {
        return type;
    }
}

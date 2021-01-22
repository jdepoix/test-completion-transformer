package org.jdepoix.dataset.ast.serialization;

public class SerializedAST {
    static class MetaModelNotFound extends Exception {
        public MetaModelNotFound(String message) {
            super(message);
        }
    }

    private final TypeNode root;

    SerializedAST(TypeNode root) {
        this.root = root;
    }

    TypeNode getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return this.root.toTreeString(0);
    }
}

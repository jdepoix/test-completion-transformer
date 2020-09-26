package org.jdepoix.ast.serialization;

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

    public String printTree() {
        return this.root.printTree(0);
    }

    TypeNode getRoot() {
        return root;
    }
}

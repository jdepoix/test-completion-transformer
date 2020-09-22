package org.jdepoix.ast.serialization;

import com.github.javaparser.ast.Node;

import java.lang.reflect.InvocationTargetException;

public class AST {
    static class MetaModelNotFound extends Exception {
        public MetaModelNotFound(String message) {
            super(message);
        }
    }

    private final TypeNode root;

    private AST(TypeNode root) {
        this.root = root;
    }

    public static AST serialize(Node rootNode) throws NoSuchFieldException, IllegalAccessException {
        return new AST(
            new TypeNode(
                // TODO use getCustomMetaModel
                rootNode.getMetaModel().getType().getName(),
                ASTNode.serialize(rootNode)
            )
        );
    }

    public static Node deserialize(AST ast) throws ClassNotFoundException, MetaModelNotFound, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return (Node) ASTNode.deserialize(ast.root);
    }

    public String printTree() {
        return this.root.printTree(0);
    }
}

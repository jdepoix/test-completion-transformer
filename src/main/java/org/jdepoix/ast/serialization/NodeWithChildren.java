package org.jdepoix.ast.serialization;

import java.util.List;
import java.util.stream.Collectors;

abstract class NodeWithChildren extends SerializedASTNode {
    private final List<? extends SerializedASTNode> childNodes;

    NodeWithChildren(String type, List<? extends SerializedASTNode> childNodes) {
        super(type);
        this.childNodes = childNodes;
    }

    String printTree(int depth) {
        return String.format(
            "%s%s\n%s",
            "\t".repeat(depth),
            this.toString(),
            this.getChildNodes()
                .stream()
                .map(
                    astNode ->
                        astNode instanceof NodeWithChildren
                            ? ((NodeWithChildren) astNode).printTree(depth + 1)
                            : String.format("%s%s", "\t".repeat(depth + 1), astNode.toString())
                )
                .collect(Collectors.joining("\n"))
        );
    }

    public List<? extends SerializedASTNode> getChildNodes() {
        return childNodes;
    }
}

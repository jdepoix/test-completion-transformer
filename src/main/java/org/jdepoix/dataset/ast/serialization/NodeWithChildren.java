package org.jdepoix.dataset.ast.serialization;

import java.util.List;
import java.util.stream.Collectors;

abstract class NodeWithChildren extends SerializedASTNode {
    private List<SerializedASTNode> childNodes;

    NodeWithChildren(String type, List<SerializedASTNode> childNodes) {
        super(type);
        this.childNodes = childNodes;
    }

    String toTreeString(int depth) {
        return String.format(
            "%s%s\n%s",
            "\t".repeat(depth),
            this.toString(),
            this.getChildNodes()
                .stream()
                .map(
                    astNode ->
                        astNode instanceof NodeWithChildren
                            ? ((NodeWithChildren) astNode).toTreeString(depth + 1)
                            : String.format("%s%s", "\t".repeat(depth + 1), astNode.toString())
                )
                .collect(Collectors.joining("\n"))
        );
    }

    public List<? extends SerializedASTNode> getChildNodes() {
        return childNodes;
    }

    public <T extends SerializedASTNode> void addChildNode(T node) {
        this.childNodes.add(node);
    }
}

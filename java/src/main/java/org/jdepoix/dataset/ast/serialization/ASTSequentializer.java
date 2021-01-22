package org.jdepoix.dataset.ast.serialization;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ASTSequentializer {
    public List<ASTToken> sequentialize(SerializedAST ast) {
        return this.sequentializeNode(ast.getRoot()).collect(Collectors.toList());
    }

    private Stream<ASTToken> sequentializeNode(SerializedASTNode node) {
        final String nodeLabel = node.toString();
        if (node instanceof ValueNode) {
            return Stream.of(new ASTToken(nodeLabel, true));
        }
        if (node instanceof TerminalNode) {
            return Stream.of(new ASTToken(String.format("<[%s]><[/]>", nodeLabel), false));
        }
        final List<? extends SerializedASTNode> childNodes = ((NodeWithChildren) node).getChildNodes();
        if (node instanceof TypeNode && childNodes.size() == 1 && childNodes.get(0) instanceof TerminalNode) {
            return Stream.of(new ASTToken(String.format("<[%s]><[/]>", nodeLabel), false));
        }
        return Stream.concat(
            Stream.concat(
                Stream.of(new ASTToken(String.format("<[%s]>", nodeLabel), false)),
                childNodes.stream().flatMap(this::sequentializeNode)
            ),
            Stream.of(new ASTToken(String.format("<[/%s]>", nodeLabel), false))
        );
    }
}

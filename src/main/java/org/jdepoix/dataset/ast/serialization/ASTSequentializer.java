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
        return Stream.concat(
            Stream.concat(
                Stream.of(new ASTToken(String.format("<[%s]>", nodeLabel), false)),
                ((NodeWithChildren) node).getChildNodes().stream().flatMap(this::sequentializeNode)
            ),
            Stream.of(new ASTToken(String.format("<[/%s]>", nodeLabel), false))
        );
    }
}

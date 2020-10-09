package org.jdepoix.dataset.ast.serialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ASTDesequentializer {
    class NotDesequentializable extends Exception {
        public NotDesequentializable() {
            super("Desequentialization failed");
        }

        public NotDesequentializable(String item) {
            super("Desequentialization failed on token: %s".format(item));
        }
    }

    public SerializedAST desequentialize(List<String> sequence) throws NotDesequentializable {
        final Stack<NodeWithChildren> parents = new Stack<>();
        TypeNode root = null;

        for (String item : sequence) {
            if (!item.startsWith("<[") || !item.endsWith("]>")) {
                if (parents.empty()) throw new NotDesequentializable(item);

                final NodeWithChildren parent = parents.peek();
                parent.addChildNode(new ValueNode(parent.getType(), item));
            } else if (item.endsWith("]><[/]>")) {
                if (parents.empty()) throw new NotDesequentializable(item);

                parents.peek().addChildNode(new TerminalNode(item.substring(2, item.length() - 7)));
            } else if (item.charAt(2) == '/') {
                if (parents.empty() || !parents.pop().toString().equals(item.substring(3, item.length() - 2))) {
                    throw new NotDesequentializable(item);
                }
            } else {
                final String[] itemValues = item.substring(2, item.length() - 2).split(":");
                NodeWithChildren node = null;
                if (itemValues.length == 1) {
                    node = new TypeNode(itemValues[0], new ArrayList<>());
                } else if (itemValues.length == 2) {
                    node = new PropertyNode(itemValues[1], new ArrayList<>(), itemValues[0].substring(1));
                } else {
                    throw new NotDesequentializable(item);
                }

                if (parents.empty() && root == null) {
                    if (!(node instanceof TypeNode)) {
                        throw new NotDesequentializable(item);
                    }
                    root = (TypeNode) node;
                } else {
                    parents.peek().addChildNode(node);
                }
                parents.add(node);
            }
        }

        if (root == null) {
            throw new NotDesequentializable();
        }

        return new SerializedAST(root);
    }
}

package org.jdepoix.ast.serialization;

import java.util.List;

class TypeNode extends NodeWithChildren {
    TypeNode(String type, List<? extends SerializedASTNode> childNodes) {
        super(type, childNodes);
    }
}

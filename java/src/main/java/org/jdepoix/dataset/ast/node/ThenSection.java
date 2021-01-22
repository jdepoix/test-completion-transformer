package org.jdepoix.dataset.ast.node;

import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.metamodel.NodeMetaModel;
import org.jdepoix.dataset.ast.metamodel.ThenSectionMetaModel;

public class ThenSection extends UnparsableStmt implements CustomNode {
    public static final ThenSectionMetaModel metaModel = ThenSectionMetaModel.registerMetaModel();

    @AllFieldsConstructor
    public ThenSection() {
        super();
    }

    @Override
    public NodeMetaModel getCustomMetaModel() {
        return this.metaModel;
    }
}

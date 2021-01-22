package org.jdepoix.dataset.ast.node;

import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.metamodel.NodeMetaModel;
import org.jdepoix.dataset.ast.metamodel.TestBodyMetaModel;

public class TestBody extends BlockStmt implements CustomNode {
    public static final TestBodyMetaModel metaModel = TestBodyMetaModel.registerMetaModel();

    @AllFieldsConstructor
    public TestBody(final NodeList<Statement> statements) {
        super(null, statements);
    }

    @Override
    public NodeMetaModel getCustomMetaModel() {
        return this.metaModel;
    }
}

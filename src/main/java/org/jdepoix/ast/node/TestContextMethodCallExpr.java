package org.jdepoix.ast.node;

import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.metamodel.NodeMetaModel;
import org.jdepoix.ast.metamodel.TestContextMethodCallExprMetaModel;

public class TestContextMethodCallExpr extends MethodCallExpr implements CustomNode {
    public static final TestContextMethodCallExprMetaModel metaModel
        = TestContextMethodCallExprMetaModel.registerMetaModel();

    @AllFieldsConstructor
    public TestContextMethodCallExpr(
        final Expression scope,
        final NodeList<Type> typeArguments,
        final SimpleName name,
        final NodeList<Expression> arguments
    ) {
        super(null, scope, typeArguments, name, arguments);
    }

    @Override
    public NodeMetaModel getCustomMetaModel() {
        return this.metaModel;
    }

    public static TestContextMethodCallExpr fromMethodCallExpr(MethodCallExpr methodCall) {
        return new TestContextMethodCallExpr(
            methodCall.getScope().orElse(null),
            methodCall.getTypeArguments().orElse(null),
            methodCall.getName(),
            methodCall.getArguments()
        );
    }
}

package org.jdepoix.ast.node;

import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.metamodel.NodeMetaModel;
import org.jdepoix.ast.metamodel.ContextMethodCallExprMetaModel;

public class ContextMethodCallExpr extends MethodCallExpr implements CustomNode {
    public static final ContextMethodCallExprMetaModel metaModel = ContextMethodCallExprMetaModel.registerMetaModel();

    @AllFieldsConstructor
    public ContextMethodCallExpr(
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

    public static ContextMethodCallExpr fromMethodCallExpr(MethodCallExpr methodCall) {
        return new ContextMethodCallExpr(
            methodCall.getScope().orElse(null),
            methodCall.getTypeArguments().orElse(null),
            methodCall.getName(),
            methodCall.getArguments()
        );
    }
}

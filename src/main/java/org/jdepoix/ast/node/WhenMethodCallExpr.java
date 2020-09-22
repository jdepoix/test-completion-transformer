package org.jdepoix.ast.node;

import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.metamodel.NodeMetaModel;
import org.jdepoix.ast.metamodel.WhenMethodCallExprMetaModel;

public class WhenMethodCallExpr extends MethodCallExpr implements CustomNode {
    public static final WhenMethodCallExprMetaModel metaModel = WhenMethodCallExprMetaModel.registerMetaModel();

    @AllFieldsConstructor
    public WhenMethodCallExpr(
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
}

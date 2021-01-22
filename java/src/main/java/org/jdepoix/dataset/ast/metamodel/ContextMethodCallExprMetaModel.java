package org.jdepoix.dataset.ast.metamodel;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.ExpressionMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import org.jdepoix.dataset.ast.node.ContextMethodCallExpr;

import java.util.Optional;

public class ContextMethodCallExprMetaModel extends ExpressionMetaModel {
    public PropertyMetaModel argumentsPropertyMetaModel;
    public PropertyMetaModel namePropertyMetaModel;
    public PropertyMetaModel scopePropertyMetaModel;
    public PropertyMetaModel typeArgumentsPropertyMetaModel;
    public PropertyMetaModel usingDiamondOperatorPropertyMetaModel;

    ContextMethodCallExprMetaModel(Optional<BaseNodeMetaModel> superBaseNodeMetaModel) {
        super(
            superBaseNodeMetaModel,
            ContextMethodCallExpr.class,
            "ContextMethodCallExpr",
            "org.jdepoix.dataset.ast.node",
            false,
            false
        );
    }

    public static ContextMethodCallExprMetaModel registerMetaModel() {
        final ContextMethodCallExprMetaModel metaModel = new ContextMethodCallExprMetaModel(Optional.of(JavaParserMetaModel.expressionMetaModel));
        JavaParserMetaModel.getNodeMetaModels().add(metaModel);

        metaModel.getDeclaredPropertyMetaModels().add(metaModel.argumentsPropertyMetaModel);
        metaModel.argumentsPropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "arguments",
            Expression.class,
            Optional.of(JavaParserMetaModel.expressionMetaModel),
            false,
            false,
            true,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.argumentsPropertyMetaModel);
        metaModel.namePropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "name",
            SimpleName.class,
            Optional.of(JavaParserMetaModel.simpleNameMetaModel),
            false,
            false,
            false,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.namePropertyMetaModel);
        metaModel.scopePropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "scope",
            Expression.class,
            Optional.of(JavaParserMetaModel.expressionMetaModel),
            true,
            false,
            false,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.scopePropertyMetaModel);
        metaModel.typeArgumentsPropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "typeArguments",
            Type.class,
            Optional.of(JavaParserMetaModel.typeMetaModel),
            true,
            false,
            true,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.typeArgumentsPropertyMetaModel);
        metaModel.usingDiamondOperatorPropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "usingDiamondOperator",
            Boolean.TYPE,
            Optional.empty(),
            false,
            false,
            false,
            false
        );
        metaModel.getDerivedPropertyMetaModels().add(metaModel.usingDiamondOperatorPropertyMetaModel);

        metaModel.getConstructorParameters().add(metaModel.scopePropertyMetaModel);
        metaModel.getConstructorParameters().add(metaModel.typeArgumentsPropertyMetaModel);
        metaModel.getConstructorParameters().add(metaModel.namePropertyMetaModel);
        metaModel.getConstructorParameters().add(metaModel.argumentsPropertyMetaModel);

        return metaModel;
    }
}

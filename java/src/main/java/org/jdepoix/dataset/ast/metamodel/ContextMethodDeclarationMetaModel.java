package org.jdepoix.dataset.ast.metamodel;

import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.CallableDeclarationMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import org.jdepoix.dataset.ast.node.ContextMethodDeclaration;

import java.util.Optional;

public class ContextMethodDeclarationMetaModel extends CallableDeclarationMetaModel {
    public PropertyMetaModel bodyPropertyMetaModel;
    public PropertyMetaModel typePropertyMetaModel;

    ContextMethodDeclarationMetaModel(Optional<BaseNodeMetaModel> superBaseNodeMetaModel) {
        super(
            superBaseNodeMetaModel,
            ContextMethodDeclaration.class,
            "ContextMethodDeclaration",
            "org.jdepoix.dataset.ast.node",
            false,
            false
        );
    }

    public static ContextMethodDeclarationMetaModel registerMetaModel() {
        ContextMethodDeclarationMetaModel metaModel = new ContextMethodDeclarationMetaModel(
            Optional.of(JavaParserMetaModel.callableDeclarationMetaModel)
        );
        JavaParserMetaModel.getNodeMetaModels().add(metaModel);

        metaModel.bodyPropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "body",
            BlockStmt.class,
            Optional.of(JavaParserMetaModel.blockStmtMetaModel),
            true,
            false,
            false,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.bodyPropertyMetaModel);
        metaModel.typePropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "type",
            Type.class,
            Optional.of(JavaParserMetaModel.typeMetaModel),
            false,
            false,
            false,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.typePropertyMetaModel);

        metaModel.getConstructorParameters().add(
            JavaParserMetaModel.callableDeclarationMetaModel.modifiersPropertyMetaModel
        );
        metaModel.getConstructorParameters().add(
            JavaParserMetaModel.bodyDeclarationMetaModel.annotationsPropertyMetaModel
        );
        metaModel.getConstructorParameters().add(
            JavaParserMetaModel.callableDeclarationMetaModel.typeParametersPropertyMetaModel
        );
        metaModel.getConstructorParameters().add(
            metaModel.typePropertyMetaModel
        );
        metaModel.getConstructorParameters().add(
            JavaParserMetaModel.callableDeclarationMetaModel.namePropertyMetaModel
        );
        metaModel.getConstructorParameters().add(
            JavaParserMetaModel.callableDeclarationMetaModel.parametersPropertyMetaModel
        );
        metaModel.getConstructorParameters().add(
            JavaParserMetaModel.callableDeclarationMetaModel.thrownExceptionsPropertyMetaModel
        );
        metaModel.getConstructorParameters().add(
            metaModel.bodyPropertyMetaModel
        );
        metaModel.getConstructorParameters().add(
            JavaParserMetaModel.callableDeclarationMetaModel.receiverParameterPropertyMetaModel
        );

        return metaModel;
    }
}

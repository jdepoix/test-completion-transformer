package org.jdepoix.ast.metamodel;

import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import com.github.javaparser.metamodel.StatementMetaModel;
import org.jdepoix.ast.node.*;

import java.util.Optional;

public class TestDeclarationMetaModel extends StatementMetaModel {
    public PropertyMetaModel testBodyPropertyMetaModel;
    public PropertyMetaModel testContextMethodDeclarationsPropertyMetaModel;
    public PropertyMetaModel whenMethodDeclarationPropertyMetaModel;
    public PropertyMetaModel contextMethodDeclarationsPropertyMetaModel;
    public PropertyMetaModel namePropertyMetaModel;

    TestDeclarationMetaModel(Optional<BaseNodeMetaModel> superBaseNodeMetaModel) {
        super(
            superBaseNodeMetaModel,
            TestDeclaration.class,
            "TestDeclaration",
            "org.jdepoix.ast.node",
            false,
            false
        );
    }

    public static TestDeclarationMetaModel registerMetaModel() {
        final TestDeclarationMetaModel metaModel = new TestDeclarationMetaModel(
            Optional.of(JavaParserMetaModel.nodeMetaModel)
        );
        JavaParserMetaModel.getNodeMetaModels().add(metaModel);


        metaModel.testBodyPropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "testBody",
            TestBody.class,
            Optional.of(TestBody.metaModel),
            false,
            false,
            false,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.testBodyPropertyMetaModel);
        metaModel.testContextMethodDeclarationsPropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "testContextMethodDeclarations",
            TestContextMethodDeclaration.class,
            Optional.of(TestContextMethodDeclaration.metaModel),
            false,
            false,
            false,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.testContextMethodDeclarationsPropertyMetaModel);
        metaModel.whenMethodDeclarationPropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "whenMethodDeclaration",
            WhenMethodDeclaration.class,
            Optional.of(WhenMethodDeclaration.metaModel),
            false,
            false,
            true,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.whenMethodDeclarationPropertyMetaModel);
        metaModel.contextMethodDeclarationsPropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "contextMethodDeclarations",
            ContextMethodDeclaration.class,
            Optional.of(ContextMethodDeclaration.metaModel),
            false,
            false,
            true,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.contextMethodDeclarationsPropertyMetaModel);
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

        metaModel.getConstructorParameters().add(metaModel.testBodyPropertyMetaModel);
        metaModel.getConstructorParameters().add(metaModel.testContextMethodDeclarationsPropertyMetaModel);
        metaModel.getConstructorParameters().add(metaModel.whenMethodDeclarationPropertyMetaModel);
        metaModel.getConstructorParameters().add(metaModel.contextMethodDeclarationsPropertyMetaModel);
        metaModel.getConstructorParameters().add(metaModel.namePropertyMetaModel);

        return metaModel;
    }
}

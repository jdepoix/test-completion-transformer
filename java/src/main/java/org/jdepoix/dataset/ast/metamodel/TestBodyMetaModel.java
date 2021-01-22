package org.jdepoix.dataset.ast.metamodel;

import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.metamodel.*;
import org.jdepoix.dataset.ast.node.TestBody;

import java.util.Optional;

public class TestBodyMetaModel extends StatementMetaModel {
    public PropertyMetaModel statementsPropertyMetaModel;

    TestBodyMetaModel(Optional<BaseNodeMetaModel> superBaseNodeMetaModel) {
        super(
            superBaseNodeMetaModel,
            TestBody.class,
            "TestBody",
            "org.jdepoix.dataset.ast.node",
            false,
            false
        );
    }

    public static TestBodyMetaModel registerMetaModel() {
        final TestBodyMetaModel metaModel = new TestBodyMetaModel(Optional.of(JavaParserMetaModel.statementMetaModel));
        JavaParserMetaModel.getNodeMetaModels().add(metaModel);

        metaModel.statementsPropertyMetaModel = new PropertyMetaModel(
            metaModel,
            "statements",
            Statement.class,
            Optional.of(JavaParserMetaModel.statementMetaModel),
            false,
            false,
            true,
            false
        );
        metaModel.getDeclaredPropertyMetaModels().add(metaModel.statementsPropertyMetaModel);

        metaModel.getConstructorParameters().add(metaModel.statementsPropertyMetaModel);

        return metaModel;
    }
}

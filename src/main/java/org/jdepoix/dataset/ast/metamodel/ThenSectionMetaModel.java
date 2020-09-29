package org.jdepoix.dataset.ast.metamodel;

import com.github.javaparser.metamodel.*;
import org.jdepoix.dataset.ast.node.ThenSection;

import java.util.Optional;

public class ThenSectionMetaModel extends StatementMetaModel {
    ThenSectionMetaModel(Optional<BaseNodeMetaModel> superBaseNodeMetaModel) {
        super(superBaseNodeMetaModel, ThenSection.class, "ThenSection", "org.jdepoix.dataset.ast.node", false, false);
    }

    public static ThenSectionMetaModel registerMetaModel() {
        final ThenSectionMetaModel metaModel = new ThenSectionMetaModel(
            Optional.of(JavaParserMetaModel.expressionMetaModel)
        );
        JavaParserMetaModel.getNodeMetaModels().add(metaModel);
        return metaModel;
    }
}

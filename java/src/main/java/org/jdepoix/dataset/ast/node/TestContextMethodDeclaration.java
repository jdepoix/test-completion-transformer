package org.jdepoix.dataset.ast.node;

import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.metamodel.NodeMetaModel;
import org.jdepoix.dataset.ast.metamodel.TestContextMethodDeclarationMetaModel;

public class TestContextMethodDeclaration extends MethodDeclaration implements CustomNode {
    public final static TestContextMethodDeclarationMetaModel metaModel
        = TestContextMethodDeclarationMetaModel.registerMetaModel();

    @AllFieldsConstructor
    public TestContextMethodDeclaration(
        final NodeList<Modifier> modifiers,
        final NodeList<AnnotationExpr> annotations,
        final NodeList<TypeParameter> typeParameters,
        final Type type,
        final SimpleName name,
        final NodeList<Parameter> parameters,
        final NodeList<ReferenceType> thrownExceptions,
        final BlockStmt body,
        ReceiverParameter receiverParameter
    ) {
        super(
            null,
            modifiers,
            annotations,
            typeParameters,
            type,
            name,
            parameters,
            thrownExceptions,
            body,
            receiverParameter
        );
    }

    @Override
    public NodeMetaModel getCustomMetaModel() {
        return this.metaModel;
    }

    public static TestContextMethodDeclaration fromMethodDeclaration(MethodDeclaration declaration) {
        return new TestContextMethodDeclaration(
            declaration.getModifiers(),
            declaration.getAnnotations(),
            declaration.getTypeParameters(),
            declaration.getType(),
            declaration.getName(),
            declaration.getParameters(),
            declaration.getThrownExceptions(),
            declaration.getBody().orElse(null),
            declaration.getReceiverParameter().orElse(null)
        );
    }
}

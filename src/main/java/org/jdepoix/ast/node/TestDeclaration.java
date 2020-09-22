package org.jdepoix.ast.node;

import com.github.javaparser.TokenRange;
import com.github.javaparser.ast.AllFieldsConstructor;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.metamodel.NodeMetaModel;
import org.jdepoix.ast.metamodel.TestDeclarationMetaModel;

public class TestDeclaration extends Node implements CustomNode, NodeWithSimpleName<TestDeclaration> {
    public static final TestDeclarationMetaModel metaModel = TestDeclarationMetaModel.registerMetaModel();

    private TestBody testBody;
    private NodeList<TestContextMethodDeclaration> testContextMethodDeclarations;
    private WhenMethodDeclaration whenMethodDeclaration;
    private NodeList<ContextMethodDeclaration> contextMethodDeclarations;
    private SimpleName name;

    @AllFieldsConstructor
    public TestDeclaration(
        TestBody testBody,
        NodeList<TestContextMethodDeclaration> testContextMethodDeclarations,
        WhenMethodDeclaration whenMethodDeclaration,
        NodeList<ContextMethodDeclaration> contextMethodDeclarations,
        SimpleName name
    ) {
        this(
            null,
            testBody,
            testContextMethodDeclarations,
            whenMethodDeclaration,
            contextMethodDeclarations,
            name
        );
    }

    public TestDeclaration(
        TokenRange tokenRange,
        TestBody testBody,
        NodeList<TestContextMethodDeclaration> testContextMethodDeclarations,
        WhenMethodDeclaration whenMethodDeclaration,
        NodeList<ContextMethodDeclaration> contextMethodDeclarations,
        SimpleName name
    ) {
        super(tokenRange);
        this.setTestBody(testBody);
        this.setTestContextMethodDeclarations(testContextMethodDeclarations);
        this.setWhenMethodDeclaration(whenMethodDeclaration);
        this.setContextMethodDeclarations(contextMethodDeclarations);
        this.setName(name);
    }

    @Override
    public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
        return null;
    }

    @Override
    public <A> void accept(VoidVisitor<A> v, A arg) { }

    @Override
    public NodeMetaModel getCustomMetaModel() {
        return this.metaModel;
    }

    @Override
    public SimpleName getName() {
        return this.name;
    }

    @Override
    public TestDeclaration setName(SimpleName name) {
        this.name = name;
        return this;
    }

    public TestBody getTestBody() {
        return testBody;
    }

    public TestDeclaration setTestBody(TestBody testBody) {
        this.testBody = testBody;
        return this;
    }

    public NodeList<TestContextMethodDeclaration> getTestContextMethodDeclarations() {
        return testContextMethodDeclarations;
    }

    public TestDeclaration setTestContextMethodDeclarations(
        NodeList<TestContextMethodDeclaration> testContextMethodDeclarations
    ) {
        this.testContextMethodDeclarations = testContextMethodDeclarations;
        return this;
    }

    public WhenMethodDeclaration getWhenMethodDeclaration() {
        return whenMethodDeclaration;
    }

    public TestDeclaration setWhenMethodDeclaration(WhenMethodDeclaration whenMethodDeclaration) {
        this.whenMethodDeclaration = whenMethodDeclaration;
        return this;
    }

    public NodeList<ContextMethodDeclaration> getContextMethodDeclarations() {
        return contextMethodDeclarations;
    }

    public TestDeclaration setContextMethodDeclarations(NodeList<ContextMethodDeclaration> contextMethodDeclarations) {
        this.contextMethodDeclarations = contextMethodDeclarations;
        return this;
    }
}

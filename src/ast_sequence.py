class Token():
    OPEN_TEST_CONTEXT = '<[.testContextMethodDeclarations:org.jdepoix.dataset.ast.node.TestContextMethodDeclaration]>'
    CLOSE_TEST_CONTEXT = '<[/.testContextMethodDeclarations:org.jdepoix.dataset.ast.node.TestContextMethodDeclaration]>'
    OPEN_CONTEXT = '<[.contextMethodDeclarations:org.jdepoix.dataset.ast.node.ContextMethodDeclaration]>'
    CLOSE_CONTEXT = '<[/.contextMethodDeclarations:org.jdepoix.dataset.ast.node.ContextMethodDeclaration]>'

    @staticmethod
    def is_value(token):
        return not (token.startswith('<[') and token.endswith(']>'))

class Token():
    TEST_CONTEXT_OPEN = '<[.testContextMethodDeclarations:org.jdepoix.dataset.ast.node.TestContextMethodDeclaration]>'
    TEST_CONTEXT_CLOSE = '<[/.testContextMethodDeclarations:org.jdepoix.dataset.ast.node.TestContextMethodDeclaration]>'
    CONTEXT_OPEN = '<[.contextMethodDeclarations:org.jdepoix.dataset.ast.node.ContextMethodDeclaration]>'
    CONTEXT_CLOSE = '<[/.contextMethodDeclarations:org.jdepoix.dataset.ast.node.ContextMethodDeclaration]>'
    SIMPLE_NAME_OPEN = '<[.name:com.github.javaparser.ast.expr.SimpleName]>'
    SIMPLE_NAME_CLOSE = '<[/.name:com.github.javaparser.ast.expr.SimpleName]>'
    IDENTIFIER_OPEN = '<[.identifier:java.lang.String]>'
    IDENTIFIER_CLOSE = '<[/.identifier:java.lang.String]>'

    @staticmethod
    def is_value(token):
        return not (token.startswith('<[') and token.endswith(']>'))


class AstSequence():
    @staticmethod
    def get_identifier_tokens(sequence):
        preceding_nodes = []
        identifier_tokens = set()

        for token in sequence:
            if Token.is_value(token):
                if (
                    len(preceding_nodes) == 2
                    and preceding_nodes[0] == Token.SIMPLE_NAME_OPEN
                    and preceding_nodes[1] == Token.IDENTIFIER_OPEN
                ):
                    identifier_tokens.add(token)
            else:
                while len(preceding_nodes) >= 2:
                    preceding_nodes.pop(0)
                preceding_nodes.append(token)

        return identifier_tokens

    @staticmethod
    def ends_with_open_identifier(sequence):
        preceding_nodes = []
        for token in reversed(sequence):
            if not Token.is_value(token):
                preceding_nodes.append(token)
                if len(preceding_nodes) >= 2:
                    return preceding_nodes[0] == Token.IDENTIFIER_OPEN and preceding_nodes[1] == Token.SIMPLE_NAME_OPEN
            else:
                if len(preceding_nodes) != 0:
                    return False
        return False

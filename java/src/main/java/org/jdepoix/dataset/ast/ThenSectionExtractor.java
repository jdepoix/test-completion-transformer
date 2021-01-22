package org.jdepoix.dataset.ast;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

import java.util.ArrayList;
import java.util.List;

public class ThenSectionExtractor {
    public NodeList<Statement> extract(MethodDeclaration testMethod, int startIndex) {
        final NodeList<Statement> statements = testMethod.getBody().get().getStatements();
        List<Statement> thenStatements = new ArrayList<>();
        while (statements.size() > startIndex) {
            thenStatements.add(statements.remove(startIndex));
        }

        return new NodeList<>(thenStatements);
    }
}

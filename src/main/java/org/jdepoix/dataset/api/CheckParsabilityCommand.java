package org.jdepoix.dataset.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.util.stream.Collectors;

public class CheckParsabilityCommand implements Command {
    private static final String METHOD_ID = "______method______";
    private static final String CLASS_TEMPLATE = "class ClassTemplate { void " + METHOD_ID + "() {%s} }";

    @Override
    public JsonNode execute(JsonNode data) throws Exception {
        final String code = new ObjectMapper().treeToValue(data, String.class);
        return new ObjectMapper().valueToTree(
            StaticJavaParser
                .parse(String.format(CLASS_TEMPLATE, code))
                .findFirst(MethodDeclaration.class, declaration -> declaration.getNameAsString().equals(METHOD_ID))
                .get()
                .getBody().get()
                .getStatements()
                .stream()
                .map(Node::toString)
                .collect(Collectors.joining("\n"))
        );
    }
}

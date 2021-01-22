package org.jdepoix.dataset.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.Node;
import org.jdepoix.dataset.ast.serialization.ASTDesequentializer;
import org.jdepoix.dataset.ast.serialization.ASTDeserializer;

import java.util.ArrayList;
import java.util.stream.Collectors;

class ThenSequenceToCodeCommand implements Command {
    @Override
    public JsonNode execute(JsonNode data) throws Exception {
        final ArrayList<String> sequence = new ObjectMapper().treeToValue(data, ArrayList.class);
        final ASTDeserializer astDeserializer = new ASTDeserializer();
        final String code = new ASTDesequentializer()
            .desequentialize(sequence)
            .stream()
            .map(serializedAST -> {
                try {
                    return astDeserializer.deserialize(serializedAST);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .map(Node::toString)
            .collect(Collectors.joining("\n"));
        return new ObjectMapper().valueToTree(code);
    }
}

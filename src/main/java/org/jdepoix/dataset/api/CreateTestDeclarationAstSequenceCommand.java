package org.jdepoix.dataset.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdepoix.dataset.ast.node.TestDeclaration;
import org.jdepoix.dataset.ast.serialization.ASTSequentializer;
import org.jdepoix.dataset.ast.serialization.ASTSerializer;
import org.jdepoix.dataset.ast.serialization.ASTToken;

import java.util.List;
import java.util.stream.Collectors;

class CreateTestDeclarationAstSequenceCommand extends TestDeclarationCreationCommand {
    @Override
    public JsonNode execute(JsonNode data) throws Exception {
        final TestDeclaration testDeclaration = parseTestDeclaration(data);
        final List<String> sequence = new ASTSequentializer()
            .sequentialize(new ASTSerializer().serialize(testDeclaration))
            .stream()
            .map(ASTToken::getToken)
            .collect(Collectors.toList());
        return new ObjectMapper().valueToTree(sequence);
    }
}

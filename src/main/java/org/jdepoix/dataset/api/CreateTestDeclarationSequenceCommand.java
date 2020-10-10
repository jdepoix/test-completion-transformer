package org.jdepoix.dataset.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.jdepoix.dataset.ast.AstUtils;
import org.jdepoix.dataset.ast.DefaultParser;
import org.jdepoix.dataset.ast.TestDeclarationCreator;
import org.jdepoix.dataset.ast.node.TestDeclaration;
import org.jdepoix.dataset.ast.serialization.ASTSequentializer;
import org.jdepoix.dataset.ast.serialization.ASTSerializer;
import org.jdepoix.dataset.ast.serialization.ASTToken;

import java.util.List;
import java.util.stream.Collectors;

class MessageData {
    public String testFileContent;
    public String testClassName;
    public String testMethodSignature;
    public String relatedFileContent;
    public String relatedClassName;
    public String relatedMethodSignature;
}

class CreateTestDeclarationSequenceCommand implements Command {
    @Override
    public JsonNode execute(JsonNode data) throws Exception {
        final MessageData messageData = new ObjectMapper().treeToValue(data, MessageData.class);
        final MethodDeclaration testMethod = parseMethod(
            messageData.testFileContent,
            messageData.testClassName,
            messageData.testMethodSignature
        );
        final MethodDeclaration relatedMethod = parseMethod(
            messageData.relatedFileContent,
            messageData.relatedClassName,
            messageData.relatedMethodSignature
        );
        final TestDeclaration testDeclaration = new TestDeclarationCreator().create(
            testMethod,
            relatedMethod
        );
        final List<String> sequence = new ASTSequentializer()
            .sequentialize(
                new ASTSerializer().serialize(testDeclaration)
            ).stream()
            .map(ASTToken::getToken)
            .collect(Collectors.toList());
        return new ObjectMapper().valueToTree(sequence);
    }

    private MethodDeclaration parseMethod(
        String code,
        String className,
        String methodSignature
    ) throws AstUtils.Unresolvable {
        return AstUtils.findMethodDeclarationBySignature(
            DefaultParser.parse(code),
            className,
            methodSignature
        );
    }
}

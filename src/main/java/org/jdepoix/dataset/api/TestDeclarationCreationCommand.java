package org.jdepoix.dataset.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.jdepoix.dataset.ast.AstUtils;
import org.jdepoix.dataset.ast.DefaultParser;
import org.jdepoix.dataset.ast.TestDeclarationCreator;
import org.jdepoix.dataset.ast.ThenSectionExtractor;
import org.jdepoix.dataset.ast.node.TestDeclaration;


public abstract class TestDeclarationCreationCommand implements Command {
    private static class RequestData {
        public String testFileContent;
        public String testClassName;
        public String testMethodSignature;
        public String relatedFileContent;
        public String relatedClassName;
        public String relatedMethodSignature;
        public Integer thenSectionStartIndex;
    }

    protected TestDeclaration parseTestDeclaration(
        JsonNode data
    ) throws JsonProcessingException, AstUtils.Unresolvable {
        final RequestData messageData = new ObjectMapper().treeToValue(data, RequestData.class);
        final MethodDeclaration testMethod = parseMethod(
            messageData.testFileContent,
            messageData.testClassName,
            messageData.testMethodSignature
        );
        if (messageData.thenSectionStartIndex != null) {
            new ThenSectionExtractor().extract(testMethod, messageData.thenSectionStartIndex);
        }
        final MethodDeclaration relatedMethod = parseMethod(
            messageData.relatedFileContent,
            messageData.relatedClassName,
            messageData.relatedMethodSignature
        );
        return new TestDeclarationCreator().create(
            testMethod,
            relatedMethod
        );
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

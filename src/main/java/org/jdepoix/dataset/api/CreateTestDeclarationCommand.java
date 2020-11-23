package org.jdepoix.dataset.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.jdepoix.dataset.ast.node.TestDeclaration;

import java.util.List;
import java.util.stream.Collectors;

public class CreateTestDeclarationCommand extends TestDeclarationCreationCommand {
    private static class ResponseData {
        public String name;
        public String body;
        public List<String> testCtx;
        public String when;
        public List<String> ctx;

        public ResponseData(String name, String body, List<String> testCtx, String when, List<String> ctx) {
            this.name = name;
            this.body = body;
            this.testCtx = testCtx;
            this.when = when;
            this.ctx = ctx;
        }
    }

    @Override
    public JsonNode execute(JsonNode data) throws Exception {
        final TestDeclaration testDeclaration = parseTestDeclaration(data);
        final ResponseData responseData = new ResponseData(
            testDeclaration.getNameAsString(),
            testDeclaration.getTestBody().toString(),
            stringifyMethodDeclarations(testDeclaration.getTestContextMethodDeclarations()),
            testDeclaration.getWhenMethodDeclaration().toString(),
            stringifyMethodDeclarations(testDeclaration.getContextMethodDeclarations())
        );
        return new ObjectMapper().valueToTree(responseData);
    }

    private List<String> stringifyMethodDeclarations(List<? extends MethodDeclaration> methodDeclarations) {
        return methodDeclarations
            .stream()
            .map(MethodDeclaration::toString)
            .collect(Collectors.toList());
    }
}

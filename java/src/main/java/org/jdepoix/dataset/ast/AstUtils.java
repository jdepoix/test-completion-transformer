package org.jdepoix.dataset.ast;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithMembers;

import java.util.Optional;

public class AstUtils {
    public static class Unresolvable extends Exception {
        public Unresolvable(String message) {
            super(message);
        }
    }

    public static MethodDeclaration findMethodDeclarationBySignature(
        CompilationUnit compilationUnit,
        String className,
        String methodSignature
    ) throws Unresolvable {
        Optional<? extends NodeWithMembers<? extends Node>> parentDeclaration = compilationUnit
            .findFirst(
                ClassOrInterfaceDeclaration.class,
                classOrInterfaceDeclaration -> classOrInterfaceDeclaration.resolve().getClassName().equals(className)
            );
        if (parentDeclaration.isEmpty()) {
            parentDeclaration = compilationUnit
                .findFirst(
                    EnumDeclaration.class,
                    enumDeclaration -> enumDeclaration.resolve().getClassName().equals(className)
                );
        }

        return parentDeclaration
            .orElseThrow(() -> new Unresolvable(String.format("can't find class/enum %s", className)))
            .getMethods()
            .stream()
            .filter(methodDeclaration -> methodDeclaration.getDeclarationAsString().equals(methodSignature))
            .findFirst()
            .orElseThrow(() -> new Unresolvable(String.format("can't find method %s", methodSignature)));
    }
}

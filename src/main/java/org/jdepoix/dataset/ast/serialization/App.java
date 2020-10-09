package org.jdepoix.dataset.ast.serialization;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.jdepoix.dataset.ast.node.ThenSection;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class App {
    public static void main(String[] args) throws Exception {
        String code = "class Test { void test() {" +
            "packager.setIncludeRelevantJarModeJars(false);\n" +
            "execute(packager, (callback) -> {\n" +
            "    callback.library(new Library(libJarFile1, LibraryScope.COMPILE));\n" +
            "    callback.library(new Library(libJarFile2, LibraryScope.COMPILE));\n" +
            "    callback.library(new Library(libJarFile3, LibraryScope.COMPILE));\n" +
            "});\n" +
            "assertThat(hasPackagedEntry(\"BOOT-INF/classpath.idx\")).isTrue();\n" +
            "String classpathIndex = getPackagedEntryContent(\"BOOT-INF/classpath.idx\");\n" +
            "List<String> expectedClasspathIndex = Stream.of(libJarFile1, libJarFile2, libJarFile3).map((file) -> \"- \\\"\" + file.getName() + \"\\\"\").collect(Collectors.toList());\n" +
            "assertThat(Arrays.asList(classpathIndex.split(\"\\\\n\"))).containsExactlyElementsOf(expectedClasspathIndex);\n" +
            "assertThat(hasPackagedEntry(\"BOOT-INF/layers.idx\")).isTrue();\n" +
            "String layersIndex = getPackagedEntryContent(\"BOOT-INF/layers.idx\");\n" +
            "List<String> expectedLayers = new ArrayList<>();\n" +
            "expectedLayers.add(\"- 'default':\");\n" +
            "expectedLayers.add(\"  - 'BOOT-INF/classes/'\");\n" +
            "expectedLayers.add(\"  - 'BOOT-INF/classpath.idx'\");\n" +
            "expectedLayers.add(\"  - 'BOOT-INF/layers.idx'\");\n" +
            "expectedLayers.add(\"  - 'META-INF/'\");\n" +
            "expectedLayers.add(\"  - 'org/'\");\n" +
            "expectedLayers.add(\"- '0001':\");\n" +
            "expectedLayers.add(\"  - 'BOOT-INF/lib/\" + libJarFile1.getName() + \"'\");\n" +
            "expectedLayers.add(\"- '0002':\");\n" +
            "expectedLayers.add(\"  - 'BOOT-INF/lib/\" + libJarFile2.getName() + \"'\");\n" +
            "expectedLayers.add(\"- '0003':\");\n" +
            "expectedLayers.add(\"  - 'BOOT-INF/lib/\" + libJarFile3.getName() + \"'\");\n" +
            "assertThat(layersIndex.split(\"\\\\n\")).containsExactly(expectedLayers.stream().map((s) -> s.replace('\\'', '\"')).toArray(String[]::new));" +
            "}}";

        final CompilationUnit compilationUnit = StaticJavaParser.parse(code);
        compilationUnit.findFirst(MethodDeclaration.class).get().getBody().get().getStatements().add(new ThenSection());
        final SerializedAST ast = new ASTSerializer().serialize(compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).get());
        System.out.println(ast.toString());
        System.out.println();


        final ASTSequentializer astSequentializer = new ASTSequentializer();
        final List<ASTToken> sequentialize = astSequentializer.sequentialize(ast);
        final List<String> sequence = sequentialize.stream().map(astToken -> astToken.toString()).collect(Collectors.toList());
        System.out.println(String.join("\n", sequence));
        final ASTDesequentializer astDesequentializer = new ASTDesequentializer();
        final ASTDeserializer astDeserializer = new ASTDeserializer();


        final NodeList<Node> nodes = new NodeList<>(
            astDesequentializer.desequentialize(
                Stream.concat(sequence.stream(), sequence.stream()).collect(Collectors.toList())
            ).stream().map(serializedAST -> {
                try {
                    return astDeserializer.deserialize(serializedAST);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList())
        );
        System.out.println(nodes.toString());
    }
}

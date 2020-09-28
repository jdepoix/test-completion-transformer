package org.jdepoix.ast.serialization;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

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
        final SerializedAST ast = new ASTSerializer().serialize(compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).get());
        System.out.println(ast.toString());
        System.out.println();
//        final Node node = new ASTDeserializer().deserialize(ast);
//        System.out.println(node.toString());

        final ASTSequentializer astSequentializer = new ASTSequentializer();
        final List<ASTToken> sequentialize = astSequentializer.sequentialize(ast);
        System.out.println(String.join("\n", sequentialize.stream().map(astToken -> StringEscapeUtils.escapeJava(astToken.toString())).collect(Collectors.toList())));


        System.out.println();
        final String x = StringEscapeUtils.escapeJson("test/te'st/test");
        System.out.println(x);
    }
}

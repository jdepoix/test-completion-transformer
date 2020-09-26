package org.jdepoix.ast.serialization;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

public class App {
    public static void main(String[] args) throws Exception {
        String code = "import java.swing;\n" +
            "\n" +
            "        /*\n" +
            "         * Java implementation of the approach\n" +
            "         */ \n" +
            "        public class GFG { \n" +
            "\n" +
            "            // Function that returns true if \n" +
            "            // str is a palindrome \n" +
            "            static boolean isPalindrome(String str) \n" +
            "            { \n" +
            "\n" +
            "                // Pointers pointing to the beginning \n" +
            "                // and the end of the string \n" +
            "                int i = 0, j = str.length() - 1; \n" +
            "\n" +
            "                // While there are characters toc compare \n" +
            "                while (i < j) { \n" +
            "\n" +
            "                    // If there is a mismatch \n" +
            "                    if (str.charAt(i) != str.charAt(j)) \n" +
            "                        return false; \n" +
            "\n" +
            "                    // Increment first pointer and \n" +
            "                    // decrement the other \n" +
            "                    i++; \n" +
            "                    j--; \n" +
            "                } \n" +
            "\n" +
            "                // Given string is a palindrome \n" +
            "                return true; \n" +
            "            } \n" +
            "\n" +
            "            // Driver code \n" +
            "            public static void main(String[] args) \n" +
            "            { \n" +
            "                String str = \"geeks is a palindrome\"; \n" +
            "\n" +
            "                if (isPalindrome(str)) \n" +
            "                    System.out.print(\"Yes\"); \n" +
            "                else\n" +
            "                    System.out.print(\"No\"); \n" +
            "            } \n" +
            "        } ";

        final CompilationUnit compilationUnit = StaticJavaParser.parse(code);
        final SerializedAST ast = new ASTSerializer().serialize(compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).get());
        System.out.println(ast.printTree());
        System.out.println();
        final Node node = new ASTDeserializer().deserialize(ast);
        System.out.println(node.toString());
    }
}

package org.jdepoix.ast.serialization;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.NodeMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import org.jdepoix.ast.node.CustomNode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

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
        final AST ast = AST.serialize(compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).get());
        System.out.println(ast.printTree());
        System.out.println();
        final Node node = AST.deserialize(ast);
        System.out.println(node.toString());
    }
}

class ReflectionUtils {
    public static Field findDeclaredFieldInSuperClasses(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            final Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                return ReflectionUtils.findDeclaredFieldInSuperClasses(superclass, fieldName);
            }
            throw e;
        }
    }
}

// TODO refactor to single node with children, properties, value and type
abstract class ASTNode {
    public static final Map<String, Class<?>> BOXED_TYPE_MAP = Map.of(
        java.lang.Boolean.TYPE.getName(), java.lang.Boolean.class,
        java.lang.Character.TYPE.getName(), java.lang.Character.class,
        java.lang.Byte.TYPE.getName(), java.lang.Byte.class,
        java.lang.Short.TYPE.getName(), java.lang.Short.class,
        java.lang.Integer.TYPE.getName(), java.lang.Integer.class,
        java.lang.Long.TYPE.getName(), java.lang.Long.class,
        java.lang.Float.TYPE.getName(), java.lang.Float.class,
        java.lang.Double.TYPE.getName(), java.lang.Double.class,
        java.lang.Void.TYPE.getName(), java.lang.Void.class
    );

    private final String type;

    ASTNode(String type) {
        this.type = type;
    }

    static NodeMetaModel getNodeMetaModel(Node node) {
        return node instanceof CustomNode ? ((CustomNode) node).getCustomMetaModel() : node.getMetaModel();
    }

    static List<ASTNode> serialize(Node parentNode) throws NoSuchFieldException, IllegalAccessException {
        final ArrayList<ASTNode> entries = new ArrayList<>();

        final NodeMetaModel parentNodeMetaModel = getNodeMetaModel(parentNode);
        final List<PropertyMetaModel> constructorParameters = parentNodeMetaModel.getConstructorParameters();
        if (constructorParameters.isEmpty()) {
            return List.of(new TerminalNode(parentNodeMetaModel.getType().getName()));
        }

        for (PropertyMetaModel propertyMetaModel : constructorParameters) {
            final Field propertyField = ReflectionUtils.findDeclaredFieldInSuperClasses(parentNode.getClass(), propertyMetaModel.getName());
            propertyField.setAccessible(true);
            final Object propertyValue = propertyField.get(parentNode);

            if (propertyValue == null) {
                continue;
            }

            if (propertyValue instanceof Node) {
                final Node propertyNode = (Node) propertyValue;
                final List<ASTNode> serializedChildren = serialize(propertyNode);
                final NodeMetaModel nodeMetaModel = getNodeMetaModel(propertyNode);
                if (serializedChildren.isEmpty()) {
                    entries.add(new PropertyNode(
                        nodeMetaModel.getType().getName(),
                        List.of(new TerminalNode(nodeMetaModel.getType().getName())),
                        propertyMetaModel.getName()
                    ));
                } else {
                    entries.add(new PropertyNode(
                        nodeMetaModel.getType().getName(),
                        serializedChildren,
                        propertyMetaModel.getName()
                    ));
                }
            } else if (propertyValue instanceof NodeList) {
                final NodeList<Node> nodeList = (NodeList<Node>) propertyValue;
                if (nodeList.isEmpty()) {
                    continue;
                }

                final List<TypeNode> serializedNodeList = nodeList.stream()
                    .map(node -> {
                        try {
                            final List<ASTNode> serializedChildren = serialize(node);
                            if (serializedChildren.isEmpty()) {
                                return Optional.<TypeNode>empty();
                            }
                            return Optional.of(new TypeNode(getNodeMetaModel(node).getType().getName(), serializedChildren));
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(o -> o.isPresent())
                    .map(Optional::get)
                    .collect(Collectors.toList());

                if (!serializedNodeList.isEmpty()) {
                    entries.add(new PropertyNode(
                        propertyMetaModel.getType().getName(),
                        serializedNodeList,
                        propertyMetaModel.getName()
                    ));
                }
            } else {
                entries.add(new PropertyNode(
                    propertyMetaModel.getType().getName(),
                    List.of(new ValueNode(
                        propertyMetaModel.getType().getName(),
                        propertyValue.toString()
                    )),
                    propertyMetaModel.getName()
                ));
            }
        }

        return entries;
    }

    static Object deserialize(ASTNode node) throws ClassNotFoundException, AST.MetaModelNotFound, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (node instanceof TypeNode) {
            return constructChildrenFromMetaModel((TypeNode) node);
        } else if (node instanceof PropertyNode) {
            final PropertyNode propertyNode = (PropertyNode) node;
            if (propertyNode.getChildNodes().get(0) instanceof TypeNode) {
                List<Object> nodeListParams = new ArrayList<>();
                for (ASTNode childNode : propertyNode.getChildNodes()) {
                    nodeListParams.add(deserialize(childNode));
                }
                return new NodeList(nodeListParams);
            }
            return constructChildrenFromMetaModel(propertyNode);
        } else if (node instanceof ValueNode) {
            final ValueNode valueNode = (ValueNode) node;
            if (valueNode.getValue() == null) {
                return null;
            }
            if (BOXED_TYPE_MAP.containsKey(valueNode.getType())) {
                return BOXED_TYPE_MAP.get(valueNode.getType()).getConstructor(String.class).newInstance(valueNode.getValue());
            }
            final Class<?> clazz = Class.forName(valueNode.getType());
            if (clazz.isEnum()) {
                return Enum.valueOf((Class<Enum>) clazz, valueNode.getValue());
            }
            return Class.forName(valueNode.getType()).getConstructor(String.class).newInstance(valueNode.getValue());
        } else {
            return getMetaModel(Class.forName(node.getType())).construct(Map.of());
        }
    }

    private static BaseNodeMetaModel getMetaModel(Class<?> clazz) throws AST.MetaModelNotFound {
        return JavaParserMetaModel
            .getNodeMetaModel(clazz)
            .orElseThrow(() -> new AST.MetaModelNotFound(String.format("MetaModel for %s not found", clazz.getName())));
    }

    private static Node constructChildrenFromMetaModel(NodeWithChildren node) throws AST.MetaModelNotFound, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final BaseNodeMetaModel metaModel = getMetaModel(Class.forName(node.getType()));

        final HashMap<String, Object> params = new HashMap<>();
        for (ASTNode childNode : node.getChildNodes()) {
            Object deserializedChild;
            final PropertyNode propertyNode = (PropertyNode) childNode;
            final ASTNode firstChild = propertyNode.getChildNodes().get(0);
            if (firstChild instanceof ValueNode || firstChild instanceof TerminalNode) {
                deserializedChild = deserialize(firstChild);
            } else {
                deserializedChild = deserialize(childNode);
            }

            params.put(
                propertyNode.getProperty(),
                deserializedChild
            );
        }
        return metaModel.construct(params);
    }

    @Override
    public String toString() {
        return this.type;
    }

    public String getType() {
        return type;
    }
}

abstract class NodeWithChildren extends ASTNode {
    private final List<? extends ASTNode> childNodes;

    NodeWithChildren(String type, List<? extends ASTNode> childNodes) {
        super(type);
        this.childNodes = childNodes;
    }

    String printTree(int depth) {
        return String.format(
            "%s%s\n%s",
            "\t".repeat(depth),
            this.toString(),
            this.getChildNodes()
                .stream()
                .map(
                    astNode ->
                        astNode instanceof NodeWithChildren
                            ? ((NodeWithChildren) astNode).printTree(depth + 1)
                            : String.format("%s%s", "\t".repeat(depth + 1), astNode.toString())
                )
                .collect(Collectors.joining("\n"))
        );
    }

    public List<? extends ASTNode> getChildNodes() {
        return childNodes;
    }
}

class PropertyNode extends NodeWithChildren {
    private final String property;

    PropertyNode(String type, List<? extends ASTNode> childNodes, String property) {
        super(type, childNodes);
        this.property = property;
    }

    @Override
    public String toString() {
        return String.format(".%s:%s", this.getProperty(), this.getType());
    }

    public String getProperty() {
        return property;
    }
}

class TypeNode extends NodeWithChildren {
    TypeNode(String type, List<? extends ASTNode> childNodes) {
        super(type, childNodes);
    }
}

class ValueNode extends ASTNode {
    private final String value;

    ValueNode(String type, String value) {
        super(type);
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("%s", this.getValue());
    }

    public String getValue() {
        return value;
    }
}

class TerminalNode extends ASTNode {
    TerminalNode(String type) {
        super(type);
    }
}


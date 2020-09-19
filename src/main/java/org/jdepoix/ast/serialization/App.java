package org.jdepoix.ast.serialization;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class App {
    public static void main(String[] args) throws Exception {
        String code = "//\n" +
            "// Source code recreated from a .class file by IntelliJ IDEA\n" +
            "// (powered by FernFlower decompiler)\n" +
            "//\n" +
            "\n" +
            "package com.github.javaparser.ast.type;\n" +
            "\n" +
            "import com.github.javaparser.TokenRange;\n" +
            "import com.github.javaparser.ast.AllFieldsConstructor;\n" +
            "import com.github.javaparser.ast.Node;\n" +
            "import com.github.javaparser.ast.NodeList;\n" +
            "import com.github.javaparser.ast.expr.AnnotationExpr;\n" +
            "import com.github.javaparser.ast.expr.SimpleName;\n" +
            "import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;\n" +
            "import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;\n" +
            "import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;\n" +
            "import com.github.javaparser.ast.observer.ObservableProperty;\n" +
            "import com.github.javaparser.ast.type.PrimitiveType.Primitive;\n" +
            "import com.github.javaparser.ast.visitor.CloneVisitor;\n" +
            "import com.github.javaparser.ast.visitor.GenericVisitor;\n" +
            "import com.github.javaparser.ast.visitor.VoidVisitor;\n" +
            "import com.github.javaparser.metamodel.ClassOrInterfaceTypeMetaModel;\n" +
            "import com.github.javaparser.metamodel.JavaParserMetaModel;\n" +
            "import com.github.javaparser.metamodel.OptionalProperty;\n" +
            "import com.github.javaparser.resolution.types.ResolvedReferenceType;\n" +
            "import com.github.javaparser.utils.Utils;\n" +
            "import java.util.Optional;\n" +
            "import java.util.function.Consumer;\n" +
            "import java.util.stream.Collectors;\n" +
            "\n" +
            "public class ClassOrInterfaceType extends ReferenceType implements NodeWithSimpleName<ClassOrInterfaceType>, NodeWithAnnotations<ClassOrInterfaceType>, NodeWithTypeArguments<ClassOrInterfaceType> {\n" +
            "    @OptionalProperty\n" +
            "    private ClassOrInterfaceType scope;\n" +
            "    private SimpleName name;\n" +
            "    @OptionalProperty\n" +
            "    private NodeList<Type> typeArguments;\n" +
            "\n" +
            "    public ClassOrInterfaceType() {\n" +
            "        this((TokenRange)null, (ClassOrInterfaceType)null, new SimpleName(), (NodeList)null, new NodeList());\n" +
            "    }\n" +
            "\n" +
            "    /** @deprecated */\n" +
            "    public ClassOrInterfaceType(final String name) {\n" +
            "        this((TokenRange)null, (ClassOrInterfaceType)null, new SimpleName(name), (NodeList)null, new NodeList());\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceType(final ClassOrInterfaceType scope, final String name) {\n" +
            "        this((TokenRange)null, scope, new SimpleName(name), (NodeList)null, new NodeList());\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceType(final ClassOrInterfaceType scope, final SimpleName name, final NodeList<Type> typeArguments) {\n" +
            "        this((TokenRange)null, scope, name, typeArguments, new NodeList());\n" +
            "    }\n" +
            "\n" +
            "    @AllFieldsConstructor\n" +
            "    public ClassOrInterfaceType(final ClassOrInterfaceType scope, final SimpleName name, final NodeList<Type> typeArguments, final NodeList<AnnotationExpr> annotations) {\n" +
            "        this((TokenRange)null, scope, name, typeArguments, annotations);\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceType(TokenRange tokenRange, ClassOrInterfaceType scope, SimpleName name, NodeList<Type> typeArguments, NodeList<AnnotationExpr> annotations) {\n" +
            "        super(tokenRange, annotations);\n" +
            "        this.setScope(scope);\n" +
            "        this.setName(name);\n" +
            "        this.setTypeArguments(typeArguments);\n" +
            "        this.customInitialization();\n" +
            "    }\n" +
            "\n" +
            "    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {\n" +
            "        return v.visit(this, arg);\n" +
            "    }\n" +
            "\n" +
            "    public <A> void accept(final VoidVisitor<A> v, final A arg) {\n" +
            "        v.visit(this, arg);\n" +
            "    }\n" +
            "\n" +
            "    public SimpleName getName() {\n" +
            "        return this.name;\n" +
            "    }\n" +
            "\n" +
            "    public Optional<ClassOrInterfaceType> getScope() {\n" +
            "        return Optional.ofNullable(this.scope);\n" +
            "    }\n" +
            "\n" +
            "    public boolean isBoxedType() {\n" +
            "        return PrimitiveType.unboxMap.containsKey(this.name.getIdentifier());\n" +
            "    }\n" +
            "\n" +
            "    public PrimitiveType toUnboxedType() throws UnsupportedOperationException {\n" +
            "        if (!this.isBoxedType()) {\n" +
            "            throw new UnsupportedOperationException(this.name + \" isn't a boxed type.\");\n" +
            "        } else {\n" +
            "            return new PrimitiveType((Primitive)PrimitiveType.unboxMap.get(this.name.getIdentifier()));\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceType setName(final SimpleName name) {\n" +
            "        Utils.assertNotNull(name);\n" +
            "        if (name == this.name) {\n" +
            "            return this;\n" +
            "        } else {\n" +
            "            this.notifyPropertyChange(ObservableProperty.NAME, this.name, name);\n" +
            "            if (this.name != null) {\n" +
            "                this.name.setParentNode((Node)null);\n" +
            "            }\n" +
            "\n" +
            "            this.name = name;\n" +
            "            this.setAsParentNodeOf(name);\n" +
            "            return this;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceType setScope(final ClassOrInterfaceType scope) {\n" +
            "        if (scope == this.scope) {\n" +
            "            return this;\n" +
            "        } else {\n" +
            "            this.notifyPropertyChange(ObservableProperty.SCOPE, this.scope, scope);\n" +
            "            if (this.scope != null) {\n" +
            "                this.scope.setParentNode((Node)null);\n" +
            "            }\n" +
            "\n" +
            "            this.scope = scope;\n" +
            "            this.setAsParentNodeOf(scope);\n" +
            "            return this;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public Optional<NodeList<Type>> getTypeArguments() {\n" +
            "        return Optional.ofNullable(this.typeArguments);\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceType setTypeArguments(final NodeList<Type> typeArguments) {\n" +
            "        if (typeArguments == this.typeArguments) {\n" +
            "            return this;\n" +
            "        } else {\n" +
            "            this.notifyPropertyChange(ObservableProperty.TYPE_ARGUMENTS, this.typeArguments, typeArguments);\n" +
            "            if (this.typeArguments != null) {\n" +
            "                this.typeArguments.setParentNode((Node)null);\n" +
            "            }\n" +
            "\n" +
            "            this.typeArguments = typeArguments;\n" +
            "            this.setAsParentNodeOf(typeArguments);\n" +
            "            return this;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceType setAnnotations(NodeList<AnnotationExpr> annotations) {\n" +
            "        return (ClassOrInterfaceType)super.setAnnotations(annotations);\n" +
            "    }\n" +
            "\n" +
            "    public boolean remove(Node node) {\n" +
            "        if (node == null) {\n" +
            "            return false;\n" +
            "        } else if (this.scope != null && node == this.scope) {\n" +
            "            this.removeScope();\n" +
            "            return true;\n" +
            "        } else {\n" +
            "            if (this.typeArguments != null) {\n" +
            "                for(int i = 0; i < this.typeArguments.size(); ++i) {\n" +
            "                    if (this.typeArguments.get(i) == node) {\n" +
            "                        this.typeArguments.remove(i);\n" +
            "                        return true;\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "\n" +
            "            return super.remove(node);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public String asString() {\n" +
            "        StringBuilder str = new StringBuilder();\n" +
            "        this.getScope().ifPresent((s) -> {\n" +
            "            str.append(s.asString()).append(\".\");\n" +
            "        });\n" +
            "        str.append(this.name.asString());\n" +
            "        this.getTypeArguments().ifPresent((ta) -> {\n" +
            "            str.append((String)ta.stream().map(Type::asString).collect(Collectors.joining(\",\", \"<\", \">\")));\n" +
            "        });\n" +
            "        return str.toString();\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceType removeScope() {\n" +
            "        return this.setScope((ClassOrInterfaceType)null);\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceType clone() {\n" +
            "        return (ClassOrInterfaceType)this.accept((GenericVisitor)(new CloneVisitor()), (Object)null);\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceTypeMetaModel getMetaModel() {\n" +
            "        return JavaParserMetaModel.classOrInterfaceTypeMetaModel;\n" +
            "    }\n" +
            "\n" +
            "    public boolean replace(Node node, Node replacementNode) {\n" +
            "        if (node == null) {\n" +
            "            return false;\n" +
            "        } else if (node == this.name) {\n" +
            "            this.setName((SimpleName)replacementNode);\n" +
            "            return true;\n" +
            "        } else if (this.scope != null && node == this.scope) {\n" +
            "            this.setScope((ClassOrInterfaceType)replacementNode);\n" +
            "            return true;\n" +
            "        } else {\n" +
            "            if (this.typeArguments != null) {\n" +
            "                for(int i = 0; i < this.typeArguments.size(); ++i) {\n" +
            "                    if (this.typeArguments.get(i) == node) {\n" +
            "                        this.typeArguments.set(i, (Type)replacementNode);\n" +
            "                        return true;\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "\n" +
            "            return super.replace(node, replacementNode);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public boolean isClassOrInterfaceType() {\n" +
            "        return true;\n" +
            "    }\n" +
            "\n" +
            "    public ClassOrInterfaceType asClassOrInterfaceType() {\n" +
            "        return this;\n" +
            "    }\n" +
            "\n" +
            "    public void ifClassOrInterfaceType(Consumer<ClassOrInterfaceType> action) {\n" +
            "        action.accept(this);\n" +
            "    }\n" +
            "\n" +
            "    public ResolvedReferenceType resolve() {\n" +
            "        return (ResolvedReferenceType)this.getSymbolResolver().toResolvedType(this, ResolvedReferenceType.class);\n" +
            "    }\n" +
            "\n" +
            "    public Optional<ClassOrInterfaceType> toClassOrInterfaceType() {\n" +
            "        return Optional.of(this);\n" +
            "    }\n" +
            "}\n";

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

class AST {
    static class MetaModelNotFound extends Exception {
        public MetaModelNotFound(String message) {
            super(message);
        }
    }

    private final TypeNode root;

    private AST(TypeNode root) {
        this.root = root;
    }

    public static AST serialize(Node rootNode) throws NoSuchFieldException, IllegalAccessException {
        return new AST(
            new TypeNode(
                rootNode.getMetaModel().getType().getName(),
                ASTNode.serialize(rootNode)
            )
        );
    }

    public static Node deserialize(AST ast) throws ClassNotFoundException, MetaModelNotFound, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return (Node) ASTNode.deserialize(ast.root);
    }

    public String printTree() {
        return this.root.printTree(0);
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

    static List<ASTNode> serialize(Node parentNode) throws NoSuchFieldException, IllegalAccessException {
        final ArrayList<ASTNode> entries = new ArrayList<>();

        final List<PropertyMetaModel> constructorParameters = parentNode.getMetaModel().getConstructorParameters();
        if (constructorParameters.isEmpty()) {
            return List.of(new TerminalNode(parentNode.getMetaModel().getType().getName()));
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
                if (serializedChildren.isEmpty()) {
                    entries.add(new PropertyNode(
                        propertyNode.getMetaModel().getType().getName(),
                        List.of(new TerminalNode(propertyNode.getMetaModel().getType().getName())),
                        propertyMetaModel.getName()
                    ));
                } else {
                    entries.add(new PropertyNode(
                        propertyNode.getMetaModel().getType().getName(),
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
                            return Optional.of(new TypeNode(node.getMetaModel().getType().getName(), serializedChildren));
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
        return String.format("%s:%s", this.getType(), this.getValue());
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


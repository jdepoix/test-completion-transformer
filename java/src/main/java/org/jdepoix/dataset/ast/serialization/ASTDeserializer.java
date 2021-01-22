package org.jdepoix.dataset.ast.serialization;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.metamodel.BaseNodeMetaModel;
import com.github.javaparser.metamodel.JavaParserMetaModel;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO refactor to single node with children, properties, value and type
public class ASTDeserializer {
    private static final Map<String, Class<?>> BOXED_TYPE_MAP = Map.of(
        Boolean.TYPE.getName(), Boolean.class,
        Character.TYPE.getName(), Character.class,
        Byte.TYPE.getName(), Byte.class,
        Short.TYPE.getName(), Short.class,
        Integer.TYPE.getName(), Integer.class,
        Long.TYPE.getName(), Long.class,
        Float.TYPE.getName(), Float.class,
        Double.TYPE.getName(), Double.class,
        Void.TYPE.getName(), Void.class
    );

    public Node deserialize(SerializedAST serializedAST) throws
        NoSuchMethodException,
        InstantiationException,
        SerializedAST.MetaModelNotFound,
        IllegalAccessException,
        InvocationTargetException,
        ClassNotFoundException
    {
        return (Node) deserializeNode(serializedAST.getRoot());
    }

    public Object deserializeNode(SerializedASTNode node) throws
        NoSuchMethodException,
        InstantiationException,
        SerializedAST.MetaModelNotFound,
        IllegalAccessException,
        InvocationTargetException,
        ClassNotFoundException
    {
        if (node instanceof TypeNode) {
            return constructChildrenFromMetaModel((TypeNode) node);
        } else if (node instanceof PropertyNode) {
            final PropertyNode propertyNode = (PropertyNode) node;
            if (propertyNode.getChildNodes().get(0) instanceof TypeNode) {
                List<Object> nodeListParams = new ArrayList<>();
                for (SerializedASTNode childNode : propertyNode.getChildNodes()) {
                    nodeListParams.add(deserializeNode(childNode));
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
                return BOXED_TYPE_MAP
                    .get(valueNode.getType())
                    .getConstructor(String.class)
                    .newInstance(valueNode.getValue());
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

    private Node constructChildrenFromMetaModel(NodeWithChildren node) throws
        SerializedAST.MetaModelNotFound,
        ClassNotFoundException,
        NoSuchMethodException,
        InvocationTargetException,
        InstantiationException,
        IllegalAccessException
    {
        final BaseNodeMetaModel metaModel = getMetaModel(Class.forName(node.getType()));

        final HashMap<String, Object> params = new HashMap<>();
        for (SerializedASTNode childNode : node.getChildNodes()) {
            Object deserializedChild;
            final PropertyNode propertyNode = (PropertyNode) childNode;
            final SerializedASTNode firstChild = propertyNode.getChildNodes().get(0);
            if (firstChild instanceof ValueNode || firstChild instanceof TerminalNode) {
                deserializedChild = deserializeNode(firstChild);
            } else {
                deserializedChild = deserializeNode(childNode);
            }

            params.put(
                propertyNode.getProperty(),
                deserializedChild
            );
        }
        return metaModel.construct(params);
    }

    private BaseNodeMetaModel getMetaModel(Class<?> clazz) throws SerializedAST.MetaModelNotFound {
        return JavaParserMetaModel
            .getNodeMetaModel(clazz)
            .orElseThrow(
                () -> new SerializedAST.MetaModelNotFound(String.format("MetaModel for %s not found", clazz.getName()))
            );
    }
}

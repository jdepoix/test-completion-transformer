package org.jdepoix.dataset.ast.serialization;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.metamodel.NodeMetaModel;
import com.github.javaparser.metamodel.PropertyMetaModel;
import org.jdepoix.dataset.ast.node.CustomNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ASTSerializer {
    public SerializedAST serialize(Node root) throws NoSuchFieldException, IllegalAccessException {
        return new SerializedAST(
            new TypeNode(
                getNodeMetaModel(root).getType().getName(),
                serializeChildren(root)
            )
        );
    }

    private List<SerializedASTNode> serializeChildren(Node parentNode)
        throws IllegalAccessException, NoSuchFieldException
    {
        final ArrayList<SerializedASTNode> entries = new ArrayList<>();

        final NodeMetaModel parentNodeMetaModel = getNodeMetaModel(parentNode);
        final List<PropertyMetaModel> constructorParameters = parentNodeMetaModel.getConstructorParameters();
        if (constructorParameters.isEmpty()) {
            return List.of(new TerminalNode(parentNodeMetaModel.getType().getName()));
        }

        for (PropertyMetaModel propertyMetaModel : constructorParameters) {
            final Field propertyField = ReflectionUtils.findDeclaredFieldInSuperClasses(
                parentNode.getClass(),
                propertyMetaModel.getName()
            );
            propertyField.setAccessible(true);
            final Object propertyValue = propertyField.get(parentNode);

            if (propertyValue == null) {
                continue;
            }

            if (propertyValue instanceof Node) {
                entries.add(serializeNode(propertyMetaModel, (Node) propertyValue));
            } else if (propertyValue instanceof NodeList) {
                serializeNodeList(propertyMetaModel, (NodeList<Node>) propertyValue).ifPresent(
                    propertyNode -> entries.add(propertyNode)
                );
            } else {
                entries.add(serializeValueNode(propertyMetaModel, propertyValue));
            }
        }

        return entries;
    }

    private PropertyNode serializeValueNode(PropertyMetaModel propertyMetaModel, Object propertyValue) {
        return new PropertyNode(
            propertyMetaModel.getType().getName(),
            List.of(new ValueNode(
                propertyMetaModel.getType().getName(),
                propertyValue.toString()
            )),
            propertyMetaModel.getName()
        );
    }

    private Optional<PropertyNode> serializeNodeList(PropertyMetaModel propertyMetaModel, NodeList<Node> nodeList) {
        if (nodeList.isEmpty()) {
            return Optional.empty();
        }

        final List<SerializedASTNode> serializedNodeList = nodeList.stream()
            .map(node -> {
                try {
                    final List<SerializedASTNode> serializedChildren = serializeChildren(node);
                    if (serializedChildren.isEmpty()) {
                        return Optional.<TypeNode>empty();
                    }
                    return Optional.of(
                        new TypeNode(getNodeMetaModel(node).getType().getName(), serializedChildren)
                    );
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            })
            .filter(o -> o.isPresent())
            .map(Optional::get)
            .collect(Collectors.toList());

        if (!serializedNodeList.isEmpty()) {
            return Optional.of(new PropertyNode(
                propertyMetaModel.getType().getName(),
                serializedNodeList,
                propertyMetaModel.getName()
            ));
        }

        return Optional.empty();
    }

    private PropertyNode serializeNode(PropertyMetaModel propertyMetaModel, Node propertyNode)
        throws IllegalAccessException, NoSuchFieldException
    {
        final List<SerializedASTNode> serializedChildren = serializeChildren(propertyNode);
        final NodeMetaModel nodeMetaModel = getNodeMetaModel(propertyNode);
        if (serializedChildren.isEmpty()) {
            return new PropertyNode(
                nodeMetaModel.getType().getName(),
                List.of(new TerminalNode(nodeMetaModel.getType().getName())),
                propertyMetaModel.getName()
            );
        } else {
            return new PropertyNode(
                nodeMetaModel.getType().getName(),
                serializedChildren,
                propertyMetaModel.getName()
            );
        }
    }

    private NodeMetaModel getNodeMetaModel(Node node) {
        return node instanceof CustomNode ? ((CustomNode) node).getCustomMetaModel() : node.getMetaModel();
    }
}


package de.tum.in.opcua.server.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.UnsignedByte;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.Node;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.ObjectNode;
import org.opcfoundation.ua.core.ReferenceNode;
import org.opcfoundation.ua.core.VariableNode;

import de.tum.in.opcua.server.core.UAServerException;
import de.tum.in.opcua.server.core.addressspace.AddressSpace;
import de.tum.in.opcua.server.core.addressspace.NodeFactory;
import de.tum.in.opcua.server.core.util.NodeUtils;

/**
 * builds type nodes for a given nodemapping
 * 
 * @author harald
 *
 */
public class TypeNodeBuilder {

	private static final String TYPE_SUFFIX = "Type";
	private static final Logger LOG = Logger.getLogger(TypeNodeBuilder.class);

	private final Locale locale;
	private final int nsIndex;
	private final AddressSpace addrSpace;

	public TypeNodeBuilder(Locale locale, int nsIndex) {
		this.locale = locale;
		this.nsIndex = nsIndex;
		addrSpace = AddressSpace.getInstance();
	}

	/**
	 * builds a typenode for the given nodeMapping. the typenode is wired up
	 * with all child instance declarations. the built node and all its child
	 * nodes are returned as a flat list. the first node in the list is allways
	 * the typenode as root. the nodes are not added to the addressspace here,
	 * the caller has to handle this
	 * 
	 * @param nodeMapping
	 * @return
	 */
	public List<Node> buildTypeNode(NodeMapping nodeMapping) {
		final List<Node> createdNodes = new ArrayList<Node>();

		/*
		 * the name of the type is unique.
		 */
		final String typeName = nodeMapping.getNodeName() + TYPE_SUFFIX;
		final NodeId typeId = new NodeId(nsIndex, typeName);

		Node typeNode = null;

		if (NodeClass.Object.equals(nodeMapping.getNodeClass())) {
			typeNode = NodeFactory.getObjectTypeNodeInstance(typeName, null,
					typeName, locale, typeId, false);
		} else if (NodeClass.Variable.equals(nodeMapping.getNodeClass())) {
			typeNode = NodeFactory.getVariableTypeNodeInstance(typeName, null,
					typeName, locale, typeId, false);
		}

		createdNodes.add(typeNode);

		// add properties
		for (final String fieldName : nodeMapping.getReferencesByName()
				.keySet()) {
			final ReferenceMapping refMapping = nodeMapping
					.getReferenceByName(fieldName);

			// Create the instance declaration
			final Node instDecl = buildInstanceDeclaration(refMapping, typeName);

			// create a reference to them
			NodeUtils.addReferenceToNode(typeNode,
					new ReferenceNode(refMapping.getReferenceType(), false,
							new ExpandedNodeId(instDecl.getNodeId())));

			// collect nodes to return them later on
			createdNodes.add(instDecl);
		}

		try {
			// add reference to the parent type
			final Node parentType = addrSpace.getNode(nodeMapping
					.getParentType());
			NodeUtils.addReferenceToNode(parentType,
					new ReferenceNode(Identifiers.HasSubtype, false,
							new ExpandedNodeId(typeNode.getNodeId())));
		} catch (final UAServerException e) {
			LOG.error(e.getMessage(), e);
		}

		return createdNodes;
	}

	/**
	 * builds an Instance Declaration fitting the given {@link ReferenceMapping}
	 * .
	 * 
	 * @param refMapping
	 * @return
	 */
	public Node buildInstanceDeclaration(ReferenceMapping refMapping,
			String parentTypeName) {
		// generate an id by concatenating the parents typename with the
		// browsename. for example FloorType:fireDoor
		final NodeId id = new NodeId(nsIndex, parentTypeName + ":"
				+ refMapping.getBrowseName());

		Node newNode = null;
		if (NodeClass.Object.equals(refMapping.getNodeClass())) {

			newNode = new ObjectNode();

		} else if (NodeClass.Variable.equals(refMapping.getNodeClass())) {
			final UnsignedByte access = AccessLevel.getMask(
					AccessLevel.CurrentRead, AccessLevel.HistoryRead);

			final VariableNode vn = new VariableNode();
			UaNodeAnnoIntrospector.setDataTypeFields(vn, refMapping.getField());
			vn.setValue(new Variant(null));
			vn.setUserAccessLevel(access);
			vn.setAccessLevel(access);
			newNode = vn;

		}

		// generate the node
		newNode.setNodeId(id);
		newNode.setNodeClass(refMapping.getNodeClass());
		newNode.setBrowseName(new QualifiedName(nsIndex, refMapping
				.getBrowseName()));
		newNode.setDisplayName(new LocalizedText(refMapping.getDisplayName(),
				locale));
		newNode.setDescription(new LocalizedText(refMapping.getDescription(),
				locale));
		newNode.setWriteMask(UnsignedInteger.ZERO);
		newNode.setUserWriteMask(UnsignedInteger.ZERO);

		// add typedefinition
		NodeUtils.addReferenceToNode(newNode, new ReferenceNode(
				Identifiers.HasTypeDefinition, false, new ExpandedNodeId(
						refMapping.getTypeDefinition())));

		// TODO add modelling rule; are they really mandatory?

		return newNode;
	}

	/**
	 * creates either a Mandatory or Optional Modelling Rule for the given node
	 * and adds it to his references.
	 * 
	 * @param node
	 * @param mandatory
	 */
	private void addModellingRuleNode(Node node, boolean mandatory) {
	}
}

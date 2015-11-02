package de.tum.in.opcua.server.core.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.core.AddReferencesItem;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.Node;

import de.tum.in.opcua.server.core.addressspace.INodeIDs;
import de.tum.in.opcua.server.core.util.NodeUtils;
import de.tum.in.opcua.server.core.util.StringUtils;

public abstract class AbstractElementParser implements IXmlElementParser {

	private static final Logger LOG = Logger
			.getLogger(AbstractElementParser.class);

	protected static final Namespace NS = Namespace
			.getNamespace("http://opcfoundation.org/UA/ModelDesign.xsd");

	/**
	 * the {@link Element} which will be parsed
	 */
	protected Element elem;

	/**
	 * to fetch {@link NodeId}s from by their name
	 */
	protected INodeIDs nodeIds;

	/**
	 * used locale
	 */
	protected Locale locale;

	protected NodeId readTypeDefinition() {
		NodeId typeDefNodeId = null;
		final String typeDef = readAttribute("TypeDefinition");
		if (!StringUtils.isNullOrEmtpy(typeDef)) {
			typeDefNodeId = nodeIds.getNodeIdByName(StringUtils
					.removeNamespacePraefix(typeDef));
		}
		return typeDefNodeId;
	}

	/**
	 * reads the browsename of the element. if it is not set, the symbolicname
	 * is used as browsename, because the browsename must not be null.
	 * 
	 * @return
	 */
	protected String readBrowseName() {
		String browseName = elem.getChildText("BrowseName", NS);
		if (StringUtils.isNullOrEmtpy(browseName)) {
			// set the browsename to the symbolicname
			browseName = readSymbolicName();
		}
		return browseName;
	}

	protected String readSymbolicName() {
		return readAttribute("SymbolicName");
	}

	protected boolean readIsAbstract() {
		boolean isAbstract = false;
		final String attrIsAbstract = readAttribute("isAbstract");
		if (!StringUtils.isNullOrEmtpy(attrIsAbstract)) {
			isAbstract = Boolean.parseBoolean(attrIsAbstract);
		}
		return isAbstract;
	}

	protected boolean readIsSymmetric() {
		boolean isSymmetric = false;
		final String attrIsSymmetric = readAttribute("Symmetric");
		if (!StringUtils.isNullOrEmtpy(attrIsSymmetric)) {
			isSymmetric = Boolean.parseBoolean(attrIsSymmetric);
		}
		return isSymmetric;
	}

	protected String readInverseName() {
		return elem.getChildText("InverseName", NS);
	}

	protected NodeId readBaseType() {
		NodeId baseTypeNodeId = null;
		final String attrBaseType = readAttribute("BaseType");
		if (!StringUtils.isNullOrEmtpy(attrBaseType)) {
			baseTypeNodeId = nodeIds.getNodeIdByName(StringUtils
					.removeNamespacePraefix(attrBaseType));
		}
		return baseTypeNodeId;
	}

	protected String readAttribute(String attrName) {
		return readAttribute(attrName, elem);
	}

	protected String readAttribute(String attrName, Element elem) {
		final String attrVal = elem.getAttributeValue(attrName);
		return StringUtils.removeNamespacePraefix(attrVal);
	}

	protected NodeId getNodeIdBySymName() {
		final String symName = readSymbolicName();
		return nodeIds.getNodeIdByName(symName);
	}

	/**
	 * this method parses the references-element of the this{@link #elem}
	 * property. for non-inverse references, the current node is the source. for
	 * inverse references, the current node is the target.
	 * 
	 * @param node
	 * @param nodeClass
	 * @return
	 */
	protected List<AddReferencesItem> readReferences(Node node) {
		// list of references where the given node acts as target-node
		final List<AddReferencesItem> referenceList = new ArrayList<AddReferencesItem>();

		final Element references = elem.getChild("References", NS);
		if (references != null) {
			final List<Element> refList = references.getChildren("Reference",
					NS);
			for (final Element ref : refList) {
				final Attribute attIsInverse = ref.getAttribute("IsInverse");
				boolean isInverse = false;
				if (attIsInverse != null) {
					isInverse = Boolean.parseBoolean(attIsInverse.getValue());
				}
				final String refTypeName = StringUtils
						.removeNamespacePraefix(ref.getChildText(
								"ReferenceType", NS));
				final String targetIdName = StringUtils
						.removeNamespacePraefix(ref
								.getChildText("TargetId", NS));

				final NodeId refType = nodeIds.getNodeIdByName(refTypeName);
				final NodeId targetNodeId = nodeIds
						.getNodeIdByName(targetIdName);

				// LOG.debug(String.format("refType: %s, targetId: %s, isInverse:%s",
				// refTypeName, targetIdName, isInverse));

				if (targetNodeId != null) {
					final AddReferencesItem newRef = new AddReferencesItem(
							node.getNodeId(), refType, !isInverse, null,
							NodeUtils.toExpandedNodeId(targetNodeId), null);
					referenceList.add(newRef);
				} else {
					LOG.warn("no NodeID found for node " + targetIdName);
				}
			}
		}

		return referenceList;
	}

	/**
	 * reads all child-elements of the current {@link Element} to parse
	 * 
	 * @param node
	 * @return
	 */
	protected ParsedElement readChildren(Node parentNode) {
		final ParsedElement parsedChildren = new ParsedElement();

		final Element children = elem.getChild("Children", NS);
		if (children != null) {
			final String symName = readSymbolicName();

			if ("ServerType".equals(symName)) {
				LOG.debug("parsing servertype");
			}

			for (final Element child : children.getChildren()) {
				final String childName = child.getName();

				// create a decorator, so that the parser finds the childs
				// NodeId with the parens name prepended.
				// we need the symName to find the nodeid for the children.
				// cause in the standardtypes all children are named like
				// symName_childName
				final INodeIDs prefixedNodeIds = new NodeIDsPrefixDecorator(
						symName + "_", nodeIds);

				/*
				 * property, variable, object here we use the factory again to
				 * parse the element correctly. this also leads to a recursion,
				 * cause children of the current children are also parsed this
				 * way.
				 */
				final IXmlElementParser elemParser = XmlElementParserFactory
						.getInstance(childName);
				if (elemParser != null) {
					// this should contain only one Node, but let us add
					// everything which was parsed
					final ParsedElement pe = elemParser.parseElement(child,
							prefixedNodeIds, locale);
					if (pe != null && !pe.getNodes().isEmpty()) {
						parsedChildren.addNodeList(pe.getNodes());
						parsedChildren.addAddReferencesItemList(pe
								.getRefItems());

						// TODO add reference from parent node to the newly
						// created one
						/*
						 * this should be the one node which was parsed. if the
						 * list has more nodes than one, than they are its
						 * children
						 */
						final Node parsedChild = (Node) pe.getNodes().get(0);
						final NodeId refType = getRefTypeForChildElement(childName);
						final AddReferencesItem newRef = new AddReferencesItem(
								parentNode.getNodeId(), refType, true, null,
								NodeUtils.toExpandedNodeId(parsedChild
										.getNodeId()),
								parentNode.getNodeClass());
						parsedChildren.addAddReferencesItem(newRef);
					}
				}
			}
		}

		return parsedChildren;
	}

	protected NodeId getRefTypeForChildElement(String elemName) {
		NodeId refType = null;

		if ("Property".equalsIgnoreCase(elemName)) {
			refType = Identifiers.HasProperty;
		} else if ("Object".equalsIgnoreCase(elemName)
				|| "Variable".equalsIgnoreCase(elemName)) {
			refType = Identifiers.HasComponent;
		}

		return refType;
	}

}

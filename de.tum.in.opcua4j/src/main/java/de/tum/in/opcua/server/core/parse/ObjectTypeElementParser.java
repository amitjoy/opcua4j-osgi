package de.tum.in.opcua.server.core.parse;

import java.util.Locale;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.core.AddReferencesItem;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.Node;

import de.tum.in.opcua.server.core.addressspace.INodeIDs;
import de.tum.in.opcua.server.core.addressspace.NodeFactory;
import de.tum.in.opcua.server.core.util.NodeUtils;

public class ObjectTypeElementParser extends AbstractElementParser {

	private static final Logger LOG = Logger
			.getLogger(ObjectTypeElementParser.class);

	@Override
	public ParsedElement parseElement(Element elem, INodeIDs nodeIds,
			Locale locale) {
		this.elem = elem;
		this.nodeIds = nodeIds;
		this.locale = locale;

		final ParsedElement pe = new ParsedElement();

		final String browseName = readBrowseName();
		final NodeId nodeId = getNodeIdBySymName();
		if (nodeId != null) {
			final boolean isAbstract = readIsAbstract();

			final Node newNode = NodeFactory.getObjectTypeNodeInstance(
					browseName, "description goes here", browseName, locale,
					nodeId, isAbstract);

			// parse basetype
			final NodeId baseTypeId = readBaseType();
			if (baseTypeId != null) {
				// add a reference from the basetype to the newNode
				final AddReferencesItem newRef = new AddReferencesItem(
						baseTypeId, Identifiers.HasSubtype, true, null,
						NodeUtils.toExpandedNodeId(newNode.getNodeId()),
						newNode.getNodeClass());
				pe.addAddReferencesItem(newRef);
			}

			// parse children
			final ParsedElement children = readChildren(newNode);
			LOG.debug(String.format("got %d children", children.getNodes()
					.size()));
			pe.addNodeList(children.getNodes());
			pe.addAddReferencesItemList(children.getRefItems());

			// TODO parse fields
			// TODO parse description

			pe.addNode(newNode);

			// LOG.debug("parsed browsename: " + browseName);
		} else {
			LOG.debug("no nodeid found four element " + readSymbolicName());
		}

		return pe;
	}

}

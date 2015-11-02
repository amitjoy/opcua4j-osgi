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

public class VariableTypeElementParser extends AbstractElementParser {

	private static final Logger LOG = Logger
			.getLogger(VariableTypeElementParser.class);

	@Override
	public ParsedElement parseElement(Element elem, INodeIDs nodeIds,
			Locale locale) {
		this.elem = elem;
		this.nodeIds = nodeIds;

		final ParsedElement pe = new ParsedElement();

		final String browseName = readBrowseName();
		final NodeId nodeId = getNodeIdBySymName();
		if (nodeId != null) {
			final boolean isAbstract = readIsAbstract();
			// TODO parse the other fields

			final Node newNode = NodeFactory.getVariableTypeNodeInstance(
					browseName, "description goes here", browseName, locale,
					nodeId, null, null, null, null, isAbstract);

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

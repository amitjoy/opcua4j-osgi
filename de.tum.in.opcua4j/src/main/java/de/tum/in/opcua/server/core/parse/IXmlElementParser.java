package de.tum.in.opcua.server.core.parse;

import java.util.Locale;

import org.jdom2.Element;

import de.tum.in.opcua.server.core.addressspace.INodeIDs;

/**
 * is able to parse a specific xml element
 * 
 * @author harald
 *
 */
public interface IXmlElementParser {

	public ParsedElement parseElement(Element elem, INodeIDs nodeIds,
			Locale locale);

}

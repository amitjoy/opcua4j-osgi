package de.tum.in.opcua.server.core.parse;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.opcfoundation.ua.builtintypes.NodeId;

import de.tum.in.opcua.server.core.addressspace.AddressSpace;
import de.tum.in.opcua.server.core.addressspace.INodeIDs;

/**
 * is able to parse xml files which are valid against
 * http://opcfoundation.org/UA/ModelDesign.xsd
 * 
 * for example the UA Defined Types.xml defines all standard types which are
 * parsed and added to the {@link AddressSpace}.
 * 
 * @author harald
 * 
 */
public class XmlModelParser {

	private static final Logger LOG = Logger.getLogger(XmlModelParser.class);

	public static final Namespace NS = Namespace
			.getNamespace("http://opcfoundation.org/UA/ModelDesign.xsd");

	private final Locale locale = Locale.ENGLISH;

	/**
	 * file handle to the xml file
	 */
	private File file;

	/**
	 * stream to the xml file
	 */
	private InputStream inStream;

	/**
	 * {@link INodeIDs} to be able to match nodenames to {@link NodeId}s
	 */
	private final INodeIDs nodeIds;

	/**
	 * @param file
	 * @param nodeIds
	 */
	public XmlModelParser(File file, INodeIDs nodeIds) {
		this.file = file;
		this.nodeIds = nodeIds;
	}

	public XmlModelParser(InputStream inStream, INodeIDs nodeIds) {
		this.inStream = inStream;
		this.nodeIds = nodeIds;
	}

	/**
	 * inserts parsed nodes from the xml file into the {@link AddressSpace}
	 * 
	 * @param addrSpace
	 */
	public List<ParsedElement> parseNodes() {
		final List<ParsedElement> parsedElements = new ArrayList<ParsedElement>();

		final SAXBuilder builder = new SAXBuilder();
		Document doc;

		if (file != null) {
			if (file.exists()) {
				try {
					doc = builder.build(file);
					// collect parsed rest of nodes
					parsedElements.addAll(parseElements(doc));
				} catch (final Exception e) {
					LOG.error(e.getMessage(), e);
				}
			} else {
				LOG.info(file.getPath()
						+ " does not exist, no nodes added to addressspace");
			}
		} else if (inStream != null) {
			try {
				doc = builder.build(inStream);
				// collect parsed rest of nodes
				parsedElements.addAll(parseElements(doc));
			} catch (final Exception e) {
				LOG.error(e.getMessage(), e);
			}
		} else {
			LOG.warn("nothing given to parse");
		}

		return parsedElements;
	}

	private List<ParsedElement> parseElements(Document doc) {
		final List<ParsedElement> parsedElements = new ArrayList<ParsedElement>();

		for (final Element elem : doc.getRootElement().getChildren()) {

			final ParsedElement pe = parseElement(elem);
			if (pe != null) {
				parsedElements.add(pe);
			}
		}

		return parsedElements;
	}

	/**
	 * parses one element and all its childelements to nodes and references and
	 * wrapps all parsed objects in an {@link ParsedElement}. a factory is used
	 * to use the correct parse-implementation dependent on the xml-elements
	 * name which should be parsed
	 * 
	 * @param elem
	 * @param addrSpace
	 * @return
	 */
	private ParsedElement parseElement(Element elem) {
		final String elemName = elem.getName();

		ParsedElement pe = null;

		final IXmlElementParser elemParser = XmlElementParserFactory
				.getInstance(elemName);
		if (elemParser != null) {
			pe = elemParser.parseElement(elem, nodeIds, locale);
		}

		return pe;
	}

	/**
	 * returns the element in the {@link Document} whose attribute
	 * "SymbolicName" equals to the given symbolicName.
	 * 
	 * @param doc
	 * @param symbolicName
	 * @return
	 */
	private Element getElementBySymbolicName(Document doc, String symbolicName) {
		final XPathExpression<Element> xpath = XPathFactory.instance().compile(
				String.format("//*[@SymbolicName='%s']", symbolicName),
				Filters.element());
		final Element element = xpath.evaluateFirst(doc);
		return element;
	}
}

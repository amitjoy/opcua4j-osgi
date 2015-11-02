package de.tum.in.opcua.server.annotation;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.VariableNode;

import de.tum.in.opcua.server.core.UAServerException;
import de.tum.in.opcua.server.core.addressspace.NodeFactory;
import de.tum.in.opcua.server.core.util.NodeUtils;

/**
 * introspects classes which are annotated witch {@link UaNode} and creates a
 * {@link NodeMapping} object of the given class
 * 
 * TODO could be extended to scan packages for annotated classes.
 * 
 * @author harald
 *
 */
public class UaNodeAnnoIntrospector {

	private static final Logger LOG = Logger
			.getLogger(UaNodeAnnoIntrospector.class);

	/**
	 * returns a NodeMapping object for the given Object if its annotated as
	 * UANode; throws exception otherwhise.
	 * 
	 * @param obj
	 * @return
	 * @throws UAServerException
	 */
	public static NodeMapping introspect(Object obj) throws UAServerException {
		final UaNode objAnno = obj != null ? obj.getClass().getAnnotation(
				UaNode.class) : null;
		if (objAnno != null) {
			// the bean is correctly annotated as an uaobject. now we try to
			// collect all annotated information

			Field idField = null;
			Field displNameField = null;
			Field descField = null;
			Field valueField = null;
			final Map<String, ReferenceMapping> referencesByName = new HashMap<String, ReferenceMapping>();

			for (final Field field : obj.getClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(ID.class)) {
					idField = field;
				}

				if (field.isAnnotationPresent(DisplayName.class)) {
					displNameField = field;
				}

				if (field.isAnnotationPresent(Description.class)) {
					descField = field;
				}

				if (field.isAnnotationPresent(Property.class)) {
					referencesByName.put(field.getName(),
							refMapForProperty(field));
				} else if (field.isAnnotationPresent(Reference.class)) {
					referencesByName.put(field.getName(),
							refMapForReference(field));
				}

				if (field.isAnnotationPresent(Value.class)) {
					valueField = field;
				}
			}

			// TODO do some validation and throw exception if for example
			// displayname is missing
			final NodeMapping nodeMap = new NodeMapping(obj.getClass(),
					objAnno.nodeClass(), idField, displNameField, descField,
					referencesByName);
			nodeMap.setTypeDefinition(NodeUtils.toExpandedNodeId(NodeUtils
					.getBaseTypeNodeId(objAnno.nodeClass())));
			nodeMap.setParentType(NodeUtils.getBaseTypeNodeId(objAnno
					.nodeClass()));
			nodeMap.setValueField(valueField);
			return nodeMap;
		} else {
			throw new UAServerException(
					"class not correctly annotated with UANode(nodeClass=...)");
		}
	}

	private static ReferenceMapping refMapForProperty(Field field) {
		final ReferenceMapping refMapping = new ReferenceMapping();
		refMapping.setField(field);
		refMapping.setDisplayName(field.getName());
		refMapping.setBrowseName(field.getName());
		refMapping.setNodeClass(NodeClass.Variable);
		refMapping.setReferenceType(Identifiers.HasProperty);
		refMapping.setTypeDefinition(Identifiers.PropertyType);
		return refMapping;
	}

	private static ReferenceMapping refMapForReference(Field field) {
		final ReferenceMapping refMapping = new ReferenceMapping();

		final Reference refAnno = field.getAnnotation(Reference.class);

		refMapping.setField(field);
		refMapping.setDisplayName(field.getName());
		refMapping.setBrowseName(field.getName());

		refMapping.setTypeDefinition(Identifiers.BaseObjectType);

		// TODO find out actual node class
		refMapping.setNodeClass(NodeClass.Variable);
		refMapping.setReferenceType(refAnno.refType().nodeId());
		return refMapping;
	}

	/**
	 * tries to introspect the given Object. if it is correctly annotated, a new
	 * NodeMapping object is returned containing all introspected information.
	 * otherwhise null is returned, hence no exceptions are thrown.
	 * 
	 * @param obj
	 * @return
	 */
	public static NodeMapping tryIntrospect(Object obj) {
		NodeMapping nm = null;
		try {
			nm = introspect(obj);
		} catch (final Exception e) {
			LOG.debug(e.getMessage(), e);
		}
		return nm;
	}

	/**
	 * 
	 * sets the fields DataType, ValueRank and ArrayDimension in the given
	 * {@link VariableNode} so that they represent the given {@link Field}s
	 * type.
	 * 
	 * @param variable
	 * @param f
	 */
	public static void setDataTypeFields(VariableNode variable, Field f) {
		final Class<?> type = f.getType();

		variable.setValueRank(NodeUtils.getValueRank(type));
		variable.setDataType(NodeFactory.getNodeIdByDataType(type));
	}
}

package de.tum.in.opcua.server.annotation.history;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.ExtensionObject;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.core.HistoryData;
import org.opcfoundation.ua.core.HistoryReadResult;
import org.opcfoundation.ua.core.HistoryReadValueId;
import org.opcfoundation.ua.core.ReadRawModifiedDetails;
import org.opcfoundation.ua.encoding.EncodingException;

import de.tum.in.opcua.server.annotation.AnnotationNodeManager;
import de.tum.in.opcua.server.annotation.IAnnotationHistoryManager;
import de.tum.in.opcua.server.annotation.NodeMapping;
import de.tum.in.opcua.server.core.history.HistoryValue;
import de.tum.in.opcua.server.core.history.IHistoryManager;
import de.tum.in.opcua.server.core.util.ArrayUtils;

public class AnnotationHistoryManager implements IHistoryManager {

	private static final Logger LOG = Logger
			.getLogger(AnnotationHistoryManager.class);

	private final AnnotationNodeManager nodeMngr;
	private final IAnnotationHistoryManager histMngr;

	/**
	 * @param nodeMngr
	 */
	public AnnotationHistoryManager(AnnotationNodeManager nodeMngr,
			IAnnotationHistoryManager histMngr) {
		this.nodeMngr = nodeMngr;
		this.histMngr = histMngr;
	}

	@Override
	public HistoryReadResult readRawModifiedDetails(
			HistoryReadValueId histReadValId,
			ReadRawModifiedDetails rawModifiedDetails) {
		final String nodeid = (String) histReadValId.getNodeId().getValue();
		final String[] idParts = nodeid
				.split(AnnotationNodeManager.ID_SEPARATOR);

		final String nodeName = idParts[0];
		final String beanId = idParts[1];

		List<HistoryValue> historyValues = null;
		if (idParts.length == 2) {

		} else if (idParts.length == 3) {

			/*
			 * this is pretty hardcoded for our desires here. after the
			 * structure nodemapping->referenceMapping->nodemapping is
			 * refactored this can be changed to support different combinations
			 * of nodes. for now we assume that only Variables classes linked
			 * with @Reference are annotated with @HistoryRead
			 */

			final String fieldName = idParts[2];

			NodeMapping nodeMapping;
			nodeMapping = nodeMngr.getNodeMapping(nodeName);
			final Class<?> targetType = nodeMapping
					.getReferencedDataType(fieldName);

			final NodeMapping fieldMapping = nodeMngr.getNodeMapping(targetType
					.getSimpleName());

			final Date start = new Date(rawModifiedDetails.getStartTime()
					.getTimeInMillis());
			final Date end = new Date(rawModifiedDetails.getEndTime()
					.getTimeInMillis());

			if (histMngr != null) {
				// TODO find out the actual historyRead ID and do not call the
				// implementation with the "fieldName"
				historyValues = histMngr.getHistoryValues(
						nodeMapping.getClazz(), beanId, fieldName, start, end);
			}

		}

		HistoryReadResult historyResult = null;
		if (historyValues != null) {
			historyResult = new HistoryReadResult();
			final HistoryData data = new HistoryData();

			// map the values from the client(implementor of
			// (IAnnotationHistoryManager) to an valid opc DataValue object
			final List<DataValue> opcDataValues = new ArrayList<DataValue>();
			for (final HistoryValue histValue : historyValues) {
				final Calendar cal = Calendar.getInstance();
				cal.setTime(histValue.getTimestamp());
				final DateTime dateTime = new DateTime(cal);
				opcDataValues.add(new DataValue(new Variant(histValue
						.getValue()), StatusCode.GOOD, dateTime, dateTime));
			}

			data.setDataValues(ArrayUtils.toArray(opcDataValues,
					DataValue.class));
			try {
				historyResult
						.setHistoryData(ExtensionObject.binaryEncode(data));
			} catch (final EncodingException e) {
				LOG.error(e.getMessage(), e);
			}
		}

		return historyResult;
	}

}

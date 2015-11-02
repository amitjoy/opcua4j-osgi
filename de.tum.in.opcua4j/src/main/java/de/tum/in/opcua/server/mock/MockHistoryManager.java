package de.tum.in.opcua.server.mock;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

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

import de.tum.in.opcua.server.core.history.IHistoryManager;
import de.tum.in.opcua.server.core.util.ArrayUtils;

public class MockHistoryManager implements IHistoryManager {

	private static final Logger LOG = Logger
			.getLogger(MockHistoryManager.class);

	@Override
	public HistoryReadResult readRawModifiedDetails(
			HistoryReadValueId histReadValId,
			ReadRawModifiedDetails rawModifiedDetails) {
		LOG.debug("handling readRawModifiedDetails-history-request");
		final Date start = new Date(rawModifiedDetails.getStartTime()
				.getTimeInMillis());
		final Date end = new Date(rawModifiedDetails.getEndTime()
				.getTimeInMillis());
		final SimpleDateFormat dFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");

		final HistoryReadResult historyResult = new HistoryReadResult();
		final HistoryData data = new HistoryData();
		final List<DataValue> values = new ArrayList<DataValue>();

		// lets mock some data
		final Double[] mockData = new Double[] { 1.12, 1.23, 1.55, 1.22, 1.65,
				2.75, 2.03, 2.23, 2.11, 1.77, 1.23, 1.66 };
		final Random rand = new Random(System.currentTimeMillis());
		final double randFactor = rand.nextInt(10 - 1 + 1) + 1;
		final long diff = end.getTime() - start.getTime();
		for (int i = 0; i < mockData.length; i++) {
			// create timestamps so that the datavalues are equally distributed
			// over the requested time range
			final Calendar cal = Calendar.getInstance();
			cal.setTime(new Date(diff / (mockData.length - 1) * i
					+ start.getTime()));
			final DateTime dateTime = new DateTime(cal);
			values.add(new DataValue(new Variant(randFactor * mockData[i]),
					StatusCode.GOOD, dateTime, dateTime));
		}

		data.setDataValues(ArrayUtils.toArray(values, DataValue.class));
		try {
			historyResult.setHistoryData(ExtensionObject.binaryEncode(data));
		} catch (final EncodingException e) {
			LOG.error(e.getMessage(), e);
		}
		return historyResult;
	}

}

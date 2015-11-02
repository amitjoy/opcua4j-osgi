package de.tum.in.opcua.server.basic.nodes;

import org.opcfoundation.ua.core.NodeClass;

import de.tum.in.opcua.server.annotation.AnnotationNodeManager;
import de.tum.in.opcua.server.annotation.DisplayName;
import de.tum.in.opcua.server.annotation.HistoryRead;
import de.tum.in.opcua.server.annotation.Property;
import de.tum.in.opcua.server.annotation.UaNode;
import de.tum.in.opcua.server.annotation.Value;

@UaNode(nodeClass = NodeClass.Variable)
public class HumiditySensor {

	public static final String HISTORY = "humHistory";

	/**
	 * current value of the sensor
	 */
	@Value
	@HistoryRead(qualifier = HISTORY)
	private Double value;

	@DisplayName
	private String displName = "Relative Humidity";

	@Property
	private String unit = "value between 0 and 1";

	/**
	 * empty constructor is mandatory when using {@link AnnotationNodeManager}
	 */
	public HumiditySensor() {

	}

	/**
	 * 
	 */
	public HumiditySensor(Double value) {
		this.value = value;
	}

	/**
	 * @return the value
	 */
	public double getValue() {
		return value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(double value) {
		this.value = value;
	}

	/**
	 * @return the displName
	 */
	public String getDisplName() {
		return displName;
	}

	/**
	 * @param displName
	 *            the displName to set
	 */
	public void setDisplName(String displName) {
		this.displName = displName;
	}

	/**
	 * @return the unit
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * @param unit
	 *            the unit to set
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}
}

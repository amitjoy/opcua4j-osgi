package de.tum.in.opcua.server.annotation;

import de.tum.in.opcua.server.core.addressspace.INodeManager;

/**
 * has to be implemented if {@link INodeManager}s want to support monitored
 * items and hence notify clients about value changes in the addressspace
 * 
 * @author harald
 *
 */
public interface MonitorItemManagable {

}

package de.einwesen.heimklangwelle.upnpsupport;

import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;
import org.jupnp.support.connectionmanager.ConnectionManagerService;
import org.jupnp.support.model.ConnectionInfo;
import org.jupnp.support.model.Protocol;
import org.jupnp.support.model.ProtocolInfo;
import org.jupnp.support.model.ProtocolInfos;

//UPNP annotations are inherited from parent
public abstract class AbstractConnectionManagerService extends ConnectionManagerService {
	public static final String DNLA_OP_NO_SEEKING_SUPPORTED = "DLNA.ORG_OP=00";
	public static final String DNLA_OP_TIMESEEKING_SUPPORTED = "DLNA.ORG_OP=01";
	public static final String DNLA_OP_BYTESEEKING_SUPPORTED = "DLNA.ORG_OP=10";
	public static final String DNLA_OP_ALL_SEEKING_SUPPORTED = "DLNA.ORG_OP=11";
	
	public static final String DNLA_FLAGS_SEEK_AND_INTERACTIVE = "DLNA.ORG_FLAGS=01700000000000000000000000000000";
	public static final String DNLA_FLAGS_SEEK_ONLY = "DLNA.ORG_FLAGS=01500000000000000000000000000000";
	public static final String DNLA_FLAGS_UNDEFINED = "";
	
	/**
	 * 
	 */
	public AbstractConnectionManagerService() {
		super();
	}

	/**
	 * @param activeConnections
	 */
	public AbstractConnectionManagerService(ConnectionInfo... activeConnections) {
		super(activeConnections);
	}

	/**
	 * @param propertyChangeSupport
	 * @param sourceProtocolInfo
	 * @param sinkProtocolInfo
	 * @param activeConnections
	 */
	public AbstractConnectionManagerService(PropertyChangeSupport propertyChangeSupport,
			ProtocolInfos sourceProtocolInfo, ProtocolInfos sinkProtocolInfo, ConnectionInfo... activeConnections) {
		super(propertyChangeSupport, sourceProtocolInfo, sinkProtocolInfo, activeConnections);
	}

	/**
	 * @param sourceProtocolInfo
	 * @param sinkProtocolInfo
	 * @param activeConnections
	 */
	public AbstractConnectionManagerService(ProtocolInfos sourceProtocolInfo, ProtocolInfos sinkProtocolInfo,
			ConnectionInfo... activeConnections) {
		super(sourceProtocolInfo, sinkProtocolInfo, activeConnections);
	}

	/**
	 * @param sourceProtocolInfo
	 * @param sinkProtocolInfo
	 */
	public AbstractConnectionManagerService(ProtocolInfos sourceProtocolInfo, ProtocolInfos sinkProtocolInfo) {
		super(sourceProtocolInfo, sinkProtocolInfo);
	}

	public static ProtocolInfo newDefaultPlayerProtocolInfo(String contentType, String dnlaProfile) {
        return new ProtocolInfo(
                Protocol.HTTP_GET,
                "*",
                contentType,
                dnlaProfile + ";" + DNLA_OP_NO_SEEKING_SUPPORTED + ";" + DNLA_FLAGS_UNDEFINED
            );
	}
	
	public static ProtocolInfo newDefaultServerProtocolInfo(String contentType, String dnlaProfile) {
        return new ProtocolInfo(
                Protocol.HTTP_GET,
                "*",
                contentType,
                dnlaProfile + ";" + DNLA_OP_BYTESEEKING_SUPPORTED + ";" + DNLA_FLAGS_SEEK_ONLY
            );
	}
	
}

package de.einwesen.heimklangwelle.upnpsupport.services;

import org.jupnp.support.connectionmanager.ConnectionManagerService;
import org.jupnp.support.model.Protocol;
import org.jupnp.support.model.ProtocolInfo;
import org.jupnp.support.model.ProtocolInfos;

// UPNP annotations are inherited from parent
public class ConnectionManagerServiceImpl extends ConnectionManagerService {
    
    public ConnectionManagerServiceImpl() {
        super(
                new ProtocolInfos(), // sourceProtocolInfo (empty for renderer)
                new ProtocolInfos(
                        new ProtocolInfo(Protocol.HTTP_GET, "*", "video/*", 
                            "DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000"),
                        new ProtocolInfo(Protocol.HTTP_GET, "*", "audio/*", 
                            "DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000")
                    ) // sink
            );
    }
    
}

package de.einwesen.heimklangwelle.contentdirectory;

import org.jupnp.support.model.ProtocolInfos;

import de.einwesen.heimklangwelle.upnpsupport.AbstractConnectionManagerService;

// UPNP annotations are inherited from parent
public class MediaServerConnectionManagerServiceImpl extends AbstractConnectionManagerService {
    
	public static ProtocolInfos SUPPORTED_PROTOCOLS = new ProtocolInfos(
    		newDefaultServerProtocolInfo("audio/mpeg"),
    		newDefaultServerProtocolInfo("audio/flac"),
    		newDefaultServerProtocolInfo("audio/ogg"),
    		newDefaultServerProtocolInfo("audio/mp4"), //m4a
    		newDefaultServerProtocolInfo("audio/aac"),
    		newDefaultServerProtocolInfo("audio/x-matroska"), //mka
    		newDefaultServerProtocolInfo("video/x-matroska"),  
    		newDefaultServerProtocolInfo("video/mp4"),                 		
    		newDefaultServerProtocolInfo("audio/x-mpegurl"),
    		newDefaultServerProtocolInfo("audio/mpegurl"),
    		newDefaultServerProtocolInfo("application/vnd.apple.mpegurl"),
    		newDefaultServerProtocolInfo("image/png"),
    		newDefaultServerProtocolInfo("image/jpeg"),
    		newDefaultServerProtocolInfo("image/gif")
    );
	
    public MediaServerConnectionManagerServiceImpl() {
        super(	SUPPORTED_PROTOCOLS, 
                new ProtocolInfos() // sinkProtocolInfo (empty for server) 
            );
    }
        
}


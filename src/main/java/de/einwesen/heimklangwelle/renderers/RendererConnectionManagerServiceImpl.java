package de.einwesen.heimklangwelle.renderers;

import org.jupnp.support.model.ProtocolInfos;

import de.einwesen.heimklangwelle.upnpsupport.AbstractConnectionManagerService;

// UPNP annotations are inherited from parent
public class RendererConnectionManagerServiceImpl extends AbstractConnectionManagerService {
    
    public RendererConnectionManagerServiceImpl() {
        super(
                new ProtocolInfos(), // sourceProtocolInfo (empty for renderer)
                new ProtocolInfos( // sink
                		newDefaultPlayerProtocolInfo("audio/mpeg"),
                		newDefaultPlayerProtocolInfo("audio/flac"),
                		newDefaultPlayerProtocolInfo("audio/ogg"),
                		newDefaultPlayerProtocolInfo("audio/mp4"),
                		newDefaultPlayerProtocolInfo("video/mp4"),
                		newDefaultPlayerProtocolInfo("audio/aac"),
                		newDefaultPlayerProtocolInfo("audio/x-matroska"), //
                		newDefaultPlayerProtocolInfo("video/x-matroska"),        		
                		newDefaultPlayerProtocolInfo("audio/x-mpegurl"),
                		newDefaultPlayerProtocolInfo("audio/mpegurl"),
                		newDefaultPlayerProtocolInfo("application/vnd.apple.mpegurl")
                ) 
        );
    }
    
}

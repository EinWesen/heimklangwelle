package de.einwesen.heimklangwelle.renderers;

import org.jupnp.support.connectionmanager.ConnectionManagerService;
import org.jupnp.support.model.Protocol;
import org.jupnp.support.model.ProtocolInfo;
import org.jupnp.support.model.ProtocolInfos;

// UPNP annotations are inherited from parent
public class RendererConnectionManagerServiceImpl extends ConnectionManagerService {
    
    public RendererConnectionManagerServiceImpl() {
        super(
                new ProtocolInfos(), // sourceProtocolInfo (empty for renderer)
                new ProtocolInfos( // sink
                        // MP3
                        new ProtocolInfo(
                            Protocol.HTTP_GET,
                            "*",
                            "audio/mpeg",
                            "DLNA.ORG_PN=MP3;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000"
                        ),

                        // AAC (ISO base media)
                        new ProtocolInfo(
                            Protocol.HTTP_GET,
                            "*",
                            "audio/mp4",
                            "DLNA.ORG_PN=AAC_ISO;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000"
                        ),

                        // FLAC (DLNA 1.5)
                        new ProtocolInfo(
                            Protocol.HTTP_GET,
                            "*",
                            "audio/flac",
                            "DLNA.ORG_PN=FLAC;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000"
                        ),

                        // OGG / Vorbis
                        new ProtocolInfo(
                            Protocol.HTTP_GET,
                            "*",
                            "audio/ogg",
                            "DLNA.ORG_PN=OGG;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000"
                        ),

                        // H.264 + AAC in MP4
                        new ProtocolInfo(
                            Protocol.HTTP_GET,
                            "*",
                            "video/mp4",
                            "DLNA.ORG_PN=AVC_MP4_BL_CIF15_AAC;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01700000000000000000000000000000"
                        )                		
                ) 
            );
    }
    
    
    
}

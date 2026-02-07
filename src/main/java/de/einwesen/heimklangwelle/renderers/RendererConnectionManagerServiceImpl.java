package de.einwesen.heimklangwelle.renderers;

import org.jupnp.support.model.ProtocolInfos;

import de.einwesen.heimklangwelle.upnpsupport.AbstractConnectionManagerService;

// UPNP annotations are inherited from parent
public class RendererConnectionManagerServiceImpl extends AbstractConnectionManagerService {
    
    public RendererConnectionManagerServiceImpl() {
        super(
                new ProtocolInfos(), // sourceProtocolInfo (empty for renderer)
                new ProtocolInfos( // sink
                		newDefaultPlayerProtocolInfo("audio/mpeg", "DLNA.ORG_PN=MP3"),
                		newDefaultPlayerProtocolInfo("audio/flac", "DLNA.ORG_PN=FLAC"),
                		newDefaultPlayerProtocolInfo("audio/ogg", "DLNA.ORG_PN=OGG"),
                		newDefaultPlayerProtocolInfo("audio/mp4", "DLNA.ORG_PN=AAC_ISO"),
                		newDefaultPlayerProtocolInfo("video/mp4", "DLNA.ORG_PN=AVC_MP4_BL_CIF15_AAC"),
                		newDefaultPlayerProtocolInfo("video/mp4", "DLNA.ORG_PN=AVC_MP4_MP_SD_AAC"),
                		newDefaultPlayerProtocolInfo("video/mp4", "DLNA.ORG_PN=AVC_MP4_MP_HD_720p_AAC")                                                    
                ) 
        );
    }
    
}

package de.einwesen.heimklangwelle.contentdirectory;

import org.jupnp.support.model.ProtocolInfos;

import de.einwesen.heimklangwelle.upnpsupport.AbstractConnectionManagerService;

// UPNP annotations are inherited from parent
public class MediaServerConnectionManagerServiceImpl extends AbstractConnectionManagerService {
    
    public MediaServerConnectionManagerServiceImpl() {
        super(
                new ProtocolInfos(
                		newDefaultServerProtocolInfo("audio/mpeg", "DLNA.ORG_PN=MP3"),
                		newDefaultServerProtocolInfo("audio/flac", "DLNA.ORG_PN=FLAC"),
                		newDefaultServerProtocolInfo("audio/ogg", "DLNA.ORG_PN=OGG"),
                		newDefaultServerProtocolInfo("audio/mp4", "DLNA.ORG_PN=AAC_ISO"),
                		newDefaultServerProtocolInfo("video/mp4", "DLNA.ORG_PN=AVC_MP4_BL_CIF15_AAC"),
                		newDefaultServerProtocolInfo("video/mp4", "DLNA.ORG_PN=AVC_MP4_MP_SD_AAC"),
                		newDefaultServerProtocolInfo("video/mp4", "DLNA.ORG_PN=AVC_MP4_MP_HD_720p_AAC")              
                ), 
                new ProtocolInfos() // sinkProtocolInfo (empty for server) 
            );
    }
        
}

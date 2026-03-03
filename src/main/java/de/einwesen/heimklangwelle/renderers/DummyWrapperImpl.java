package de.einwesen.heimklangwelle.renderers;

import java.util.List;

import org.jupnp.model.action.ActionException;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.model.Channel;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.renderingcontrol.RenderingControlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyWrapperImpl extends AbstractRendererWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRendererWrapper.class);
	
	private volatile boolean isMute = false;
	private volatile long volume = 100;
    private volatile long playlistSize = 0;	

	public DummyWrapperImpl() {
		super();
		LOGGER.info("created");
		this.ready = true;
	}

	@Override
	public boolean isMute(Channel channel) throws RenderingControlException {
		return this.isMute;
	}

	@Override
	public void setMute(Channel channel, boolean desiredMute) throws RenderingControlException {
		LOGGER.info("SetMute: " +  channel.name() + " " + String.valueOf(desiredMute));
		this.isMute = desiredMute;
		firePlayerVolumneChangedEvent();
	}

	@Override
	public void setVolume(Channel channel, long desiredVolume) throws RenderingControlException {
		LOGGER.info("SetVol: " +  channel.name() + " " + String.valueOf(desiredVolume));
		this.isMute = false;
		this.volume = desiredVolume;		
		firePlayerVolumneChangedEvent();
	}

	@Override
	public long getVolume(Channel channel) throws RenderingControlException {
		return this.volume;
	}

	@Override
	public boolean loadCurrentContentMetaData() throws AVTransportException {
		String u = this.getCurrentTransportURI();
    	if (DATAURI_DYNAMIC_PLAYLIST.equals(u)) {
    		return false; 
    	} else {
    		if (u.toLowerCase().endsWith(".m3u")) {
    			this.playlistSize = 2;
    		} else {
    			this.playlistSize = 1;
    		}		
    		this.currentTrack = 1;
    		LOGGER.info("loadCurrentContent: " +  getCurrentTransportURI() + " -> " + this.getPlaylistSize());
    		return false;
    	}
	}

	@Override
	public void nextTrack() throws AVTransportException {
		if (this.currentTrack < this.playlistSize) {
			this.currentTrack += 1;
		} else {
			throw new AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET);
		}		
	}

	@Override
	public void previousTrack() throws AVTransportException {
		if (this.currentTrack > 1) {
			this.currentTrack -= 1;
		} else {
			throw new AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET);
		}				
	}


	@Override
	public void play() throws AVTransportException {
		if (getCurrentTransportURI() != null) {
			LOGGER.info("play!");
			setPlayerStateAndFire(TransportState.PLAYING);
		} else {
			throw new AVTransportException(AVTransportErrorCode.TRANSITION_NOT_AVAILABLE);
		}
	}

	@Override
	public void stop() throws AVTransportException {
		LOGGER.info("stop!");
		if (this.playerState != TransportState.NO_MEDIA_PRESENT) {
			setPlayerStateAndFire(TransportState.STOPPED);	
		} else {
			throw new AVTransportException(AVTransportErrorCode.TRANSITION_NOT_AVAILABLE);
		}		
	}

	@Override
	public void pause() throws AVTransportException {
		if (this.playerState == TransportState.PLAYING) {
			LOGGER.info("pause!");
			setPlayerStateAndFire(TransportState.PAUSED_PLAYBACK);
		} else if (this.playerState != TransportState.PAUSED_PLAYBACK) {
			throw new AVTransportException(AVTransportErrorCode.TRANSITION_NOT_AVAILABLE);
		}		
	}

	@Override
	public long getPlaylistSize() throws AVTransportException {
		return this.playlistSize;
	}

	@Override
	public long getCurrentTrack() throws AVTransportException {
		return this.currentTrack;
	}

	@Override
	public long getCurrentTrackPosition() throws AVTransportException {
		return 0;
	}

	@Override
	public void addAVTransportURI(String currentURI, String currentURIMetaData) throws AVTransportException {
		throw new AVTransportException(AVTransportErrorCode.MEDIA_PROTECTED);		
	}

	@Override
	public void ejectMedia() throws AVTransportException {
		this.playlistSize = 0;	
		super.ejectMedia();
	}

	@Override
	public List<String> getTrackURIsMetaData() throws ActionException {
		throw new ActionException(ErrorCode.ACTION_NOT_AUTHORIZED);
	}

	@Override
	public void removeTrackAtIndex(long index) throws ActionException {
		throw new ActionException(ErrorCode.ACTION_NOT_AUTHORIZED);
		
	}

	@Override
	public void moveTrackAtIndex(long index, long toIndex) throws ActionException {
		throw new ActionException(ErrorCode.ACTION_NOT_AUTHORIZED);		
	}	
	
}

package de.einwesen.heimklangwelle.renderers;

import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.model.Channel;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.renderingcontrol.RenderingControlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyWrapperImpl extends AbstractRendererWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRendererWrapper.class);
	
	private boolean isMute = false;
	private long volume = 100;

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
		this.volume = desiredVolume;
		firePlayerVolumneChangedEvent();
	}

	@Override
	public long getVolume(Channel channel) throws RenderingControlException {
		return this.volume;
	}

	@Override
	public void loadCurrentContent() throws AVTransportException {
		LOGGER.info("loadCurrentContent: " +  getCurrentURI());
		setPlayerStateAndFire(TransportState.STOPPED);
	}

	@Override
	public void nextTrack() throws AVTransportException {
		LOGGER.info("next!");
		throw new AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET);
		//firePlayerStateChangedEvent();
	}

	@Override
	public void previousTrack() throws AVTransportException {
		LOGGER.info("prev!");
		throw new AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET);
		//firePlayerStateChangedEvent();		
	}

	@Override
	public void play() throws AVTransportException {
		if (getCurrentURI() != null) {
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
		return getCurrentTrack();
	}

	@Override
	public long getCurrentTrack() throws AVTransportException {
		if (!"".equalsIgnoreCase(getCurrentURI())) {
			return 1;			
		} else {
			return 0;
		}
	}

	@Override
	public long getCurrentTrackPosition() throws AVTransportException {
		return -1;
	}
	
}

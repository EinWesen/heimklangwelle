package de.einwesen.heimklangwelle.renderers;

import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.model.Channel;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.renderingcontrol.RenderingControlException;

public class DummyWrapperImpl extends AbstractRendererWrapper {

	private boolean isMute = false;
	private long volume = 100;

	public DummyWrapperImpl() {
		super();
		System.out.println("new Dummy Impl");
	}

	@Override
	public boolean isMute(Channel channel) throws RenderingControlException {
		return this.isMute;
	}

	@Override
	public void setMute(Channel channel, boolean desiredMute) throws RenderingControlException {
		System.out.println("SetMute: " +  channel.name() + " " + String.valueOf(desiredMute));
		this.isMute = desiredMute;
	}

	@Override
	public void setVolume(Channel channel, long desiredVolume) throws RenderingControlException {
		System.out.println("SetVol: " +  channel.name() + " " + String.valueOf(desiredVolume));
		this.volume = desiredVolume;
	}

	@Override
	public long getVolume(Channel channel) throws RenderingControlException {
		return this.volume;
	}

	@Override
	public void loadCurrentContent() throws AVTransportException {
		System.out.println("loadCurrentContent: " +  getCurrentURI());
		this.playerState = TransportState.STOPPED;
		firePlayerStateChangedEvent();
	}

	@Override
	public void nextTrack() throws AVTransportException {
		System.out.println("next!");
		throw new AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET);
		//firePlayerStateChangedEvent();
	}

	@Override
	public void previousTrack() throws AVTransportException {
		System.out.println("prev!");
		throw new AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET);
		//firePlayerStateChangedEvent();		
	}

	@Override
	public void play() throws AVTransportException {
		if (getCurrentURI() != null) {
			System.out.println("play!");
			if (this.playerState != TransportState.PLAYING) {
				this.playerState = TransportState.PLAYING;
				firePlayerStateChangedEvent();						
			}
		} else {
			throw new AVTransportException(AVTransportErrorCode.TRANSITION_NOT_AVAILABLE);
		}
	}

	@Override
	public void stop() throws AVTransportException {
		System.out.println("stop!");
		if (this.playerState != TransportState.NO_MEDIA_PRESENT) {
				if (this.playerState != TransportState.STOPPED) {
					this.playerState = TransportState.STOPPED;
					firePlayerStateChangedEvent();
				}
		} else {
			throw new AVTransportException(AVTransportErrorCode.TRANSITION_NOT_AVAILABLE);
		}		
	}

	@Override
	public void pause() throws AVTransportException {
		if (this.playerState == TransportState.PLAYING) {
			System.out.println("pause!");
			this.playerState = TransportState.PAUSED_PLAYBACK;
			firePlayerStateChangedEvent();
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

}

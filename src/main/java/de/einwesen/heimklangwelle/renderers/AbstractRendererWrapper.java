package de.einwesen.heimklangwelle.renderers;

import java.util.ArrayList;

import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.model.Channel;
import org.jupnp.support.model.TransportAction;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.model.TransportStatus;
import org.jupnp.support.renderingcontrol.RenderingControlException;

import de.einwesen.heimklangwelle.upnpsupport.RendererChangeEventListener;

public abstract class AbstractRendererWrapper {
	
	protected UnsignedIntegerFourBytes instanceId = new UnsignedIntegerFourBytes(0);
	protected Channel[] channels = new Channel[]{Channel.Master};
	protected TransportState playerState = TransportState.NO_MEDIA_PRESENT;
	protected TransportStatus errorState = TransportStatus.OK;
	
	protected String currentURI = ""; 
	protected String currentURIMetaData = "";
	protected ArrayList<RendererChangeEventListener> changeListeners = new ArrayList<>(); 
	
	public boolean addListener(RendererChangeEventListener e) {
		return changeListeners.add(e);
	}
	
	protected void firePlayerStateChangedEvent() {
		for (RendererChangeEventListener l : this.changeListeners) {
			l.firePlayerStateChangedEvent(this.instanceId);
		}
	}
	
	public UnsignedIntegerFourBytes getInstanceId() {
		return instanceId;
	}

	public Channel[] getCurrentChannels() {
		return channels;
	}	
	
	public TransportState getPlayState() {
		return playerState;
	}		
	
	public TransportStatus getErrorState() {
		return errorState;
	}
	
	public TransportAction[] getCurrentAllowedPlayerOperations() throws Exception {
		//TODO: Needs to check for loaded tracks and such
		
		switch (this.playerState) {
			case PAUSED_PLAYBACK:
				if (this.getPlaylistSize()>1) {
					return new TransportAction[] {TransportAction.Play, TransportAction.Next, TransportAction.Previous, /* TransportAction.Seek */};
				} else {
					return new TransportAction[] {TransportAction.Play, /* TransportAction.Seek */};					
				}
			
			case PLAYING:
				return new TransportAction[] {TransportAction.Pause, TransportAction.Next, TransportAction.Previous, /* TransportAction.Seek */};
			
			case STOPPED:
				if (this.getPlaylistSize()>1) {
					return new TransportAction[] {TransportAction.Play, TransportAction.Next, TransportAction.Previous};
				} else {
					return new TransportAction[] {TransportAction.Play, TransportAction.Next, TransportAction.Previous};
				}
						
			case TRANSITIONING:
			case NO_MEDIA_PRESENT:
				// no action allowed during this
				return new TransportAction[0];
			
			case PAUSED_RECORDING:
			case RECORDING:
			case CUSTOM:
			default:
				// 	Doont accept anything, my state is bad
				return new TransportAction[0]; 
		}
	}	
	
	public void setCurrentContent(String currentURI, String currentURIMetaData) throws AVTransportException {
		if (currentURI != null) {
			this.currentURI = currentURI;			
		} else {
			this.currentURI = "";
		}
		
		if (currentURIMetaData != null) {
			this.currentURIMetaData = currentURI;			
		} else {
			this.currentURIMetaData = "";
		}
		
		this.loadCurrentContent();
	}

	public String getCurrentURI() {
		return currentURI;
	}

	public String getCurrentURIMetaData() {
		return currentURIMetaData;
	}
	
	public String getDescription() {
		return this.getClass().getName();
	}

	public abstract boolean isMute(Channel channel) throws RenderingControlException;
	public abstract void setMute(Channel channel, boolean desiredMute)  throws RenderingControlException;
	public abstract void setVolume(Channel channel, long desiredVolume) throws RenderingControlException;
	public abstract long getVolume(Channel channel) throws RenderingControlException;

	//throw new AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET)
	public abstract void loadCurrentContent() throws AVTransportException;
	public abstract void nextTrack() throws AVTransportException;
	public abstract void previousTrack() throws AVTransportException;
	public abstract void play() throws AVTransportException;
	public abstract void stop() throws AVTransportException;
	public abstract void pause() throws AVTransportException;
	public abstract long getPlaylistSize() throws AVTransportException;
	public abstract long getCurrentTrack() throws AVTransportException;
}

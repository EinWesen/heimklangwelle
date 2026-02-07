package de.einwesen.heimklangwelle.renderers;

import java.util.ArrayList;

import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.model.Channel;
import org.jupnp.support.model.TransportAction;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.model.TransportStatus;
import org.jupnp.support.renderingcontrol.RenderingControlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.upnpsupport.RendererChangeEventListener;

public abstract class AbstractRendererWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRendererWrapper.class);
	
	protected UnsignedIntegerFourBytes instanceId = new UnsignedIntegerFourBytes(0);
	protected Channel[] channels = new Channel[]{Channel.Master};
	
	protected volatile TransportState playerState = TransportState.NO_MEDIA_PRESENT;
	protected volatile TransportStatus errorState = TransportStatus.OK;
	
	protected volatile String currentURI = ""; 
	protected volatile String currentURIMetaData = "";
	protected volatile boolean ready = false;
	
	protected ArrayList<RendererChangeEventListener> changeListeners = new ArrayList<>(); 
	
	public boolean addListener(RendererChangeEventListener e) {
		return changeListeners.add(e);
	}
		
	protected void fireEvent(Runnable r) {
		// JAVA 21:  Thread.startVirtualThread(r);
		new Thread(r).start();
	}
	
	protected void firePlayerStateChangedEvent() {
		LOGGER.debug("fire!");
		fireEvent(new Runnable() {
			@Override
			public void run() {
				synchronized (changeListeners) {
					for (RendererChangeEventListener l : changeListeners) {
						l.firePlayerStateChangedEvent(instanceId);
					}								
				}
			}
		});
	}
	
    protected void setPlayerStateAndFire(TransportState state) {
    	if (this.playerState != state) {    		
    		LOGGER.debug("fire!:" + state);
    		System.out.println("fore:" +  state);
    		this.playerState = state;
    		this.firePlayerStateChangedEvent();
    	}
    }	
	
	protected void firePlayerVolumneChangedEvent() {
		LOGGER.debug("fire!");
		fireEvent(new Runnable() {
			@Override
			public void run() {		
				synchronized (changeListeners) {
					for (RendererChangeEventListener l : changeListeners) {			
						l.firePlayerVolumneChangedEvent(instanceId);
					}			
				}
			}
		});
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
		//FIXME: Next & Previous depends on Playlist position
		switch (this.playerState) {
			case PAUSED_PLAYBACK:
				if (this.getPlaylistSize()>1) {
					return new TransportAction[] {TransportAction.Play, TransportAction.Next, TransportAction.Previous, /* TransportAction.Seek */};
				} else {
					return new TransportAction[] {TransportAction.Play, /* TransportAction.Seek */};					
				}
			
			case PLAYING:
				if (this.getPlaylistSize()>1) {
					return new TransportAction[] {TransportAction.Pause, TransportAction.Stop, TransportAction.Next, TransportAction.Previous, /* TransportAction.Seek */};
				} else {
					return new TransportAction[] {TransportAction.Pause, TransportAction.Stop};
				}
			
			case STOPPED:
				if (this.getPlaylistSize()>1) {
					return new TransportAction[] {TransportAction.Play, TransportAction.Next, TransportAction.Previous};
				} else {
					return new TransportAction[] {TransportAction.Play};
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
			this.currentURIMetaData = currentURIMetaData;			
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
	
	public boolean isReady() {
		return ready;
	}

	public void shutdown() {
		// Overwrite if needed
	}

	public abstract boolean isMute(Channel channel) throws RenderingControlException;
	public abstract void setMute(Channel channel, boolean desiredMute)  throws RenderingControlException;
	public abstract void setVolume(Channel channel, long desiredVolume) throws RenderingControlException;
	public abstract long getVolume(Channel channel) throws RenderingControlException;

	public abstract void loadCurrentContent() throws AVTransportException;
	public abstract void nextTrack() throws AVTransportException;
	public abstract void previousTrack() throws AVTransportException;
	public abstract void play() throws AVTransportException;
	public abstract void stop() throws AVTransportException;
	public abstract void pause() throws AVTransportException;
	public abstract long getPlaylistSize() throws AVTransportException;
	public abstract long getCurrentTrack() throws AVTransportException;
	public abstract long getCurrentTrackPosition() throws AVTransportException;
	
	
}

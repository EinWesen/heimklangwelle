package de.einwesen.heimklangwelle.renderers;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.Channel;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.TransportAction;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.model.TransportStatus;
import org.jupnp.support.model.item.AudioItem;
import org.jupnp.support.model.item.Item;
import org.jupnp.support.renderingcontrol.RenderingControlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRendererWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRendererWrapper.class);
	
	protected UnsignedIntegerFourBytes instanceId = new UnsignedIntegerFourBytes(0);
	protected Channel[] channels = new Channel[]{Channel.Master};
	
	protected volatile TransportState playerState = TransportState.NO_MEDIA_PRESENT;
	protected volatile TransportStatus errorState = TransportStatus.OK;
	
	protected volatile String currentTransportURI = ""; 
	protected volatile String currentTransportURIMetaData = "";
	protected volatile String currentTrackURI = ""; 
	protected volatile String currentTrackURIMetaData = "";
	
	protected volatile boolean ready = false;
	
	protected ArrayList<RendererChangeEventListener> changeListeners = new ArrayList<>();
	
	private final ScheduledExecutorService eventScheduler = Executors.newSingleThreadScheduledExecutor();
	private volatile ScheduledFuture<?> pendingStateTask = null;
	private volatile ScheduledFuture<?> pendingVolumneTask = null;
	
	public boolean addListener(RendererChangeEventListener e) {
		return changeListeners.add(e);
	}
	
	protected void firePlayerStateChangedEvent() {
		LOGGER.trace("fire!");
		if (this.pendingStateTask != null) {
			if(this.pendingStateTask.cancel(false)) {
				LOGGER.trace("Debounced previous event!");
			};
		}
		
		this.pendingStateTask = this.eventScheduler.schedule(new Runnable() {
			@Override
			public void run() {
				LOGGER.debug("Scheduled stateupdate fired!");
				synchronized (changeListeners) {
					for (RendererChangeEventListener l : changeListeners) {
						l.firePlayerStateChangedEvent(instanceId);
					}								
				}
			}
		}, 500, TimeUnit.MILLISECONDS);			
	}
	
    protected void setPlayerStateAndFire(TransportState state) {
    	if (this.playerState != state) {    		
    		LOGGER.trace("fire!:" + state);
    		this.playerState = state;
    		this.firePlayerStateChangedEvent();
    	}
    }	
	
	protected void firePlayerVolumneChangedEvent() {
		LOGGER.trace("fire!");
		if (this.pendingVolumneTask != null) {
			if(this.pendingVolumneTask.cancel(false)) {
				LOGGER.trace("Debounced previous event!");
			};
		}
		
		this.pendingVolumneTask = this.eventScheduler.schedule(new Runnable() {
			@Override
			public void run() {
				LOGGER.debug("Scheduled volumeupdate fired!");
				synchronized (changeListeners) {
					for (RendererChangeEventListener l : changeListeners) {
						l.firePlayerStateChangedEvent(instanceId);
					}								
				}
			}
		}, 500, TimeUnit.MILLISECONDS);		
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
			this.currentTransportURI = currentURI;			
		} else {
			this.currentTransportURI = "";
		}
		
		
		if (currentURIMetaData != null) {
			this.currentTransportURIMetaData = currentURIMetaData;			
		} else {
			this.currentTransportURIMetaData = "";
		}
		
		this.loadCurrentContent();
	}

	public String getCurrentTransportURI() {
		return currentTransportURI;
	}

	public String getCurrentTransportURIMetaData() {
		return currentTransportURIMetaData;
	}
	
	public String getDescription() {
		return this.getClass().getName();
	}
	
	public boolean isReady() {
		return ready;
	}

	public void shutdown() {
		this.eventScheduler.shutdownNow();
	}

	public String getCurrentTrackURI() {
		if (this.currentTrackURI != null) {
			return this.currentTrackURI;			
		} else {
			return this.currentTransportURI;
		}
	}

	public String getCurrentTrackURIMetaData() {
		if (this.currentTrackURIMetaData != null) {
			return this.currentTrackURIMetaData;			
		} else {
			return this.currentTransportURIMetaData;
		}
	}

	protected String generateSubTrackMetaData() {
		final DIDLParser parser = new DIDLParser();
		
		String trackDisplay = " [?/?]";
		long currentTrack = -1;		
		try {
			currentTrack = this.getCurrentTrack();
			trackDisplay = " ["+ currentTrack + " / "+  this.getPlaylistSize() +"]"; 			
		} catch (Throwable t) {
			LOGGER.debug("Could not get currentTrackInfo: " + t.toString() );
		}
		
		String title = this.currentTransportURI + trackDisplay;
		String transportId = null;
		if (this.currentTransportURIMetaData != null) {
			try {
				final DIDLContent content = parser.parse(this.currentTransportURIMetaData);
				final Item item =  content.getItems().get(0);
				title = item.getTitle() + trackDisplay;
				transportId = item.getId();
			} catch (Throwable t) {
				LOGGER.debug("Could not parse currentTrackURIMetaData: " + t.toString() );
			}
		}
		
		final AudioItem audioItem = new AudioItem();
		audioItem.setId("-1_" + transportId + "_" + currentTrack);
		audioItem.setParentID("-1");
		audioItem.setTitle(title);
		audioItem.setRestricted(true);
		
		try {
			DIDLContent result = new DIDLContent();
			result.addItem(audioItem);
			return parser.generate(result);		
		} catch (Throwable e) {
			LOGGER.debug("Could generate currentTransportURIMetaData: " + e.toString() );
			return "";
		}

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

package de.einwesen.heimklangwelle.renderers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.model.Channel;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.renderingcontrol.RenderingControlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.HeimklangStation;
import de.einwesen.heimklangwelle.util.IpcChannelBridge;
import de.einwesen.heimklangwelle.util.RollingSequence;

public class MPVRendererWrapper extends AbstractRendererWrapper {
	
	public static final String CONFIG_PROPERTY_MPV_PATH = "HEIMKLANG_CONFIG_MPV_PATH";
	public static final String CONFIG_PROPERTY_MPVPIPE_PATH = "HEIMKLANG_CONFIG_MPVPIPE_PATH";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MPVRendererWrapper.class);
	private static final String CMD_OBSERVE_PROPERTY = "observe_property";
	private static final String CMD_SET_PROPERTY = "set_property";
	private static final String CMD_GET_PROPERTY = "get_property";

	private static enum MPVEventType {
		UNKNOWN_EVENT(""), 
		START_FILE("start-file"), 
		FILE_LOADED("file-loaded"), 
		PLAYBACK_RESTART("playback-restart"), 
		END_FILE("end-file"), 
		IDLE("idle"), 
		PROPERTY_CHANGE("property-change"),
		AUDIO_RECONFIG("audio-reconfig");		
		
		MPVEventType(String s) {
			this.jsonKey = s;
		}
		private final String jsonKey;
		
		public static MPVEventType fromEventName(String n) {
			for (MPVEventType t : MPVEventType.values()) {
				if (t.jsonKey.equals(n)) {
					return t;
				}
			}
			return MPVEventType.UNKNOWN_EVENT;
		}		
	}
	
	private static enum ObservedProperty {
		UNKNOWN_PROPERTY(""), 
		PLAYLIST_COUNT("playlist-count"), 
		PLAYLIST_POS("playlist-playing-pos"),
		PAUSE("pause"),
		VOLUME("volume"),
		TIME_POS("time-pos"),
		PLAYLIST_ITEM_PATH("path");
		
		private final String propertyName;
		ObservedProperty(String s) {
			this.propertyName = s;
		}
		public String getPropertyName() {
			return propertyName;
		}
		public static ObservedProperty fromPropertyName(String n) {
			for (ObservedProperty t : ObservedProperty.values()) {
				if (t.propertyName.equals(n)) {
					return t;
				}
			}
			return ObservedProperty.UNKNOWN_PROPERTY;
		}		
	}
	
    private final String mpvPath;
    private final String ipcPipePath;
    private final Class<? extends java.nio.channels.Channel> ipcType;

    private Process mpv;
    private IpcChannelBridge ipc;
    private Thread ipcConsumer;

    private volatile boolean isPaused = false;
    private volatile long volume = 100;  
    private volatile long preMuteVolume = -1;
    private volatile long currentTrackTimePos = 0;       
    private volatile long playlistSize = 0;
    private volatile List<String> currentPlaylistMetaData = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean isPlaylistWriteable = false;
    
    private volatile String version = this.getClass().getName();
    
    private RollingSequence requestIdSeq = new RollingSequence(0, 100, 0);
    private Map<Integer, CompletableFuture<JSONObject>> waitingRequests = Collections.synchronizedMap(new HashMap<>(10)); 
    
    public MPVRendererWrapper() throws IOException {
    	if (HeimklangStation.isOnWindows()) {
    		this.ipcType = java.nio.channels.AsynchronousFileChannel.class;
    		this.ipcPipePath = HeimklangStation.getConfigProperty(CONFIG_PROPERTY_MPVPIPE_PATH, "\\\\.\\pipe\\heimklang_mpvpipe") + "_" + Integer.toHexString(this.hashCode());
    		this.mpvPath = HeimklangStation.getConfigProperty(CONFIG_PROPERTY_MPV_PATH, "mpv.exe");
    	} else {
    		this.ipcType = java.nio.channels.SocketChannel.class;
    		this.ipcPipePath = HeimklangStation.getConfigProperty(CONFIG_PROPERTY_MPVPIPE_PATH, "/tmp/heimklang_mpvpipe_" + Integer.toHexString(this.hashCode())+".sock");
    		this.mpvPath = HeimklangStation.getConfigProperty(CONFIG_PROPERTY_MPV_PATH, "mpv");    		
    	}
    	startMPV();
    }

    private void startMPV() throws IOException {
        mpv = new ProcessBuilder(
                mpvPath,
                "--idle",
                "--no-terminal",
                "--no-video",
                "--quiet",
                "--input-ipc-server=" + ipcPipePath
        ).start();        

        ipc = new IpcChannelBridge(this.ipcType, this.ipcPipePath, this::ipcError);
        //JAVA 21: this.ipcConsumer = Thread.startVirtualThread(this.ipcConsumer());
        this.ipcConsumer = new Thread(this.ipcConsumer());
        this.ipcConsumer.start();
        
        for (ObservedProperty p : ObservedProperty.values()) {
        	if (p != ObservedProperty.UNKNOWN_PROPERTY) {
        		sendCommand(new Object[]{CMD_OBSERVE_PROPERTY, p.ordinal(),p.getPropertyName()});        		
        	}
        }
        
        this.version = initVesionString();
        this.ready = true;
        LOGGER.info(this.version + " ready");
    }
    
	private String initVesionString() {
    	try {
			final JSONObject result = fetchCommandResult(new String[] {CMD_GET_PROPERTY,"mpv-version"}).get(5, TimeUnit.SECONDS);
			return result.optString("data", this.getClass().getName());
		} catch (Throwable e) {
			return this.getClass().getName();
		}
	}
    
    private int getNextRequestId() {
    	synchronized (requestIdSeq) {
    		return requestIdSeq.nextInt();
		}
    }	
    
    private CompletableFuture<JSONObject> fetchCommandResult(Object[] cmd) {
    	
    	LOGGER.debug(Arrays.toString(cmd));    		
    	
    	final CompletableFuture<JSONObject> future = new CompletableFuture<>();
    	final Integer requestId = Integer.valueOf(getNextRequestId());
	    	
    	try {
	    
		    // Build command String
		    final JSONArray cmdParts = new JSONArray(); 
		    for (Object p : cmd) {
		    	cmdParts.put(p);
		    }
		    	
		    final String cmdString = new JSONObject().put("command",  cmdParts).put("request_id", requestId).toString();
	       	
	    	
	    	if (cmdString != null) {

	    		// Put a future in a global map, so that teh async response can set the result
	    		this.waitingRequests.put(requestId, future);
	    		
	    		LOGGER.trace(cmdString);	    		

	    		// if this returns true, is accepted into the sending queue
	    		if (!this.ipc.writeln(cmdString)) {
	    			throw new IOException("command was not accepted by ipc channel");
	    		}
	    	
	    	} else {
	    		throw new JSONException("JSON-Result-String was null");
	    	}	    	
	    	
    	} catch (Throwable e) { 
    		LOGGER.error("Could not send command '"+Arrays.toString(cmd)+"'", e);
    		future.completeExceptionally(e);
    	}			

    	return future;
    }   

    public boolean sendCommand(Object[] cmd) {
    	try {
			final JSONObject result = fetchCommandResult(cmd).get(10, TimeUnit.SECONDS);
			return Boolean.valueOf("success".equals(result.optString("error", "")));
		} catch (TimeoutException e) {
			LOGGER.error("Waited too long for response",e);
			return false;
		} catch (Throwable e) {
			return false;
		}
    }    
	
    private void sendCommandElseThrowTransportException(Object[] command, AVTransportErrorCode errorCode) throws AVTransportException {
		if (!sendCommand(command)) {
			throw new AVTransportException(errorCode);
		}				
	}

	private void sendCommandElseThrowTransportException(Object[] command) throws AVTransportException {
		sendCommandElseThrowTransportException(command, AVTransportErrorCode.TRANSPORT_LOCKED);
	}
	
    private void sendCommandElseThrowActionException(Object[] command, ErrorCode errorCode) throws ActionException {
		if (!sendCommand(command)) {
			throw new ActionException(errorCode);
		}				
	}
	
    
    private void ipcError(ExecutionException e) {
    	LOGGER.warn("Error in IPC", e);
    }
    
    private Runnable ipcConsumer() {
    	return new Runnable() {			
			@Override
			public void run() {
				try {    		
					while(ipc.isReading() || !ipc.isOutputBufferEmpty()) {
						final String data = ipc.readLine(2, TimeUnit.SECONDS);
						if (data != null) { // Only null if timeout was reached
							handleEvent(new JSONObject(data));
						}
					}
				} catch (InterruptedException t) {
					// This should mean thread is being killed
					LOGGER.error("ipcConsumer was interrupted", t);
				}    	
				
			}
		};
    }
    
    private void handleEvent(JSONObject event) {
    	if (LOGGER.isTraceEnabled() ||  !"time-pos".equals(event.optString("name", ""))) {
    		LOGGER.debug(event.toString());    		    		
    	} 
    	switch(MPVEventType.fromEventName(event.optString("event", ""))) {
	    	case START_FILE:
	    		this.playerState = TransportState.TRANSITIONING;
	    		this.firePlayerStateChangedEvent();
	    		break;
	    	case PLAYBACK_RESTART:
	    		// Fires when a track is loaded during pause, but does not start play
	    		if (!this.isPaused) {
	    			this.setPlayerStateAndFire(TransportState.PLAYING);	    			
	    		}
	    		break;
			case END_FILE:
				this.currentTrackTimePos = 0;
				
				// If paused and a new track is loaded, this triggers a stop, but the player staýs paused
				if (this.playlistSize > 1 &&  this.currentTrack < this.playlistSize) {
					this.setPlayerStateAndFire(TransportState.TRANSITIONING);					
				} else {
					this.setPlayerStateAndFire(TransportState.STOPPED);										
				}
				
				break;			
			case IDLE:
				if (this.playerState != TransportState.NO_MEDIA_PRESENT) {
					this.currentTrack = 0;
					this.playerState = TransportState.STOPPED;
					this.firePlayerStateChangedEvent();
					
//					// We want to keep the playlist, so lets just readd everything
//					if (DATAURI_DYNAMIC_PLAYLIST.equals(this.currentTransportURI) && this.isPlaylistWriteable) {
//						this.addAVTransportURIsFromMetaData();
//					}
					
				}
				break;
			case PROPERTY_CHANGE:
				switch(ObservedProperty.fromPropertyName(event.getString("name"))) {
					case PLAYLIST_COUNT:
						final long cnt = event.getLong("data");
						if (cnt != this.playlistSize) {
							this.playlistSize = cnt;
							this.firePlayerStateChangedEvent();
						}
						break;
					case PAUSE:
						this.isPaused = event.getBoolean("data");
						// MPV send pause off at the start for some reason, even without media
						if (this.playerState != TransportState.NO_MEDIA_PRESENT) {							
							if (this.isPaused) {
								this.setPlayerStateAndFire(TransportState.PAUSED_PLAYBACK);						
							} else {
								this.setPlayerStateAndFire(TransportState.PLAYING);
							}							
						}
						break;
					case VOLUME:
						final long v = event.getBigDecimal("data").longValue();
						if (v != this.volume) {
							this.volume = v;
							this.firePlayerVolumneChangedEvent();
						}
						break;
					case TIME_POS:
						if (event.has("data")) {
							this.currentTrackTimePos = event.getBigDecimal("data").longValue();							
						}
						break;
					case PLAYLIST_POS:
						this.currentTrack =  event.getLong("data") + 1;
						this.firePlayerStateChangedEvent();
						break;
					case PLAYLIST_ITEM_PATH: {						
						final String trackUri = event.optString("data", ""); 
						if (!this.currentTransportURI.equals(trackUri) && !"".equals(trackUri)) {
							this.currentTrackURI = trackUri;						
							if (this.currentPlaylistMetaData.size() > 0 ) {
								try {
									this.currentTrackURIMetaData = this.currentPlaylistMetaData.get(Long.valueOf(this.currentTrack-1).intValue());
								} catch (ArrayIndexOutOfBoundsException a) {
									this.currentTrackURIMetaData = generateSubTrackMetaData();
								}
							} else {
								this.currentTrackURIMetaData = generateSubTrackMetaData();
							}
							
							if (this.playerState == TransportState.TRANSITIONING) {
								this.setPlayerStateAndFire(TransportState.STOPPED);
							}
						} else {
							this.currentTrackURI = null;
							this.currentTrackURIMetaData = null;
							
						}
						break;
					}
					case UNKNOWN_PROPERTY:					
					default:
				}
				break;
			case FILE_LOADED:
				if (this.playerState == TransportState.TRANSITIONING) {
					this.setPlayerStateAndFire(TransportState.STOPPED);
				}
				break;
			case UNKNOWN_EVENT:
				if (event.has("error")) {					
					handleCommandResult(event);					
				}
				break;
			case AUDIO_RECONFIG:
			default:
    	}
    }
    
    private void handleCommandResult(JSONObject event) {
    	if (event.has("request_id")) {
	    	final Integer id = event.getInt("request_id");
	    	final CompletableFuture<JSONObject> future = this.waitingRequests.get(id);
	    	if (future != null) {
	    		this.waitingRequests.remove(id);
	    		future.complete(event);
	    	}
    	}
    }
       
    // Cleanup
    public void shutdown() {    
    	this.ready = false;

    	// tell mpv to quite
    	if (this.mpv.isAlive()) {
    		
    		// if we still can
    		if (!ipc.isClosed()) {
    			this.ipc.expectCloseByRemote();
    			try {
					sendCommand(new Object[]{"quit"});
				} catch (Throwable e) {
					LOGGER.warn("could not send quit to process", e);
				}    		    			
    		}
    		
    	}
    	
    	// If mpv is still alive, kill the process
        try {
        	if (this.mpv.isAlive()) {
        		this.mpv.destroy();         		
        	}
        } catch (Throwable e) {
        	LOGGER.warn("could not stop process", e);
        }

        // close the ipc if needed
        if (!this.ipc.isClosed()) {
        	try { this.ipc.close(); } catch (Throwable ignored) {}        	
        }
        
        if (this.ipcConsumer.isAlive()) {
        	// Lets  wait a short moment
        	try { Thread.sleep(TimeUnit.SECONDS.toMillis(5));} catch (InterruptedException ignored) {}
        	try {
        		if (this.ipcConsumer.isAlive()) {
        			this.ipcConsumer.isInterrupted();        			
        		}
			} catch (Throwable ignored) {}
        }
        
        if (this.playerState != TransportState.NO_MEDIA_PRESENT) {
        	this.setPlayerStateAndFire(TransportState.STOPPED);
        	this.currentTransportURI = "";
        	this.currentTransportURIMetaData = "";
        	this.currentTrackTimePos = 0;
        	this.currentTrack = 0;
        	this.setPlayerStateAndFire(TransportState.NO_MEDIA_PRESENT);        	
        }
        
        super.shutdown();
    }
        
    @Override
    public boolean loadCurrentContentMetaData() {
    	this.currentPlaylistMetaData.clear();
    	this.currentTrackTimePos = 0;
    	this.isPlaylistWriteable = false;
    	this.playlistSize = 0;
    	this.currentTrack = 1;
    	
    	final String u = getCurrentTransportURI();
    	LOGGER.debug(u);

    	if (DATAURI_DYNAMIC_PLAYLIST.equals(u)) {
    		this.isPlaylistWriteable = true;
    		this.currentTrack = 0;
    	} else if (u.toLowerCase().endsWith(".m3u") || u.toLowerCase().endsWith(".m3u8")) {
			this.currentPlaylistMetaData.addAll(parseSubTrackMetaData(u));
			this.playlistSize = this.currentPlaylistMetaData.size();
		} else {
			this.currentPlaylistMetaData.add(getCurrentTransportURIMetaData());
			this.playlistSize = 1;
		}
    	
    	return true;
    }
	
	@Override
	public void setMute(Channel channel, boolean desiredMute) throws RenderingControlException {
		synchronized (this.mpv) {
			final long tmp = this.preMuteVolume;
			try {
				
				if (desiredMute) {
					if (this.preMuteVolume == -1) {
						this.preMuteVolume = this.volume;
						this.setVolume(channel, 0);
					}
				} else {
					if (this.preMuteVolume > -1) {
						this.preMuteVolume = -1;
						this.setVolume(channel, tmp);
					}
				}
			} catch (RenderingControlException e) {
				this.preMuteVolume = tmp;
				throw e;
			}
			
		}
		
	}
    
	@Override
	public void setVolume(Channel channel, long desiredVolume) throws RenderingControlException {
		if (desiredVolume != this.volume || this.preMuteVolume > -1) {
			if (!sendCommand(new Object[]{CMD_SET_PROPERTY, "volume", Long.valueOf(desiredVolume)})) {
				throw new RenderingControlException(ErrorCode.ACTION_FAILED);
			};			
		}
	}	
	
	@Override
	public void nextTrack() throws AVTransportException {
		if (this.currentTrack < this.playlistSize) {
			sendCommandElseThrowTransportException(new String[]{"playlist-next"});
		} else {
			throw new AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET);
		}		
	}

	@Override
	public void previousTrack() throws AVTransportException {
		if (this.currentTrack > 1) {
			sendCommandElseThrowTransportException(new String[]{"playlist-prev"});
		} else {
			throw new AVTransportException(AVTransportErrorCode.ILLEGAL_SEEK_TARGET);
		}				
	}

	
	@Override
	public void play() throws AVTransportException {
		final String u = getCurrentTransportURI();
    	
		if (DATAURI_DYNAMIC_PLAYLIST.equals(u)) {
			if(this.playlistSize == 0) {
				throw new AVTransportException(AVTransportErrorCode.NO_CONTENTS);
			}
			
			if (this.currentTrack == 0 && this.playlistSize > 0) {
				this.seekTrack(1);
			}
			
		} else {

			if (this.playerState != TransportState.PAUSED_PLAYBACK) {				
				
				if (u.toLowerCase().endsWith(".m3u") || u.toLowerCase().endsWith(".m3u8")) {
					sendCommandElseThrowTransportException(new Object[]{"loadlist", u, "replace"}, AVTransportErrorCode.READ_ERROR);
				} else {
					sendCommandElseThrowTransportException(new Object[]{"loadfile", u, "replace"}, AVTransportErrorCode.READ_ERROR);
				}
				
			}
			
		}
		
		if (this.isPaused) {
			sendCommandElseThrowTransportException(new Object[]{CMD_SET_PROPERTY, "pause", Boolean.FALSE});			
		}
	}

	@Override
	public void pause() throws AVTransportException {
		sendCommandElseThrowTransportException(new Object[]{CMD_SET_PROPERTY, "pause", Boolean.TRUE});
	}	

	@Override
	public void stop() throws AVTransportException {
		sendCommandElseThrowTransportException(new Object[]{"stop"});
	}
	
	@Override
	public void seekTrack(long trackNo) throws AVTransportException {
		sendCommandElseThrowTransportException(new Object[]{"playlist-play-index", trackNo-1}, AVTransportErrorCode.ILLEGAL_SEEK_TARGET);
	}	
	
	@Override
	public boolean isMute(Channel channel) throws RenderingControlException {
		return this.preMuteVolume > -1;
	}

	@Override
	public long getVolume(Channel channel) throws RenderingControlException {
		return this.volume;
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
	public String getDescription() {
		return this.version;
	}
    
	@Override
	public long getCurrentTrackPosition() throws AVTransportException {
		return this.currentTrackTimePos;
	}

	@Override
	public void ejectMedia() throws AVTransportException {
		if (this.playerState == TransportState.PLAYING || this.playerState == TransportState.PAUSED_PLAYBACK) {
			this.stop();
		}		
		sendCommandElseThrowTransportException(new Object[]{"playlist-clear"}, AVTransportErrorCode.CONTENT_BUSY);
		sendCommandElseThrowTransportException(new Object[]{CMD_SET_PROPERTY, "pause", Boolean.TRUE});
		this.playlistSize = 0;
		this.currentPlaylistMetaData.clear();
		this.currentTrackTimePos = 0;
		super.ejectMedia();
	}

	@Override
	public void addAVTransportURI(String nextURI, String nextURIMetaData) throws AVTransportException {
		if (this.isPlaylistWriteable) {

			if (nextURI.toLowerCase().endsWith(".m3u") || nextURI.toLowerCase().endsWith(".m3u8")) {
				// We do not support adding full lists for now
				
				//sendCommandElseThrowTransportException(new Object[]{"loadlist", u, "append"}, AVTransportErrorCode.READ_ERROR);
				throw new AVTransportException(AVTransportErrorCode.PLAYBACK_FORMAT_NOT_SUPPORTED);
			} else {
				synchronized (this.currentPlaylistMetaData) {
					this.currentPlaylistMetaData.add(nextURIMetaData);
					try {
						sendCommandElseThrowTransportException(new Object[]{"loadfile", nextURI, "append"}, AVTransportErrorCode.READ_ERROR);
						
						try {
							if (this.currentPlaylistMetaData.size() == 1) {
								this.seekTrack(1);
							}
						} catch (Throwable t) {
							LOGGER.warn("could not seek to first track in list", t);
						}
						
					} catch (AVTransportException e) {
						this.currentPlaylistMetaData.remove(this.currentPlaylistMetaData.size()-1);
						throw e;
					}				
				}
			}
			
		} else {
			throw new AVTransportException(AVTransportErrorCode.MEDIA_PROTECTED);
		}
		
	}
	

		
	@Override
	public List<String> getTrackURIsMetaData() throws ActionException {
		return new ArrayList<>(this.currentPlaylistMetaData);
	}	
		
	@Override
	public void removeTrackAtIndex(long index) throws ActionException {
		sendCommandElseThrowActionException(new Object[] {"playlist-remove", index}, ErrorCode.ACTION_FAILED);
		this.currentPlaylistMetaData.remove((int)index);
		
		if ((index + 1) < this.currentTrack) {
			this.currentTrack -= 1;				
		}
		
		// MPV fires an event it seems
		//this.playlistSize -= 1; 		
		//this.firePlayerStateChangedEvent();
		
	}

	@Override
	public void moveTrackAtIndex(long index, long toIndex) throws ActionException {
		/* playlist-move <index1> <index2>
		 * 
		 * Move the playlist entry at index1, so that it takes the place of the entry index2.
		 * (Paradoxically, the moved playlist entry will not have the index value index2 after moving
		 * if index1 was lower than index2, because index2 refers to the target entry, not the index the entry will have after moving.)
		 * 
		 * => Meaning:
		 * The moved entry is placed before index2 
		 */
				
		// The value may changed after command (at leats when being teh current track) 
		final long trackBeforeMove = this.currentTrack;
		
		if (index > toIndex) {
			sendCommandElseThrowActionException(new Object[] {"playlist-move", index, toIndex}, ErrorCode.ACTION_FAILED);			
		} else {
			// +1 because its an actual move, and weed to set it after, just like teh dom
			sendCommandElseThrowActionException(new Object[] {"playlist-move", index, toIndex + 1}, ErrorCode.ACTION_FAILED);
		}
		
		// Update metadata, this works as it is, because we rmeove the index first
		final String prevValue = this.currentPlaylistMetaData.remove((int)index);		
		this.currentPlaylistMetaData.add((int)toIndex, prevValue);

		// NOt update the current track, mpv send an event only when its teh current
		if ((index + 1) == trackBeforeMove ) {
			this.currentTrack = toIndex + 1;
			this.firePlayerStateChangedEvent();	
		} else if ((index + 1) < trackBeforeMove && (toIndex + 1) > trackBeforeMove) {
			this.currentTrack = trackBeforeMove - 1;
			this.firePlayerStateChangedEvent();	
		} else if ((index + 1) > trackBeforeMove && (toIndex + 1) <= trackBeforeMove) {
			this.currentTrack = trackBeforeMove + 1;
			this.firePlayerStateChangedEvent();	
		}
		
	}		
	
}

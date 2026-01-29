package de.einwesen.heimklangwelle.upnpsupport.services;

import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.avtransport.AbstractAVTransportService;
import org.jupnp.support.model.DeviceCapabilities;
import org.jupnp.support.model.MediaInfo;
import org.jupnp.support.model.PlayMode;
import org.jupnp.support.model.PositionInfo;
import org.jupnp.support.model.RecordMediumWriteStatus;
import org.jupnp.support.model.StorageMedium;
import org.jupnp.support.model.TransportAction;
import org.jupnp.support.model.TransportInfo;
import org.jupnp.support.model.TransportSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.HeimklangStation;
import de.einwesen.heimklangwelle.renderers.AbstractRendererWrapper;
import de.einwesen.heimklangwelle.upnpsupport.RendererChangeEventListener;
import de.einwesen.heimklangwelle.upnpsupport.annotations.UpnpExclude;

public class SingleInstanceAVTransportServiceImpl extends AbstractAVTransportService implements RendererChangeEventListener {
	
	private final Logger LOGGER = LoggerFactory.getLogger(SingleInstanceAVTransportServiceImpl.class);
	private static final DeviceCapabilities SUPPORTED_CAPABILITIES = new DeviceCapabilities(new StorageMedium[] {StorageMedium.NETWORK});

	private final AbstractRendererWrapper backendInstance;
	private final UnsignedIntegerFourBytes[] instanceIds;
	
	public SingleInstanceAVTransportServiceImpl() {
		super();
		this.backendInstance = HeimklangStation.getCurrentRendererInstance(this);
		this.instanceIds = new UnsignedIntegerFourBytes[] {this.backendInstance.getInstanceId()};
	}
	
	private void validateInstanceId(UnsignedIntegerFourBytes instanceId) throws AVTransportException {		
		if (!instanceId.equals(instanceIds[0])) {
			throw new AVTransportException(AVTransportErrorCode.INVALID_INSTANCE_ID);
		}
	}

	@Override
	public void firePlayerStateChangedEvent(UnsignedIntegerFourBytes instanceId) {
		try {
			this.appendCurrentState(getLastChange(), instanceId);			
		} catch (Throwable t) {
			LOGGER.warn("Updating lastChanges failed", t);
		}
	}
	
	@Override
	//REQUIRED
	public void setAVTransportURI(UnsignedIntegerFourBytes instanceId, String currentURI, String currentURIMetaData) throws AVTransportException {
		validateInstanceId(instanceId);
		this.backendInstance.setCurrentContent(currentURI, currentURIMetaData);
		firePlayerStateChangedEvent(instanceId);
	}

	@Override
	public void stop(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		validateInstanceId(instanceId);
		this.backendInstance.stop();
		firePlayerStateChangedEvent(instanceId);
	}

	@Override
	public void play(UnsignedIntegerFourBytes instanceId, String speed) throws AVTransportException {
		validateInstanceId(instanceId);
		if (!"1".equals(speed)) {
			throw new AVTransportException(717, "Play speed not supported");			
		}
		this.backendInstance.play();
		firePlayerStateChangedEvent(instanceId);
	}

	@Override
	public void pause(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		validateInstanceId(instanceId);
		this.backendInstance.pause();
		firePlayerStateChangedEvent(instanceId);
	}

	@Override
	public void next(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		validateInstanceId(instanceId);
		this.backendInstance.nextTrack();
		firePlayerStateChangedEvent(instanceId);
	}

	@Override
	public void previous(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		validateInstanceId(instanceId);
		this.backendInstance.previousTrack();	
		firePlayerStateChangedEvent(instanceId);
	}
	
	@Override
	@UpnpExclude // We don't want to support this right now
	public void seek(UnsignedIntegerFourBytes instanceId, String unit, String target) throws AVTransportException {
		validateInstanceId(instanceId);
		throw new AVTransportException(AVTransportErrorCode.SEEKMODE_NOT_SUPPORTED, "Unsupported seek mode: " + unit);
		
//		SeekMode seekMode;
//        try {
//            seekMode = SeekMode.valueOrExceptionOf(unit);
//        } catch (IllegalArgumentException e) {
//            throw new AVTransportException(AVTransportErrorCode.SEEKMODE_NOT_SUPPORTED, "Unsupported seek mode: " + unit);
//        }
        
		/*
		 * AVTransport:3 — Standardized DCP (SDCP) – March 31, 2013 38
		Table 20 — Format of A_ARG_TYPE_SeekTarget
		Value of A_ARG_TYPE_SeekMode Format of A_ARG_TYPE_SeekTarget
		“TRACK_NR” ui4
		“ABS_TIME” Formatted as specified in subclause 5.2.16
		“REL_TIME” Formatted as specified in subclause 5.2.16
		“ABS_COUNT” ui4
		“REL_COUNT” i4
		“CHANNEL_FREQ” float, expressed in Hz.
		“TAPE-INDEX” ui4
		“REL_TAPE-INDEX” i4
		“FRAME” ui4
		“REL_FRAME” i4        
		 */        
		
	}	

	@Override
	@UpnpExclude // We don't want to support this right now
	public void setPlayMode(UnsignedIntegerFourBytes instanceId, String newPlayMode) throws AVTransportException {
		validateInstanceId(instanceId);
		if (PlayMode.valueOf(newPlayMode) != PlayMode.NORMAL) {
			throw new AVTransportException(AVTransportErrorCode.PLAYMODE_NOT_SUPPORTED);
		}		
	}
	
	@Override
	@UpnpExclude // We don't want to support this right now
	public void setNextAVTransportURI(UnsignedIntegerFourBytes instanceId, String nextURI, String nextURIMetaData) throws AVTransportException {				
		validateInstanceId(instanceId);
		throw new AVTransportException(AVTransportErrorCode.CONTENT_BUSY);
	}	
	
	@Override
	@UpnpExclude // We don't want to support this right now
	public void setRecordQualityMode(UnsignedIntegerFourBytes instanceId, String newRecordQualityMode) throws AVTransportException {
		validateInstanceId(instanceId);
		throw new AVTransportException(AVTransportErrorCode.RECORDQUALITYMODE_NOT_SUPPORTED);
	}
	
	@Override
	@UpnpExclude // We don't want to support this right now
	public void record(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		validateInstanceId(instanceId);
		throw new AVTransportException(AVTransportErrorCode.MEDIA_PROTECTED);		
	}		
	
	
	@Override
	//REQUIRED
	public MediaInfo getMediaInfo(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		/*
		 *  NumberOfTracks state variable description:
		 *
		 *  This variable indicates the number of tracks available in the currently selected media resource.
		 *
		 *  Rules for setting NumberOfTracks:
		 *  - If no resource is associated (via SetAVTransportURI()) and there's no default resource, NumberOfTracks shall be 0.
		 *  - If the implementation can't determine the track count, NumberOfTracks shall be 0.
		 *  - Otherwise, it must be 1 or higher.
		 *
		 *  Determining Track Count:
		 *  - It might take time to determine the exact track count (e.g., large playlists).
		 *  - Implementations can use a placeholder value (e.g., 1) until the count is known, updating periodically.
		 *  - A LastChange event with defined moderation period is always generated when NumberOfTracks is updated.
		 *
		 *  Track Definition:
		 *  - For track-unaware media (e.g., radio), NumberOfTracks will always be 1.
		 *  - For LD/DVD media, a track is a chapter number.
		 *  - For Tuners with indexed channels, a track is an index in the channel list.
		 *
		 *  Consistency with AVTransportURI:
		 *    - NumberOfTracks must match the resource identified by AVTransportURI.
		 *    - Single MP3 file: NumberOfTracks = 1
		 *    - Playlist file: NumberOfTracks = number of playlist entries.
		 */
		final UnsignedIntegerFourBytes numberOfTracks = new UnsignedIntegerFourBytes(this.backendInstance.getPlaylistSize());
		
		/*
		 * CurrentMediaDuration: This state variable holds the duration of the currently selected media.
		 * 
		 *  - Format: Same as CurrentTrackDuration (see spec for details).
		 *  - Value when no content is associated: "00:00:00".
		 *  - Value if duration can't be determined: "00:00:00" (implementation dependent until exact duration is known).
		 * 
		 *  - Fractional components (optional): set to either "0" or "0/<F1>".
		 *  - Updates: Implementation dependent, but LastChange event with defined moderation period shall be generated on update.
		 *  - NOT_IMPLEMENTED: Set if media duration information is not supported by the implementation. 
		 */
		final String mediaDuration = "NOT_IMPLEMENTED";								
 
		/*
		 * AVTransportURI:  This required state variable is a URI referencing the resource controlled by the AVTransport instance.
		 *
		 * - Represents either a single item (e.g., a song) or a collection of items (e.g., a playlist).
		 * - Collection case: AVTransport has multiple tracks, AVTransportURI remains constant during track changes.
		 * - Single item case: AVTransport has 1 track and AVTransportURI equals CurrentTrackURI.
		 * - Can be used to retrieve metadata via the ContentDirectory service. 
		 */ 		
	    final String currentURI = this.backendInstance.getCurrentURI();
	    
	    /*
	     * CurrentMetadata: This required state variable holds metadata associated with the resource pointed to by AVTransportURI.
	     * 
	     * - Format: DIDL-Lite XML Fragment (see ContentDirectory service specification [7] for details).
	     *  - Value if not supported: "NOT_IMPLEMENTED".
	     */
	    final String currentURIMetaData = this.backendInstance.getCurrentURIMetaData();
		
	    /*
	     * NextAVTransportURI: This required state variable holds the URI to be played after the current AVTransportURI completes playback. 
	     * 
	     * - Used for seamless transitions, especially with protocols requiring buffering (e.g., HTTP).
	     * - Set using SetNextAVTransportURI() action.
	     * - Does NOT represent track-level transitions within a playlist. Plays when the entire playlist finishes.
	     * - Value if not supported: "NOT_IMPLEMENTED".  
	     */ 
	    final String nextURI = "NOT_IMPLEMENTED";
	    
	    /*
	     * NextMetadata: This required state variable holds metadata associated with the resource pointed to by NextAVTransportURI.
	     * 
	     * - Format: DIDL-Lite XML Fragment (see ContentDirectory service specification [7] for details).
	     * - Value if not supported: "NOT_IMPLEMENTED".
	     */
	    final String nextURIMetaData = "NOT_IMPLEMENTED";
	    
	    final StorageMedium playMedium = StorageMedium.NETWORK;
	    final StorageMedium recordMedium = StorageMedium.NOT_IMPLEMENTED;
	    final RecordMediumWriteStatus recordWriteStatus = RecordMediumWriteStatus.NOT_IMPLEMENTED;
		
		return new MediaInfo(currentURI, currentURIMetaData, nextURI, nextURIMetaData, numberOfTracks, mediaDuration, playMedium, recordMedium, recordWriteStatus);
	}

	@Override
	// REQUIRED
	public PositionInfo getPositionInfo(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		/*
		 * CurrentTrack: This required state variable indicates the index number of the currently selected track.
		 * 
		 * - Value when NumberOfTracks is 0: 0
		 * - Range: 1 to NumberOfTracks (inclusive) for track-aware media.
		 * - Always 1 for track-unaware media.
		 * - Equivalent to chapter number for LD and DVD media.
		 * - Represents the current index in a channel list for Tuners.  
		 */
		final long track = this.backendInstance.getCurrentTrack();
	
		/*
		 * CurrentTrackDuration:  This required state variable holds the duration of the current track in the format H+:MM:SS[.F+].
		 *
		 * - Format: H+:MM:SS[.F+] or H+:MM:SS[.F0/F1]
		 *   - H+: One or more digits (hours)
		 *   - MM: Exactly two digits (minutes, 00 to 59)
		 *   - SS: Exactly two digits (seconds, 00 to 59)
		 *   - F+: One or more digits (fractional seconds)
		 *   - F0/F1: A fraction with F0 and F1 at least one digit long, and F0 < F1.
		 * - May be preceded by "+" or "-" sign. Decimal point is omitted if no fractional seconds.
		 * 
		 * - Value if duration can't be determined: "00:00:00".
		 * - Fractional components (optional): set to either "0" or "0/<F1>".
		 * 
		 * - Implementation-dependent until exact duration is known.  May use a default value ("00:00:00")
		 *   or periodically update the value. 
		 * - LastChange event with defined moderation period generated upon update.
		 * - Value set to "NOT_IMPLEMENTED" if track duration information is not supported.
		 */
		final String trackDuration = "NOT_IMPLEMENTED";
	    
		/*
		 * CurrentTrackMetaData: This required state variable holds metadata associated with the resource pointed to by CurrentTrackURI.
		 * 
		 * - Format: DIDL-Lite XML Fragment (see ContentDirectory service specification).
		 * - Source: Either extracted from AVTransportURIMetaData or directly from the resource binary itself (e.g., ID3 tags in MP3 files). Implementation dependent.
		 * - Value if not supported: "NOT_IMPLEMENTED". 
		 */  		
	    final String trackMetaData = "NOT_IMPLEMENTED";
	    
	    /*
	     *  CurrentTrackURI: This state variable holds a URI reference to the current track. 
	     *
	     *  This URI allows control points to retrieve metadata (title, author etc.) 
	     *  about the current track using the ContentDirectory service's Browse() or Search() actions.
	     *  If the media contains multiple tracks but doesn't have individual URIs for each,
	     * CurrentTrackURI is set to AVTransportURI.
	     */ 
	    final String trackURI = this.backendInstance.getCurrentURI();
	    
	    /*
	     *  The current position in the media, measured from a zero reference point. 
	     *  
	     *  For track-aware media (e.g., a CD), this is the time elapsed into the 
	     *  current track, ranging from "00:00:00" to the duration of the current track
	     *  (CurrentTrackDuration). The value will always be positive.
	     *  
	     *  For track-unaware media (e.g., a single tape), this is the time elapsed 
	     *  from the zero reference point on the media, which can range from negative 
	     *  values (before the zero reference point) to the end of the media.
	     *  
	     *  The time format follows the same standard as CurrentTrackDuration.
	     *  If relative time-based position information is not supported, this will 
	     *  be set to "NOT_IMPLEMENTED".
	     */
	    final String relTime = "NOT_IMPLEMENTED";
	    
	    /*
	     * The current absolute time position within the media, measured from the beginning.
	     *
	     *  - Range: "00:00:00" to the duration of the media (CurrentMediaDuration).
	     *  - Format: Same as CurrentTrackDuration.
	     *  - Value: Always positive. 
	     *
	     * If position information is not supported, this will be set to "NOT_IMPLEMENTED".
	     * Devices without time positions but able to detect end of media will use 
	     * "END_OF_MEDIA" when at the end, and "NOT_IMPLEMENTED" otherwise.
	     */ 
	    final String absTime = "NOT_IMPLEMENTED";
	    
	    /*
	     *  The current position in the media, represented as a dimensionless counter.
	     *  
	     *  For track-aware media (e.g., a CD), this counts from 0 to the end of the 
	     *  current track. Always positive.
	     *  
	     *  For track-unaware media (e.g., a single tape), this counts from a zero 
	     *  reference point on the media. Can be negative if the zero reference point
	     *  does not coincide with the beginning of the media.
	     *
	     *  Devices with addressable ranges exceeding the counter's limits will scale
	     *  actual media addresses to fit within this counter's range.
	     * 
	     * If count-based position information is not supported, this will be set
	     * to the maximum value of the i4 data type.
	     */ 
	    final int relCount = Integer.MAX_VALUE; 
	    
	    /*
	     * The current absolute position within the loaded media, represented as a dimensionless counter.
	     * 
	     *  - Range: [0, 2147483646] (inclusive).
	     *  - Values exceeding this range will be scaled by the AVTransport service.
	     * 
	     * If absolute count-based position information is not supported,
	     * this will be set to 2147483647. 
	     *
	     * Note: While the data type is ui4, the range is restricted to [0, Max(i4)] 
	     * for backward compatibility.
	     */ 
	    final int absCount = Integer.MAX_VALUE;

	    return new PositionInfo(track, trackDuration, trackMetaData, trackURI, relTime, absTime, relCount, absCount);
	}	
	
	@Override
	public TransportInfo getTransportInfo(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		validateInstanceId(instanceId);
		return new TransportInfo(this.backendInstance.getPlayState(), this.backendInstance.getErrorState(), "1");
	}		
	
	@Override
	// REQUIRED
	public TransportSettings getTransportSettings(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		validateInstanceId(instanceId);
		return new TransportSettings(PlayMode.NORMAL);
	}
	

	
	@Override
	protected TransportAction[] getCurrentTransportActions(UnsignedIntegerFourBytes instanceId) throws Exception {
		validateInstanceId(instanceId);
		return this.backendInstance.getCurrentAllowedPlayerOperations();
	}
	
	
	@Override
	public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
		return instanceIds;
	}	
	
	@Override
	public DeviceCapabilities getDeviceCapabilities(UnsignedIntegerFourBytes instanceId) throws AVTransportException {
		return SUPPORTED_CAPABILITIES;
	}
	
}

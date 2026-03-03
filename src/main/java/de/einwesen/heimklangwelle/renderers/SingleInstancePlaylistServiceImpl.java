package de.einwesen.heimklangwelle.renderers;

import java.util.List;

import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.binding.annotations.UpnpStateVariables;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.StorageMedium;
import org.jupnp.support.model.WriteStatus;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.container.PlaylistContainer;
import org.jupnp.support.model.item.Item;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;


@UpnpService(serviceId = @UpnpServiceId("HeimklangPlaylistManagement"), serviceType = @UpnpServiceType(value = "HeimklangPlaylistManagement", version = 1) /*, stringConvertibleTypes = LastChange.class*/)
@UpnpStateVariables({
	// TODO: Without evcenting, there will be problems when multiple client change the playlist, but i dont plan to... 
	@UpnpStateVariable(name = "CurrentPlaylist", sendEvents = false, datatype = "string", defaultValue = "NOT_IMPLEMENTED"),
	
	@UpnpStateVariable(name = "A_ARG_TYPE_InstanceID", sendEvents = false, datatype = "ui4"),
	@UpnpStateVariable(name = "A_ARG_TYPE_TransportURI", sendEvents = false, datatype = "string", defaultValue = "NOT_IMPLEMENTED"),
	@UpnpStateVariable(name = "A_ARG_TYPE_TransportURIMetaData", sendEvents = false, datatype = "string", defaultValue = "NOT_IMPLEMENTED"),
	@UpnpStateVariable(name = "A_ARG_TYPE_PlaylistIndex", sendEvents = false, datatype = "ui4"),
})
public class SingleInstancePlaylistServiceImpl {

	private static final String EPHEMERAL_PLAYLIST_TITLE = "🎶🎶🎶";
	private static final String EPHEMERAL_PROTECTED_PLAYLIST_TITLE = "🎶";

	//private static final Logger LOGGER = LoggerFactory.getLogger(SingleInstancePlaylistServiceImpl.class);
		
	private final AbstractRendererWrapper backendInstance;
	private final UnsignedIntegerFourBytes[] instanceIds;
	
	public SingleInstancePlaylistServiceImpl() {
		super();
		this.backendInstance = HeimklangServiceRegistry.getCurrentRendererInstance(null);
		this.instanceIds = new UnsignedIntegerFourBytes[] {this.backendInstance.getInstanceId()};
	}
	
	private void validateInstanceId(UnsignedIntegerFourBytes instanceId) throws AVTransportException {		
		if (!instanceId.equals(instanceIds[0])) {
			throw new AVTransportException(AVTransportErrorCode.INVALID_INSTANCE_ID);
		}
	}	
	
    @UpnpAction(name = "ClearAVTransportUris")
    public void clearAVTransportUris(@UpnpInputArgument(name = "InstanceID", stateVariable = "A_ARG_TYPE_InstanceID") UnsignedIntegerFourBytes instanceId) throws ActionException {
    	validateInstanceId(instanceId);
    	this.backendInstance.setCurrentContent(AbstractRendererWrapper.DATAURI_DYNAMIC_PLAYLIST, generateDynamicPlaylistMetaData());		
    }	
	
    @UpnpAction(name = "AddAVTransportURI")
    public void addAVTransportURI(@UpnpInputArgument(name = "InstanceID", stateVariable = "A_ARG_TYPE_InstanceID") UnsignedIntegerFourBytes instanceId,
            					  @UpnpInputArgument(name = "NextAVTransportURI", stateVariable = "A_ARG_TYPE_TransportURI") String nextURI,
            					  @UpnpInputArgument(name = "NextAVTransportURIMetaData", stateVariable = "A_ARG_TYPE_TransportURIMetaData") String nextURIMetaData) throws ActionException {
    	
    	validateInstanceId(instanceId);
    	
    	if ("".equals(this.backendInstance.getCurrentTransportURI())) {
    		this.clearAVTransportUris(instanceId);
    	}
    	
    	this.backendInstance.addAVTransportURI(nextURI, nextURIMetaData);    	
    }
    
    @UpnpAction(name = "MoveAVTransportUri")    
    public void moveAVTransportUri(@UpnpInputArgument(name = "InstanceID", stateVariable = "A_ARG_TYPE_InstanceID") UnsignedIntegerFourBytes instanceId,
            					   @UpnpInputArgument(name = "PlIndexFrom", stateVariable = "A_ARG_TYPE_PlaylistIndex") UnsignedIntegerFourBytes plIndexFrom,
            					   @UpnpInputArgument(name = "PlIndexTo", stateVariable = "A_ARG_TYPE_PlaylistIndex") UnsignedIntegerFourBytes plIndexTo) throws ActionException {
    	

    	validateInstanceId(instanceId);
    	this.backendInstance.moveTrackAtIndex(plIndexFrom.getValue().longValue(), plIndexTo.getValue().longValue());
    }
    
    @UpnpAction(name = "RemoveAVTransportUri")
    public void removeAVTransportUri(@UpnpInputArgument(name = "InstanceID", stateVariable = "A_ARG_TYPE_InstanceID") UnsignedIntegerFourBytes instanceId,
    								 @UpnpInputArgument(name = "PlIndex", stateVariable = "A_ARG_TYPE_PlaylistIndex") UnsignedIntegerFourBytes plIndexFrom) throws ActionException {

    	validateInstanceId(instanceId);
    	this.backendInstance.removeTrackAtIndex(plIndexFrom.getValue().longValue());    	
    }
    
    @UpnpAction(name = "GetPlaylist", out = @UpnpOutputArgument(name = "CurrentPlaylist", stateVariable = "CurrentPlaylist"))
    public String getPlaylist(@UpnpInputArgument(name = "InstanceID", stateVariable = "A_ARG_TYPE_InstanceID") UnsignedIntegerFourBytes instanceId) throws ActionException {    
    	validateInstanceId(instanceId);
    	
    	final String curentTransportUrl = this.backendInstance.getCurrentTransportURI();
    	final List<String> trackMetaData = this.backendInstance.getTrackURIsMetaData();    	
    	
    	final Container plContainer;
    	if (AbstractRendererWrapper.DATAURI_DYNAMIC_PLAYLIST.equals(curentTransportUrl)) {
    		plContainer = getPlaylistContainer(EPHEMERAL_PLAYLIST_TITLE, WriteStatus.WRITABLE);
    	} else if (curentTransportUrl.endsWith(".m3u") || curentTransportUrl.endsWith(".m3u8")) {
    		plContainer = getPlaylistContainer(curentTransportUrl, WriteStatus.NOT_WRITABLE);
    	} else {
    		plContainer = getPlaylistContainer(EPHEMERAL_PROTECTED_PLAYLIST_TITLE, WriteStatus.NOT_WRITABLE);
    	}
    	
    	plContainer.setChildCount(trackMetaData.size());
    	
    	try {
			final DIDLParser parser = new DIDLParser();
			
			// Each entry has a header and footer, so we need to extract the item form each 
			// and add them to a new combined onject (similir to te result of "browse" on a server)
			for (String metaData : trackMetaData) {
				final DIDLContent content = parser.parse(metaData);
				// There is only oen Item to expected in any entry, 
				// but we dont loose to much by doing a loop 
				for (Item item : content.getItems()) {
					plContainer.addItem(item);
				}
			}
			
			
			final DIDLContent containerContent = new DIDLContent();
			containerContent.addContainer(plContainer);
			
			// Generating a container including items is most likely not spec comliant
			// But the whole action is not, so its fine ;)
			// I do this mostly , to be able to transport the information wheather the list is writeble or not
			return parser.generate(containerContent, true);
		} catch (Exception e) {
			throw new ActionException(ErrorCode.HUMAN_INTERVENTION_REQUIRED, e.toString());		
		}
    }
    
    private static String generateDynamicPlaylistMetaData() {
    	try {
    		final DIDLContent content = new DIDLContent();
			content.addContainer(getPlaylistContainer(EPHEMERAL_PLAYLIST_TITLE, WriteStatus.WRITABLE));
			return new DIDLParser().generate(content);
		} catch (Exception e) {
			throw new IllegalStateException("Implementation of " + DIDLParser.class.getName() + " changed",e);
		}    	
    }
    
    private static Container getPlaylistContainer(String title, WriteStatus writeStatus) {
    	final PlaylistContainer pl = new PlaylistContainer();
    	pl.setId("-1");
    	pl.setParentID("-1");
    	pl.setTitle(title);
    	pl.setStorageMedium(StorageMedium.NONE);
    	pl.setRestricted(writeStatus == WriteStatus.NOT_WRITABLE);
    	pl.setWriteStatus(writeStatus);
    	pl.setSearchable(false);
    	return pl;
    }
    
}

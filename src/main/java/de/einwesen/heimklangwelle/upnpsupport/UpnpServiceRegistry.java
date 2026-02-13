package de.einwesen.heimklangwelle.upnpsupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.UpnpResponse.Status;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpnpServiceRegistry {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(UpnpServiceRegistry.class);
	
	@SuppressWarnings("rawtypes")
	private final Map<String, Device> registeredMediaDevices = Collections.synchronizedMap(new HashMap<>());
	
	private final RegistryListener registryListener = new RegistryListener() {

	    @Override
	    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
	    	LOGGER.trace("Discovery Started: " + device.getType().getDisplayString() + " - " + device.getDisplayString());    	
	    }
	
	    @Override
	    public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception e) {
	    	LOGGER.debug("Discovery failed: " + device.getType().getDisplayString() + " - " + device.getDisplayString(), e);
	    }
	
	    @Override
	    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
	    	if (device.getType().getType().startsWith("Media")) {    		
	    		LOGGER.debug("Remote device available: " + device.getType().getDisplayString() + " - " + device.getDisplayString());    			    		
                updateDeviceCache(true, device, registeredMediaDevices);
	    	} else {
	    		LOGGER.trace("Remote device available: " + device.getType().getDisplayString() + " - " + device.getDisplayString());
	    	}
	    }
	    
	    @Override
	    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
	    	if (device.getType().getType().startsWith("Media")) {    		
	    		LOGGER.debug("Remote device removed: " + device.getType().getDisplayString() + " - " + device.getDisplayString());
	    		updateDeviceCache(false, device, registeredMediaDevices);
	    	} else {
	    		LOGGER.trace("Remote device removed: " + device.getType().getDisplayString() + " - " + device.getDisplayString());
	    	}
	    }
	
	    @Override
	    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
	    	LOGGER.trace("Remote device update: " + device.getDisplayString());
	    }
	
	    @Override
	    public void localDeviceAdded(Registry registry, LocalDevice device) {
	    	LOGGER.info("Local device available: " + device.getType().getDisplayString() + " - " + device.getDisplayString());
	    	updateDeviceCache(true, device, registeredMediaDevices);
	    }
	
	    @Override
	    public void localDeviceRemoved(Registry registry, LocalDevice device) {
	    	LOGGER.info("Local device removed: " + device.getType().getDisplayString() + " - " + device.getDisplayString());
	    	updateDeviceCache(false, device, registeredMediaDevices);
	    }
	
	    @Override
	    public void beforeShutdown(Registry registry) {
	    	LOGGER.info("Before shutdown, the registry has devices: " + registry.getDevices().size());
	    	registeredMediaDevices.clear();
	    }
	
	    @Override
	    public void afterShutdown() {
	    	LOGGER.info("Shutdown of registry complete!");
	    }
	};
    
	protected final UpnpService upnpService;
	
	public UpnpServiceRegistry(UpnpServiceConfiguration configuration) {
		this.upnpService = new UpnpServiceImpl(configuration);
	}
	
	public void startup() {
    	LOGGER.info("Starting jUPnP...");            
        this.upnpService.startup();           
        this.upnpService.getRegistry().addListener(this.registryListener);
	}    
    
	@SuppressWarnings("rawtypes")
	private void updateDeviceCache(boolean add, Device device, Map<String, Device> deviceCache) {
		final String identifierString = device.getIdentity().getUdn().getIdentifierString();
		
		if (device.getType().getType().startsWith("Media")) {
			if (add) {
				registeredMediaDevices.put(identifierString, device);
			} else {					
				registeredMediaDevices.remove(identifierString);
			}
		}
	}
    
    @SuppressWarnings("rawtypes")
	public Collection<Device> getRegisteredMediaDevices() {
		return new ArrayList<>(registeredMediaDevices.values());
	}
    
	@SuppressWarnings("rawtypes")
	public Device getRegisteredMediaDevice(final String udn) throws NoSuchElementException {
		return this.registeredMediaDevices.get(udn);
	}    

	public SubscriptionCallback registerCallback(SubscriptionCallback callback) {
		upnpService.getControlPoint().execute(callback);
		return callback;
	}
	
	/**
	 * This method executes the invocation, and returns the response object, even if 
	 * that respons is a fail-state.
	 * 
	 * Exceptions xshould only occur in unforseen states
	 * 
	 * @param invocation
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Future<UpnpResponse> executeAndReturnReponse(ActionInvocation invocation) {
		
		final CompletableFuture<UpnpResponse> future = new CompletableFuture<UpnpResponse>();
		
		this.upnpService.getControlPoint().execute(
			new ActionCallback(invocation) {
				@Override
			    public void success(ActionInvocation invocation) {
					future.complete(new UpnpResponse(Status.OK));
			    }

			    @Override
			    public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
			        if (operation != null) {
			        	future.complete(new UpnpResponse(operation.getStatusCode(), defaultMsg));			        		
			        } else {
			        	future.complete(new UpnpResponse(0, defaultMsg));
			        }
			    }
			}
		);
		
		return future;		
	}
	
	public void shutdown() {
    	this.upnpService.shutdown();
    }
	
}

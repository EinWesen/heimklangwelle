package de.einwesen.heimklangwelle.upnpsupport;

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpnpServiceRegistry {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(UpnpServiceRegistry.class);
		
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
	    	} else {
	    		LOGGER.trace("Remote device available: " + device.getType().getDisplayString() + " - " + device.getDisplayString());
	    	}
	    }
	
	    @Override
	    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
	    	LOGGER.trace("Remote device update: " + device.getDisplayString());
	    }
	
	    @Override
	    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
	    	if (device.getType().getType().startsWith("Media")) {    		
	    		LOGGER.debug("Remote device removed: " + device.getType().getDisplayString() + " - " + device.getDisplayString());    		
	    	} else {
	    		LOGGER.trace("Remote device removed: " + device.getType().getDisplayString() + " - " + device.getDisplayString());
	    	}
	    }
	
	    @Override
	    public void localDeviceAdded(Registry registry, LocalDevice device) {
	    	LOGGER.info("Local device available: " + device.getType().getDisplayString() + " - " + device.getDisplayString());
	    }
	
	    @Override
	    public void localDeviceRemoved(Registry registry, LocalDevice device) {
	    	LOGGER.info("Local device removed: " + device.getType().getDisplayString() + " - " + device.getDisplayString());
	    }
	
	    @Override
	    public void beforeShutdown(Registry registry) {
	    	LOGGER.info("Before shutdown, the registry has devices: " + registry.getDevices().size());
	    }
	
	    @Override
	    public void afterShutdown() {
	    	LOGGER.info("Shutdown of registry complete!");
	    }
	};
    
	protected final UpnpService upnpService;
	
	public UpnpServiceRegistry() {
		this.upnpService = new UpnpServiceImpl(new DefaultUpnpServiceConfiguration());
	}
	
	public void startup() {
    	LOGGER.info("Starting jUPnP...");            
        this.upnpService.startup();           
        this.upnpService.getRegistry().addListener(this.registryListener);
	}    
        
    public void shutdown() {
    	this.upnpService.shutdown();
    }
	
}

package de.einwesen.heimklangwelle;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.renderers.AbstractRendererWrapper;
import de.einwesen.heimklangwelle.renderers.MPVRendererWrapper;
import de.einwesen.heimklangwelle.upnpsupport.FilteredAnnotationLocalServiceBinderImpl;
import de.einwesen.heimklangwelle.upnpsupport.RendererChangeEventListener;
import de.einwesen.heimklangwelle.upnpsupport.services.ConnectionManagerServiceImpl;
import de.einwesen.heimklangwelle.upnpsupport.services.SingleInstanceAVTransportServiceImpl;
import de.einwesen.heimklangwelle.upnpsupport.services.SingleInstanceRenderingControlServiceImpl;

public class HeimklangStation implements RegistryListener {
	private final static Logger LOGGER = LoggerFactory.getLogger(HeimklangStation.class);
	
	private static HeimklangStation instance = null;
    
	private final UpnpService upnpService;
	private final AbstractRendererWrapper rendererInstance;
    
	private LocalDevice localDevice;
    
    private HeimklangStation() throws ValidationException, IOException {
        if (instance == null) {
        	instance = this;
        	
        	LOGGER.info("Initializing renderer...");
        	this.rendererInstance = new MPVRendererWrapper();
        	
        	LOGGER.info("Starting jUPnP...");            
            this.upnpService = new UpnpServiceImpl(new DefaultUpnpServiceConfiguration());
            this.upnpService.startup();
            
            this.upnpService.getRegistry().addListener(this);
            
        	this.localDevice = createMediaRendererDevice();
        	this.upnpService.getRegistry().addDevice(localDevice);
        } else {
        	throw new IllegalStateException("There may only be one renderer instance ");
        }
    }

    private LocalDevice createMediaRendererDevice() throws ValidationException, IOException {
        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier(rendererInstance.getClass().getName()+ "-" + UUID.randomUUID()));
        DeviceType type = new UDADeviceType("MediaRenderer", 1);

        DeviceDetails details = new DeviceDetails(
                this.getClass().getSimpleName() + " " +  type.getDisplayString(),
                new ManufacturerDetails("https://github.com/EinWesen"),
                new ModelDetails(
                        rendererInstance.getClass().getSimpleName(),
                        rendererInstance.getDescription(),
                        "?" // FIXME: commit version Version                        
                )
        );
        
        final AnnotationLocalServiceBinder annotationBinder = new FilteredAnnotationLocalServiceBinderImpl();

        @SuppressWarnings("unchecked")
		final LocalService<ConnectionManagerServiceImpl> cmService = annotationBinder.read(ConnectionManagerServiceImpl.class);
        cmService.setManager(new DefaultServiceManager<>(cmService, ConnectionManagerServiceImpl.class));

        @SuppressWarnings("unchecked")
		final LocalService<SingleInstanceRenderingControlServiceImpl> rcService = annotationBinder.read(SingleInstanceRenderingControlServiceImpl.class);
        rcService.setManager(new DefaultServiceManager<>(rcService, SingleInstanceRenderingControlServiceImpl.class));

        @SuppressWarnings("unchecked")
		final LocalService<SingleInstanceAVTransportServiceImpl> avService = annotationBinder.read(SingleInstanceAVTransportServiceImpl.class);
        avService.setManager(new DefaultServiceManager<>(avService, SingleInstanceAVTransportServiceImpl.class));

        Icon icon = null; // optional, you can provide a PNG icon for the device

        return new LocalDevice(
                identity,
                type,
                details,
                new Icon[]{icon},
                new LocalService[]{cmService, rcService, avService}
        );
    }
    

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
    
    public void shutdown() {
    	this.rendererInstance.shutdown();
        this.upnpService.shutdown();
    }
    
	public static AbstractRendererWrapper getCurrentRendererInstance() {
		return getCurrentRendererInstance(null);
	}

	public static AbstractRendererWrapper getCurrentRendererInstance(RendererChangeEventListener listener) {		
		if (listener != null) {
			instance.rendererInstance.addListener(listener);
		}
		return instance.rendererInstance;
	}
    
	public static String getConfigProperty(final String name, final String defaultValue) {
    	
		String val = null;
		
		try { val = System.getProperty(name); } catch (Throwable ignore) {}
		
		if (val == null) {
			try { val = System.getenv(name); } 	catch (Throwable ignore) {}			
		}
		
		if (val != null && val.length()>0) {
			return val;
		}
		
    	return defaultValue;    	
    }
	
	public static boolean isOnWindows() {
		return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
	}
	
    public static void main(String[] args) throws Exception {
        instance = new HeimklangStation();
        System.out.println(HeimklangStation.class.getSimpleName() +  " running, press ENTER to exit...");
        System.in.read();
        instance.shutdown();
    }	
    
}
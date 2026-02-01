package de.einwesen.heimklangwelle;

import java.io.IOException;
import java.util.UUID;

import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.ServiceManager;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;
import org.jupnp.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.jupnp.support.lastchange.LastChangeAwareServiceManager;
import org.jupnp.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;

import de.einwesen.heimklangwelle.renderers.AbstractRendererWrapper;
import de.einwesen.heimklangwelle.upnpsupport.FilteredAnnotationLocalServiceBinderImpl;
import de.einwesen.heimklangwelle.upnpsupport.RendererChangeEventListener;
import de.einwesen.heimklangwelle.upnpsupport.UpnpServiceRegistry;
import de.einwesen.heimklangwelle.upnpsupport.services.ConnectionManagerServiceImpl;
import de.einwesen.heimklangwelle.upnpsupport.services.SingleInstanceAVTransportServiceImpl;
import de.einwesen.heimklangwelle.upnpsupport.services.SingleInstanceRenderingControlServiceImpl;

public class HeimklangServiceRegistry extends UpnpServiceRegistry {
	
	private static HeimklangServiceRegistry instance = new HeimklangServiceRegistry();
	
	private AbstractRendererWrapper rendererInstance;
	
    public LocalDevice registerLocalRendererDevice(AbstractRendererWrapper rendererInstance) throws ValidationException, IOException {
        this.rendererInstance = rendererInstance;
        
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
		final LocalService<SingleInstanceRenderingControlServiceImpl> rcService = 
			annotationBinder.read(SingleInstanceRenderingControlServiceImpl.class);
        
        final ServiceManager<SingleInstanceRenderingControlServiceImpl> rcServiceManager = 
        		new LastChangeAwareServiceManager<SingleInstanceRenderingControlServiceImpl>(
        				rcService, 
        				SingleInstanceRenderingControlServiceImpl.class, 
        				new RenderingControlLastChangeParser());

        rcService.setManager(rcServiceManager);
        
        @SuppressWarnings("unchecked")        
        final LocalService<SingleInstanceAVTransportServiceImpl> avService = 
        	annotationBinder.read(SingleInstanceAVTransportServiceImpl.class);

        final ServiceManager<SingleInstanceAVTransportServiceImpl> avServiceManager = 
        		new LastChangeAwareServiceManager<SingleInstanceAVTransportServiceImpl>(
        				avService, 
        				SingleInstanceAVTransportServiceImpl.class,
        				new AVTransportLastChangeParser());
        
        avService.setManager(avServiceManager);

        Icon icon = null; // optional, you can provide a PNG icon for the device

        LocalDevice device = new LocalDevice(
                identity,
                type,
                details,
                new Icon[]{icon},
                new LocalService[]{cmService, rcService, avService}
        );
        this.upnpService.getRegistry().addDevice(device);
        
        return device;
    }

	public static AbstractRendererWrapper getCurrentRendererInstance(RendererChangeEventListener listener) {		
		if (listener != null) {
			instance.rendererInstance.addListener(listener);
		}
		return instance.rendererInstance;
	}


	public static HeimklangServiceRegistry getInstance() {
    	return instance;
    }
	
}

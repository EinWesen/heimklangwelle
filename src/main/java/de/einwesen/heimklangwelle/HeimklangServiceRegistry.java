package de.einwesen.heimklangwelle;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.ServiceManager;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.ValidationException;
import org.jupnp.model.message.control.ActionMessage;
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
import org.jupnp.transport.impl.SOAPActionProcessorImpl;
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.ServletStreamServerImpl;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.SOAPActionProcessor;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.contentdirectory.ContentDirectoryServiceImpl;
import de.einwesen.heimklangwelle.contentdirectory.MediaServerConnectionManagerServiceImpl;
import de.einwesen.heimklangwelle.renderers.AbstractRendererWrapper;
import de.einwesen.heimklangwelle.renderers.RendererChangeEventListener;
import de.einwesen.heimklangwelle.renderers.RendererConnectionManagerServiceImpl;
import de.einwesen.heimklangwelle.renderers.SingleInstanceAVTransportServiceImpl;
import de.einwesen.heimklangwelle.renderers.SingleInstanceRenderingControlServiceImpl;
import de.einwesen.heimklangwelle.upnpsupport.FilteredAnnotationLocalServiceBinderImpl;
import de.einwesen.heimklangwelle.upnpsupport.UpnpServiceRegistry;

public class HeimklangServiceRegistry extends UpnpServiceRegistry {

	private final static Logger LOGGER = LoggerFactory.getLogger(HeimklangServiceRegistry.class);
	
	private static HeimklangServiceRegistry instance = new HeimklangServiceRegistry();		
	private static HeimklangJettyContainer jettyServer = new HeimklangJettyContainer();
		
	public HeimklangServiceRegistry() {
		super(new DefaultUpnpServiceConfiguration() {			
			@SuppressWarnings("rawtypes")
			@Override
			public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {				
				return new ServletStreamServerImpl(new ServletStreamServerConfigurationImpl(
					jettyServer, 
					networkAddressFactory.getStreamListenPort()));
			}

			@Override
			protected SOAPActionProcessor createSOAPActionProcessor() {
				return new SOAPActionProcessorImpl() {
					@Override
					protected String getMessageBody(ActionMessage message) throws UnsupportedDataException {
						final String tmp = super.getMessageBody(message).replace("\0", "").replace("\u0000", "");
						if (tmp.length() == 0) {
				            throw new UnsupportedDataException("Can't transform null or non-string/zero-length body of: " + message);
				        }
						return tmp;
					}
				};
			}
		});
	}
	
	private AbstractRendererWrapper rendererInstance;
	private String contentServerBase = null; 
	
    public LocalDevice registerLocalRendererDevice(AbstractRendererWrapper rendererInstance) throws ValidationException, IOException {
        this.rendererInstance = rendererInstance;
        
    	final DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier(rendererInstance.getClass().getName()));
        DeviceType type = new UDADeviceType("MediaRenderer", 1);

        final DeviceDetails details = new DeviceDetails(
                HeimklangStation.class.getPackageName() + " " +  type.getDisplayString(),
                new ManufacturerDetails("https://github.com/EinWesen"),
                new ModelDetails(
                        rendererInstance.getClass().getSimpleName(),
                        rendererInstance.getDescription(),
                        "?" // FIXME: commit version Version                        
                )
        );
        
        final AnnotationLocalServiceBinder annotationBinder = new FilteredAnnotationLocalServiceBinderImpl();

        @SuppressWarnings("unchecked")
		final LocalService<RendererConnectionManagerServiceImpl> cmService = annotationBinder.read(RendererConnectionManagerServiceImpl.class);
        cmService.setManager(new DefaultServiceManager<>(cmService, RendererConnectionManagerServiceImpl.class));

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

	public LocalDevice registerLocalContentServerDevice() throws ValidationException, IOException {
		
		final String sourceDir = System.getProperty("user.dir");

		// 1. Create the ServletHolder (Jetty's wrapper for servlets)
		ServletHolder staticHolder = new ServletHolder(new DefaultServlet());

		// 2. Point it to your files (can be a folder on disk or in your JAR)
		staticHolder.setInitParameter("resourceBase", sourceDir);

		// 3. (Optional) Recommended settings for sub-path serving
		staticHolder.setInitParameter("pathInfoOnly", "true"); // Ensures correct file lookups
		staticHolder.setInitParameter("dirAllowed", "false"); // Security: disable directory browsing

		// 4. Register it at a specific path
		final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		servletHandler.setContextPath("/heimklang/welle");
		servletHandler.addServlet(staticHolder, "/*");

		// 5. register with server 
		jettyServer.registerHandler(servletHandler);	
		
		// 6. Register addional port
		final int port = jettyServer.addConnector(null, 0);		
		
		final Iterator<InetAddress> it = this.upnpService.getConfiguration().createNetworkAddressFactory().getBindAddresses();
		final InetAddress addr = it.hasNext() ? it.next() : InetAddress.getLocalHost();
		
		this.contentServerBase = "http://" + addr.getHostAddress() + ":" + port + servletHandler.getContextPath();		
		LOGGER.info("Added Jetty connector for '%s' at %s/*".formatted(sourceDir, this.contentServerBase ));
		
    	DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier(ContentDirectoryServiceImpl.class.getName()));
        DeviceType type = new UDADeviceType("MediaServer", 1);

        DeviceDetails details = new DeviceDetails(
        		HeimklangStation.class.getPackageName() + " " +  type.getDisplayString(),
                new ManufacturerDetails("https://github.com/EinWesen")
        );
        
        final AnnotationLocalServiceBinder annotationBinder = new FilteredAnnotationLocalServiceBinderImpl();

        
        @SuppressWarnings("unchecked")
		final LocalService<MediaServerConnectionManagerServiceImpl> cmService = annotationBinder.read(MediaServerConnectionManagerServiceImpl.class);
        cmService.setManager(new DefaultServiceManager<>(cmService, MediaServerConnectionManagerServiceImpl.class));

        @SuppressWarnings("unchecked")
		final LocalService<ContentDirectoryServiceImpl> contentService = annotationBinder.read(ContentDirectoryServiceImpl.class);
        contentService.setManager(new DefaultServiceManager<>(contentService, ContentDirectoryServiceImpl.class));

        Icon icon = null; // optional, you can provide a PNG icon for the device

        LocalDevice device = new LocalDevice(
                identity,
                type,
                details,
                new Icon[]{icon},
                new LocalService[]{cmService, contentService}
        );
        this.upnpService.getRegistry().addDevice(device);
        return device;		
	}
	
	@Override
	public void startup() {
		super.startup();
	}

	public static HeimklangServiceRegistry getInstance() {
    	return instance;
    }

	public static String getContentServerBase() {
		return instance.contentServerBase;
	}
	
}

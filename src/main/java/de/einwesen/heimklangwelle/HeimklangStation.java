package de.einwesen.heimklangwelle;

import java.io.IOException;
import java.util.Locale;

import org.jupnp.model.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.renderers.AbstractRendererWrapper;
import de.einwesen.heimklangwelle.renderers.DummyWrapperImpl;
import de.einwesen.heimklangwelle.renderers.MPVRendererWrapper;

public class HeimklangStation {
	public static final String CONFIG_PROPERTY_NORENDERER = "HEIMKLANG_CONFIG_NORENDERER";
	public static final String CONFIG_PROPERTY_NOSERVER = "HEIMKLANG_CONFIG_NOSERVER";
	public static final String CONFIG_PROPERTY_NOCONTROLLER = "HEIMKLANG_CONFIG_NOCONTROLLER";

	private final static Logger LOGGER = LoggerFactory.getLogger(HeimklangStation.class);
	
	private static HeimklangStation instance = null;
    
    private final HeimklangServiceRegistry serviceRegistry;
    private final AbstractRendererWrapper rendererInstance;
    
    private HeimklangStation() throws ValidationException, IOException {
        if (instance == null) {
        	instance = this;
        	
        	LOGGER.info("Initializing service registry...");
        	this.serviceRegistry = HeimklangServiceRegistry.getInstance();
        	this.serviceRegistry.startup();
        	
        	if (!Boolean.valueOf(getConfigProperty(CONFIG_PROPERTY_NORENDERER, "false"))) {
        		LOGGER.info("Initializing renderer...");
        		this.rendererInstance = initRenderer();        	
        		this.serviceRegistry.registerLocalRendererDevice(this.rendererInstance);        		
        	} else {
        		this.rendererInstance = null;
        	}
        	
        	if (!Boolean.valueOf(getConfigProperty(CONFIG_PROPERTY_NOSERVER, "false"))) {
        		this.serviceRegistry.registerLocalContentServerDevice();        		
        	}
        	
        	if (!Boolean.valueOf(getConfigProperty(CONFIG_PROPERTY_NOCONTROLLER, "false"))) {
        		this.serviceRegistry.registerLocalController();        		
        	}        	
            
        } else {
        	throw new IllegalStateException("There may only be one serviceInstance");
        }
    }
    
    private static AbstractRendererWrapper initRenderer() {
    	try {
			return new MPVRendererWrapper();
		} catch (Throwable e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.error("Failed to initialize renderer", e.toString());								
			} else {
				LOGGER.error("Failed to initialize renderer: " + e.toString());
			}
			return new DummyWrapperImpl();
		}
    }
    
    public void shutdown() {
    	if (this.rendererInstance != null) {
    		this.rendererInstance.shutdown();    		
    	}
        this.serviceRegistry.shutdown();
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
		try {			
			instance = new HeimklangStation();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				instance.shutdown();
			}));		    	
			System.out.println(HeimklangStation.class.getSimpleName() +  " running, press ENTER to exit...");
			System.in.read();
			System.out.println("... shutdown requested!");
			System.exit(0);
		} catch (Throwable e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
    }	
    
}
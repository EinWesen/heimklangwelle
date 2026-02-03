package de.einwesen.heimklangwelle;

import java.io.IOException;
import java.util.Locale;

import org.jupnp.model.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.renderers.AbstractRendererWrapper;
import de.einwesen.heimklangwelle.renderers.MPVRendererWrapper;

public class HeimklangStation {
	private final static Logger LOGGER = LoggerFactory.getLogger(HeimklangStation.class);
	
	private static HeimklangStation instance = null;
    
    private final HeimklangServiceRegistry serviceRegistry;
    private final AbstractRendererWrapper rendererInstance;
    
    private HeimklangStation() throws ValidationException, IOException {
        if (instance == null) {
        	instance = this;
        	
        	LOGGER.info("Initializing renderer...");
        	this.rendererInstance = new MPVRendererWrapper();        	
            
        	LOGGER.info("Initializing service registry...");
        	this.serviceRegistry = HeimklangServiceRegistry.getInstance();
        	this.serviceRegistry.startup();
            this.serviceRegistry.registerLocalRendererDevice(this.rendererInstance);
            this.serviceRegistry.registerLocalContentServer();
            
        } else {
        	throw new IllegalStateException("There may only be one serviceInstance");
        }
    }
    
    public void shutdown() {
    	this.rendererInstance.shutdown();
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
    	instance = new HeimklangStation();
        System.out.println(HeimklangStation.class.getSimpleName() +  " running, press ENTER to exit...");
        System.in.read();
        instance.shutdown();
    }	
    
}
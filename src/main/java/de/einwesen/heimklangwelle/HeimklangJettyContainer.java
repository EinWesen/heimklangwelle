package de.einwesen.heimklangwelle;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.servlet.Servlet;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jupnp.transport.spi.ServletContainerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/* A lot of code is copied from org.jupnp.transport.impl.jetty.JettyServletContainer
 * I just wanted access to the jetty server instance to register to register my own
 * context 
 */
public class HeimklangJettyContainer implements ServletContainerAdapter {

	private final static Logger LOGGER = LoggerFactory.getLogger(HeimklangJettyContainer.class);
		
	private Server server = new Server();
	private boolean jUpnpServletRegistered = false;	
    /**
     * Might be called several times to integrate the servlet container with jUPnP's executor
     * configuration. You can ignore this call if you want to configure the container's thread
     * pooling independently from jUPnP. If you use the given jUPnP <code>ExecutorService</code>,
     * make sure the Jetty container won't shut it down when {@link #stopIfRunning()} is called!
     *
     * @param executorService The service to use when spawning new servlet execution threads.
     */
    public void setExecutorService(ExecutorService executorService) {
    	LOGGER.trace("Ignored for now");
    }

    /**
     * Might be called several times to set up the connectors. This is the host/address
     * and the port jUPnP expects to receive HTTP requests on. If you set up your HTTP
     * server connectors elsewhere and ignore when jUPnP calls this method, make sure
     * you configure jUPnP with the correct host/port of your servlet container.
     *
     * @param host The host address for the socket.
     * @param port The port, might be <code>-1</code> to bind to an ephemeral port.
     * @return The actual registered local port.
     * @throws IOException If the connector couldn't be opened to retrieve the registered local port.
     */
    public synchronized int addConnector(String host, int port) throws IOException {
        LOGGER.trace("%s : %d".formatted(host, port));
    	
    	ServerConnector connector = new ServerConnector(server);
        connector.setHost(host);
        connector.setPort(port);

        // Open immediately so we can get the assigned local port
        connector.open();

        // Only add if open() succeeded
        server.addConnector(connector);

        // starts the connector if the server is started (server starts all connectors when started)
        if (server.isStarted()) {
            try {
                connector.start();
            } catch (Exception e) {
            	LOGGER.error("Couldn't start connector: {}", connector, e);
                throw new RuntimeException("Couldn't start connector", e);
            }
        }
        return connector.getLocalPort();    	
    }
    
    /**
     * Might be called several times to register (the same) handler for UPnP
     * requests, should only register it once.
     *
     * @param contextPath The context path prefix for all UPnP requests.
     * @param servlet The servlet handling all UPnP requests.
     */
    @Override
    public synchronized void registerServlet(String contextPath, Servlet servlet) {
    	if (!jUpnpServletRegistered) {
    		LOGGER.debug("Registering UPnP servlet " + servlet.getClass().getName() + " under context path: " + contextPath);
    		
    		final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);    		
    		if (contextPath != null && !contextPath.isEmpty()) {
    			servletHandler.setContextPath(contextPath);
    		}
    		servletHandler.addServlet(new ServletHolder(servlet), "/*");
    		registerHandler(servletHandler);
    		this.jUpnpServletRegistered = true;
    		
    	} else {
    		LOGGER.debug("Ignoring known servlet " + servlet.getClass().getName());
    	}
    	
    }
    
    public synchronized void registerHandler(Handler handler) {
		
    	ContextHandlerCollection rootHandler = (ContextHandlerCollection)server.getHandler();

    	if (rootHandler == null) {
    		rootHandler = new ContextHandlerCollection();	
    		server.setHandler(rootHandler);
    	}    	
    	
    	rootHandler.addHandler(handler);
		
		if (rootHandler.isStarted()) {
			try {
				handler.start();
			} catch (Exception e) {
                LOGGER.warn("Couldn't start " + handler.getClass().getName() + "#" + handler.hashCode());
                throw new RuntimeException("Couldn't start handler", e);
            }
		}    	
    }
    

    /**
     * Start your servlet container if it isn't already running, might be called multiple times.
     */
    public synchronized void startIfNotRunning() {
        if (!server.isStarted() && !server.isStarting()) {
            LOGGER.debug("Starting Jetty server... ");
            try {
                server.start();
            } catch (Exception e) {
                LOGGER.warn("Couldn't start Jetty server", e);
                throw new RuntimeException(e);
            }
        }
    }
    /**
     * Stop your servlet container if it's still running, might be called multiple times.
     */
    public synchronized void stopIfRunning() {
        if (!server.isStopped() && !server.isStopping()) {
            LOGGER.debug("Stopping Jetty server...");
            try {
                server.stop();
            } catch (Exception e) {
                LOGGER.error("Couldn't stop Jetty server", e);
                throw new RuntimeException(e);
            } finally {
                resetServer();
            }
        }
    }

    protected void resetServer() {
    	this.jUpnpServletRegistered = false;
        this.server = new Server(); // Has its own QueuedThreadPool
    }    

}

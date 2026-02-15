package de.einwesen.heimklangwelle.controller.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.UDAServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;
import de.einwesen.heimklangwelle.controller.RendererSubscriptionPublisherCallback;

public class RendererEndpointServlet extends HttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(RendererEndpointServlet.class);

	private static final long serialVersionUID = 3678093008074765231L;
	
    @Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		if (req.getPathInfo().endsWith("/subscribe")) {
			_doStreamEventSubscription(req, resp);
		} else if (req.getPathInfo().endsWith("/actions") || req.getPathInfo().endsWith("/actions/")) {
			_doGetActions(req, resp);	        
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "action does not exists");
		}
	}
	
    @Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {    	
    	resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	protected void _doStreamEventSubscription(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
		@SuppressWarnings("rawtypes")
		final Device device = Utils.getDeviceOrFail(req, resp);
		if (device == null) {		
			return;
		}   
        
        @SuppressWarnings("rawtypes")
		final Service avTransport = device.findService(new UDAServiceType("AVTransport"));
        @SuppressWarnings("rawtypes")
		final Service renderingControl = device.findService(new UDAServiceType("RenderingControl"));

        if (avTransport == null || renderingControl == null) {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, "No eventable services");
            return;
        }        
    	    	
        final String remoteHost = req.getRemoteHost();
        final Object writeLock = new Object();
        final AtomicBoolean closed = new AtomicBoolean(false);
        final ArrayList<SubscriptionCallback> callbacks = new ArrayList<>(2);
        final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();

        // --- START ASYNC ---
        final AsyncContext async = req.startAsync();
        async.setTimeout(0); // disable container timeout
        
        // --- SET SSE HEADERS ---
        final HttpServletResponse asyncResp = (HttpServletResponse) async.getResponse();
        asyncResp.setStatus(HttpServletResponse.SC_OK); // Explicit 200 OK
        asyncResp.setContentType("text/event-stream; charset=UTF-8");
        asyncResp.setCharacterEncoding("UTF-8");
        asyncResp.setHeader("Cache-Control", "no-cache");
        asyncResp.setHeader("Connection", "keep-alive");
        
        final PrintWriter writer = asyncResp.getWriter();

        // --- Central cleanup ---
        final Runnable cleanup = () -> {
            if (!closed.compareAndSet(false, true)) return;
            LOGGER.debug("Doing cleanup for: " + remoteHost);
            heartbeat.shutdownNow();
            callbacks.forEach(callback -> callback.end());
            async.complete();
        };        
        
        // --- AsyncListener to detect disconnects ---
        async.addListener(new AsyncListener() {
        	@Override public void onStartAsync(AsyncEvent event) throws IOException {}
        	@Override public void onComplete(AsyncEvent event) throws IOException {cleanup.run();}
        	@Override public void onTimeout(AsyncEvent event) throws IOException {cleanup.run();}
        	@Override public void onError(AsyncEvent event) throws IOException {cleanup.run();}
        });
        
        // schedule a heartbeat
        heartbeat.scheduleAtFixedRate(() -> {
            if (closed.get()) return;
            try {
                synchronized (writeLock) {
                    writer.write(":\n\n"); // SSE comment heartbeat
                    writer.flush();
                }
            } catch (Throwable e) {
                cleanup.run();
            }
        }, 15, 15, TimeUnit.SECONDS);  
        
        synchronized (writeLock) {
        	writer.write("event: X_PROPERTIES\n");
            writer.write("data: "+new JSONArray(RendererSubscriptionPublisherCallback.SUPPORTED_PROPERTIES).toString()+"\n\n");
            writer.flush();
        }        
        
        // create callbacks
        callbacks.add(HeimklangServiceRegistry.getInstance().registerCallback(new RendererSubscriptionPublisherCallback(avTransport, 600, RendererSubscriptionPublisherCallback.SUBSCRIPTION_AVTRANSPORT) {
        	@Override protected void stopped(@SuppressWarnings("rawtypes") GENASubscription subscription) {cleanup.run();}
        	@Override protected void publish(@SuppressWarnings("rawtypes") GENASubscription subscription, String propertyName, String propertyValueString) {
        	    if (closed.get()) return;
        	    try {
        	        synchronized (writeLock) {
        	        	writer.write("event: "+propertyName+"\n");
        	            writer.write("data: "+propertyValueString+"\n\n");
        	            writer.flush();
        	        }
        	    } catch (Throwable e) {
        	        cleanup.run();
        	    }			
        	}        	
        }));
        callbacks.add(HeimklangServiceRegistry.getInstance().registerCallback(new RendererSubscriptionPublisherCallback(renderingControl, 600, RendererSubscriptionPublisherCallback.SUBSCRIPTION_RENDERINGCONTROL) {
        	@Override protected void stopped(@SuppressWarnings("rawtypes") GENASubscription subscription) {cleanup.run();}
        	@Override protected void publish(@SuppressWarnings("rawtypes") GENASubscription subscription, String propertyName, String propertyValueString) {
        	    if (closed.get()) return;
        	    try {
        	        synchronized (writeLock) {
        	        	writer.write("event: "+propertyName+"\n");
        	            writer.write("data: "+propertyValueString+"\n\n");
        	            writer.flush();
        	        }
        	    } catch (Throwable e) {
        	        cleanup.run();
        	    }			
        	}        	
        }));

        LOGGER.debug("Serving events to: " + remoteHost);
        return;
    }
	
	protected void _doGetActions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		@SuppressWarnings("rawtypes")
		final Device device = Utils.getDeviceOrFail(req, resp);
		
		if (device == null) {		
			return;
		} 
		
		final JSONObject jsonResponse = new JSONObject();
		final JSONArray jsonActions = new JSONArray();
		jsonResponse.put("actions", jsonActions);

		try {
			jsonActions.putAll(Utils.getActionListArray(device.findService(new UDAServiceType("AVTransport")), true));			
		} catch (Throwable t) {
			LOGGER.warn("Error listing actions", t);
		}
		
		try {
			jsonActions.putAll(Utils.getActionListArray(device.findService(new UDAServiceType("RenderingControl")), true));
		} catch (Throwable t) {
			LOGGER.warn("Error listing actions", t);
		}
		
		resp.setStatus(HttpServletResponse.SC_OK);
		Utils.sendJSON(jsonResponse, resp);		
	}	
	

}

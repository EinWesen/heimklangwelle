package de.einwesen.heimklangwelle.controller.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;

public class Utils {

	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
	
	public static boolean sendException(int status, Throwable t, HttpServletResponse resp) {
		try {
			final StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw, true));
			resp.sendError(status, sw.toString());
			resp.flushBuffer();
			return true;
		} catch (Throwable e) {
			LOGGER.debug("Could not send: " + t.toString(), e);
			return false;
		}		
	}
	
	public static void sendJSON(JSONObject json, HttpServletResponse resp) {
        try {
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().write(json.toString());
			resp.flushBuffer();
		} catch (IOException e) {
			LOGGER.warn("Could not send data", e);
		}		
	}
	
	@SuppressWarnings("rawtypes")
	public static JSONArray getActionListArray(Service serviceObj, boolean setService) {
		final JSONArray jsonActions = new JSONArray();

		if (serviceObj != null) {
			for (Action actionCall : serviceObj.getActions()) {
				
				final JSONObject jsonAction = new JSONObject();
				if (setService) {
					jsonAction.put("serviceId", serviceObj.getServiceType().toFriendlyString());					
				}
				jsonAction.put("name", actionCall.getName());
				
				final JSONArray jsonArguments = new JSONArray();
				jsonAction.put("arguments", jsonArguments);
				
				for (ActionArgument arg : actionCall.getArguments()) {
					final JSONObject jsonArgument = new JSONObject();
					jsonArgument.put("name", arg.getName());
					jsonArgument.put("direction", arg.getDirection().name());
					jsonArgument.put("datatype", arg.getDatatype().getDisplayString());
					jsonArguments.put(jsonArgument);
				}
				
				jsonActions.put(jsonAction);
			}			
		}
        
        return jsonActions;
	}

	@SuppressWarnings("rawtypes")
	public static Device getDeviceOrFail(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	    String path = req.getPathInfo(); // /{udn}/events
	    String[] parts = path != null ? path.split("/") : new String[0];
	    if (parts.length < 2) {
	        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing renderer UDN");
	        return null;
	    }    	
	
		final Device device = HeimklangServiceRegistry.getInstance().getRegisteredMediaDevice(parts[1]);
		if (device == null) {		
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Renderer not found");
			return null;
		} 
		
		return device;
	}		

}

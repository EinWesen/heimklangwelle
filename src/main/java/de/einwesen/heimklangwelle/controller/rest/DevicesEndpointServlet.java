package de.einwesen.heimklangwelle.controller.rest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.UDAServiceType;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DevicesEndpointServlet extends HttpServlet {

	private static final long serialVersionUID = 1720026526479863390L;

	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
		JSONObject jsonResponse = new JSONObject();

		if ("/callAction".equalsIgnoreCase(req.getPathInfo())) {
			resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		} else if (req.getPathInfo() == null || "/".equalsIgnoreCase(req.getPathInfo())) {

			final JSONArray devicesJson = new JSONArray();
			for (@SuppressWarnings("rawtypes")Device device : HeimklangServiceRegistry.getInstance().getRegisteredMediaDevices()) {
				final JSONObject dev = new JSONObject();
				dev.put("udn", device.getIdentity().getUdn().getIdentifierString());
				dev.put("friendlyName", device.getDetails().getFriendlyName());
				dev.put("type", device.getType().getType());
				devicesJson.put(dev);
			}

			jsonResponse.put("devices", devicesJson);

		} else {
			@SuppressWarnings("rawtypes")
			final Device device = Utils.getDeviceOrFail(req, resp);
			if (device == null) {		
				return;
			}
			
			final JSONArray jsonServices = new JSONArray();
			for (@SuppressWarnings("rawtypes") Service service : device.getServices()) {
				final JSONObject jsonService = new JSONObject();
				jsonService.put("id", service.getServiceId().getId());
				jsonService.put("type", service.getServiceType().toFriendlyString());
				jsonService.put("actions", Utils.getActionListArray(service, false));
				jsonServices.put(jsonService);
			}
			
			jsonResponse.put("udn", device.getIdentity().getUdn().getIdentifierString());
			jsonResponse.put("friendlyName", device.getDetails().getFriendlyName());
			jsonResponse.put("type", device.getType().getType());			
			jsonResponse.put("services", jsonServices);
		}

        Utils.sendJSON(jsonResponse, resp);
    }

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (req.getPathInfo() != null && req.getPathInfo().endsWith("/callAction")) {
			_doExecuteAction(req, Utils.getJSONObjectBody(req, resp), resp);
		} else {
			resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		}		
	}
	
	
	protected void _doExecuteAction(HttpServletRequest req, JSONObject requestBody, HttpServletResponse resp) throws IOException {
		
		if (requestBody == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing fields in request body");
			return;
		}
		
		final String serviceId;
		final String actionName;
		final JSONObject inputArguments;

		try {
			serviceId = requestBody.getString("serviceId");
			actionName = requestBody.getString("action");
			inputArguments = requestBody.getJSONObject("inputArguments");
		} catch (JSONException e) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing fields in request body");
			return;
		}
		
		@SuppressWarnings("rawtypes")
		final Device device = Utils.getDeviceOrFail(req, resp);
		
		if (device == null) {		
			return;
		} 		
		
		@SuppressWarnings("rawtypes")
		final Service service = device.findService(new UDAServiceType(serviceId));
        
		if (service == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "service not found");
			return;					
		}
		
		@SuppressWarnings("rawtypes")
		final Action action = service.getAction(actionName);
		if (action == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "action not found");
			return;					
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final ActionInvocation invocation = new ActionInvocation<>(action);
		
		try {
			
		    for (ActionArgument<?> argument : action.getInputArguments()) {
	
		        final String argName = argument.getName();
		        final String rawValue = inputArguments.getString(argName);
	
		        // Convert raw value to correct UPnP type
		        final Object convertedValue = argument.getDatatype().valueOf(rawValue.toString());
	
		        invocation.setInput(argName, convertedValue);
		    }
		
		} catch (JSONException e) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing inputArgument in request body");
			return;
		}	    

	    try {
			final UpnpResponse result = HeimklangServiceRegistry.getInstance().executeAndReturnReponse(invocation).get(15, TimeUnit.SECONDS);
			final JSONObject jsonResult = new JSONObject();
			jsonResult.put("statusCode", result.getStatusCode());
			jsonResult.put("message", result.getStatusMessage());
			
			resp.setStatus(result.getStatusCode() == UpnpResponse.Status.OK.getStatusCode() ? HttpServletResponse.SC_CREATED : HttpServletResponse.SC_ACCEPTED);
			Utils.sendJSON(jsonResult, resp);
		} catch (Throwable e) {
			Utils.sendException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e, resp);
		}
	    
	}	
	
}
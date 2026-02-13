package de.einwesen.heimklangwelle.controller.rest;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class DevicesEndpointServlet extends HttpServlet {

	private static final long serialVersionUID = 1720026526479863390L;

	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        
		JSONObject jsonResponse = new JSONObject();
		
		if (req.getPathInfo() == null || "/".equalsIgnoreCase(req.getPathInfo())) {

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
}
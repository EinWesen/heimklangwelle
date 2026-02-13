package de.einwesen.heimklangwelle.controller.rest;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jupnp.model.meta.Device;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class DevicesEndpointServlet extends HttpServlet {

	private static final long serialVersionUID = 1720026526479863390L;

	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final JSONArray devicesJson = new JSONArray();
        for (@SuppressWarnings("rawtypes")Device device : HeimklangServiceRegistry.getInstance().getRegisteredMediaDevices()) {
            final JSONObject dev = new JSONObject();
            dev.put("udn", device.getIdentity().getUdn().getIdentifierString());
            dev.put("friendlyName", device.getDetails().getFriendlyName());
            dev.put("type", device.getType().getType());
            devicesJson.put(dev);
        }

        Utils.sendJSON(null, resp);
    }
}
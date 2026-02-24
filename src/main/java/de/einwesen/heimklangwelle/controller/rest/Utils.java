package de.einwesen.heimklangwelle.controller.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.IO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.Res;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.item.Item;
import org.jupnp.xml.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;

public class Utils {

	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
	
	public static boolean sendException(int status, Throwable t, HttpServletResponse resp) {
		try {
			final StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw, true));
			resp.addHeader("Cache-Control", "no-cache,no-store");
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
        	resp.addHeader("Cache-Control", "no-cache,no-store");
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
	
	public static JSONObject getJSONObjectBody(HttpServletRequest req, HttpServletResponse resp) {
		final String cType = ("" + req.getContentType()).toLowerCase();
		if (cType.startsWith("application/json")) {
			
			final JSONObject json;
			final String requestBody;
			
			try (InputStream in = req.getInputStream()) {
				requestBody = IO.toString(req.getInputStream(), StandardCharsets.UTF_8);     				
			} catch (IOException io) {
				Utils.sendException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, io, resp);
				return null;
			}
			
			try {
				json = new JSONObject(requestBody);
				return json;
			} catch (JSONException j) {
				Utils.sendException(HttpServletResponse.SC_BAD_REQUEST, j , resp);				
			} 
			
		} else {
			try {
				LOGGER.trace("Wrong contenttype: ", req.getContentType());
				resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			} catch (IOException e) {
				LOGGER.debug("Could set status", e);
			}
		}		
		
		return null;
	}
	
	public static JSONObject getJSONContent(DIDLContent didlContent) throws ParserException {
		final DIDLParser didlParser = new DIDLParser();
		return getJSONContent(didlContent, didlParser);
	}
	
	public static JSONObject getJSONContainer(Container container) throws ParserException {
		final DIDLParser didlParser = new DIDLParser();
		return getJSONContainer(container, didlParser);
	}
	
	public static JSONObject getJSONContent(DIDLContent didlContent, DIDLParser didlParser) throws ParserException {
    	final JSONObject jsonResult = new JSONObject();
    	final JSONArray jsonChildren = new JSONArray();
    	jsonResult.put("children", jsonChildren);		
    	
    	if (didlContent != null) {
			jsonResult.put("childCount", didlContent.getCount());			
			
			for (Container container : didlContent.getContainers()) {
				jsonChildren.put(getJSONContainer(container, didlParser));
			}
			
			appendItemsToArray(jsonChildren, didlContent.getItems(), didlParser);
		
    	}  else {
    		jsonResult.put("childCount", 0);
    	}
    	
    	return jsonResult;
	}
	
	public static JSONObject getJSONContainer(Container container, DIDLParser didlParser) throws ParserException {
		final JSONObject jsonChild = new JSONObject();
		jsonChild.put("id", container.getId());
		jsonChild.put("parentId", container.getParentID());
		jsonChild.put("title", container.getTitle());
		jsonChild.put("childCount", container.getChildCount());
		jsonChild.put("isContainer", true);
		if (container.getWriteStatus() != null) {
			jsonChild.put("writeStatus", container.getWriteStatus().name());					
		}
		
		if (container.getItems() != null && container.getItems().size() > 0) {
			final JSONArray jsonSubChildren = new JSONArray();
			appendItemsToArray(jsonSubChildren, container.getItems(), didlParser);
			jsonChild.put("children", jsonSubChildren);
		}
		
		return jsonChild;		
	}
	
	private static void appendItemsToArray(final JSONArray jsonChildren, final List<Item> items, final DIDLParser didlParser) throws ParserException {
		
		for (Item item : items) {
			final JSONObject jsonChild = new JSONObject();
			jsonChild.put("id", item.getId());
			jsonChild.put("parentId", item.getParentID());
			jsonChild.put("title", item.getTitle());
			
			if (item.getResources().size() == 1) {					
				final Res res = item.getResources().get(0);
				jsonChild.put("uri", res.getValue());
				jsonChild.put("mimeType", res.getProtocolInfo().getContentFormatMimeType().getType());
				
				final DIDLContent content =  new DIDLContent();
				content.addItem(item);		
				
				String uriMetaData;
				try {
					uriMetaData = didlParser.generate(content);
				} catch (Exception e) {
					throw new ParserException("Error generating metaData", e);
				}
				
				jsonChild.put("uriMetaData", uriMetaData);
			} else if (item.getResources().size() > 1) {
				throw new IllegalStateException("Items with multipole resources are not supported right now");
			} 
			jsonChild.put("isContainer", false);
			jsonChildren.put(jsonChild);
		}
		
	}

}

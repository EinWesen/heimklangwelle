package de.einwesen.heimklangwelle.controller.rest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.Res;
import org.jupnp.support.model.SortCriterion;
import org.jupnp.support.model.container.Container;
import org.jupnp.support.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;
import de.einwesen.heimklangwelle.controller.BrowseCallbackFuture;

public class ContentDirectoryEndpointServlet extends HttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContentDirectoryEndpointServlet.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = 6475489953699206709L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		if (req.getPathInfo() == null || !req.getPathInfo().endsWith("/browse")) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "wrong path");
		}
		
		final String id = req.getParameter("objectId");
		if (id == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing parameter objectId");
			return;
		}

		@SuppressWarnings("rawtypes")
		final Device device = Utils.getDeviceOrFail(req, resp);
		
		if (device == null) {		
			return;
		}
		
        @SuppressWarnings("rawtypes")
		final Service contentDirectory = device.findService(new UDAServiceType("ContentDirectory"));

        if (contentDirectory == null) {
            resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Service not found on device");
            return;
        }      		
        
        final String filter = "*";
        final long firstResult = 0;
        final Long maxResults = null;
        final SortCriterion sort = new SortCriterion(true, "dc:title");
        
        try {
			final BrowseCallbackFuture browseCallback = new BrowseCallbackFuture(contentDirectory, id, BrowseFlag.DIRECT_CHILDREN, filter , firstResult , maxResults, sort);
			final DIDLContent didlContent =  HeimklangServiceRegistry.getInstance().execute(browseCallback).get(30, TimeUnit.SECONDS);
			
			final JSONObject jsonResult = new JSONObject();
			jsonResult.put("childCount", didlContent.getCount());
			
			final JSONArray jsonChildren = new JSONArray(); 
			for (Container container : didlContent.getContainers()) {
				final JSONObject jsonChild = new JSONObject();
				jsonChild.put("id", container.getId());
				jsonChild.put("parentId", container.getParentID());
				jsonChild.put("title", container.getTitle());
				jsonChild.put("childCount", container.getChildCount());
				jsonChild.put("isContainer", true);
				jsonChildren.put(jsonChild);
			}
			
			final DIDLParser didlParser = new DIDLParser();
			
			for (Item item : didlContent.getItems()) {
				final JSONObject jsonChild = new JSONObject();
				jsonChild.put("id", item.getId());
				jsonChild.put("parentId", item.getParentID());
				jsonChild.put("title", item.getTitle());
				
				if (item.getResources().size() == 1) {					
					final Res res = item.getResources().getFirst();
					jsonChild.put("uri", res.getValue());
					jsonChild.put("mimeType", res.getProtocolInfo().getContentFormatMimeType().getType());
					
					final DIDLContent content =  new DIDLContent();
					content.addItem(item);					
					jsonChild.put("metaDataUri", didlParser.generate(content));
				} else {
					throw new IllegalStateException("Items with multipole resources are not supported right now");
				}
				jsonChild.put("isContainer", false);
				jsonChildren.put(jsonChild);
			}
			
			jsonResult.put("children", jsonChildren);
			
			Utils.sendJSON(jsonResult, resp);

		} catch (Throwable e) {
			LOGGER.error("", e);
			Utils.sendException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e, resp);
		}
	
	}

}

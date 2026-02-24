package de.einwesen.heimklangwelle.controller.rest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.support.contentdirectory.callback.Browse;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.SortCriterion;
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
		
		final String id = req.getParameter("ObjectID");
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

        final SortCriterion sort = new SortCriterion(true, "dc:title");  //TODO: Support sort, this can be multiple values

        final String filter = req.getParameter("Filter") != null ? req.getParameter("Filter") : Browse.CAPS_WILDCARD;        
    	        
        try {
        	final long firstResult;
			try {
				firstResult = req.getParameter("StartingIndex") != null ? Long.valueOf(req.getParameter("StartingIndex")).longValue() : 0;
			} catch (NumberFormatException e) {
    			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "StartingIndex; " + e.toString());
    			return;				
			}
        	
        	/* The default maxResults seems to be 999. That should be more than enough for most cases,
        	 * but we can not be sure. So, if we don't have a limit, make sure we fetch everything, by  
        	 * checking what the total is.
        	 */        	
        	boolean resultWillBeEmpty = false;
        	final Long maxResults;
        	
        	if (req.getParameter("RequestedCount") == null) {
        		
        		final BrowseCallbackFuture metaCallback = new BrowseCallbackFuture(contentDirectory, id, BrowseFlag.METADATA);
        		final DIDLContent didlContent =  HeimklangServiceRegistry.getInstance().execute(metaCallback).get(30, TimeUnit.SECONDS);
        		maxResults = didlContent.getFirstContainer().getChildCount().longValue();
        		resultWillBeEmpty = maxResults.longValue() == 0; // flag to show, that where is no need to fetch children
        		
        	} else {
        		try {
        			maxResults = Long.valueOf(req.getParameter("RequestedCount"));
        		} catch (NumberFormatException e) {
        			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "RequestedCount; " + e.toString());
        			return;
        		}        	
        	}
        	

        	final JSONObject jsonResult;
        	if (resultWillBeEmpty) {
        		// We already know there are no children 
        		jsonResult = Utils.getJSONContent(null);        		
        	} else {
	        	final BrowseCallbackFuture browseCallback = new BrowseCallbackFuture(contentDirectory, id, BrowseFlag.DIRECT_CHILDREN, filter , firstResult , maxResults, sort);
				final DIDLContent didlContent =  HeimklangServiceRegistry.getInstance().execute(browseCallback).get(30, TimeUnit.SECONDS);
				jsonResult = Utils.getJSONContent(didlContent);
        	}
			
			Utils.sendJSON(jsonResult, resp);

		} catch (Throwable e) {
			LOGGER.error("", e);
			Utils.sendException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e, resp);
		}
	
	}

}

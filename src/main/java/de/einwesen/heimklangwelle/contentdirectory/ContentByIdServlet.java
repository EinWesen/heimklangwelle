package de.einwesen.heimklangwelle.contentdirectory;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentByIdServlet extends DefaultServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(ContentByIdServlet.class);

	private static final long serialVersionUID = -8426298330997491306L;

	private static File decodeItemIdPath(String pathInContext) {
		//pathInContext includes a leading /
		final String[] fileparts = pathInContext.substring(1).split("\\.", 2);
		
		if (fileparts.length == 2) {
			return  ContentDirectoryServiceImpl.decodeItemId(fileparts[0]);				
		} else {
			throw new IllegalArgumentException("Format of path does not match expectations");
		}
	}
	
	@Override
	public Resource getResource(String pathInContext) {
				
		if (pathInContext.equals("/") || pathInContext.equalsIgnoreCase("index.html")) {
			return super.getResource(pathInContext);
		} else {
			File fileToServe = null;
			
			try {
				fileToServe = decodeItemIdPath(pathInContext);			
			} catch (Throwable t) {
				LOGGER.warn("Could not decode item '" + pathInContext + "'", t);
			}
			
			if (fileToServe != null) {
				return Resource.newResource(fileToServe);			
			}			
			
			return null;
		}
		
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String pathInContext = request.getPathInfo();
		
		try {
			response.addHeader("Cache-control", "no-cache");
			
			File fileToServe = decodeItemIdPath(pathInContext);
					
			if (fileToServe != null) {				
				response.setContentLengthLong(fileToServe.length());
				response.addHeader("Content-Type", ContentDirectoryServiceImpl.fileExtensionMimeTypes.getMimeByExtension(fileToServe.getName()));
				response.setStatus(HttpServletResponse.SC_OK);
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		
		} catch (Throwable t) {
			LOGGER.warn("Could not serve item '" + pathInContext + "'", t);
			try { 
				response.reset(); 
				response.addHeader("Cache-control", "no-cache");
			} catch (Throwable ignore) { 
				// Ignore
			}
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
			
		response.flushBuffer();
		
	}
	
}

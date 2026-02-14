package de.einwesen.heimklangwelle.contentdirectory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.http.MimeTypes;
import org.jupnp.support.contentdirectory.AbstractContentDirectoryService;
import org.jupnp.support.contentdirectory.ContentDirectoryErrorCode;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.DIDLObject;
import org.jupnp.support.model.ProtocolInfo;
import org.jupnp.support.model.Res;
import org.jupnp.support.model.SortCriterion;
import org.jupnp.support.model.container.StorageFolder;
import org.jupnp.support.model.container.StorageSystem;
import org.jupnp.support.model.container.StorageVolume;
import org.jupnp.support.model.item.AudioItem;
import org.jupnp.support.model.item.ImageItem;
import org.jupnp.support.model.item.MusicTrack;
import org.jupnp.support.model.item.VideoItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;

//UPNP annotations are inherited from parent
public class ContentDirectoryServiceImpl extends AbstractContentDirectoryService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ContentDirectoryServiceImpl.class);
		
	private static final Map<String, ProtocolInfo> supportedMimetypeProtocols = new HashMap<>();
	{
		for (ProtocolInfo info : MediaServerConnectionManagerServiceImpl.SUPPORTED_PROTOCOLS) {
			supportedMimetypeProtocols.put(info.getContentFormat(), info);
		}		
	}
	
	public static MimeTypes fileExtensionMimeTypes = new MimeTypes();
	{
		fileExtensionMimeTypes.addMimeMapping("mp3", "audio/mpeg");
		fileExtensionMimeTypes.addMimeMapping("flac", "audio/flac");
		fileExtensionMimeTypes.addMimeMapping("ogg", "audio/ogg");
		fileExtensionMimeTypes.addMimeMapping("m4a", "audio/mp4");
		fileExtensionMimeTypes.addMimeMapping("aac", "audio/aac");
		fileExtensionMimeTypes.addMimeMapping("mka", "audio/x-matroska");
		fileExtensionMimeTypes.addMimeMapping("mkv", "video/x-matroska");
		fileExtensionMimeTypes.addMimeMapping("mp4", "video/mp4");
		fileExtensionMimeTypes.addMimeMapping("m3u", "audio/x-mpegurl");
		fileExtensionMimeTypes.addMimeMapping("m3u8", "application/vnd.apple.mpegurl");
		fileExtensionMimeTypes.addMimeMapping("png", "image/png");
		fileExtensionMimeTypes.addMimeMapping("jpg", "image/jpeg");
		fileExtensionMimeTypes.addMimeMapping("gif", "image/gif");
	}	
	
	private static final Encoder BASE64ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Decoder BASE64DECODER = Base64.getUrlDecoder();

	@Override
	public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult, long maxResults,
			SortCriterion[] orderby) throws ContentDirectoryException {

		try {
			ArrayList<DIDLObject> didlObjects = new ArrayList<>();
			LOGGER.trace("%s \"%s\" (%s) %d - %d".formatted(browseFlag, objectID, filter, firstResult, maxResults));			
			
			// root container
			if ("0".equals(objectID)) {
				
				switch (browseFlag) {
					case METADATA:	// Return info about self
						StorageSystem rootContainer = new StorageSystem();
						rootContainer.setId("0");
						rootContainer.setParentID("-1"); // no parent
						rootContainer.setTitle(cleanTitle("SystemRoots"));
						rootContainer.setRestricted(true);
						rootContainer.setSearchable(false);
						rootContainer.setChildCount(File.listRoots().length);
						didlObjects.add(rootContainer);
						break;
					case DIRECT_CHILDREN: // Return children
						for (File drive : File.listRoots()) {
							StorageVolume  driveContainer = new StorageVolume();						
							driveContainer.setId(encodeItemId(drive));
							driveContainer.setParentID("0");
							driveContainer.setTitle(cleanTitle(drive.getAbsolutePath()));
							driveContainer.setRestricted(true);
							driveContainer.setSearchable(false);
							driveContainer.setChildCount(getChildCount(drive));
							didlObjects.add(driveContainer);        		
						}											
						break;
				default:
					throw new IllegalArgumentException("browseFlag = " + browseFlag);
				}        	
								
			} else {
				
				final File requestedObject = decodeItemId(objectID);
				LOGGER.trace("Decoded:" + requestedObject.getAbsolutePath());
				
				if (requestedObject.exists() && requestedObject.canRead()) {
					
					switch (browseFlag) {
						case METADATA:					
							didlObjects.add(getTypedDIDLObject(requestedObject));
							break;
						case DIRECT_CHILDREN:
							if (requestedObject.isDirectory()) { 
								final File[] children = requestedObject.listFiles();
								if (children != null) {
									for (File child : children) {
										if (!child.isHidden()) {
											final DIDLObject typedDIDLObject = getTypedDIDLObject(child);
											if (typedDIDLObject != null) {
												didlObjects.add(typedDIDLObject);										
											}
											
										}
									}																	
								}
							} else {
								throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, "Object is not an container"); 
							}
							break;
						default:
							throw new IllegalArgumentException("browseFlag = " + browseFlag);
					}
					
				} else {
					throw new ContentDirectoryException(ContentDirectoryErrorCode.NO_SUCH_OBJECT, objectID);
				}
				
			}

			
			final DIDLContent didl = new DIDLContent();
			if (didlObjects.size() > 0) {
			
				if (firstResult < didlObjects.size()) {
					
					if (didlObjects.size() > 1) {
						// We sort for convinience, but also need predictabel order for paging
						
						//TODO: Needs to take locale into account 
						didlObjects.sort(new Comparator<DIDLObject>() {
							@Override
							public int compare(DIDLObject o1, DIDLObject o2) {
								return o1.getTitle().compareTo(o2.getTitle());
							}				
						});
					}
					
					final int maxEntry = Long.valueOf(Math.max(Math.min(firstResult + maxResults, didlObjects.size()),1)).intValue();
					for (DIDLObject dObj : didlObjects.subList(Long.valueOf(firstResult).intValue(), maxEntry)) {
						didl.addObject(dObj);					
					}
					
					
				} else {
					throw new IllegalArgumentException("firstresult > childCount ("+didlObjects.size()+")");
				}
			}
			
			final String xml = new DIDLParser().generate(didl);
			return new org.jupnp.support.model.BrowseResult(xml, didl.getCount(), didlObjects.size());
		
		} catch (ContentDirectoryException c) {
			throw c;
		} catch (Throwable t) {
			LOGGER.debug("Unexpected error", t);
			throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS.getCode(),t.toString());			
		}
		
	}
	
	private static int getChildCount(File parent) {
		int count = 0;

		final File[] children = parent.listFiles();
		if (children != null) {
			for (File child : children) {
				if (child.canRead() && !child.isHidden()) {
					if (child.isDirectory()) {
						count += 1;
					} else {
						final String mimeTypeStr = getMimetype(child);
						if (mimeTypeStr != null && supportedMimetypeProtocols.get(mimeTypeStr) != null) {
							count += 1; 
						}
					}
				}
			}			
		}
		
		return count;		
	}	

	private static DIDLObject getTypedDIDLObject(File fileObject) {
		if (fileObject.isDirectory()) {
			final StorageFolder folder =  new StorageFolder();
			folder.setSearchable(false);
			folder.setChildCount(getChildCount(fileObject));
			if (folder.getChildCount().intValue()>0) {
				return updateTypedObject(folder, fileObject, null);				
			} else {
				return null;
			}
		}
		
		final String mimeTypeStr = getMimetype(fileObject);
		
		if (mimeTypeStr == null) return null;
		
		
		final ProtocolInfo protocolinfo = supportedMimetypeProtocols.get(mimeTypeStr);
		
		if (protocolinfo == null) return null;		

		if (mimeTypeStr.startsWith("audio/")) {
			if (fileObject.getName().toLowerCase().endsWith("m3u") || fileObject.getName().toLowerCase().endsWith("m3u8")) {				
				return updateTypedObject(new AudioItem(), fileObject, protocolinfo);
			} else {
				return updateTypedObject(new MusicTrack(), fileObject, protocolinfo);
			}
		} else if (mimeTypeStr.startsWith("video/")) {
			return updateTypedObject(new VideoItem(), fileObject, protocolinfo);
		} else if (mimeTypeStr.startsWith("image/")) {
			return updateTypedObject(new ImageItem(), fileObject, protocolinfo);
		} 
		
		return null;
		
	}
		
	
	private static <D extends DIDLObject> D updateTypedObject(D dObj, File filedObject, ProtocolInfo protocolinfo) {
		dObj.setId(encodeItemId(filedObject));
		if (filedObject.getParentFile() != null) {
			dObj.setParentID(encodeItemId(filedObject.getParentFile())); 			
		} else {
			dObj.setParentID("0");
		}
		dObj.setTitle(cleanTitle(filedObject.getName()));
		dObj.setRestricted(true);

		// Can only happen for directories
		if (protocolinfo != null) {			
			// We add the file extension to help jetty infer the mimetype mostly 
			final String streamUrl = HeimklangServiceRegistry.getContentServerBase() + "/" + dObj.getId() + "."+ getFileExtension(filedObject);
			dObj.addResource(new Res(protocolinfo, filedObject.length(), streamUrl));
		}
		return dObj;
	}
	

	
	private static String getMimetype(File child) {
		String mimeTypeStr = null;
		try {
			mimeTypeStr = Files.probeContentType(Path.of(child.getAbsolutePath()));
		} catch (IOException e) {
			LOGGER.warn("\"" + child.getAbsolutePath() + "\" : " + e.toString() );
		}
		
		if (mimeTypeStr == null) {
			final String ext = getFileExtension(child);
			if (ext != null) {
				mimeTypeStr = fileExtensionMimeTypes.getMimeByExtension(mimeTypeStr);
			}
		}
		
		if (mimeTypeStr != null) {
			mimeTypeStr = mimeTypeStr.toLowerCase();
		}
		
		return mimeTypeStr;
	}
	
	public static String getFileExtension(File child) {
	    final String fileName = child.getName();
	    final int lastIndex = fileName.lastIndexOf('.');
	    if (lastIndex > 0 && lastIndex < fileName.length() - 1) { // dot not at start or end
	        return fileName.substring(lastIndex + 1).toLowerCase(); // optionally lowercase
	    } else {
	    	return null;	    	
	    }
	}
		
	private static String encodeItemId(File fileObject) {		
		// I used just teh absolute path before, and it worked semantically, but some device reject obcect with ids that contain : or \ it seems
		return BASE64ENCODER.encodeToString(fileObject.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
	}
	
	public static File decodeItemId(String objectId) {
		return new File(new String(BASE64DECODER.decode(objectId), StandardCharsets.UTF_8));
	}
	
	private static String cleanTitle(String s) {
		// At least some unnamed vendor devices has problems when this character is in a title
		return s.replace("\\", "");
	}
}

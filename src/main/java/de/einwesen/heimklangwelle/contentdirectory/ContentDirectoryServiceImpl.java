package de.einwesen.heimklangwelle.contentdirectory;

import org.jupnp.model.types.csv.CSV;
import org.jupnp.support.contentdirectory.AbstractContentDirectoryService;
import org.jupnp.support.contentdirectory.ContentDirectoryErrorCode;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.Res;
import org.jupnp.support.model.SortCriterion;
import org.jupnp.support.model.container.StorageFolder;
import org.jupnp.support.model.item.VideoItem;
import org.jupnp.util.MimeType;

import de.einwesen.heimklangwelle.HeimklangServiceRegistry;

//UPNP annotations are inherited from parent
public class ContentDirectoryServiceImpl extends AbstractContentDirectoryService {


	@Override
	public CSV<String> getSearchCapabilities() {
		// TODO Auto-generated method stub
		return super.getSearchCapabilities();
	}

	@Override
	public CSV<String> getSortCapabilities() {
		// TODO Auto-generated method stub
		return super.getSortCapabilities();
	}

	@Override
	public BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult, long maxResults,
			SortCriterion[] orderby) throws ContentDirectoryException {

        DIDLContent didl = new DIDLContent();

        // root container
        if ("0".equals(objectID)) {
        	StorageFolder  videos = new StorageFolder();
            videos.setId("1");
            videos.setParentID("0");
            videos.setTitle("Videos");
            videos.setRestricted(true);
            //videos.setClazz(null)
            didl.addContainer(videos);
        }

        // “Videos” container content
        if ("1".equals(objectID)) {
            VideoItem video = new VideoItem();
            video.setId("100");
            video.setParentID("1");
            video.setTitle("Sample MP3");
            

            // Correct Res with String URI + ProtocolInfo
            MimeType mimeType = new MimeType("audio", "mpeg");

         // 2. Resource (URL zum Stream) hinzufügen
         // Die URL muss für den Controller (z.B. Handy) erreichbar sein!
         String streamUrl = HeimklangServiceRegistry.getContentServerBase() + "/sample.mp3";
         Res res = new Res(mimeType, 1234567L, streamUrl); 
         
            video.addResource(res);
            didl.addItem(video);
        }

        try {
            String xml = new DIDLParser().generate(didl);
            return new org.jupnp.support.model.BrowseResult(xml, didl.getCount(), didl.getCount());
        } catch (Exception ex) {
            throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS, ex.getMessage());
        }		
		
	}

}

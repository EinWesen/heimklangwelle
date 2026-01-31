package de.einwesen.heimklangwelle.test.manual;

import java.io.File;

import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.model.Channel;

import de.einwesen.heimklangwelle.renderers.MPVRendererWrapper;

public class MpvBasics {
    
	public static void main(String[] args) throws Exception {
    	
		//System.setProperty(MPVRendererWrapper.CONFIG_PROPERTY_MPV_PATH, "/path/to/File.exe");
		String testPl = new File(MpvBasics.class.getResource("/internetstreams.m3u").getFile()).getAbsolutePath(); 
		
		MPVRendererWrapper mp3 = new MPVRendererWrapper();
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shutdown!");
			mp3.shutdown();
		}));
		
		while(!mp3.isReady()) {
			Thread.sleep(50);
		}
				
    	try {
    		System.out.println("Load content...");
			mp3.setCurrentContent(testPl, "");
			Thread.sleep(10000);

			if (mp3.getPlaylistSize() != 2) {
				throw new IllegalStateException("Playlist size wa snot 2 as expected");
			} else {
				System.out.println("... found 2 tracks as expected");	
			}
			
			System.out.println("Set Volumne lower");
			mp3.setVolume(Channel.Master, 50);
			Thread.sleep(5000);
			
			
			System.out.println("Mute");
			mp3.setMute(Channel.Master, true);
			Thread.sleep(10000);
			
			
			System.out.println("Unmute");
			mp3.setMute(Channel.Master, false);
			Thread.sleep(5000);
			
			System.out.println("Next Track [2]");
			mp3.nextTrack();
			Thread.sleep(10000);
			
			boolean failedAsExpected = false;
			try {
				System.out.println("Next Track [3] (Should fail) ...");
				mp3.nextTrack();
			} catch (AVTransportException a) {
				if (a.getErrorCode() == AVTransportErrorCode.ILLEGAL_SEEK_TARGET.getCode()) {
					failedAsExpected = true;
				} else {
					throw a;
				}
			}
			if (!failedAsExpected) {
				throw new IllegalStateException("Skip on last track did not fail as expected");
			} else {
				System.out.println("... did fail as expected");				
			}
						
			Thread.sleep(10000);
			System.out.println("Prev Track [1]");
			mp3.previousTrack();
			Thread.sleep(10000);
			
			
		} catch (AVTransportException e) {
			e.printStackTrace();
		}
    	
    	System.exit(0);
		
    }	
}

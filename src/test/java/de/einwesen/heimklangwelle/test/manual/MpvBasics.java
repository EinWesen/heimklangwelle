package de.einwesen.heimklangwelle.test.manual;

import java.io.File;

import org.jupnp.support.avtransport.AVTransportErrorCode;
import org.jupnp.support.avtransport.AVTransportException;
import org.jupnp.support.model.Channel;

import de.einwesen.heimklangwelle.renderers.MPVRendererWrapper;

public class MpvBasics implements ManualTestCase {
    
	private MPVRendererWrapper mpv = null;
	public void runTest() throws Exception {
    	
		//System.setProperty(MPVRendererWrapper.CONFIG_PROPERTY_MPV_PATH, "/path/to/file.exe");
				
		String testPl = new File(MpvBasics.class.getResource("/internetstreams.m3u").getFile()).getAbsolutePath(); 

		mpv = new MPVRendererWrapper();
		
		while(!mpv.isReady()) {
			Thread.sleep(50);
		}
				
    	try {
    		System.out.println("Load content...");
			mpv.setCurrentContent(testPl, "");
			Thread.sleep(10000);

			if (mpv.getPlaylistSize() != 2) {
				throw new IllegalStateException("Playlist size was not 2 as expected");
			} else {
				System.out.println("... found 2 tracks as expected");	
			}
			
			System.out.println("Set Volumne lower");
			mpv.setVolume(Channel.Master, 50);
			Thread.sleep(5000);
			
			
			System.out.println("Mute");
			mpv.setMute(Channel.Master, true);
			Thread.sleep(10000);
			
			
			System.out.println("Unmute");
			mpv.setMute(Channel.Master, false);
			Thread.sleep(5000);
			
			System.out.println("Next Track [2]");
			mpv.nextTrack();
			Thread.sleep(10000);
			
			boolean failedAsExpected = false;
			try {
				System.out.println("Next Track [3] (Should fail) ...");
				mpv.nextTrack();
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
			mpv.previousTrack();
			Thread.sleep(10000);
			
			
		} catch (AVTransportException e) {
			e.printStackTrace();
		} finally {
			System.out.println("Shutdown!");
			mpv.shutdown();
		}
    }
	
	public static void main(String[] args) throws Exception {
		MpvBasics test = new MpvBasics();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (test.mpv != null && test.mpv.isReady()) {
				System.out.println("Forced shutdown!");
				test.mpv.shutdown();				
			}
		}));		
		
		test.runTest();
		
	}
	
}

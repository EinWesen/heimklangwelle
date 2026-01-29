package de.einwesen.heimklangwelle.upnpsupport;

import org.jupnp.model.types.UnsignedIntegerFourBytes;

public interface RendererChangeEventListener {

	default void firePlayerStateChangedEvent(UnsignedIntegerFourBytes instanceId) {
		// Do nothing
	}

	default void firePlayerVolumneChangedEvent(UnsignedIntegerFourBytes instanceId) {
		// Do nothing
	}
	
}
package de.einwesen.heimklangwelle.upnpsupport;

import java.util.concurrent.CompletableFuture;

import org.jupnp.controlpoint.ActionCallback;

public interface FetchActionCallback<T> {
	public CompletableFuture<T> getFutureResult();
	public default ActionCallback castToActionCallback() {
		return (ActionCallback)this;			
	}
}

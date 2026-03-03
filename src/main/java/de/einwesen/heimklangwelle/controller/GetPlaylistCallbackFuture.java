package de.einwesen.heimklangwelle.controller;

import java.util.concurrent.CompletableFuture;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.DIDLContent;

import de.einwesen.heimklangwelle.upnpsupport.FetchActionCallback;

public class GetPlaylistCallbackFuture extends ActionCallback implements FetchActionCallback<DIDLContent> {

	public GetPlaylistCallbackFuture(Service<?, ?> service, int instanceId) {
		super(new ActionInvocation<>(service.getAction("GetPlaylist")));
        getActionInvocation().setInput("InstanceID", new UnsignedIntegerFourBytes(instanceId));
	}

	private CompletableFuture<DIDLContent> completableFuture = new CompletableFuture<DIDLContent>();

	@Override
	public void success(@SuppressWarnings("rawtypes") ActionInvocation invocation) {
		final String didl = invocation.getOutput("CurrentPlaylist").getValue().toString();
		if (didl != null && !"".equals(didl.trim())) {
			try {
				
				final DIDLParser didlParser = new DIDLParser();
				final DIDLContent didlContent = didlParser.parse(didl);
				completableFuture.complete(didlContent);
				
			} catch (Exception e) {
				invocation.setFailure( new ActionException(ErrorCode.ACTION_FAILED, "Can't parse DIDL XML response: " + e, e));
				failure(invocation, null);
			}
			
		} else {
			completableFuture.complete(new DIDLContent());
		}
		
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {

		if (invocation.getFailure() != null) {
			this.completableFuture.completeExceptionally(invocation.getFailure());
		} else {			
			this.completableFuture.completeExceptionally(new ActionException(operation != null ? operation.getStatusCode() : 0, defaultMsg));		
		}
		
	}
	
	@Override
	public CompletableFuture<DIDLContent> getFutureResult() {
		return completableFuture;
	}

}

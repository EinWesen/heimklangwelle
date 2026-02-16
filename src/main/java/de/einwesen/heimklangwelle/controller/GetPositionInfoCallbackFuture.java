package de.einwesen.heimklangwelle.controller;

import java.util.concurrent.CompletableFuture;

import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Service;
import org.jupnp.support.avtransport.callback.GetPositionInfo;
import org.jupnp.support.model.PositionInfo;

import de.einwesen.heimklangwelle.upnpsupport.FetchActionCallback;

public class GetPositionInfoCallbackFuture extends GetPositionInfo implements FetchActionCallback<PositionInfo>{

	private CompletableFuture<PositionInfo> completableFuture = new CompletableFuture<PositionInfo>();
	
	public GetPositionInfoCallbackFuture(Service<?, ?> service) {
		super(service);
	}

	@Override
	public void received(ActionInvocation<?> invocation, PositionInfo positionInfo) {
		this.completableFuture.complete(positionInfo);		
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
		this.completableFuture.completeExceptionally(new ActionException(operation != null ? operation.getStatusCode() : 0, defaultMsg));		
	}

	@Override
	public CompletableFuture<PositionInfo> getFutureResult() {
		return this.completableFuture;
	}

}

package de.einwesen.heimklangwelle.controller;

import java.util.concurrent.CompletableFuture;

import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Service;
import org.jupnp.support.contentdirectory.callback.Browse;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.SortCriterion;

import de.einwesen.heimklangwelle.upnpsupport.FetchActionCallback;

public class BrowseCallbackFuture extends Browse implements FetchActionCallback<DIDLContent> {

	private CompletableFuture<DIDLContent> completableFuture = new CompletableFuture<DIDLContent>();
	
	//public BrowseCallbackFuture(Service<?, ?> service, String objectID, BrowseFlag flag, String filter, long firstResult, Long maxResults, SortCriterion... orderBy) {
	public BrowseCallbackFuture( Service<?, ?> service, String objectID, BrowseFlag flag, String filter, long firstResult, Long maxResults, SortCriterion... orderBy) {
		super(service, objectID, flag, filter, firstResult, maxResults, orderBy);
	}

	@Override
	public void received(ActionInvocation<?> actionInvocation, DIDLContent didl) {
		this.completableFuture.complete(didl);		
	}

	@Override
	public void updateStatus(Status status) {
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
		this.completableFuture.completeExceptionally(new ActionException(operation != null ? operation.getStatusCode() : 0, defaultMsg));		
	}
	
	@Override
	public CompletableFuture<DIDLContent> getFutureResult() {
		return completableFuture;
	}
	
}

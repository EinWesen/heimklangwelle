import * as api from "./restupnp.js";

export class DefaultRemoteRenderer {
	static PLAYLIST_TYPE_LOCAL = 10;
	static PLAYLIST_TYPE_REMOTE = 20;
	
	constructor() {
		this.playlistType=DefaultRemoteRenderer.PLAYLIST_TYPE_LOCAL;
		this.deviceUdn = undefined;
		this.instanceId = undefined;		
	}
	
	async _tryExecuteServiceCall(callOptions) {
		if (this.deviceUdn) {
			return api.callServiceAction(this.deviceUdn, callOptions);		
		} else {
			return Promise.reject({
				summary: 'No connected device',
			    isError: false,
			    error: 0,
			    response: undefined,
			    data: ''
			});
		}
	}
		
	async _tryExecuteAVTransportCall0(actionName) {
		return this._tryExecuteServiceCall({
		  "serviceId": "AVTransport",
		  "action": actionName,
		  "inputArguments": {
		     "InstanceID": this.instanceId
		  }
		});  
	}	
	
	 async setAVTransportItem(item) {
		return this._tryExecuteServiceCall({
	   		"serviceId": "AVTransport",
	   		"action": "SetAVTransportURI",
	   		"inputArguments": {
	      		"InstanceID": this.instanceId,
	      		"CurrentURI": item.uri,
	      		"CurrentURIMetaData": item.uriMetaData
	   		}
		});
	}	
	
	async play() {
	 	return this._tryExecuteServiceCall({
	   		"serviceId": "AVTransport",
	   		"action": "Play",
	   		"inputArguments": {
	      		"InstanceID": this.instanceId,
	      		"Speed": "1"
	   		}
	 	});
	}
	
	async pause() {
		return this._tryExecuteAVTransportCall0("Pause");
	}

	 async stop() {
		return this._tryExecuteAVTransportCall0("Stop");
	 }

	 async next() {
	 	return this._tryExecuteAVTransportCall0("Next");
	 }

	 async previous() {
		this._tryExecuteAVTransportCall0("Previous");  
	 }

	 async setVolume(volAsString) {
		return this._tryExecuteServiceCall({
		   "serviceId": "RenderingControl",
		   "action": "SetVolume",
		   "inputArguments": {
		      "InstanceID": this.instanceId,
		      "Channel": "Master",
		      "DesiredVolume": volAsString
		   }
		});
	 }

	 async setMute(booleanStr) {	
		return this._tryExecuteServiceCall({
		   "serviceId": "RenderingControl",
		   "action": "SetMute",
		   "inputArguments": {
		      "InstanceID": this.instanceId,
		      "Channel": "Master",
		      "DesiredMute": (booleanStr == 'true' ? 'false' : 'true')
		   }
		});		
	 }  
	 
	 async playEntry({itemindex, item}) {
		return this.setAVTransportItem(item).then((apiResult) => {
			return this.play();	
		});			
	 }
	 
	 async newPlaylist() {
		//Nothing to do here
		return Promise.resolve(true);
	 }
	 
	 async addToPlaylist(item, replace) {
		//Nothing to do here
		return Promise.resolve(true);		
	 }
	 async removeFromPlaylist(itemidx) {
		//Nothing to do here
		return Promise.resolve(true);		
	 }
	 async movePlaylistItem(from,to) {
		//Nothing to do here
		return Promise.resolve(true);		
	 }
	 async getInitialPlaylist() {
		return Promise.resolve(undefined);
	 }
	
	 createRendererEventSubcription(eventHandler) {
		return api.createRendererEventSubcription(this.deviceUdn, this.instanceId, eventHandler);
	 }	
}


export class HeimklangRemoteRenderer extends DefaultRemoteRenderer {
	constructor() {
		super();
		this.playlistType=DefaultRemoteRenderer.PLAYLIST_TYPE_REMOTE;
	}
	async getInitialPlaylist() {
		if (this.deviceUdn) {
			return api.fetchCurrentPlaylist(this.deviceUdn, this.instanceId).then(({ response, data }) => {
				return data.children;
			});					
		} else {
			return Promise.reject({
				summary: 'No connected device',
			    isError: false,
			    error: 0,
			    response: undefined,
			    data: ''
			});
		}		
	}
	
	async newPlaylist() {
		return this._tryExecuteServiceCall({
		  "serviceId": "HeimklangPlaylistManagement",
		  "action": 'ClearAVTransportUris',
		  "inputArguments": {
		     "InstanceID": this.instanceId
		  }
		});  		
	}

	async playEntry({itemindex, item}) {
		await this._tryExecuteServiceCall({
		   "serviceId": "AVTransport",
		   "action": "Seek",
		   "inputArguments": {
		      "InstanceID": this.instanceId,
		      "Unit": "TRACK_NR",
		      "Target": ""+(parseInt(itemindex)+1)
		   }
		});	
		return this.play();	
	}	
	
	async addToPlaylist(item, replace) {
		// If we try to replace and load a list, we do exactly that
		if (replace && (item.uri.toLowerCase().endsWith('.m3u') || item.uri.toLowerCase().endsWith('.m3u8')) ) {
			return this.setAVTransportItem(item);
		} else {
			/* This will fail if:
			 * - A playlist is not active and replace is false
			 * - The current playlist is not writable
			 */
			
			if (replace) {
				// Will switch to playlist mode, if we were not already
				await this.newPlaylist();
			} 
			return this._tryExecuteServiceCall({
				"serviceId": "HeimklangPlaylistManagement",
				"action": "AddAVTransportURI",
				"inputArguments": {
			  		"InstanceID": this.instanceId,
			  		"NextAVTransportURI": item.uri,
			  		"NextAVTransportURIMetaData": item.uriMetaData
				}
			});									
		}
	}
	
	async removeFromPlaylist(itemidx) { 
		return this._tryExecuteServiceCall({
		   "serviceId": "HeimklangPlaylistManagement",
		   "action": "RemoveAVTransportUri",
		   "inputArguments": {
		      "InstanceID": this.instanceId,
		      "PlIndex": ""+itemidx
		   }
		});	
	}
	
	async movePlaylistItem(from,to) {
		return this._tryExecuteServiceCall({
		   "serviceId": "HeimklangPlaylistManagement",
		   "action": "MoveAVTransportUri",
		   "inputArguments": {
		      "InstanceID": this.instanceId,
		      "PlIndexFrom": ""+from,
			  "PlIndexTo": ""+to
		   }
		});	
	}	
	
}








    



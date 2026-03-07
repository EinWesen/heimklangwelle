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
	 
	 // TODO: It's bad design, that the renderer does not know its own state
	 // But i don't feel like refactoring everything right now, just to know 
	 // when to stop first  
	 async playEntry({itemindex, item}, currentTransportState) {
		if (currentTransportState == 'PLAYING' || currentTransportState == 'PAUSED_PLAYBACK') {			
			await this.stop();								
		}
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

	async playEntry({itemindex, item}, currentTransportState) {
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

export class LocalBrowserRenderer {
	constructor() {
		this.playlistType=DefaultRemoteRenderer.PLAYLIST_TYPE_LOCAL;
		this.deviceUdn = '-';
		this.instanceId = 0;
		
		this._audioElement = new Audio();
		this._metadata = '';
		this._eventHandler = undefined;
		this._lastTime = 0;
		this._stopRequest=false;
		this.onerror='willbeignored';
		
		this._audioElement.addEventListener('loadstart', () => {
		  this.dispatchLastChange({
			'TransportState': 'TRANSITIONING',
			'AVTransportURIMetaData': this._metadata,
			'AVTransportURI': this._audioElement.src,
			'CurrentTrack': 'NOT_IMPLEMENTED',			
			'NumberOfTracks': 'NOT_IMPLEMENTED',
			'RelativeTimePosition': '00:00:00',
			'CurrentTrackURI': this._audioElement.src,
			'CurrentTrackMetaData': this._metadata,
		  });		  
		});
		
		this._audioElement.addEventListener('pause', () => {
			if (!this._audioElement.paused) {
				this.dispatchLastChange({
					'TransportState': this._stopRequest? 'STOPPED':'PAUSED_PLAYBACK',					
				});							
				this.stopRequest = false;
			}
		});
		
		this._audioElement.addEventListener('playing', () => {
			this.dispatchLastChange({
				'TransportState': 'PLAYING',
			});			
		});		

		this._audioElement.addEventListener('ended', () => {
			this._lastTime = 0;
			this.dispatchLastChange({
				'TransportState': 'STOPPED',
				'RelativeTimePosition': '00:00:00',
			});			
		});				
		
		this._audioElement.addEventListener('volumechange', () => {
			this.dispatchLastChange({
				'Volume': '' + Math.trunc(this._audioElement.volume * 100),
				'Mute': '' + this._audioElement.muted
			});			
		});			

		this._audioElement.addEventListener('error', () => {
			this.dispatchLastChange({
				'TransportState': 'STOPPED',
				'RelativeTimePosition': '00:00:00',
			});			
		});
		
		this._audioElement.addEventListener("timeupdate", (event) => { 
			if (this._eventHandler != undefined) {
				const ct = this._audioElement.currentTime;
				if (Math.abs(this._lastTime - ct) > 10) {
					this._lastTime = ct;
	
					const dateObj = new Date(0);
					dateObj.setSeconds(ct); 					
					
					const dummyEvent = {
						type: 'RelativeTimePosition',
						data: dateObj.toISOString().substring(11, 19)
					};
					this._eventHandler(dummyEvent);				
				}				
			}
		});
		
	}
	
	dispatchLastChange(data) {

		if (this._eventHandler != undefined) {
			const dummyEvent = {
				type: 'LastChange',
				data: JSON.stringify(data)
			};
			this._eventHandler(dummyEvent);			
		}
	}
	
	 async setAVTransportItem(item) {
		this._metadata = item.uriMetaData;
		this._audioElement.src = item.uri;
		return Promise.resolve({response: undefined, data: ''}); 
	 }	
	
	async play() {
		return this._audioElement.play()		
		.then( (_) => { return {response: undefined, data: ''}; })
		.catch(error => Promise.reject({
			summary: 'Error playing track:' + error.message,
			isError: true,
			error: error,
			response: undefined,
			data: ''
		}));
	}
	
	async pause() {
		this._audioElement.pause();
		return Promise.resolve({response: undefined, data: ''});
	}

	 async stop() {		
		this._stopRequest=true;
		this._audioElement.pause();
		this._audioElement.currentTime = 0;
		this.dispatchLastChange({
			'TransportState': 'STOPPED',
			'RelativeTimePosition': '00:00:00',
		});	
	 }

	 async next() {
		return Promise.reject({
			summary: 'No next track available',
			isError: false,
			error: 0,
			response: undefined,
			data: ''
		});
	 }

	 async previous() {
		return Promise.reject({
			summary: 'No previous track available',
			isError: false,
			error: 0,
			response: undefined,
			data: ''
		});
	 }

	 async setVolume(volAsString) {
		
		return new Promise((resolve, reject) => {
			try {
				this._audioElement.volume = (parseInt(volAsString)/100);
				resolve({response: undefined, data: ''});				
			} catch (error) {
				reject({
					summary: 'Could not set volume:' + error.message,
					isError: true,
					error: error,
					response: undefined,
					data: ''
				});
			}
		});
	 }

	 async setMute(booleanStr) {	
		this._audioElement.muted = ('true' == booleanStr);
		return Promise.resolve({response: undefined, data: ''});	
	 }  
	 
	 async playEntry({itemindex, item}, currentTransportState) {
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
		this._eventHandler = eventHandler;
		this.dispatchLastChange({
			'TransportState': 'NO_MEDIA_PRESENT',
			'AVTransportURIMetaData': '',
			'AVTransportURI': '',
			'CurrentTrack': 'NOT_IMPLEMENTED',			
			'NumberOfTracks': 'NOT_IMPLEMENTED',
			'RelativeTimePosition': '00:00:00',
			'CurrentTrackURI': '',
			'CurrentTrackMetaData': '',
			'Volume': 100,
			'Mute': 'false'			
		});	
		return this;
	 }
	 
	 async close() {
		// for mocking eventsource
		await this.stop();
		this._audioElement = undefined;
	 }
}







    



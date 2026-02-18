import * as api from "./restupnp.js";

function tryParseTitleFromDidl(metaData) {
	try {
		const parser = new DOMParser();
		const xmlDoc = parser.parseFromString(metaData, "application/xml");

		const errorNode = xmlDoc.querySelector("parsererror");
		if (errorNode) {
		  console.error(errorNode, metaData);
		} else {
	      const titles = xmlDoc.getElementsByTagName("dc:title");
		  if (titles) {
			if (titles.length == 1) {
				return titles[0].textContent;				
			}
		  }
		  console.warn("Not title found", metaData);  
		}
		
	} catch (error) {
		console.error(error, metaData);
	}
	
	return undefined;
}

function createPlaylistLi(item, index) {
	const li = document.createElement("li");
	li.textContent = item.title;
	
	li.draggable = true;
	li.dataset.itemindex = index;

/*	li.addEventListener("dragstart", e => {
	    draggedIndex = Number(e.target.dataset.index);
	});

	li.addEventListener("dragover", e => e.preventDefault());

	li.addEventListener("drop", e => {
	    e.preventDefault();
	    const targetIndex = Number(e.target.dataset.index);
	    reorder(draggedIndex, targetIndex);
	});
*/		
	return li;
}

export class RemoteRenderer { 
  static EVENT_NAME_ACTIONFAILED = 'actionFailed';
  static EVENT_NAME_PLAYLIST_DBLCLICK = 'PlaylistDblClick';
  
  constructor(options) {
	this._deviceUdn = undefined;
	this._instanceId = undefined;
	this._properties = undefined;		
	this._eventSource = undefined;
	this._playlist = undefined;		
	
	this._volDebounceTimer = undefined;
	this._stateDebounceTimer = undefined;
	this._currentPlaylistItemUrl = ''; 
	
	this._containerElement = document.getElementById(options["player-panel"]);	
	
	this._titleElement = document.getElementById(options["transport-title"]);
	this._trackTitleElement = document.getElementById(options["track-title"]);
	this._timeElement = document.getElementById(options["time-info"]);
	this._volumeElement = document.getElementById(options["volume-slider"]);
	this._stateElement = document.getElementById(options["transport-state"]);
		
	this._playlistContainerElement = document.getElementById(options["playlist-container"]);
	
	this.selectDevice(undefined, undefined);
	
	document.getElementById(options["btn-play"]).onclick = (event) => this.play();
	document.getElementById(options["btn-pause"]).onclick = (event) => this.pause();
	document.getElementById(options["btn-stop"]).onclick = (event) => this.stop();
	document.getElementById(options["btn-next"]).onclick = (event) => this.next();
	document.getElementById(options["btn-prev"]).onclick = (event) => this.previous();
	document.getElementById(options["btn-next-media"]).onclick = (event) => this.nextMedia();
	document.getElementById(options["btn-prev-media"]).onclick = (event) => this.previousMedia();	
	
	this._volumeElement.oninput = (event) => {
	   clearTimeout(this._volDebounceTimer);	
	   this._volDebounceTimer = setTimeout(() => {
	      this.setVolume(event.target.value);
	   }, 500); // adjust delay as needed
	};
	
	this._playlistContainerElement.addEventListener("dblclick", (event) => {
	   const li = event.target.closest("li");
	   this._containerElement.dispatchEvent(
	   		new CustomEvent(RemoteRenderer.EVENT_NAME_PLAYLIST_DBLCLICK, {detail : this._playlist[li.dataset.itemindex]})
	   );		   
	});
	
  }
  
  async selectDevice(udn, instanceId) {
    this._deviceUdn = udn;
	this._instanceId = instanceId;
	
	if (this._eventSource) {
		this._eventSource.close();		
	}
	
	this._properties = {
		'AVTransportURI': '',
		'AVTransportURIMetaData': '',
		'CurrentTrackURI': '',
		'CurrentTrackMetaData': '',
		'CurrentTrack': '',
		'NumberOfTracks': '',
		'TransportState': 'STOPPED',
		'Mute': 'false',
		'Volume': '100',
		'RelativeTimePosition': ''
	}
	this._remotePropertyChanged();
	this.newPlaylist();
	
	if (this._deviceUdn) {
		this._eventSource = api.createRendererEventSubcription(udn, '0', (event) => {
			this._properties[event.type] = event.data;

			clearTimeout(this._stateDebounceTimer);	
			this._stateDebounceTimer = setTimeout(() => {
			   this._remotePropertyChanged();			
			}, 500); // adjust delay as needed
						
		});		
	    this._eventSource.onerror = (err) => {	        
	        this._eventSource.close();
			this._triggerActionError("Disconnected from remote event source");
	    }			
	}
	
  }  
  
  _remotePropertyChanged() {	
	this._timeElement.textContent = this._properties['RelativeTimePosition'];
	this._stateElement.textContent = this._properties['TransportState'];
	this._volumeElement.value = this._properties['Volume'];
	
	// Update tranport title display
	let transportTitle = undefined;
	
	if (this._properties['AVTransportURI'] != '' && this._properties['AVTransportURIMetaData'] != '' && this._properties['AVTransportURIMetaData'] != 'NOT_IMPLEMENTED') {
		transportTitle = tryParseTitleFromDidl(this._properties['AVTransportURIMetaData']);
	}
	if (!transportTitle) {
		transportTitle = this._properties['AVTransportURI'];		
	}
	this._titleElement.textContent = transportTitle;
	
	
	
	// Update track title display
	let trackTitle = undefined;	
	
	if (this._properties['CurrentTrackURI'] != this._properties['AVTransportURI']) {
		if (this._properties['CurrentTrackURI'] != '' && this._properties['CurrentTrackMetaData'] != '' && this._properties['CurrentTrackMetaData'] != 'NOT_IMPLEMENTED') {
			trackTitle = tryParseTitleFromDidl(this._properties['CurrentTrackMetaData']);
		}
		if (!trackTitle) {
			trackTitle = this._properties['CurrentTrackURI'];	
		} else {
			trackTitle = ' | ' + trackTitle;
		}
	} else {
		trackTitle = '';
	}

	this._trackTitleElement.textContent = trackTitle;
	
	
	// Mark current track in playlist, if possible
	const transportUri = this._properties['CurrentTrackURI'];
	let trackUri = this._properties['CurrentTrackURI'];
	if (trackUri == '') {
		trackUri = transportUri;		
	}
	
	if (this._playlist && this._playlist.length > 0) {
		if (trackUri != this._currentPlaylistItemUrl) {
			this._currentPlaylistItemUrl = trackUri;		
			const playlistNodes = this._playlistContainerElement.children;
			for (let itemIndex = 0; itemIndex < playlistNodes.length; itemIndex++) {
				if (this._playlist[itemIndex].uri == trackUri || this._playlist[itemIndex].uri == transportUri ) {
					playlistNodes[itemIndex].classList.add("active");				
				} else {
					playlistNodes[itemIndex].classList.remove("active");
				}
			} 		
		}		
	} else {
		this._currentPlaylistItemUrl = '';
	}
	
  }
    
  async play() {
	 return this._tryExecuteServiceCall({
	   "serviceId": "AVTransport",
	   "action": "Play",
	   "inputArguments": {
	      "InstanceID": this._instanceId,
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
	      "InstanceID": this._instanceId,
	      "Channel": "Master",
	      "DesiredVolume": volAsString
	   }
	}).then((apiResponse) => {
		// prevents bouncing of the control until update arrives
		this._properties['Volume'] = volAsString;
		return apiResponse;
	});
  }

  async toggleMute() {	
	return this._tryExecuteServiceCall({
	   "serviceId": "RenderingControl",
	   "action": "SetMute",
	   "inputArguments": {
	      "InstanceID": this._instanceId,
	      "Channel": "Master",
	      "DesiredMute": (this.properties['Mute'] == 'true' ? 'false' : 'true')
	   }
	});		
  }  

  async setAVTransportItem(item) {
	return this._tryExecuteServiceCall({
	   "serviceId": "AVTransport",
	   "action": "SetAVTransportURI",
	   "inputArguments": {
	      "InstanceID": this._instanceId,
	      "CurrentURI": item.uri,
	      "CurrentURIMetaData": item.uriMetaData
	   }
	});
  }
  
  addEventListener(type, listener) {
    this._containerElement.addEventListener(type, listener);
  }

  _triggerActionError(message) {
  	this._containerElement.dispatchEvent(
      new CustomEvent(RemoteRenderer.EVENT_NAME_ACTIONFAILED, {detail : message})
  	);
	return Promise.reject(message);
  }
  
  async _tryExecuteAVTransportCall0(actionName) {
	return this._tryExecuteServiceCall({
	  "serviceId": "AVTransport",
	  "action": actionName,
	  "inputArguments": {
	     "InstanceID": this._instanceId
	  }
	});  
  }
  
  async _tryExecuteServiceCall(callOptions) {
  	if (this._deviceUdn) {
		return api.callServiceAction(this._deviceUdn, callOptions).catch(errInfo => this._triggerActionError('Action failed: ' + errInfo.summary));		
	} else {
		return this._triggerActionError('No connected device');
	}
  }  
  
  newPlaylist() {
  	this._playlist = [];
  	this._playlistContainerElement.innerHTML = '';
  }
  
  addToPlaylist(item, replace) {	
	if (replace) {
		this.newPlaylist();
	}
	
	// TODO: Allow double entries in playlist
	// The only reason that is not allowed at the moment is, that i don't want to deal
	// with identifying the current track in that case
	for (const entry of this._playlist) {
		if (entry.uri == item.uri) {
			return -1;
		}
	}	
	  
	const index = this._playlist.push(item) - 1;
	this._playlistContainerElement.appendChild(createPlaylistLi(item, index));
	return index;		
  }
  
  findCurrentPlaylistIndex() {
	//TODO: Using the class as indicator breaks MVC-concept
	const playlistNodes = this._playlistContainerElement.children;
	for (let itemIndex = 0; itemIndex < playlistNodes.length; itemIndex++) {
		if (playlistNodes[itemIndex].classList.contains("active")) {
			return itemIndex;
		}			
	}
	return -1;
  }
  
  
  async nextMedia() {
	if (this._playlist.length > 0) {
		const currentItemIndex = this.findCurrentPlaylistIndex();
		let nextItem = currentItemIndex == -1 ? 0 : (currentItemIndex + 1);
		
		if (nextItem < this._playlist.length) {
			return this.setAVTransportItem(this._playlist[nextItem]);
		}
	}
	
	return this._triggerActionError('No next media in playlist');
  }

  async previousMedia() {
	if (this._playlist.length > 0) {
		const currentItemIndex = this.findCurrentPlaylistIndex();
		let nextItem = currentItemIndex == -1 ? (this._playlist.length-1) : (currentItemIndex - 1);
		
		if (nextItem >= 0) {
			return this.setAVTransportItem(this._playlist[nextItem]);
		}
	}

	return this._triggerActionError('No previous media in playlist');
  }
  

}
  
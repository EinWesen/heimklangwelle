import { DefaultRemoteRenderer,HeimklangRemoteRenderer,LocalBrowserRenderer } from "./renderer.js";

export class RelTimeHandler { 
  constructor(timeElement) {
	this._timeElement = timeElement;
	this._relTimeTimer = undefined;
	this._relTime = undefined;
	this._relTimeStr = undefined;
	this.reset();
  }
  
  setSeconds(s) {
	
	if (s != this._relTime) {
		this._relTime = s;		
		const dateObj = new Date(0);
		dateObj.setSeconds(s); 
		this._relTimeStr = dateObj.toISOString().substring(11, 19);
		this._relTimeChanged();		
	}
	return this;		
  }
  
  setTimeStr(s) {
	
	if (s != this._relTimeStr) {
		const timesplit = s.split(':');	
		const newTime = parseInt(timesplit[2]) + (parseInt(timesplit[1])*60) + (parseInt(timesplit[0])*3600);
	
		if (Math.abs(this._relTime - newTime) > 2) {
			this._relTime = newTime;
			this._relTimeStr = s;
			this._relTimeChanged();	
		}
	}
	return this;		
  }
  
  getTimeStr() {
	return this._relTimeStr; 
  }
  
  _clearTimer() {
	if (this._relTimeTimer) {
		clearInterval(this._relTimeTimer);
	}		
  }
  
  reset() {
	this._clearTimer();
	this._relTimeTimer = undefined;	
	this.restart();
	return this;
  }

  restart() {
	if (this._relTime != 0) {
		this._relTime = 0;
		this._relTimeStr = '00:00:00';
		this._relTimeChanged();		
	}
    return this;	
  }

  start() {
	const self = this;
	this._relTimeTimer = setInterval(() => {
		self.setSeconds(this._relTime + 1);
	}, 1000);
	return this;	
  }
  
  stop() {
	this.reset();
	return this;
  }
  
  pause() {	
	this._clearTimer();
	return this;
  }
  
  isRunning() {
	return this._relTimeTimer != undefined;
  }
  
  _relTimeChanged() {
  	this._timeElement.textContent = this.getTimeStr();
  }
      
}

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
	const span1 = li.appendChild(document.createElement('span'));
	const span2 = li.appendChild(document.createElement('span'));
	const span3 = li.appendChild(document.createElement('span'));
	
	let fileicon = '📄';

	// Add type-specific class
	if (item.mimeType?.startsWith("audio")) {
	    fileicon = '🎵';
	} else if (item.mimeType?.startsWith("video")) {
	    fileicon = '🎬';
	} 	
	
	span1.textContent = fileicon;
	span2.classList.add('title');
	span2.textContent = item.title;
	span3.classList.add('remove');
	span3.innerHTML = '&#9167;';
	
	li.draggable = true;
	li.dataset.itemindex = index;

	return li;
}

export class MediaController { 
  static EVENT_NAME_ACTIONFAILED = 'actionFailed';
  static EVENT_NAME_PLAYLIST_DBLCLICK = 'PlaylistDblClick';
  static EVENT_NAME_PLAYLIST_REMOVE = 'PlaylistRemoveItem';
  
  constructor(options) {
	this._properties = undefined;		
	this._eventSource = undefined;
	this._playlist = undefined;		
	
	this._volDebounceTimer = undefined;
	this._stateDebounceTimer = undefined;
	this._currentPlaylistItemUrl = '';
	this._userstop = false;

	this._relTime = new RelTimeHandler(document.getElementById(options["time-info"]));
	this._renderer = new LocalBrowserRenderer();
	
	this._containerElement = document.getElementById(options["player-panel"]);	
	
	this._titleElement = document.getElementById(options["transport-title"]);
	this._trackTitleElement = document.getElementById(options["track-title"]);
	
	this._volumeElement = document.getElementById(options["volume-slider"]);
	this._stateElement = document.getElementById(options["transport-state"]);
		
	this._playlistContainerElement = document.getElementById(options["playlist-container"]);
	
	this.selectDevice(this._renderer.deviceUdn, this._renderer.instanceId, 'L');
	
	document.getElementById(options["btn-play"]).onclick = (event) => this.play().catch(errorInfo => errorInfo); // Swallow already reported error;;
	document.getElementById(options["btn-pause"]).onclick = (event) => this.pause().catch(errorInfo => errorInfo); // Swallow already reported error;;
	document.getElementById(options["btn-stop"]).onclick = (event) => this.stop().catch(errorInfo => errorInfo); // Swallow already reported error;;
	document.getElementById(options["btn-next"]).onclick = (event) => this.next().catch(errorInfo => errorInfo); // Swallow already reported error;;
	document.getElementById(options["btn-prev"]).onclick = (event) => this.previous().catch(errorInfo => errorInfo); // Swallow already reported error;
	document.getElementById(options["btn-next-media"]).onclick = (event) => this.nextMedia().catch(text => console.log("error", text)); // Swallow already reported error
	document.getElementById(options["btn-prev-media"]).onclick = (event) => this.previousMedia().catch(text => text); // Swallow already reported error;	
	
	this._volumeElement.oninput = (event) => {
	   clearTimeout(this._volDebounceTimer);	
	   this._volDebounceTimer = setTimeout(() => {
	      this.setVolume(event.target.value);
	   }, 500); // adjust delay as needed
	};

	this._playlistContainerElement.addEventListener("dblclick", (event) => {
		const li = event.target.closest("li");
		if (li != null) {
			const span = event.target.closest('span.remove');
			if (span != null) {
				this._removePlaylistListItem(li).catch(already_reported => true);
			} else {
		   	   this._containerElement.dispatchEvent(
			   		new CustomEvent(MediaController.EVENT_NAME_PLAYLIST_DBLCLICK, {detail: { 
						itemindex: li.dataset.itemindex, 
						item: this._playlist[li.dataset.itemindex]
					}})
			   );		   					
			}			
		}
	});
	
	let draggedIndex = undefined;
	this._playlistContainerElement.addEventListener("dragstart", (event) => {
		event.dataTransfer.setData('text/plain', event.target.closest("li").dataset.itemindex);
		event.dataTransfer.effectAllowed = 'move';
	});

	this._playlistContainerElement.addEventListener("dragover", (event) => {
		const li = event.target.closest("li");
		if (li != null) {
			event.dataTransfer.dropEffect = 'move';			
		}
		event.preventDefault()
	});

	this._playlistContainerElement.addEventListener("drop", (event) => {
		const li = event.target.closest("li");
		if (li != null) {
			event.preventDefault();
			
			const from = parseInt(event.dataTransfer.getData('text/plain')); 
			const to = li.dataset.itemindex;
			
			this._movePlaylistItem(from, to);
		   
		}
	});
		
  }
  
  async selectDevice(udn, instanceId, rendererType) {
	if (rendererType === 'H') {
		this._renderer = new HeimklangRemoteRenderer();		
	} else if (rendererType === 'L') {
		this._renderer = new LocalBrowserRenderer(); 
	} else { // 'D'
		this._renderer = new DefaultRemoteRenderer();
	}		
	
    this._renderer.deviceUdn = udn;
	this._renderer.instanceId = instanceId;
	this._userstop = false; 
	
	if (this._eventSource) {
		this._eventSource.close();		
	}
	
	this._relTime.reset();
	
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
		'RelativeTimePosition': '00:00:00'
	}
	this._remotePropertyChanged();
	
	
	if (udn) {
		this._updateLocalPlaylistItems(await this._renderer.getInitialPlaylist()); 
		this._eventSource =  this._renderer.createRendererEventSubcription((event) => {
			
			if (event.type == 'LastChange') {
				
				const eventData = (() => {
				    try {
				        return JSON.parse(event.data);
				    } catch (je) {
				        console.error(je);
				        return undefined;
				    }
				})();
								
				if (eventData != undefined) {
					const nextState = eventData['TransportState'];
					const prevState = this._properties['TransportState'];
					
					// if we are stopped && were not stopped before
					if ( nextState == 'STOPPED' && prevState != 'STOPPED') {
						
						// if the user did not ask for stop
						if (this._userstop === false) {

							if (this._renderer.playlistType==DefaultRemoteRenderer.PLAYLIST_TYPE_LOCAL) {
								if (this._playlist.length > 0 && this.findCurrentPlaylistIndex() != (this._playlist.length - 1)) {								
									this.nextMedia().catch((errorInfo) => console.log("Unexpected fail of next()", errorInfo));
								}												
							}	
												
						} else {
							// reset
							this._userstop = false;
						}
						
						this._relTime.stop();					
					
					} else {
						
						if (prevState !== 'PAUSED_PLAYBACK' && nextState === 'PAUSED_PLAYBACK') {
							this._relTime.pause();
						} else if (prevState !== 'PLAYING' && nextState === 'PLAYING') {
						    this._relTime.start();
						} else if (prevState !== 'STOPPED' && nextState === 'STOPPED') {
							this._relTime.stop();
						}
						
						if (('AVTransportURI' in eventData && eventData['AVTransportURI'] != this._properties['AVTransportURI']) || ('CurrentTrackURI' in eventData && eventData['CurrentTrackURI'] != this._properties['CurrentTrackURI'])	|| ('CurrentTrack' in eventData && eventData['CurrentTrack'] != this._properties['CurrentTrack']) ) {
							this._relTime.restart();
						}
						
						if (eventData['RelativeTimePosition'] != undefined && eventData['RelativeTimePosition'] != 'NOT_IMPLEMENTED' && eventData['RelativeTimePosition'] != '') {
							this._relTime.setTimeStr(eventData['RelativeTimePosition']);
						} 

					}		
										
					Object.assign(this._properties, eventData);
					clearTimeout(this._stateDebounceTimer);	
					this._stateDebounceTimer = setTimeout(() => {
						this._remotePropertyChanged();			
					}, 500); // adjust delay as needed
					
				}				
				
			} else if (event.type == 'RelativeTimePosition') {
				this._properties[event.type] = event.data;
				this._relTime.setTimeStr(event.data);
			}
			
		});
	    this._eventSource.onerror = (err) => {	        
	        this._eventSource.close();
			this._triggerActionError("Disconnected from remote event source").catch((ignore) => 'ERROR already reported');
	    }			
	} else {
		this._updateLocalPlaylistItems(undefined);
	}
	
  }  
  
  _remotePropertyChanged() {	
	  this._stateElement.textContent = this._properties['TransportState'];
	  this._volumeElement.value = this._properties['Volume'];
	
	  const transportUri = this._properties['AVTransportURI'];
	
	  // Update tranport title display
	  let transportTitle = undefined;
	
	  if (transportUri != '' && this._properties['AVTransportURIMetaData'] != '' && this._properties['AVTransportURIMetaData'] != 'NOT_IMPLEMENTED') {
	  	transportTitle = tryParseTitleFromDidl(this._properties['AVTransportURIMetaData']);
	  }
	  if (!transportTitle) {
	  	transportTitle = transportUri;		
	  }
	  this._titleElement.textContent = transportTitle;
	
	  // Update track title display
	  let trackTitle = undefined;	
	
	  if (this._properties['CurrentTrackURI'] != transportUri && this._properties['CurrentTrackURI'] != '') {
	  	if (this._properties['CurrentTrackMetaData'] != '' && this._properties['CurrentTrackMetaData'] != 'NOT_IMPLEMENTED') {
	  		trackTitle = tryParseTitleFromDidl(this._properties['CurrentTrackMetaData']);
	  	}
	  	if (!trackTitle) {
	  		trackTitle = ' | ' + this._properties['CurrentTrackURI'];	
	  	} else {
	  		trackTitle = ' | ' + trackTitle;
	  	}
	  } else {
	  	trackTitle = '';
	  }
	
	  if (this._properties['NumberOfTracks'] != '' && (parseInt(this._properties['NumberOfTracks'])>1)) {
	  	trackTitle = trackTitle.concat(
	      	' | [',
	  		(this._properties['CurrentTrack'] != '' ?  this._properties['CurrentTrack'] : '?'),
	  		'/',
	  		this._properties['NumberOfTracks'],
	  		']'
	  	);		
	  }	
	
	  this._trackTitleElement.textContent = trackTitle;
	
	
	  // Mark current track in playlist, if possible		
	  if (this._playlist && this._playlist.length > 0) {
	  	
		let compareUri = this._properties['AVTransportURI'];
		
		if (this._renderer.playlistType==DefaultRemoteRenderer.PLAYLIST_TYPE_REMOTE) {
			if (this._properties['CurrentTrackURI'] != '') { // just to be sure
				compareUri = this._properties['CurrentTrackURI'];
			}
		}

		if (compareUri != this._currentPlaylistItemUrl) {
			this._currentPlaylistItemUrl = compareUri;		
		  	const playlistNodes = this._playlistContainerElement.children;
		  	for (let itemIndex = 0; itemIndex < playlistNodes.length; itemIndex++) {
		  		if (this._playlist[itemIndex].uri == this._currentPlaylistItemUrl ) {
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
	return this._renderer.play().catch(errInfo => this._triggerActionError('Action failed: ' + errInfo.summary));
  }

  async pause() {
	return this._renderer.pause().catch(errInfo => this._triggerActionError('Action failed: ' + errInfo.summary));
  }

  async stop() {
	return this._renderer.stop().catch(errInfo => this._triggerActionError('Action failed: ' + errInfo.summary));
  }

  async next() {
  	return this._renderer.next().catch(errInfo => this._triggerActionError('Action failed: ' + errInfo.summary));
  }

  async previous() {
	return this._renderer.previous().catch(errInfo => this._triggerActionError('Action failed: ' + errInfo.summary));  
  }

  async setVolume(volAsString) {
	return this._renderer.setVolume(volAsString).then((apiResponse) => {
		// prevents bouncing of the control until update arrives
		this._properties['Volume'] = volAsString;
		return apiResponse;
	}).catch(errInfo => this._triggerActionError('Action failed: ' + errInfo.summary));
  }

  async toggleMute() {	
	return this._renderer.setMute(this.properties['Mute'] == 'true' ? 'false' : 'true').catch(errInfo => this._triggerActionError('Action failed: ' + errInfo.summary));
  }  

  async setAVTransportItem(item) {
	return this._renderer.setAVTransportItem(item).catch(errInfo => this._triggerActionError('Action failed: ' + errInfo.summary));
  }
  
  addEventListener(type, listener) {
    this._containerElement.addEventListener(type, listener);
  }

  _triggerActionError(message) {
  	this._containerElement.dispatchEvent(
      new CustomEvent(MediaController.EVENT_NAME_ACTIONFAILED, {detail : message})
  	);
	return Promise.reject(message);
  }
  
  _updateLocalPlaylistItems(plData) {
	if (plData != undefined) {
				
		for (let i=0; i < plData.length; i++) {
			this._playlistContainerElement.appendChild(createPlaylistLi(plData[i], i));
		}
		 		
	} else {
		return this._emptyLocalPlaylist();
	}
  }
  
  async newPlaylist() {
	this._renderer.newPlaylist().then((apiResponse) => {
	  	this._emptyLocalPlaylist();			
	}).catch(errInfo => this._triggerActionError('Action failed: ' + errInfo.summary));
  }
  
  _emptyLocalPlaylist() {
	this._playlist = [];
	this._playlistContainerElement.innerHTML = '';
	this._currentPlaylistItemUrl = '';		
  }
  
  async addToPlaylist(item, replace) {	
		
	if (!replace) {
		// TODO: Allow double entries in playlist
		// The only reason that is not allowed at the moment is, that i don't want to deal
		// with identifying the current track in that case
		for (const entry of this._playlist) {
			if (entry.uri == item.uri) {
				return -1;
			}
		}	
	}
		
	try {
		await this._renderer.addToPlaylist(item, replace);		
		const index = this._playlist.push(item) - 1;
		this._playlistContainerElement.appendChild(createPlaylistLi(item, index));
		return index;		
	} catch(errInfo) {
		this._triggerActionError('Action failed: ' + errInfo.summary).catch(reportedError => undefined);
		return -2;			
	}			
	  
  }
  
  async _removePlaylistListItem(htmlLi) {
	// TODO: Allow removing active item
  	// The only reason that is not allowed at the moment is, that i don't want to deal
  	// with the implications on the remote renderer (in the future ;) )
	return new Promise((resolve, reject) => {
	  	if (!htmlLi.classList.contains('active')) {
			
			this._renderer.removeFromPlaylist(htmlLi.dataset.itemindex).then((apiResponse) => {
				this._playlistContainerElement.removeChild(htmlLi);
	  			resolve(this._playlist.splice(htmlLi.dataset.itemindex, 1)[0]);
			}).catch((errInfo) => {
				this._triggerActionError('Action failed: ' + errInfo.summary).catch(reject);
			});
			
		} else {
			this._triggerActionError('The currently active media can not be removed from the list').catch(reject);
		}		
	});
	
	
  }  
   
  async _movePlaylistItem(from,to) {
	  if (from !== to) {
	  	
		this._renderer.movePlaylistItem(from, to).then((apiResponse) => {
		
		  	// Move html node
		  	const movedNode = this._playlistContainerElement.children[from];
		
		  	if (from > to) {
		  		this._playlistContainerElement.children[to].before(movedNode);
		  	} else {
		  		this._playlistContainerElement.children[to].after(movedNode);
		  	} 
		
		  	// Update indices on html element
		  	const changeStart = Math.min(from, to);
		  	const changeEnd = Math.max(from, to);
		
		  	for (let i=changeStart;i<=changeEnd;i++) {
		  		this._playlistContainerElement.children[i].dataset.itemindex = i;
		  	}	
		  	
		  	// Move in internal playlist as well
		  	const movedItem = this._playlist.splice(from, 1)[0];
		  	this._playlist.splice(to, 0, movedItem);
			
			return true;					
		}).catch((errInfo) => {
			return this._triggerActionError('Action failed: ' + errInfo.summary).catch(ignore => false);
		});
	  }
  	
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
			if (this._renderer.playlistType==DefaultRemoteRenderer.PLAYLIST_TYPE_LOCAL) {
				if (this.isPlaying()) {
					return this.setAVTransportItem(this._playlist[nextItem]).then((apiResult) => {
						return this.play();	
					});				
				} else {
					return this.setAVTransportItem(this._playlist[nextItem]);
				}
			} else {
				return this.next();
			}
		}
	}
	
	return this._triggerActionError('No next media in playlist');
  }

  async previousMedia() {
	if (this._playlist.length > 0) {
		const currentItemIndex = this.findCurrentPlaylistIndex();
		let nextItem = currentItemIndex == -1 ? (this._playlist.length-1) : (currentItemIndex - 1);
		
		if (nextItem >= 0) {
			if (this._renderer.playlistType==DefaultRemoteRenderer.PLAYLIST_TYPE_LOCAL) {
				if (this.isPlaying()) {					
					return this.setAVTransportItem(this._playlist[nextItem]).then((apiResult) => {
						return this.play();	
					});				
				} else {
					return this.setAVTransportItem(this._playlist[nextItem]);
				}
			} else {
				return this.previous();
			}
		}
	}

	return this._triggerActionError('No previous media in playlist');
  }
  
  async playEntry(entryObject) {
	return this._renderer.playEntry(entryObject).catch((errInfo) => {
		this._triggerActionError('Action failed: ' + errInfo.summary).catch(AlreadyReportedError => undefined);
	});
  } 
  
  isPlaying() {
	return 	this._properties['TransportState'] == 'PLAYING';
  }  

}
  
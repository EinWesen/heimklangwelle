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

export class RemoteRenderer { 
  static EVENT_NAME_ACTIONFAILED = 'actionFailed';
  
  constructor(options) {
	this._deviceUdn = undefined;
	this._instanceId = undefined;
	this._properties = undefined;		
	this._eventSource = undefined;	
	
	this._volDebounceTimer = undefined;
	this._stateDebounceTimer = undefined;
	
	this._containerElement = document.getElementById(options["player-panel"]);	
	
	this._titleElement = document.getElementById(options["track-title"]);
	this._timeElement = document.getElementById(options["time-info"]);
	this._volumeElement = document.getElementById(options["volume-slider"]);
	this._stateElement = document.getElementById(options["transport-state"]);
		
	this.selectDevice(undefined, undefined);
	
	document.getElementById(options["btn-play"]).onclick = (event) => this.play();
	document.getElementById(options["btn-pause"]).onclick = (event) => this.pause();
	document.getElementById(options["btn-stop"]).onclick = (event) => this.stop();
	document.getElementById(options["btn-next"]).onclick = (event) => this.next();
	document.getElementById(options["btn-prev"]).onclick = (event) => this.previous();
	this._volumeElement.oninput = (event) => {
	   clearTimeout(this._volDebounceTimer);	
	   this._volDebounceTimer = setTimeout(() => {
	      this.setVolume(event.target.value);
	   }, 500); // adjust delay as needed
	};
	
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
		'CurrentTrack': '',
		'NumberOfTracks': '',
		'TransportState': 'STOPPED',
		'Mute': 'false',
		'Volume': '100',
		'RelativeTimePosition': ''
	}
	this._remotePropertyChanged();

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
	// TODO: Needs debouncing	
	this._timeElement.textContent = this._properties['RelativeTimePosition'];
	this._stateElement.textContent = this._properties['TransportState'];
	this._volumeElement.value = this._properties['Volume'];
		
	let title = undefined;
	if (this._properties['AVTransportURI'] != '' && this._properties['AVTransportURIMetaData'] != '') {
		title = tryParseTitleFromDidl(this._properties['AVTransportURIMetaData']);
	} 
	if (!title) {
		title = this._properties['AVTransportURI'];
	}
	this._titleElement.textContent = title;
	
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
		this._triggerActionError('No connected device');
		return Promise.reject('No connected device');
	}
  }  

}
  
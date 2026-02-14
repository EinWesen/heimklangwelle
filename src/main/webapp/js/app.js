import * as api from "./restupnp.js";
import { ContentServerBrowser } from "./contentbrowser.js";

const SERVER_SELECT_ID = 'select-server';
const RENDERER_SELECT_ID = 'select-renderer';
const REFRESH_DEVICE_BUTTON = 'btn-device-refresh';

const CONTENTBROWSER = new ContentServerBrowser('folder-list');

async function loadDevices() {
	let selRenderer = document.getElementById(RENDERER_SELECT_ID),
	    selServer = document.getElementById(SERVER_SELECT_ID),
		selectedRenderer = selRenderer.value,
		selectedServer = selServer.value;
	
		if (!selectedRenderer) selectedRenderer = '-';
		if (!selectedServer) selectedServer = '-';
		
		selRenderer.innerHTML = '<option value="-">Select MediaRenderer</option>';
		selServer.innerHTML = '<option value="-">Select MediaServer</option>'; 
    	
		api.listDevices().then((apiResult) => {
			apiResult.data.devices.forEach(device => {
                const o = document.createElement('option');                
                o.textContent = device.friendlyName;
				o.setAttribute('value', device.udn)                
				
				if (device.type == 'MediaRenderer') {					
					if (device.udn == selectedRenderer) {
						o.setAttribute('selected', 'selected');
					}
					selRenderer.appendChild(o);
				} else if (device.type == 'MediaServer') {
					if (device.udn == selectedServer) {
						o.setAttribute('selected', 'selected');
					}					
					selServer.appendChild(o);
				}				
            });
        })
        .catch(errInfo => alert('Error loading devices: ' + errInfo.summary));
}



async function init() {	
	document.getElementById(REFRESH_DEVICE_BUTTON).onclick = loadDevices;
	document.getElementById(SERVER_SELECT_ID).onchange = (event) => {
		CONTENTBROWSER.selectDevice(event.target.value == '-' ? undefined : event.target.value);
	};
	loadDevices();
}

init();
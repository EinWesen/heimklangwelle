import * as api from "./restupnp.js";

async function loadDevices() {
	let selRenderer = document.getElementById('select-renderer'),
	    selServer = document.getElementById('select-server'),
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


import { loadRoot, renderBrowser } from "./browser.js";

async function init() {
	document.getElementById('btn-device-refresh').onclick = loadDevices;
	loadDevices();
	loadRoot();
}

init();
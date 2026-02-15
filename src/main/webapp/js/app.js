import * as api from "./restupnp.js";
import { ContentServerBrowser } from "./contentbrowser.js";
import { RemoteRenderer } from "./renderer.js";

const SERVER_SELECT_ID = 'select-server';
const RENDERER_SELECT_ID = 'select-renderer';
const REFRESH_DEVICE_BUTTON = 'btn-device-refresh';

const CONTENTBROWSER = new ContentServerBrowser('folder-list');
const MEDIARENDERER = new RemoteRenderer({
	'player-panel' : 'player-panel',
	'btn-play' : 'btn-play',
	'btn-pause' : 'btn-pause',
	'btn-stop' : 'btn-stop',
	'btn-next' : 'btn-next',
	'btn-prev' : 'btn-prev',
	'volume-slider' : 'volume-slider',
	'transport-state' : 'transport-state',
	'time-info' : 'time-info',
	'track-title' : 'track-title',
});

function showToast(message, duration = 3000) {
  const toast = document.createElement('div');
  toast.className = 'toast';
  toast.textContent = message;

  document.body.appendChild(toast);

  // allow DOM paint before animation
  requestAnimationFrame(() => {
    toast.classList.add('show');
  });

  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 250);
  }, duration);
}

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
	MEDIARENDERER.addEventListener(RemoteRenderer.EVENT_NAME_ACTIONFAILED, (event) => {
		showToast(event.detail);
	});
	
	CONTENTBROWSER.addEventListener(ContentServerBrowser.EVENT_NAME_DBLCLICKITEM, (event) => {
		MEDIARENDERER.setAVTransportItem(event.detail).then((apiResult) => {
			MEDIARENDERER.play();
		}).catch(() => {
			// Error is already reporte dinternall
			return;
		});
	});	
	
	document.getElementById(SERVER_SELECT_ID).onchange = (event) => {
		CONTENTBROWSER.selectDevice(event.target.value == '-' ? undefined : event.target.value);
	};
	
	document.getElementById(RENDERER_SELECT_ID).onchange = (event) => {
		if (event.target.value == '-') {
			MEDIARENDERER.selectDevice(undefined, undefined);			
		} else {			
			// TODO: Where to get instanceId ? 
			MEDIARENDERER.selectDevice(event.target.value, "0");
		}
	};	
	
	document.getElementById(REFRESH_DEVICE_BUTTON).onclick = loadDevices;

	loadDevices();
}

init();


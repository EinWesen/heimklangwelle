import * as api from "./restupnp.js";
import { ContentServerBrowser } from "./contentbrowser.js";
import { MediaController } from "./mediacontroller.js";

const SERVER_SELECT_ID = 'select-server';
const RENDERER_SELECT_ID = 'select-renderer';
const REFRESH_DEVICE_BUTTON = 'btn-device-refresh';
const NEW_PL_BUTTON = 'btn-pl-clear';
const ADD_URI_TO_PL_BUTTON = 'btn-pl-add';

const CONTENTBROWSER = new ContentServerBrowser('folder-list');
const MEDIACONTROLLER = new MediaController({
	'player-panel' : 'player-panel',
	'btn-play' : 'btn-play',
	'btn-pause' : 'btn-pause',
	'btn-stop' : 'btn-stop',
	'btn-next' : 'btn-next',
	'btn-prev' : 'btn-prev',
	'volume-slider' : 'volume-slider',
	'transport-state' : 'transport-state',
	'time-info' : 'time-info',
	'transport-title' : 'transport-title',
	'track-title' : 'track-title',
	'playlist-container': 'playlist-ul',
	'btn-next-media' : 'btn-next-media',
	'btn-prev-media' : 'btn-prev-media',
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
					if (device.friendlyName.startsWith('HeimklangWelle')) {
						o.dataset.rtype = "H";
					} else {
						o.dataset.rtype = "D";
					}
					
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
        }).catch(errInfo => alert('Error loading devices: ' + errInfo.summary));
}


async function init() {	
	MEDIACONTROLLER.addEventListener(MediaController.EVENT_NAME_ACTIONFAILED, (event) => {
		showToast(event.detail);
	});
	
	MEDIACONTROLLER.addEventListener(MediaController.EVENT_NAME_PLAYLIST_DBLCLICK, (event) => {
		MEDIACONTROLLER.playEntry(event.detail).catch((errorInfo) => {
			// Error is already reporte dinternall
			console.error(errorInfo);
			return;
		});
	});
	
	CONTENTBROWSER.addEventListener(ContentServerBrowser.EVENT_NAME_DBLCLICKITEM, async (event) => {
		const playlistindex = (await MEDIACONTROLLER.addToPlaylist(event.detail.item, event.detail.play));
		if (playlistindex === 0 && !MEDIACONTROLLER.isPlaying()) {
			MEDIACONTROLLER.playEntry({itemindex: playlistindex, item: event.detail.item}).catch((errorInfo) => {
				// Error is already reported internally
				console.error(errorInfo);
				return;
			});
		} else if (playlistindex === -1) {
			showToast("Item is already in the playlist");
		} 
	});	
			
	document.getElementById(SERVER_SELECT_ID).onchange = (event) => {
		CONTENTBROWSER.selectDevice(event.target.value == '-' ? undefined : event.target.value);
	};
	
	document.getElementById(RENDERER_SELECT_ID).onchange = (event) => {
		if (event.target.value == '-') {
			MEDIACONTROLLER.selectDevice(undefined, undefined, "0");			
		} else {			
			// TODO: Where to get instanceId ? 
			MEDIACONTROLLER.selectDevice(event.target.value, "0", event.target.options[event.target.selectedIndex].dataset.rtype);
		}
	};	
	
	document.getElementById(ADD_URI_TO_PL_BUTTON).onclick = (event) => {
		const uri = prompt("Please enter uri", "").trim();
		if (uri != '') { 
			MEDIACONTROLLER.addToPlaylist({
				id: "-1_" + performance.now(),
				isContainer: false,
				mimeType: "audio",
				parentId: "-1",
				title: uri,
				uri: uri,
				uriMetaData: ""
			}, false);
		}		
	};
	
	document.getElementById(NEW_PL_BUTTON).onclick = (event) => {MEDIACONTROLLER.newPlaylist();};
	document.getElementById(REFRESH_DEVICE_BUTTON).onclick = loadDevices;

	loadDevices();
}

init();
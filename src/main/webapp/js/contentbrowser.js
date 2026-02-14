import * as api from "./restupnp.js";

function createBrowserLi(className, iconChar, title) {
	const li = document.createElement("li");
	li.classList.add(className);
	li.appendChild(document.createElement("span")).textContent = iconChar;
	li.appendChild(document.createElement("span")).textContent = title;	
	return li;
}

function renderBrowser(browserInstance) {
    const list = browserInstance._containerElement;
    list.innerHTML = "";

    if (browserInstance._pathStack.length > 1) {
        list.appendChild(createBrowserLi('back', '↩', '..')).onclick = (event) => {browserInstance.navigateUp()};
    }

    browserInstance._items.forEach(item => {
        let li = undefined;
					
        if (item.isContainer) {
			li = createBrowserLi('folder', '📁', item.title)            
            li.onclick = () => browserInstance.browse(item);
        } else {
			let fileicon = '📄';
			
			// Add type-specific class
			if (item.mimeType?.startsWith("audio/")) {
			    fileicon = '🎵';
			} else if (item.mimeType?.startsWith("video/")) {
			    fileicon = '🎬';
			} 
			
			li = createBrowserLi('file', fileicon, item.title)			
            li.ondblclick = () => {
                document.dispatchEvent(
                    new CustomEvent("addToPlaylist", { detail: item })
                );
            };
        }
        
        list.appendChild(li);
    });
}

export class ContentServerBrowser {
  constructor(htmlListId) {
    this._containerElement = document.getElementById(htmlListId);	
	this._deviceUdn = undefined;
	this._pathStack = [];
	this._items = []
  }
  
  async selectDevice(udn) {
	this._deviceUdn = udn;
	this._pathStack = [];
	this._items = []
	
	if (udn != undefined) {
		this.browse({id: 0});
	} else {
		this._containerElement.innerHTML = "";
	}
  }

  async browse(item) {
  	const apiResponse = await api.browseContentDirectory(this._deviceUdn, item.id);
  	this._pathStack = [...this._pathStack, item]
	this._items = apiResponse.data.children;	
  	renderBrowser(this);
  }
  
  async navigateUp() {
      const newStack = [...this._pathStack];
      newStack.pop();
  	
      const parentId = newStack.length === 1
          ? "0"
          : newStack[newStack.length - 1].id;

      const apiResponse = await api.browseContentDirectory(this._deviceUdn, parentId);
	  this._pathStack = newStack;
	  this._items = apiResponse.data.children;	
  	  renderBrowser(this);
  }  
  
}


import * as api from "./restupnp.js";

function createBrowserLi(className, iconChar, title, itemtype) {
	const li = document.createElement("li");
	li.classList.add(className);
	li.appendChild(document.createElement("span")).textContent = iconChar;
	li.appendChild(document.createElement("span")).textContent = title;	
	li.dataset.itemtype = itemtype; 			            
	return li;
}

function renderBrowser(browserInstance) {
    const list = browserInstance._containerElement;
    list.innerHTML = "";

    if (browserInstance._pathStack.length > 1) {
        list.appendChild(createBrowserLi('back', '↩', '..', 'back'));
    }

    browserInstance._items.forEach((item, index) => {
        let li = undefined;
					
        if (item.isContainer) {
			li = createBrowserLi('folder', '📁', item.title, 'container')            
        } else {
			let fileicon = '📄';
			
			// Add type-specific class
			if (item.mimeType?.startsWith("audio")) {
			    fileicon = '🎵';
			} else if (item.mimeType?.startsWith("video")) {
			    fileicon = '🎬';
			} 
			li = createBrowserLi('file', fileicon, item.title, 'item')			
        }
        
		li.dataset.itemindex = index;
        list.appendChild(li);
    });
}

export class ContentServerBrowser {
  static EVENT_NAME_DBLCLICKITEM = 'dblclickItem';
  constructor(htmlListId) {
	this._deviceUdn = undefined;
	this._pathStack = [];
	this._items = []
    this._containerElement = document.getElementById(htmlListId);	
	
	this._containerElement.addEventListener("dblclick", (event) => {
	  const li = event.target.closest("li");
	  const item = this._items[li.dataset.itemindex];
	  switch(li.dataset.itemtype) {
	  	case 'back':
			this.navigateUp();
			return true;
		case 'container':
	     	this.browse(item);
			return true;
		case 'item':
	     	this._triggerDblClick(item);
			return true;
		default:
			alert("Unknown itemtype" + li.dataset.itemtype);
			return false;		
	  }
	});
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
  
  addEventListener(type, listener) {
	this._containerElement.addEventListener(type, listener);
  }
  
  _triggerDblClick(item) {
	this._containerElement.dispatchEvent(
	    new CustomEvent(ContentServerBrowser.EVENT_NAME_DBLCLICKITEM, { detail: item })
	);	
  } 
  
}


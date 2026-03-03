/*
   .then(({ response, data }) => ...
   .catch(({ isError, error, response, data }) => ... 
*/
export function apiFetch(url, options = {}) {
  return new Promise((resolve, reject) => {
    fetch(url, options)
      .then(async response => {
        const contentType = response.headers.get("content-type") || "";
        let data;

        try {
          if (contentType.includes("application/json")) {
            data = await response.json();
          } else {
            data = await response.text();
          }
        } catch (error) {
          return reject({
			summary: error.name + ':' + error.message,
            isError: true,
            error,
            response,
            data
          });
        }

        if (response.ok) {
          resolve({ response, data });
        } else {
          reject({
			summary: response.status + ' ' + response.statusText,
            isError: false,
            error: response.status,
            response,
            data
          });
        }
      })
      .catch(nativeError => {
        reject({
		  summary: nativeError.name + ':' + nativeError.message,
          isError: true,
          error: nativeError,
          response: undefined,
          data: undefined
        });
      });
  });
}

let BASE_REST_URL = 'rest';
export function setBaseUrl(url) {
	BASE_REST_URL = url;
}

export async function listDevices() {
	return apiFetch(`${BASE_REST_URL}/devices/`);
}

export async function browseContentDirectory(udn, objectId = "0") {
	return apiFetch(`${BASE_REST_URL}/contentdirectory/${udn}/browse?ObjectID=${encodeURIComponent(objectId)}`);
}

/*
{
   "serviceId": "...",
   "action": "...",
   "inputArguments": {
      "...": "...string reprenstation...",
      "...": "...string reprenstation...",
      ...
   }
}
*/
export async function callServiceAction(udn, callOptions) {

	return new Promise((resolve, reject) => {
		
		apiFetch(`${BASE_REST_URL}/devices/${udn}/callAction`, {
		  method: "POST",
		  body: JSON.stringify(callOptions),
		  headers: {
		    "Content-type": "application/json; charset=UTF-8"
		  }
		}).then(apiResponse => {
			// 201 CREATED = OK
			// 202 ACCEPTED = VALID COMMAND FAILED
			if (apiResponse.response.status == 201) {
				resolve(apiResponse);
			} else {
				reject({
					summary: apiResponse.data.message,
				    isError: false,
				    error: apiResponse.data.statusCode,
				    response: apiResponse.response,
				    data: apiResponse.data
				});			
			}
		}).catch(errorInfo => reject(errorInfo));
	
	});
		
}

export function createRendererEventSubcription(udn, instanceId, eventHandler) {
    const es = new EventSource(`${BASE_REST_URL}/renderer/${udn}/subscribe`);
    es.addEventListener('LastChange', eventHandler);
	es.addEventListener('RelativeTimePosition', eventHandler);
    return es; // Return the EventSource in case the caller wants to close it later
}


// Starting from here, these are functions vendor-specific for HeimklangWelle 
export function fetchCurrentPlaylist(udn, instanceId) {
	return apiFetch(`${BASE_REST_URL}/renderer/${udn}/playlist/?InstanceId=${instanceId}`);
}
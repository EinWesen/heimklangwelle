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

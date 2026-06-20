let map;
const markers = {};

//Class definitions for TaskMapRequest and TaskMapResponse
//IMPORTANT: MUST match fields and enums defined in Kotlin source code
class TaskMapRequest{
    constructor(type, contents){
        this.type = type;
        this.contents = contents;
    }
    static GET_NEARBY_TASKS = "GET_NEARBY_TASKS";
    static GET_CURRENT_LOCATION = "GET_CURRENT_LOCATION";
    static GET_TASK_DETAILS = "GET_TASK_DETAILS";
}
class TaskMapResponse {
    constructor(srcData){
        if(! Object.hasOwn(srcData,"type")){
            console.log("ERROR: Invalid TaskMapResponse data has no response type.")
        }
        if(! Object.hasOwn(srcData,"contents"){
            console.log("ERROR: Invalid TaskMapResponse data has no contents field.")
        }
        this.type = srcData.type;
        this.contents = srcData.contents;
    }
    static NEARBY_TASK_LIST = "NEARBY_TASK_LIST";
    static CURRENT_LOCATION = "CURRENT_LOCATION";
    static TASK_DETAILS = "TASK_DETAILS";
}

const mapListenerExists = typeof appWebMsgListener !== 'undefined'
if(mapListenerExists){
    appWebMsgListener.onmessage = function(event){
        try{
            const response = new TaskMapResponse(JSON.parse(event.data));
            const type = response.type;
            const contents = response.contents;

            console.log("Map Event Response Type: " + type)

            if(type === TaskMapResponse.NEARBY_TASK_LIST){
                var count = 0;
                for (const [id, task] of Object.entries(contents)) {
                    addJobMarker(id,task.latitude,task.longitude,task.title,task.paymentAmount);
                    count++;
                }
                //console.log("Added " + count + " task markers to map.");
            }

            else if(type === TaskMapResponse.CURRENT_LOCATION){
                //console.log("Current Location: " + JSON.stringify(contents));
                centerOnLocation(contents.latitude,contents.longitude);
            }

            else if(type === TaskMapResponse.TASK_DETAILS){
                //Do nothing for now, but can add other UI logic when task details have loaded
                //Note: The Android app will respond to this request with a Task object
            }
        }
        catch(ex){
            console.log(ex);
        }
    }
}
else {
    console.log("ERROR: Listener for Map Events not registered!");
}

function initMap() {
    // Center map on Kuala Lumpur
    map = L.map('leaflet-map', { zoomControl: false }).setView([3.1390, 101.6869], 12);

    // Dark tiles matching the theme
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; OpenStreetMap & CartoDB',
        maxZoom: 20
    }).addTo(map);

    // Request nearby tasks and current location from Android after map has initialized
    if(mapListenerExists){
        appWebMsgListener.postMessage(JSON.stringify(new TaskMapRequest(
            TaskMapRequest.GET_NEARBY_TASKS,
            null
        )));
        appWebMsgListener.postMessage(JSON.stringify(new TaskMapRequest(
            TaskMapRequest.GET_CURRENT_LOCATION,
            null
        )));
    }
}

// Custom Neon DivIcon
const neonIcon = L.divIcon({
    className: 'custom-div-icon',
    html: `<div style="background-color: var(--primary-neon); width: 22px; height: 22px; border-radius: 50%; border: 3px solid var(--bg-dark-forest); box-shadow: 0 0 12px var(--primary-neon);"></div>`,
    iconSize: [22, 22],
    iconAnchor: [11, 11]
});

// Function called from Android to add markers dynamically
function addJobMarker(id, lat, lng, title, pay) {
    if (markers[id]) {
        map.removeLayer(markers[id]);
    }

    const popupContent = `
        <div style="font-family: sans-serif;">
            <h4 style="margin: 0 0 4px 0; color: var(--text-primary);">${title}</h4>
            <p style="margin: 0 0 8px 0; color: var(--primary-neon); font-weight: bold;">RM ${pay}</p>
            <a href="javascript:void(0);" onclick="onMarkerClick('${id}')" class="popup-btn">Lihat Butiran</a>
        </div>
    `;

    const marker = L.marker([lat, lng], { icon: neonIcon })
        .addTo(map)
        .bindPopup(popupContent);

    markers[id] = marker;
}

// Send event back to Android when user clicks "Lihat Butiran"
function onMarkerClick(jobId) {
    if(mapListenerExists) appWebMsgListener.postMessage(JSON.stringify(new TaskMapRequest(
        TaskMapRequest.GET_TASK_DETAILS,
        jobId
    )));
}

// Center on user coordinates
function centerOnLocation(lat, lng) {
    map.setView([lat, lng], 14);
}

window.onload = initMap;
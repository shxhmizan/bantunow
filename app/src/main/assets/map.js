let map;
const markers = {};
let userCircle;
let userMarker;
let userCoords = { lat: 4.1865, lng: 101.2620 };

class TaskMapRequest {
    constructor(type, contents) {
        this.type = type;
        this.contents = contents;
    }
    static GET_NEARBY_TASKS = "GET_NEARBY_TASKS";
    static GET_CURRENT_LOCATION = "GET_CURRENT_LOCATION";
    static GET_TASK_DETAILS = "GET_TASK_DETAILS";
}

class TaskMapResponse {
    constructor(srcData) {
        this.type = srcData.type;
        this.contents = srcData.contents;
    }
    static NEARBY_TASK_LIST = "NEARBY_TASK_LIST";
    static CURRENT_LOCATION = "CURRENT_LOCATION";
    static TASK_DETAILS = "TASK_DETAILS";
}

const mapListenerExists = typeof appWebMsgListener !== 'undefined';

if (mapListenerExists) {
    appWebMsgListener.onmessage = function(event) {
        try {
            const response = new TaskMapResponse(JSON.parse(event.data));
            const type = response.type;
            const contents = response.contents;

            if (type === TaskMapResponse.NEARBY_TASK_LIST) {
                clearMarkers();
                let nearbyCount = 0;
                let totalIncome = 0;
                const categories = {};

                for (const [id, task] of Object.entries(contents)) {
                    // Safety check for coordinates
                    if (task.latitude == null || task.longitude == null) {
                        console.warn("Skipping task with missing coordinates: " + id);
                        continue;
                    }

                    const dist = calculateDistance(userCoords.lat, userCoords.lng, task.latitude, task.longitude);

                    if (dist <= 10) {
                        nearbyCount++;
                        totalIncome += ((task.paymentAmount || 0) / 100);
                        const cat = task.category || "General";
                        categories[cat] = (categories[cat] || 0) + 1;
                    }

                    addJobMarker(id, task.latitude, task.longitude, task.title, task.paymentAmount, task.desc, dist);
                }

                const avgIncome = nearbyCount > 0 ? (totalIncome / nearbyCount).toFixed(0) : 0;
                const popular = Object.keys(categories).reduce((a, b) => categories[a] > categories[b] ? a : b, "-");

                console.log("INSIGHTS:" + JSON.stringify({count: nearbyCount, avg: avgIncome, pop: popular}));

            } else if (type === TaskMapResponse.CURRENT_LOCATION) {
                userCoords.lat = contents.latitude;
                userCoords.lng = contents.longitude;
                centerOnLocation(contents.latitude, contents.longitude);
            }
        } catch (ex) {
            console.error("Error in onmessage:", ex);
        }
    };
}

function clearMarkers() {
    for (const id in markers) {
        map.removeLayer(markers[id]);
        delete markers[id];
    }
}

function initMap() {
    map = L.map('leaflet-map', {
        zoomControl: true,
        scrollWheelZoom: true
    }).setView([userCoords.lat, userCoords.lng], 15);

    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; OpenStreetMap & CartoDB',
        maxZoom: 20
    }).addTo(map);

    centerOnLocation(userCoords.lat, userCoords.lng);

    if (mapListenerExists) {
        appWebMsgListener.postMessage(JSON.stringify(new TaskMapRequest(TaskMapRequest.GET_NEARBY_TASKS, null)));
        appWebMsgListener.postMessage(JSON.stringify(new TaskMapRequest(TaskMapRequest.GET_CURRENT_LOCATION, null)));
    }
}

const neonIcon = L.divIcon({
    className: 'custom-div-icon',
    html: `<div style="background-color: #00FF66; width: 30px; height: 30px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 10px rgba(0,255,102,0.5); display: flex; align-items: center; justify-content: center;">
             <img src="https://img.icons8.com/material-rounded/24/ffffff/briefcase.png" style="width: 16px; height: 16px;"/>
           </div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 15]
});

function addJobMarker(id, lat, lng, title, payCents, desc, dist) {
    const payFormatted = ((payCents || 0) / 100).toFixed(0);
    const popupContent = `
        <div class="popup-container" style="min-width: 250px; font-family: sans-serif; padding: 10px;">
            <div style="display: flex; gap: 8px; margin-bottom: 12px;">
                <span style="background: #E8F5E9; color: #4CAF50; padding: 4px 12px; border-radius: 8px; font-size: 10px; font-weight: bold; border: 1px solid #C8E6C9;">OPEN</span>
            </div>
            <div style="font-size: 18px; font-weight: 800; color: #212121; margin-bottom: 4px;">${title}</div>
            <div style="font-size: 14px; color: #757575; margin-bottom: 20px;">${desc || 'No description provided'}</div>

            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                <div style="font-size: 24px; font-weight: 900; color: #FF7043;">RM ${payFormatted}</div>
                <div style="background: #F5F5F5; padding: 6px 12px; border-radius: 20px; font-size: 12px; color: #757575; display: flex; align-items: center; gap: 4px;">
                   <span>📍</span> ${dist.toFixed(1)} km
                </div>
            </div>

            <a href="javascript:void(0);" onclick="onMarkerClick('${id}')"
               style="display: block; background: #2E7D32; color: white; text-align: center; padding: 14px; border-radius: 12px; font-weight: bold; text-decoration: none; font-size: 15px; box-shadow: 0 4px 12px rgba(46,125,50,0.2);">
               REVIEW AND ACCEPT
            </a>
        </div>
    `;

    const marker = L.marker([lat, lng], { icon: neonIcon })
        .addTo(map)
        .bindPopup(popupContent, {
            maxWidth: 300,
            className: 'custom-popup'
        });

    markers[id] = marker;
}

function onMarkerClick(jobId) {
    if (mapListenerExists) {
        appWebMsgListener.postMessage(JSON.stringify(new TaskMapRequest(TaskMapRequest.GET_TASK_DETAILS, jobId)));
    }
}

function centerOnLocation(lat, lng) {
    map.setView([lat, lng], 14);

    if (userCircle) map.removeLayer(userCircle);
    if (userMarker) map.removeLayer(userMarker);

    userCircle = L.circle([lat, lng], {
        color: '#4CAF50',
        fillColor: '#4CAF50',
        fillOpacity: 0.1,
        radius: 2000,
        weight: 1
    }).addTo(map);

    userMarker = L.circleMarker([lat, lng], {
        radius: 7,
        fillColor: "#2196F3",
        color: "#fff",
        weight: 2,
        opacity: 1,
        fillOpacity: 0.9
    }).addTo(map);
}

function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

window.onload = initMap;
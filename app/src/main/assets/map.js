let map;
const markers = {};
let userMarker;
let userCoords = { lat: 4.5921, lng: 101.0901 }; // Default to Perak/Ipoh
let currentFilters = { radius: 10, category: "All" }; // Default to 10km radius

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
                let nearbyCount = 0; // Active within 10km
                let totalActiveCount = 0; // All active tasks
                let totalIncome = 0;
                const categories = {};
                const taskTitles = [];

                const occupiedPositions = {};

                for (const [id, task] of Object.entries(contents)) {
                    if (task.latitude == null || task.longitude == null) continue;

                    const distToUser = calculateDistance(userCoords.lat, userCoords.lng, task.latitude, task.longitude);

                    // Count active work
                    const isActive = task.status !== "completed";

                    if (isActive) {
                        totalActiveCount++;

                        // Count for the "Active 10km" label
                        if (distToUser <= 10) {
                            nearbyCount++;
                            totalIncome += ((task.paymentAmount || 0) / 100);
                            const cat = task.category || "General";
                            categories[cat] = (categories[cat] || 0) + 1;
                            taskTitles.push(task.title);
                        }
                    }

                    // Only show markers for tasks that are actually "open" or "in_progress"
                    // to avoid clutter, but "show all" can mean all active ones.
                    if (task.status === "completed") continue;

                    let finalLat = task.latitude;
                    let finalLng = task.longitude;

                    const posKey = finalLat.toFixed(5) + "," + finalLng.toFixed(5);
                    if (occupiedPositions[posKey]) {
                        const angle = Math.random() * Math.PI * 2;
                        const radius = 0.00015;
                        finalLat += Math.cos(angle) * radius;
                        finalLng += Math.sin(angle) * radius;
                    }
                    occupiedPositions[posKey] = true;

                    const dist = calculateDistance(userCoords.lat, userCoords.lng, finalLat, finalLng);
                    const cat = task.category || "General";

                    // Apply filters
                    const matchesRadius = dist <= currentFilters.radius;
                    const matchesCategory = currentFilters.category === "All" || cat === currentFilters.category;

                    if (matchesRadius && matchesCategory) {
                        addJobMarker(id, finalLat, finalLng, task.title, task.paymentAmount, task.desc, dist, task.status);
                    }
                }

                const avgIncome = nearbyCount > 0 ? (totalIncome / nearbyCount).toFixed(0) : "0";
                const popular = Object.keys(categories).length > 0 ? Object.keys(categories).reduce((a, b) => categories[a] > categories[b] ? a : b) : "-";

                console.log("INSIGHTS:" + JSON.stringify({
                    count: totalActiveCount,
                    nearbyCount: nearbyCount,
                    avg: avgIncome,
                    pop: popular,
                    rawTasks: taskTitles.join(", ")
                }));

            } else if (type === TaskMapResponse.CURRENT_LOCATION) {
                userCoords.lat = contents.latitude;
                userCoords.lng = contents.longitude;
                centerOnLocation(contents.latitude, contents.longitude);

                // Refresh tasks to ensure "Active 10km" count uses the new coordinates
                if (mapListenerExists) {
                    appWebMsgListener.postMessage(JSON.stringify(new TaskMapRequest(TaskMapRequest.GET_NEARBY_TASKS, null)));
                }
            }
        } catch (ex) {
            console.error("Error in onmessage:", ex);
        }
    };
}

function setFilters(radius, category) {
    currentFilters.radius = parseFloat(radius);
    currentFilters.category = category;
    if (mapListenerExists) {
        appWebMsgListener.postMessage(JSON.stringify(new TaskMapRequest(TaskMapRequest.GET_NEARBY_TASKS, null)));
    }
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
    }).setView([userCoords.lat, userCoords.lng], 14);

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

function requestLocationRefresh() {
    if (mapListenerExists) {
        appWebMsgListener.postMessage(JSON.stringify(new TaskMapRequest(TaskMapRequest.GET_CURRENT_LOCATION, null)));
    }
}

const neonIcon = L.divIcon({
    className: 'custom-div-icon',
    html: `<div class="marker-pulse"></div>
           <div class="marker-pin">
             <img src="https://img.icons8.com/material-rounded/24/ffffff/briefcase.png"/>
           </div>`,
    iconSize: [34, 34],
    iconAnchor: [17, 34]
});

function addJobMarker(id, lat, lng, title, payCents, desc, dist, status) {
    const payFormatted = ((payCents || 0) / 100).toFixed(0);
    const statusText = (status || "open").toUpperCase().replace("_", " ");
    const statusColor = status === "open" ? "#EAEAEA" : "#C4E4E1";

    const popupContent = `
        <div class="popup-container" style="min-width: 250px; font-family: 'Google Sans', sans-serif; padding: 10px;">
            <div style="display: flex; gap: 8px; margin-bottom: 12px;">
                <span style="background: ${statusColor}; color: #1C1C1C; padding: 4px 12px; border-radius: 8px; font-size: 10px; font-weight: bold; border: 1px solid #EAEAEA;">${statusText}</span>
            </div>
            <div style="font-size: 20px; font-weight: 800; color: #1C1C1C; margin-bottom: 4px; font-family: 'Playfair Display', serif;">${title}</div>
            <div style="font-size: 14px; color: #757575; margin-bottom: 20px;">${desc || 'No description provided'}</div>

            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                <div style="font-size: 24px; font-weight: 900; color: #5d3d3a; font-family: 'Playfair Display', serif;">RM ${payFormatted}</div>
                <div style="background: #F5F2EB; padding: 6px 12px; border-radius: 20px; font-size: 12px; color: #1C1C1C; display: flex; align-items: center; gap: 4px;">
                   <span>📍</span> ${dist.toFixed(1)} km
                </div>
            </div>

            <a href="javascript:void(0);" onclick="onMarkerClick('${id}')"
               style="display: block; background: #C4E4E1; color: #1C1C1C; text-align: center; padding: 14px; border-radius: 12px; font-weight: bold; text-decoration: none; font-size: 15px; box-shadow: 0 4px 12px rgba(196,228,225,0.2);">
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

    if (userMarker) map.removeLayer(userMarker);

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
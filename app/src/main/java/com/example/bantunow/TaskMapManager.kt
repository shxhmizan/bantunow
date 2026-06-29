package com.example.bantunow

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.annotation.RequiresPermission
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import com.example.bantunow.data.TaskMapRequest
import com.example.bantunow.data.TaskMapResponse
import com.example.bantunow.data.model.Task
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * This class manages the application Task Map that displays tasks in an interactive map
 * It now interacts with Cloud Firestore with real-time updates
 */
abstract class TaskMapManager(val firestore: FirebaseFirestore, val locationClient: FusedLocationProviderClient) : WebViewCompat.WebMessageListener {
    
    private var tasksListener: ListenerRegistration? = null

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    @SuppressLint("RequiresFeature")
    override fun onPostMessage(
        view: WebView,
        msg: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy
    ) {
        val data = msg.data ?: return
        try {
            val request = Json.decodeFromString<TaskMapRequest>(data);
            Log.i("TaskMapManager", "Request Type : ${request.type}")
            when (request.type) {
                TaskMapRequest.Type.GET_NEARBY_TASKS -> {
                    // Remove old listener if any
                    tasksListener?.remove()
                    
                    // Setup real-time listener for ALL tasks
                    tasksListener = firestore.collection("tasks")
                        .addSnapshotListener { snapshots, e ->
                            if (e != null) {
                                Log.w("TaskMapManager", "Listen failed.", e)
                                return@addSnapshotListener
                            }

                            if (snapshots != null) {
                                val tasksMap = mutableMapOf<String, Task>()
                                for (document in snapshots) {
                                    try {
                                        val task = document.toObject(Task::class.java)
                                        if (task != null) {
                                            task.taskId = document.id
                                            tasksMap[document.id] = task
                                        }
                                    } catch (err: Exception) {
                                        Log.e("TaskMapManager", "Error parsing task document ${document.id}", err)
                                    }
                                }
                                
                                val taskJson = Json.encodeToString(
                                    TaskMapResponse(
                                        TaskMapResponse.Type.NEARBY_TASK_LIST,
                                        tasksMap,
                                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    )
                                )
                                Log.d("TaskMapManager", "Posting ${tasksMap.size} tasks to JS")
                                try {
                                    replyProxy.postMessage(taskJson)
                                } catch (e: Exception) {
                                    Log.e("TaskMapManager", "Error posting message to JS", e)
                                }
                            }
                        }
                }

                TaskMapRequest.Type.GET_CURRENT_LOCATION -> {
                    try {
                        locationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful && task.result != null) {
                                    val location = task.result
                                    replyProxy.postMessage(
                                        Json.encodeToString(
                                            TaskMapResponse(
                                                TaskMapResponse.Type.CURRENT_LOCATION,
                                                TaskMapResponse.Location(
                                                    latitude = location.latitude,
                                                    longitude = location.longitude
                                                ),
                                                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                            )
                                        )
                                    )
                                }
                            }
                    } catch (e: SecurityException) {
                        Log.e("TaskMapManager", "Permission denied for getCurrentLocation", e)
                    }
                }

                TaskMapRequest.Type.GET_TASK_DETAILS -> {
                    val taskID = request.contents ?: return
                    Log.d("TaskMapManager", "Fetching task details for ID: $taskID")
                    firestore.collection("tasks").document(taskID).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                try {
                                    val task = document.toObject(Task::class.java)
                                    if (task != null) {
                                        task.taskId = document.id
                                        onTaskSelected(task)
                                    }
                                } catch (e: Exception) {
                                    Log.e("TaskMapManager", "Error parsing task for ID: $taskID", e)
                                }
                            }
                        }
                }
            }
        } catch( serEx: SerializationException){
            Log.e("TaskMapManager","Error Serializing TaskMapRequest : $serEx")
        } catch(ex: Exception){
            Log.e("TaskMapManager","Error : $ex")
        }
    }

    abstract fun onTaskSelected(task: Task):Unit
}

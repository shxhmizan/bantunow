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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * This class manages the application Task Map that displays tasks in an interactive map
 * It now interacts with Cloud Firestore
 */
abstract class TaskMapManager(val firestore: FirebaseFirestore, val locationClient: FusedLocationProviderClient) : WebViewCompat.WebMessageListener {
    
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
                    // Fetch from Firestore
                    firestore.collection("tasks")
                        .whereEqualTo("status", "open")
                        .get()
                        .addOnSuccessListener { result ->
                            val tasksMap = mutableMapOf<String, Task>()
                            for (document in result) {
                                val task = document.toObject(Task::class.java)
                                tasksMap[document.id] = task
                            }
                            
                            val taskJson = Json.encodeToString(
                                TaskMapResponse(
                                    TaskMapResponse.Type.NEARBY_TASK_LIST,
                                    tasksMap
                                )
                            )
                            replyProxy.postMessage(taskJson)
                        }
                }

                TaskMapRequest.Type.GET_CURRENT_LOCATION -> {
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
                                            )
                                        )
                                    )
                                )
                            }
                        }
                }

                TaskMapRequest.Type.GET_TASK_DETAILS -> {
                    val taskID = request.contents ?: return
                    firestore.collection("tasks").document(taskID).get()
                        .addOnSuccessListener { document ->
                            val task = document.toObject(Task::class.java)
                            if (task != null) {
                                // Important: We need to trigger the fragment navigation in Android
                                // Not just send JSON back to JS
                                onTaskSelected(task)
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

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
import com.google.firebase.database.FirebaseDatabase
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class TaskMapManager(val database: FirebaseDatabase, val locationClient: FusedLocationProviderClient) : WebViewCompat.WebMessageListener {
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
        //Log.i("MapEventListener", "Event : $data ")
        try {
            val request = Json.decodeFromString<TaskMapRequest>(data);
            Log.i("TaskMapManager", "Request Type : ${request.type}")
            when (request.type) {
                TaskMapRequest.Type.GET_NEARBY_TASKS -> {
                    locationClient.lastLocation.addOnCompleteListener { task ->
                        if (task.isSuccessful) replyProxy.postMessage(
                            Json.encodeToString(
                                TaskMapResponse(
                                    TaskMapResponse.Type.CURRENT_LOCATION,
                                    TaskMapResponse.Location(
                                        latitude = task.result.latitude,
                                        longitude = task.result.longitude
                                    )
                                )
                            )
                        )
                    }
                }

                TaskMapRequest.Type.GET_CURRENT_LOCATION -> {
                    Task.applyOnNearbyTasks(database) { tasks ->
                        if (tasks == null) return@applyOnNearbyTasks
                        val taskJson = Json.encodeToString(
                            TaskMapResponse(
                                TaskMapResponse.Type.NEARBY_TASK_LIST,
                                tasks
                            )
                        )
                        replyProxy.postMessage(taskJson)
                    }
                }

                TaskMapRequest.Type.GET_TASK_DETAILS -> {
                    val taskID = request.contents ?: return
                    Task.getTaskByID(database, taskID) { task ->
                        val taskJson = Json.encodeToString(
                            TaskMapResponse(
                                TaskMapResponse.Type.TASK_DETAILS,
                                task
                            )
                        )
                        replyProxy.postMessage(taskJson)

                        //At this point need to open the fragment with the task details
                    }
                }
            }
        } catch( serEx: SerializationException){
            Log.e("TaskMapManager","Error Serializing TaskMapRequest : $serEx")
            Log.i("TaskMapManager","Raw JSON Data : $data")
        }
        catch(ex: Exception){
            Log.e("TaskMapManager","Error : $ex")
        }
    }
}
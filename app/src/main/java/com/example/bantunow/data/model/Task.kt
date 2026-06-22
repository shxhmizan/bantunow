package com.example.bantunow.data.model

import android.location.Location
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
class Task (
    var ownerID:String? = null,
    var workerID:String? = null,
    var title:String? = null,
    var desc:String? = null,
    var paymentAmount:Long? = null, //Payment amount in cents
    var latitude:Double? = null,
    val longitude:Double? = null,
    var contactNo:String? = null,
    var progressPercentage:Int = 0
){
    companion object {
        const val TASK_DOCS_ROOT = "tasks"

        fun getTaskByID(database: FirebaseDatabase, taskID:String, callback:(com.example.bantunow.data.model.Task) -> Unit){
            val root = database.getReference(TASK_DOCS_ROOT)
            val taskRef = root.child(taskID).get().addOnSuccessListener {
                taskSnapshot ->
                val task = taskSnapshot.getValue(com.example.bantunow.data.model.Task::class.java)
                if(task != null){
                    callback(task)
                }
            }
        }

        /**
         * Retrieves all nearby tasks from the database, then executes a callback on a Map of all retrieved tasks
         * @param database The FirebaseDatabase instance to query from, usually passed from an Activity
         * @param callback The callback function to run on the tasks
         */
        fun applyOnNearbyTasks(database: FirebaseDatabase, callback: (tasks: Map<String,com.example.bantunow.data.model.Task> ?) -> Unit){
            val root = database.getReference(TASK_DOCS_ROOT)
            root.get().addOnSuccessListener {
                taskSnapshot ->
                val tasks = taskSnapshot.getValue<Map<String,com.example.bantunow.data.model.Task>>()
                /*tasks?.forEach { (id, task) ->
                    Log.d("MapFragment","Task $id : $task")
                }*/
                callback(tasks)
            }
        }
    }

    fun insert(database: FirebaseDatabase) : Task<Void>{
        val root = database.getReference(TASK_DOCS_ROOT)
        val newTaskRef = root.push()
        return newTaskRef.setValue(this)
    }

    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
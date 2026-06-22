package com.example.bantunow.data.model

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import com.google.firebase.firestore.Exclude
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
    var longitude:Double? = null,
    var contactNo:String? = null,
    var progressPercentage:Int = 0,
    var status: String = "open", // open, in_progress, completed
    var category: String? = "General"
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

        fun applyOnNearbyTasks(database: FirebaseDatabase, callback: (tasks: Map<String,com.example.bantunow.data.model.Task> ?) -> Unit){
            val root = database.getReference(TASK_DOCS_ROOT)
            root.get().addOnSuccessListener {
                taskSnapshot ->
                val tasks = taskSnapshot.getValue<Map<String,com.example.bantunow.data.model.Task>>()
                callback(tasks)
            }
        }
    }

    fun insert(database: FirebaseDatabase) : com.google.android.gms.tasks.Task<Void>{
        val root = database.getReference(TASK_DOCS_ROOT)
        val newTaskRef = root.push()
        return newTaskRef.setValue(this)
    }

    override fun toString(): String {
        return Json.encodeToString(this)
    }
}
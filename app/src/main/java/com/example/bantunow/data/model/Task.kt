package com.example.bantunow.data.model

import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase

class Task (
    var ownerID:String? = null,
    var workerID:String? = null,
    var taskTitle:String? = null,
    var taskDesc:String? = null,
    var taskPaymentAmount:Long? = null,
    var location:String? = null,
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

        fun getNearbyTasks(database: FirebaseDatabase, callback: (List<com.example.bantunow.data.model.Task>) -> Unit){
            val root = database.getReference(TASK_DOCS_ROOT)
            val taskRef = root.get().addOnSuccessListener {
                taskSnapshot ->
                val tasks = taskSnapshot.children.mapNotNull {
                    it.getValue(com.example.bantunow.data.model.Task::class.java)
                }
                callback(tasks)
            }
        }
    }

    fun insert(database: FirebaseDatabase) : Task<Void>{
        val root = database.getReference(TASK_DOCS_ROOT)
        val newTaskRef = root.push()
        return newTaskRef.setValue(this)
    }
}
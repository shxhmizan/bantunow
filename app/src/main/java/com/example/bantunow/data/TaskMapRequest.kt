package com.example.bantunow.data

import kotlinx.serialization.Serializable

@Serializable
class TaskMapRequest(val type: Type, val contents:String? ) {
    @Serializable
    enum class Type {
        GET_NEARBY_TASKS,
        GET_CURRENT_LOCATION,
        GET_TASK_DETAILS
    }
}
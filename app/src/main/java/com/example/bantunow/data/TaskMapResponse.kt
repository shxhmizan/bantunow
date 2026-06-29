package com.example.bantunow.data

import kotlinx.serialization.Serializable

@Serializable
class TaskMapResponse<T>(
    val type: Type,
    val contents: T,
    val currentUserId: String? = null
) {
    @Serializable
    enum class Type {
        NEARBY_TASK_LIST,
        CURRENT_LOCATION,
        TASK_DETAILS
    }
    @Serializable
    data class Location(val latitude:Double, val longitude:Double)
}

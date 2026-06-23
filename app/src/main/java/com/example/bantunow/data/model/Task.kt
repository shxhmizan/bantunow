package com.example.bantunow.data.model

import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Task (
    var ownerID: String? = null,
    var workerID: String? = null,
    var title: String? = null,
    var desc: String? = null,
    var paymentAmount: Long? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var contactNo: String? = null,
    var progressPercentage: Long = 0,
    var status: String = "open",
    var category: String? = "General",
    @kotlinx.serialization.Transient
    var createdAt: Timestamp? = null
) {
    // Default constructor for Firestore
    constructor() : this(null, null, null, null, null, null, null, null, 0, "open", "General", null)

    override fun toString(): String {
        return Json.encodeToString(this)
    }
}

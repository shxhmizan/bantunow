package com.example.bantunow

import com.google.firebase.firestore.GeoPoint

/**
 * Data class representing a Job/Task in BantuNow
 */
data class Job(
    var jobId: String? = null,
    var ownerId: String? = null,
    var performerId: String? = null,
    var title: String? = null,
    var description: String? = null,
    var pay: Double? = null,
    var phone: String? = null,
    var status: String = "OPEN", // OPEN, IN_PROGRESS, COMPLETED
    var location: GeoPoint? = null,
    var createdAt: Long? = System.currentTimeMillis()
)

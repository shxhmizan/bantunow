package com.example.bantunow

/**
 * Data class representing a registered user in BantuNow
 */
data class User(
    var userId: String? = null,
    var fullName: String? = null,
    var email: String? = null,
    var bantuPoints: Long = 0,
    var createdAt: Long? = System.currentTimeMillis()
)

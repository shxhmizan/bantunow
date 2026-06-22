package com.example.bantunow

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object DataSeeder {
    private const val TAG = "DataSeeder"

    fun seedDataIfNeeded() {
        val db = FirebaseFirestore.getInstance()
        
        // Check if we have already seeded tasks
        db.collection("tasks").limit(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                Log.d(TAG, "Seeding 10 dummy tasks to Firestore...")
                seedDummyTasks(db)
                seedDummyUsers(db)
            } else {
                Log.d(TAG, "Firestore data already exists, skipping seed.")
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to check existing Firestore data", it)
        }
    }

    private fun seedDummyTasks(db: FirebaseFirestore) {
        val centerLat = 4.1865
        val centerLng = 101.2620
        
        val taskTitles = listOf(
            "IKEA Table Assembly", "Grass Cutting", "Delivery Runner", 
            "House Cleaning", "Pet Grooming", "Plumbing Repair", 
            "Electric Fan Install", "Grocery Shopping", "Car Wash", "Tuition for Kids"
        )
        
        val taskDescs = listOf(
            "Need help with assembly.", "Cut the grass in front yard.", "Deliver food to campus.",
            "Clean the living room.", "Wash my cat.", "Fix a leaky pipe.",
            "Install a new ceiling fan.", "Buy some groceries.", "Wash my car.", "Teach Math to 10 year old."
        )

        for (i in 0 until 10) {
            // Generate small random offsets around the center location
            val latOffset = (Math.random() - 0.5) * 0.01
            val lngOffset = (Math.random() - 0.5) * 0.01
            
            val taskData = mapOf(
                "ownerID" to "dummy_user_${(i % 3) + 1}",
                "title" to taskTitles[i],
                "desc" to taskDescs[i],
                "paymentAmount" to (1000L + (Math.random() * 9000).toLong()), // Random RM 10 - RM 100
                "latitude" to centerLat + latOffset,
                "longitude" to centerLng + lngOffset,
                "contactNo" to "01${(10000000 + (Math.random() * 89999999).toInt())}",
                "status" to "open",
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            db.collection("tasks").add(taskData)
                .addOnSuccessListener { Log.d(TAG, "Task $i seeded: ${taskData["title"]}") }
        }
    }

    private fun seedDummyUsers(db: FirebaseFirestore) {
        val dummyUsers = mapOf(
            "dummy_user_1" to mapOf("bantuPoints" to 150L, "displayName" to "Ali Bakar"),
            "dummy_user_2" to mapOf("bantuPoints" to 300L, "displayName" to "Siti Aminah"),
            "dummy_user_3" to mapOf("bantuPoints" to 50L, "displayName" to "John Doe")
        )

        dummyUsers.forEach { (id, userData) ->
            db.collection("users").document(id).set(userData)
                .addOnSuccessListener { Log.d(TAG, "User seeded: $id") }
        }
    }
}

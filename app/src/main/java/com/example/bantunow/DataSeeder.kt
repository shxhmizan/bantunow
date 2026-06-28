package com.example.bantunow

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object DataSeeder {
    private const val TAG = "DataSeeder"

    fun seedDataIfNeeded() {
        val db = FirebaseFirestore.getInstance()
        
        // Check if data already exists to avoid wiping user-uploaded tasks
        db.collection("tasks").limit(1).get().addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                Log.d(TAG, "Database is empty. Starting initial seed (Perak Region)...")
                seedNewData(db)
            } else {
                Log.d(TAG, "Database already has data. Skipping seed to preserve user tasks.")
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to check existing Firestore data", it)
        }
    }

    private fun seedNewData(db: FirebaseFirestore) {
        val targetUserIds = listOf(
            "v5Nntl9JVNXs2jmbGWsuLvoFAmB2",
            "svnSerBrd3POymBPMFNvwc8RRaw2",
            "zrRyI8mG0zP57zwEDWUVWtfSEos1"
        )
        
        // Perak Region Center (Ipoh area)
        val perakLat = 4.5921
        val perakLng = 101.0901
        
        // Seed Users first
        targetUserIds.forEachIndexed { index, uid ->
            val names = listOf("Ahmad Perak", "Siti Ipoh", "Mizann")
            val userData = mapOf(
                "displayName" to names[index],
                "walletBalance" to 500.0,
                "bantuPoints" to 100L,
                "profileImageUrl" to "",
                "rating" to 4.5 + (index * 0.2)
            )
            db.collection("users").document(uid).set(userData)
        }

        val taskTemplates = listOf(
            Triple("Assemble Kitchen Cabinet", "Need help to assemble new kitchen cabinets from IKEA Ipoh.", "Repair"),
            Triple("Lawn Mowing in Meru", "Grass is getting long, need someone to mow it this weekend.", "General"),
            Triple("Delivery to Tambun", "Pick up package from PosLaju Ipoh and deliver to Tambun.", "Delivery"),
            Triple("Home Cleaning Klebang", "Need deep cleaning for a 3-bedroom house in Klebang.", "Cleaning"),
            Triple("Pet Care in Station 18", "Need someone to walk my dog around Station 18 area.", "Pet Care"),
            Triple("Fan Installation", "Install 2 ceiling fans in the living room.", "Repair"),
            Triple("Window Cleaning", "Clean all windows for a double-story house.", "Cleaning"),
            Triple("Grocery Runner", "Buy groceries from Mydin Meru and deliver to my house.", "Delivery"),
            Triple("Car Polish", "Exterior car polish and wax.", "General"),
            Triple("Malay Language Tutor", "Teach basic BM to a primary school student.", "Education")
        )

        for (i in 0 until 15) {
            val template = taskTemplates[i % taskTemplates.size]
            val latOffset = (Math.random() - 0.5) * 0.18 
            val lngOffset = (Math.random() - 0.5) * 0.18
            
            val ownerID = targetUserIds[i % targetUserIds.size]
            val pay = (2000L + (Math.random() * 6000).toLong())

            val taskData = mutableMapOf(
                "ownerID" to ownerID,
                "title" to template.first,
                "desc" to template.second,
                "paymentAmount" to pay,
                "latitude" to perakLat + latOffset,
                "longitude" to perakLng + lngOffset,
                "contactNo" to "01${(10000000 + (Math.random() * 89999999).toInt())}",
                "status" to "open",
                "category" to template.third,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "progressPercentage" to 0L
            )
            
            db.collection("tasks").add(taskData).addOnSuccessListener {
                val transData = mapOf(
                    "walletID" to ownerID,
                    "type" to "withdrawal",
                    "amount" to pay,
                    "status" to "completed",
                    "timestamp" to com.google.firebase.Timestamp.now(),
                    "description" to "Posted task: ${template.first}"
                )
                db.collection("transactions").add(transData)
            }
        }
    }
}

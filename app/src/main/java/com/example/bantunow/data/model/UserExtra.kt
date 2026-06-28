package com.example.bantunow.data.model

import com.google.firebase.database.FirebaseDatabase
import java.io.Serializable

/**
 * The UserExtra class represents extra data of registered user, separate from user data stored in
 * Firebase Authentication service
 * @property bantuPoints The total application points for this user
 * @property walletBalance The user's wallet balance
 */
class UserExtra (
    var bantuPoints:Long = 0,
    var displayName:String? = null,
    var walletBalance: Double = 0.0,
    var profileImageUrl: String? = null
) : Serializable {
    companion object {
        /**
         * The root node for all user extra data in realtime database
         */
        const val USER_DOCS_ROOT = "users"

        fun getUserExtraByID(database: FirebaseDatabase, userID:String, callback:(UserExtra) -> Unit){
            val root = database.getReference(USER_DOCS_ROOT)
            val userRef = root.child(userID).get().addOnSuccessListener {
                userSnapshot ->
                val userExtra = userSnapshot.getValue(UserExtra::class.java)
                if(userExtra != null){
                    callback(userExtra)
                } else {
                    // Create default if doesn't exist
                    val newUser = UserExtra(0, "User", 0.0)
                    callback(newUser)
                }
            }
        }
    }

}
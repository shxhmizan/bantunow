package com.example.bantunow.data.model

import com.google.firebase.database.FirebaseDatabase

/**
 * The UserExtra class represents extra data of registered user, separate from user data stored in
 * Firebase Authentication service
 * @property bantuPoints The total application points for this user
 */
class UserExtra (
    var bantuPoints:Long = 0,
    var displayName:String? = null
){
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
                }
            }
        }
    }

}
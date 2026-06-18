package com.example.bantunow

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

/**
 * The UserExtra class represents extra data of registered user, separate from user data stored in
 * Firebase Authentication service
 * @property bantuPoints The total application points for this user
 */
class UserExtra (
    var bantuPoints:Long? = null,
){
    companion object {
        /**
         * The root node for all user extra data in realtime database
         */
        const val USER_DOCS_ROOT = "users"
    }
}
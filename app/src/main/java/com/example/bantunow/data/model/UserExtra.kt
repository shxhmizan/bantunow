package com.example.bantunow.data.model

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
    }
}
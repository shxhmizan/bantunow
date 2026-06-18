package com.example.bantunow

/**
 * The User class represents data of registered user
 * @property userID The User ID in the database
 * @property fullName The full name of the user to display in the application
 * @property email The user email
 * @property bantuPoints The total application points for this user
 */
class User (
    var userID: String? = null,
    var fullName:String? = null,
    var email:String? = null,
    var bantuPoints:Long? = null,
)
{
    companion object {
        const val MIN_USERID_LENGTH = 3
        const val MIN_FULLNAME_LENGTH = 3
        const val MIN_PASSWORD_LENGTH = 8
    }


}
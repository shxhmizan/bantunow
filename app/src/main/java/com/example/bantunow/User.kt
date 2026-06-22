package com.example.bantunow

/**
 * The User class represents data of registered user
 * @property userID The User ID in the database
 * @property fullName The full name of the user to display in the application
 * @property email The user email
 * @property password The user's password. This field is only populated for client-side password
 * validation, otherwise this field will be null.
 * @property bantuPoints The total application points for this user
 * @property createdAt The timestamp of this user's creation
 */
class User (
    var userID: String? = null,
    var fullName:String? = null,
    var email:String? = null,
    var password:String? = null,
    var bantuPoints:Long? = null,
    var createdAt:String? = null
)
{
    companion object {
        const val MIN_USERID_LENGTH = 3
        const val MIN_FULLNAME_LENGTH = 3
        const val MIN_PASSWORD_LENGTH = 8
    }

    /**
     * Checks if this user has valid data
     * @param validatePassword If set to true, the password field will also be checked.
     * The password check is only done when performing client-side password validation
     * such as during registration and changing password
     */
    fun validate(validatePassword:Boolean = true) : Boolean {
            return userID?.isNotEmpty() == true &&
                    (userID?.length ?: -1) >= MIN_USERID_LENGTH &&
                    fullName?.isNotEmpty() == true &&
                    (fullName?.length ?: -1) >= MIN_FULLNAME_LENGTH &&
                    email?.isNotEmpty() == true &&
                    (bantuPoints ?: -1) >= 0 &&
                    createdAt != null &&
                    (   (validatePassword &&
                            password?.isNotEmpty() == true &&
                            (password?.length ?: 0) >= MIN_PASSWORD_LENGTH)
                        || !validatePassword)
    }
}
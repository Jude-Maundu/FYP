package com.example.fypapplication.backend.data

import android.content.Context
import android.content.SharedPreferences
import com.example.fypapplication.backend.LoginResponse
import com.google.gson.Gson

/**
 * Repository class for managing user data
 * Handles saving and retrieving user information using SharedPreferences
 */
class UserRepository(private val context: Context? = null) {

    private val PREF_NAME = "FYPAppPrefs"
    private val KEY_USER_DATA = "user_data"

    private val sharedPreferences: SharedPreferences? = context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Save user data to SharedPreferences
     */
    fun saveUserData(loginResponse: LoginResponse) {
        val user = User(
            id = loginResponse.id,
            username = loginResponse.username,
            email = loginResponse.email,
            role = loginResponse.role,
            token = loginResponse.token
        )

        val userJson = gson.toJson(user)
        sharedPreferences?.edit()?.putString(KEY_USER_DATA, userJson)?.apply()
    }

    /**
     * Get user data from SharedPreferences
     */
    fun getUserData(): User? {
        val userJson = sharedPreferences?.getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else {
            null
        }
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return getUserData() != null
    }

    /**
     * Get user token for API requests
     */
    fun getUserToken(): String? {
        return getUserData()?.token
    }

    /**
     * Get user role for role-based access control
     */
    fun getUserRole(): String? {
        return getUserData()?.role
    }

    /**
     * Clear user data (for logout)
     */
    fun clearUserData() {
        sharedPreferences?.edit()?.remove(KEY_USER_DATA)?.apply()
    }
}
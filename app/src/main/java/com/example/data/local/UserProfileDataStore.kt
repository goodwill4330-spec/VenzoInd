package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile_prefs")

class UserProfileDataStore(private val context: Context) {

    private object PreferenceKeys {
        val IS_LOGGED_IN = booleanPreferencesKey("user_is_logged_in")
        val DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val STATUS_BIO = stringPreferencesKey("user_status_bio")
        val AVATAR_INITIAL = stringPreferencesKey("user_avatar_initial")
        val AVATAR_COLOR_HEX = stringPreferencesKey("user_avatar_color_hex")
        val CUSTOM_AVATAR_INDEX = intPreferencesKey("user_custom_avatar_index")
        val PROFILE_PIC_URI = stringPreferencesKey("user_profile_pic_uri")
        val BHARAT_ID = stringPreferencesKey("user_bharat_id")
        val PHONE = stringPreferencesKey("user_phone")
        val EMAIL = stringPreferencesKey("user_email")
        val UPI_VPA = stringPreferencesKey("user_upi_vpa")
        val WALLET_BALANCE = doublePreferencesKey("user_wallet_balance")
        val DEVICE_ID = stringPreferencesKey("user_device_id")
    }

    private val sharedPrefs = context.getSharedPreferences("app_device_identity", Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        var id = sharedPrefs.getString("device_uuid", null)
        if (id.isNullOrBlank()) {
            id = "dev_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)
            sharedPrefs.edit().putString("device_uuid", id).apply()
        }
        return id
    }

    val isLoggedInFlow: Flow<Boolean> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] ?: false
        }

    val userProfileFlow: Flow<UserProfile> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserProfile(
                name = preferences[PreferenceKeys.DISPLAY_NAME] ?: "VenzoInd User",
                statusBio = preferences[PreferenceKeys.STATUS_BIO] ?: "Hey there! I am using VenzoInd.",
                avatarInitial = preferences[PreferenceKeys.AVATAR_INITIAL] ?: "VU",
                avatarColorHex = preferences[PreferenceKeys.AVATAR_COLOR_HEX] ?: "#10B981",
                customAvatarIndex = preferences[PreferenceKeys.CUSTOM_AVATAR_INDEX] ?: 0,
                profilePicUri = preferences[PreferenceKeys.PROFILE_PIC_URI] ?: "",
                bharatId = preferences[PreferenceKeys.BHARAT_ID] ?: "@venzoind_user",
                phone = preferences[PreferenceKeys.PHONE] ?: "+91 98765 43210",
                email = preferences[PreferenceKeys.EMAIL] ?: "user@venzoind.com",
                upiVpa = preferences[PreferenceKeys.UPI_VPA] ?: "venzoind@upi",
                walletBalance = preferences[PreferenceKeys.WALLET_BALANCE] ?: 14850.50
            )
        }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] = loggedIn
        }
    }

    suspend fun saveUserProfile(
        name: String = "VenzoInd User",
        displayName: String = name,
        statusBio: String = "Hey there! I am using VenzoInd.",
        profilePicUri: String = "",
        avatarInitial: String = "VU",
        avatarColorHex: String = "#10B981",
        customAvatarIndex: Int = 0,
        avatarIndex: Int = customAvatarIndex,
        phone: String = "+91 98765 43210"
    ) {
        val finalName = if (displayName.isNotBlank()) displayName else name
        val finalAvatarIndex = if (avatarIndex != 0 || customAvatarIndex == 0) avatarIndex else customAvatarIndex
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.IS_LOGGED_IN] = true
            preferences[PreferenceKeys.DISPLAY_NAME] = finalName
            preferences[PreferenceKeys.STATUS_BIO] = statusBio
            preferences[PreferenceKeys.PROFILE_PIC_URI] = profilePicUri
            preferences[PreferenceKeys.AVATAR_INITIAL] = avatarInitial
            preferences[PreferenceKeys.AVATAR_COLOR_HEX] = avatarColorHex
            preferences[PreferenceKeys.CUSTOM_AVATAR_INDEX] = finalAvatarIndex
            preferences[PreferenceKeys.PHONE] = phone
            preferences[PreferenceKeys.BHARAT_ID] = "@${finalName.lowercase().replace(" ", "_")}"
            preferences[PreferenceKeys.UPI_VPA] = "${finalName.lowercase().replace(" ", "")}@upi"
        }
    }

    suspend fun clearAccount() {
        context.userDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun updateDisplayName(name: String) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.DISPLAY_NAME] = name
        }
    }

    suspend fun updateStatusBio(bio: String) {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.STATUS_BIO] = bio
        }
    }

    suspend fun updateProfilePicture(picUri: String, avatarIndex: Int = 0, colorHex: String = "#FF671F") {
        context.userDataStore.edit { preferences ->
            preferences[PreferenceKeys.PROFILE_PIC_URI] = picUri
            preferences[PreferenceKeys.CUSTOM_AVATAR_INDEX] = avatarIndex
            preferences[PreferenceKeys.AVATAR_COLOR_HEX] = colorHex
        }
    }
}

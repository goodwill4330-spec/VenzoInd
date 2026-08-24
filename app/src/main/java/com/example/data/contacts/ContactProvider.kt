package com.example.data.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.model.ChatEntity
import com.example.data.model.ContactEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DeviceRawContact(
    val contactId: String,
    val displayName: String,
    val phoneNumber: String,
    val normalizedPhone: String,
    val photoUri: String? = null
)

data class ContactSyncSummary(
    val totalDeviceContacts: Int,
    val registeredUsersCount: Int,
    val matchedCount: Int,
    val success: Boolean,
    val errorMessage: String? = null
)

class ContactProvider private constructor(
    private val context: Context,
    private val appDatabase: AppDatabase
) {
    private val TAG = "ContactProvider"

    /**
     * Checks if the app has READ_CONTACTS permission
     */
    fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Normalizes a phone number by stripping whitespace, hyphens, brackets,
     * and isolating the primary digits / national format for reliable comparison.
     */
    fun normalizePhoneNumber(raw: String): String {
        if (raw.isBlank()) return ""
        val digitsOnly = raw.filter { it.isDigit() }
        // For 10+ digits, take the last 10 digits (standard Indian / international mobile number matching)
        return if (digitsOnly.length >= 10) {
            digitsOnly.takeLast(10)
        } else {
            digitsOnly
        }
    }

    /**
     * Reads all contacts stored in the Android device's ContactsContract ContentProvider
     */
    suspend fun fetchDeviceContacts(): List<DeviceRawContact> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<DeviceRawContact>()
        if (!hasContactPermission()) {
            Log.w(TAG, "Cannot fetch contacts: READ_CONTACTS permission not granted")
            return@withContext emptyList()
        }

        try {
            val contentResolver = context.contentResolver
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val seenNumbers = mutableSetOf<String>()

                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getString(idIdx) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
                    val name = if (nameIdx >= 0) c.getString(nameIdx) ?: "Contact" else "Contact"
                    val number = if (numberIdx >= 0) c.getString(numberIdx) ?: "" else ""
                    val photoUri = if (photoIdx >= 0) c.getString(photoIdx) else null

                    val normalized = normalizePhoneNumber(number)
                    if (name.isNotBlank() && normalized.isNotBlank() && !seenNumbers.contains(normalized)) {
                        seenNumbers.add(normalized)
                        contactsList.add(
                            DeviceRawContact(
                                contactId = id,
                                displayName = name.trim(),
                                phoneNumber = number.trim(),
                                normalizedPhone = normalized,
                                photoUri = photoUri
                            )
                        )
                    }
                }
            }
            Log.d(TAG, "Fetched ${contactsList.size} unique device contacts from phonebook")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching device contacts: ${e.message}", e)
        }

        return@withContext contactsList
    }

    /**
     * Finds the local address book contact name for a given phone number
     */
    suspend fun getDeviceContactNameForPhone(phoneNumber: String): String? = withContext(Dispatchers.IO) {
        if (!hasContactPermission() || phoneNumber.isBlank()) return@withContext null
        val targetNormalized = normalizePhoneNumber(phoneNumber)
        if (targetNormalized.isBlank()) return@withContext null

        val deviceContacts = fetchDeviceContacts()
        return@withContext deviceContacts.firstOrNull { it.normalizedPhone == targetNormalized }?.displayName
    }

    /**
     * Primary function:
     * 1. Fetches local phonebook contacts.
     * 2. Fetches registered users from Firestore 'users' collection.
     * 3. Cross-references & maps them:
     *    - If registered on Firestore -> Sets registered user status, uses the phonebook name (so user sees their saved name like "Rohan"), updates Room & Chats.
     *    - If not registered on Firestore -> Preserves phonebook contact with isBharatChatUser = false for invitations.
     * 4. Updates Room AppDatabase.
     */
    suspend fun syncAndMapContactsWithFirestore(
        myDeviceId: String = "",
        myPhone: String = ""
    ): ContactSyncSummary = withContext(Dispatchers.IO) {
        val deviceContacts = fetchDeviceContacts()
        val myNormalizedPhone = normalizePhoneNumber(myPhone)

        val firestoreUsers = mutableListOf<Map<String, Any>>()
        var isFirestoreConnected = false

        try {
            val db = FirebaseFirestore.getInstance()
            val querySnapshot = db.collection("users").get().await()
            for (doc in querySnapshot.documents) {
                val data = doc.data
                if (data != null) {
                    val userMap = HashMap(data)
                    userMap["docId"] = doc.id
                    firestoreUsers.add(userMap)
                }
            }
            isFirestoreConnected = true
            Log.d(TAG, "Retrieved ${firestoreUsers.size} registered users from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch registered users from Firestore: ${e.message}")
        }

        // Map normalized phone to registered firestore user
        val firestorePhoneMap = mutableMapOf<String, Map<String, Any>>()
        val firestoreDeviceMap = mutableMapOf<String, Map<String, Any>>()

        for (user in firestoreUsers) {
            val userPhone = user["phone"] as? String ?: ""
            val userDevId = user["deviceId"] as? String ?: ""
            val norm = normalizePhoneNumber(userPhone)
            if (norm.isNotBlank()) {
                firestorePhoneMap[norm] = user
            }
            if (userDevId.isNotBlank()) {
                firestoreDeviceMap[userDevId] = user
            }
        }

        var matchedCount = 0
        val finalContacts = mutableListOf<ContactEntity>()
        val processedNormalizedPhones = mutableSetOf<String>()

        val colors = listOf("#0284C7", "#EC4899", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444", "#14B8A6", "#6366F1")

        // 1. Process all contacts from the user's phonebook
        for (devContact in deviceContacts) {
            // Skip own phone number
            if (myNormalizedPhone.isNotBlank() && devContact.normalizedPhone == myNormalizedPhone) {
                continue
            }

            processedNormalizedPhones.add(devContact.normalizedPhone)

            val matchedUser = firestorePhoneMap[devContact.normalizedPhone]
            val initials = devContact.displayName.split(" ")
                .filter { it.isNotBlank() }
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
                .ifEmpty { "C" }

            if (matchedUser != null) {
                // MATCHED REGISTERED USER: Phonebook name takes precedence!
                matchedCount++
                val cloudDeviceId = matchedUser["deviceId"] as? String ?: matchedUser["docId"] as? String ?: devContact.contactId
                val contactId = "contact_$cloudDeviceId"
                val chatId = "chat_$cloudDeviceId"
                val cloudBio = matchedUser["statusBio"] as? String ?: "Hey there! I am using VenzoInd."
                val cloudColor = matchedUser["avatarColorHex"] as? String ?: colors.random()
                val cloudAvatar = matchedUser["avatarInitial"] as? String ?: initials
                val cloudUpi = matchedUser["upiVpa"] as? String ?: "${devContact.displayName.lowercase().replace(" ", "")}@upi"
                val lastSeen = (matchedUser["lastSeen"] as? Long) ?: System.currentTimeMillis()

                val entity = ContactEntity(
                    id = contactId,
                    name = devContact.displayName, // Phonebook name mapped to registered user
                    phone = devContact.phoneNumber,
                    upiVpa = cloudUpi,
                    avatarInitial = cloudAvatar,
                    avatarColorHex = cloudColor,
                    statusMsg = cloudBio,
                    isBharatChatUser = true,
                    isFavorite = false,
                    publicKeyFingerprint = "KYBER-1024-${UUID.randomUUID().toString().take(6).uppercase()}",
                    lastSeenTimestamp = lastSeen,
                    profilePicUri = devContact.photoUri
                )
                finalContacts.add(entity)

                // Ensure Chat exists with the user's phonebook name
                val existingChat = appDatabase.chatDao().getChatById(chatId)
                if (existingChat == null) {
                    val newChat = ChatEntity(
                        id = chatId,
                        title = devContact.displayName, // Phonebook name in Chat screen
                        subtitle = cloudBio,
                        avatarInitial = cloudAvatar,
                        avatarColorHex = cloudColor,
                        isOnline = true,
                        lastMessage = "Connected on VenzoInd 🟢",
                        lastMessageTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                        timestamp = System.currentTimeMillis()
                    )
                    appDatabase.chatDao().insertChat(newChat)
                } else {
                    appDatabase.chatDao().updateChatTitle(chatId, devContact.displayName)
                }
            } else {
                // NON-REGISTERED DEVICE CONTACT
                val contactId = "contact_dev_${devContact.normalizedPhone}"
                val entity = ContactEntity(
                    id = contactId,
                    name = devContact.displayName,
                    phone = devContact.phoneNumber,
                    upiVpa = "${devContact.displayName.lowercase().replace(" ", "")}@upi",
                    avatarInitial = initials,
                    avatarColorHex = colors.random(),
                    statusMsg = "Invite to VenzoInd",
                    isBharatChatUser = false,
                    isFavorite = false,
                    publicKeyFingerprint = "KYBER-1024-DEF78A",
                    lastSeenTimestamp = 0L,
                    profilePicUri = devContact.photoUri
                )
                finalContacts.add(entity)
            }
        }

        // 2. Include registered Firestore users not present in the device phonebook
        for (user in firestoreUsers) {
            val userPhone = user["phone"] as? String ?: ""
            val userDevId = user["deviceId"] as? String ?: user["docId"] as? String ?: ""
            val norm = normalizePhoneNumber(userPhone)

            if (myDeviceId.isNotBlank() && (userDevId == myDeviceId || user["docId"] == myDeviceId)) continue
            if (myNormalizedPhone.isNotBlank() && norm == myNormalizedPhone) continue

            // If not already processed through device contact matching
            if (norm.isBlank() || !processedNormalizedPhones.contains(norm)) {
                val userName = user["name"] as? String ?: "Venzo User"
                val cloudBio = user["statusBio"] as? String ?: "Available on VenzoInd"
                val cloudColor = user["avatarColorHex"] as? String ?: colors.random()
                val cloudAvatar = user["avatarInitial"] as? String ?: userName.take(2).uppercase()
                val cloudUpi = user["upiVpa"] as? String ?: ""
                val lastSeen = (user["lastSeen"] as? Long) ?: System.currentTimeMillis()

                val targetId = if (userDevId.isNotBlank()) userDevId else userName.lowercase().replace(" ", "_")
                val contactId = "contact_$targetId"
                val chatId = "chat_$targetId"

                val entity = ContactEntity(
                    id = contactId,
                    name = userName,
                    phone = if (userPhone.isNotBlank()) userPhone else "+91 98000 00000",
                    upiVpa = cloudUpi,
                    avatarInitial = cloudAvatar,
                    avatarColorHex = cloudColor,
                    statusMsg = cloudBio,
                    isBharatChatUser = true,
                    isFavorite = false,
                    publicKeyFingerprint = "KYBER-1024-${UUID.randomUUID().toString().take(6).uppercase()}",
                    lastSeenTimestamp = lastSeen
                )
                finalContacts.add(entity)

                val existingChat = appDatabase.chatDao().getChatById(chatId)
                if (existingChat == null) {
                    val newChat = ChatEntity(
                        id = chatId,
                        title = userName,
                        subtitle = cloudBio,
                        avatarInitial = cloudAvatar,
                        avatarColorHex = cloudColor,
                        isOnline = true,
                        lastMessage = "Connected on VenzoInd 🟢",
                        lastMessageTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                        timestamp = System.currentTimeMillis()
                    )
                    appDatabase.chatDao().insertChat(newChat)
                }
            }
        }

        if (finalContacts.isNotEmpty()) {
            appDatabase.contactDao().insertContacts(finalContacts)
        }

        Log.i(
            TAG,
            "Sync complete: totalDevice=${deviceContacts.size}, registered=${firestoreUsers.size}, matched=${matchedCount}"
        )

        return@withContext ContactSyncSummary(
            totalDeviceContacts = deviceContacts.size,
            registeredUsersCount = firestoreUsers.size,
            matchedCount = matchedCount,
            success = true
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: ContactProvider? = null

        fun getInstance(context: Context, database: AppDatabase): ContactProvider {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ContactProvider(context.applicationContext, database).also {
                    INSTANCE = it
                }
            }
        }
    }
}

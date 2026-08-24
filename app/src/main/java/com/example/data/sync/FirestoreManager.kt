package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.ChatEntity
import com.example.data.model.ContactEntity
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreManager private constructor(private val context: Context) {

    private val TAG = "FirestoreManager"
    private var firestoreInstance: FirebaseFirestore? = null
    private val activeChatListeners = mutableMapOf<String, ListenerRegistration>()
    private var callsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isFirestoreConnected = MutableStateFlow(false)
    val isFirestoreConnected: StateFlow<Boolean> = _isFirestoreConnected.asStateFlow()

    init {
        initializeFirestore()
    }

    private fun initializeFirestore() {
        try {
            var app: FirebaseApp? = try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseApp.getInstance()
                } else {
                    FirebaseApp.initializeApp(context)
                }
            } catch (e: Exception) {
                null
            }

            if (app == null) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:582340594433:android:d6bc9bbe03aff01c70e900")
                    .setProjectId("venzo-chat-app")
                    .setApiKey("AIzaSyCdoksIc3iXRurlBDK_4TLkgLn1IeAyvyo")
                    .setStorageBucket("venzo-chat-app.firebasestorage.app")
                    .build()
                app = FirebaseApp.initializeApp(context, options)
            }

            if (app != null) {
                firestoreInstance = FirebaseFirestore.getInstance(app)
                _isFirestoreConnected.value = true
                Log.d(TAG, "Firebase Firestore initialized successfully for venzo-chat-app.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore initialization error: ${e.message}")
            _isFirestoreConnected.value = false
        }
    }

    /**
     * Persists or syncs a message to Firestore in real-time
     */
    fun syncMessageToCloud(message: MessageEntity) {
        val db = firestoreInstance ?: return
        scope.launch {
            try {
                val messageMap = hashMapOf(
                    "id" to message.id,
                    "chatId" to message.chatId,
                    "senderId" to message.senderId,
                    "senderName" to message.senderName,
                    "text" to message.text,
                    "timestamp" to message.timestamp,
                    "timeFormatted" to message.timeFormatted,
                    "isFromMe" to message.isFromMe,
                    "status" to message.status,
                    "isSeen" to message.isSeen,
                    "messageType" to message.messageType,
                    "attachmentUrl" to (message.attachmentUrl ?: ""),
                    "fileSizeStr" to (message.fileSizeStr ?: ""),
                    "upiAmount" to (message.upiAmount ?: 0.0),
                    "upiTransactionId" to (message.upiTransactionId ?: ""),
                    "upiStatus" to (message.upiStatus ?: ""),
                    "voiceDurationSec" to (message.voiceDurationSec ?: 0),
                    "audioWaveform" to (message.audioWaveform ?: ""),
                    "pollQuestion" to (message.pollQuestion ?: ""),
                    "pollOptionsJson" to (message.pollOptionsJson ?: ""),
                    "pollVotesJson" to (message.pollVotesJson ?: ""),
                    "isSecretExpiring" to message.isSecretExpiring,
                    "expireTimeMillis" to message.expireTimeMillis,
                    "isStarred" to message.isStarred,
                    "replyToText" to (message.replyToText ?: ""),
                    "replyToSender" to (message.replyToSender ?: "")
                )

                db.collection("chats")
                    .document(message.chatId)
                    .collection("messages")
                    .document(message.id)
                    .set(messageMap, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "Message ${message.id} synced to Firestore in real-time.")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firestore message sync error: ${e.message}")
                    }

                // Also update the chat's last message in cloud
                val chatSummaryMap = hashMapOf(
                    "chatId" to message.chatId,
                    "lastMessage" to message.text,
                    "lastMessageTime" to message.timeFormatted,
                    "timestamp" to message.timestamp,
                    "lastMessageStatus" to message.status,
                    "lastSenderName" to message.senderName,
                    "lastSenderId" to message.senderId
                )
                db.collection("chats")
                    .document(message.chatId)
                    .set(chatSummaryMap, SetOptions.merge())

            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload message to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Listens for incoming real-time messages in a chat from Firestore and caches to Room
     */
    fun attachChatListener(
        chatId: String,
        appDatabase: AppDatabase,
        currentUserId: String,
        currentUserName: String
    ) {
        val db = firestoreInstance ?: return
        if (activeChatListeners.containsKey(chatId)) return

        try {
            val listener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Chat listener error for $chatId: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null && !snapshots.isEmpty) {
                        scope.launch {
                            val messagesToInsert = mutableListOf<MessageEntity>()
                            var latestIncomingMsg: MessageEntity? = null

                            for (doc in snapshots.documents) {
                                try {
                                    val id = doc.getString("id") ?: doc.id
                                    val senderId = doc.getString("senderId") ?: "unknown"
                                    val senderName = doc.getString("senderName") ?: "Contact"
                                    val text = doc.getString("text") ?: ""
                                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                    val timeFormatted = doc.getString("timeFormatted") ?: "Now"
                                    val status = doc.getString("status") ?: "DELIVERED"
                                    val isSeen = doc.getBoolean("isSeen") ?: false
                                    val messageType = doc.getString("messageType") ?: MessageType.TEXT.name

                                    // Determine isFromMe dynamically for this specific phone/user
                                    val isFromMe = (senderId == currentUserId || 
                                                    senderId == "me" || 
                                                    (senderName.isNotBlank() && senderName.equals(currentUserName, ignoreCase = true)))

                                    val entity = MessageEntity(
                                        id = id,
                                        chatId = chatId,
                                        senderId = senderId,
                                        senderName = senderName,
                                        text = text,
                                        timestamp = timestamp,
                                        timeFormatted = timeFormatted,
                                        isFromMe = isFromMe,
                                        status = status,
                                        isSeen = isSeen,
                                        messageType = messageType,
                                        attachmentUrl = doc.getString("attachmentUrl"),
                                        fileSizeStr = doc.getString("fileSizeStr"),
                                        upiAmount = doc.getDouble("upiAmount"),
                                        upiTransactionId = doc.getString("upiTransactionId"),
                                        upiStatus = doc.getString("upiStatus"),
                                        voiceDurationSec = doc.getLong("voiceDurationSec")?.toInt(),
                                        audioWaveform = doc.getString("audioWaveform"),
                                        pollQuestion = doc.getString("pollQuestion"),
                                        pollOptionsJson = doc.getString("pollOptionsJson"),
                                        pollVotesJson = doc.getString("pollVotesJson"),
                                        isSecretExpiring = doc.getBoolean("isSecretExpiring") ?: false,
                                        expireTimeMillis = doc.getLong("expireTimeMillis") ?: 0L,
                                        isStarred = doc.getBoolean("isStarred") ?: false,
                                        replyToText = doc.getString("replyToText"),
                                        replyToSender = doc.getString("replyToSender")
                                    )
                                    messagesToInsert.add(entity)

                                    if (!isFromMe) {
                                        if (latestIncomingMsg == null || entity.timestamp > latestIncomingMsg!!.timestamp) {
                                            latestIncomingMsg = entity
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing Firestore message: ${e.message}")
                                }
                            }

                            if (messagesToInsert.isNotEmpty()) {
                                appDatabase.messageDao().insertMessages(messagesToInsert)
                            }

                            // Update chat last message in local Room DB
                            latestIncomingMsg?.let { incoming ->
                                appDatabase.chatDao().updateLastMessageWithStatus(
                                    chatId = chatId,
                                    lastMsg = incoming.text,
                                    time = incoming.timeFormatted,
                                    timeMillis = incoming.timestamp,
                                    status = "DELIVERED",
                                    isFromMe = false
                                )
                            }
                        }
                    }
                }
            activeChatListeners[chatId] = listener
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach Firestore chat listener: ${e.message}")
        }
    }

    fun detachChatListener(chatId: String) {
        activeChatListeners.remove(chatId)?.remove()
    }

    // ==========================================
    // Real-Time Cloud Calling (WebRTC Signaling)
    // ==========================================

    fun initiateCloudCall(
        callId: String,
        callerId: String,
        callerName: String,
        callerAvatar: String,
        receiverName: String,
        isVideo: Boolean,
        onStatusChange: (status: String) -> Unit
    ) {
        val db = firestoreInstance ?: return
        scope.launch {
            try {
                val callData = hashMapOf(
                    "callId" to callId,
                    "callerId" to callerId,
                    "callerName" to callerName,
                    "callerAvatar" to callerAvatar,
                    "receiverName" to receiverName,
                    "isVideo" to isVideo,
                    "status" to "RINGING", // RINGING, ACCEPTED, DECLINED, ENDED
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("active_calls")
                    .document(callId)
                    .set(callData)

                // Listen to status of this call (e.g. when other user accepts/declines)
                db.collection("active_calls")
                    .document(callId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                        val status = snapshot.getString("status") ?: "RINGING"
                        onStatusChange(status)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "initiateCloudCall error: ${e.message}")
            }
        }
    }

    fun listenForIncomingCalls(
        currentUserName: String,
        currentUserPhone: String,
        onIncomingCall: (callId: String, callerName: String, callerAvatar: String, isVideo: Boolean) -> Unit,
        onCallStatusChange: (callId: String, status: String) -> Unit
    ) {
        val db = firestoreInstance ?: return
        callsListener?.remove()

        try {
            callsListener = db.collection("active_calls")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val now = System.currentTimeMillis()

                    for (doc in snapshots.documents) {
                        try {
                            val callId = doc.getString("callId") ?: doc.id
                            val callerName = doc.getString("callerName") ?: "Caller"
                            val callerAvatar = doc.getString("callerAvatar") ?: callerName.take(2).uppercase()
                            val receiverName = doc.getString("receiverName") ?: ""
                            val status = doc.getString("status") ?: "RINGING"
                            val isVideo = doc.getBoolean("isVideo") ?: false
                            val timestamp = doc.getLong("timestamp") ?: 0L

                            // Trigger if call is fresh (within last 60 seconds)
                            val isRecent = (now - timestamp) < 60000

                            if (isRecent) {
                                // If I am not the caller, and call is RINGING
                                val isMeCaller = callerName.equals(currentUserName, ignoreCase = true)
                                if (!isMeCaller && status == "RINGING") {
                                    onIncomingCall(callId, callerName, callerAvatar, isVideo)
                                } else {
                                    onCallStatusChange(callId, status)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing call document: ${e.message}")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "listenForIncomingCalls error: ${e.message}")
        }
    }

    fun updateCloudCallStatus(callId: String, status: String) {
        val db = firestoreInstance ?: return
        scope.launch {
            try {
                db.collection("active_calls")
                    .document(callId)
                    .update("status", status)
            } catch (e: Exception) {
                Log.e(TAG, "updateCloudCallStatus error: ${e.message}")
            }
        }
    }

    // ==========================================
    // Cloud User Discovery & Profile Sync
    // ==========================================

    fun publishUserProfile(profile: UserProfile) {
        val db = firestoreInstance ?: return
        scope.launch {
            try {
                val userId = if (profile.phone.isNotBlank()) profile.phone.replace(" ", "").replace("+", "") else profile.bharatId
                val userData = hashMapOf(
                    "name" to profile.name,
                    "phone" to profile.phone,
                    "bharatId" to profile.bharatId,
                    "avatarInitial" to profile.avatarInitial,
                    "avatarColorHex" to profile.avatarColorHex,
                    "statusBio" to profile.statusBio,
                    "upiVpa" to profile.upiVpa,
                    "lastSeen" to System.currentTimeMillis()
                )
                db.collection("users")
                    .document(userId)
                    .set(userData, SetOptions.merge())
            } catch (e: Exception) {
                Log.e(TAG, "publishUserProfile error: ${e.message}")
            }
        }
    }

    fun listenForCloudUsers(appDatabase: AppDatabase, currentUserName: String) {
        val db = firestoreInstance ?: return
        usersListener?.remove()

        try {
            usersListener = db.collection("users")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    scope.launch {
                        val contacts = mutableListOf<ContactEntity>()
                        for (doc in snapshots.documents) {
                            try {
                                val name = doc.getString("name") ?: ""
                                if (name.isBlank() || name.equals(currentUserName, ignoreCase = true)) continue

                                val phone = doc.getString("phone") ?: ""
                                val avatarInitial = doc.getString("avatarInitial") ?: name.take(2).uppercase()
                                val avatarColorHex = doc.getString("avatarColorHex") ?: "#FF671F"
                                val statusBio = doc.getString("statusBio") ?: "Available on Bharat Chat"
                                val upiVpa = doc.getString("upiVpa") ?: ""
                                val id = "contact_${name.lowercase().replace(" ", "_")}"

                                val contact = ContactEntity(
                                    id = id,
                                    name = name,
                                    phone = phone,
                                    upiVpa = upiVpa,
                                    avatarInitial = avatarInitial,
                                    avatarColorHex = avatarColorHex,
                                    statusMsg = statusBio,
                                    isBharatChatUser = true
                                )
                                contacts.add(contact)

                                // Also ensure a chat entity exists for this user so chatting is instant
                                val chatId = "chat_${name.lowercase().replace(" ", "_")}"
                                val existingChat = appDatabase.chatDao().getChatById(chatId)
                                if (existingChat == null) {
                                    val newChat = ChatEntity(
                                        id = chatId,
                                        title = name,
                                        subtitle = statusBio,
                                        avatarInitial = avatarInitial,
                                        avatarColorHex = avatarColorHex,
                                        isOnline = true,
                                        lastMessage = "Connected on Bharat Chat 🇮🇳",
                                        lastMessageTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                                        timestamp = System.currentTimeMillis()
                                    )
                                    appDatabase.chatDao().insertChat(newChat)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing user: ${e.message}")
                            }
                        }
                        if (contacts.isNotEmpty()) {
                            appDatabase.contactDao().insertContacts(contacts)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "listenForCloudUsers error: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirestoreManager? = null

        fun getInstance(context: Context): FirestoreManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

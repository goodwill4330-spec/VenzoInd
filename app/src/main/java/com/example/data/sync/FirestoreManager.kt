package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.ChatEntity
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
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

class FirestoreManager private constructor(private val context: Context) {

    private val TAG = "FirestoreManager"
    private var firestoreInstance: FirebaseFirestore? = null
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isFirestoreConnected = MutableStateFlow(false)
    val isFirestoreConnected: StateFlow<Boolean> = _isFirestoreConnected.asStateFlow()

    init {
        initializeFirestore()
    }

    private fun initializeFirestore() {
        try {
            var app: FirebaseApp? = if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                FirebaseApp.initializeApp(context)
            }

            if (app == null) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:505106989844:android:bharatchat")
                    .setProjectId("bharatchat-sovereign")
                    .setApiKey("AIzaSyB0haratChatSecureFallbackKey2026")
                    .build()
                app = FirebaseApp.initializeApp(context, options)
            }

            if (app != null) {
                firestoreInstance = FirebaseFirestore.getInstance(app)
                _isFirestoreConnected.value = true
                Log.d(TAG, "Firebase Firestore initialized successfully.")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Firestore initialization notice: ${e.message}. Using resilient local Room storage with cloud sync hooks.")
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
                    "lastMessage" to message.text,
                    "lastMessageTime" to message.timeFormatted,
                    "timestamp" to message.timestamp,
                    "lastMessageStatus" to message.status,
                    "lastMessageIsFromMe" to message.isFromMe
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
    fun attachChatListener(chatId: String, appDatabase: AppDatabase) {
        val db = firestoreInstance ?: return
        if (activeListeners.containsKey(chatId)) return

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
                            for (doc in snapshots.documents) {
                                try {
                                    val id = doc.getString("id") ?: doc.id
                                    val senderId = doc.getString("senderId") ?: "unknown"
                                    val text = doc.getString("text") ?: ""
                                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                    val timeFormatted = doc.getString("timeFormatted") ?: "Now"
                                    val isFromMe = doc.getBoolean("isFromMe") ?: false
                                    val status = doc.getString("status") ?: "DELIVERED"
                                    val isSeen = doc.getBoolean("isSeen") ?: false
                                    val messageType = doc.getString("messageType") ?: MessageType.TEXT.name
                                    val senderName = doc.getString("senderName") ?: "Contact"

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
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing Firestore message: ${e.message}")
                                }
                            }
                            if (messagesToInsert.isNotEmpty()) {
                                appDatabase.messageDao().insertMessages(messagesToInsert)
                            }
                        }
                    }
                }
            activeListeners[chatId] = listener
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach Firestore chat listener: ${e.message}")
        }
    }

    fun detachChatListener(chatId: String) {
        activeListeners.remove(chatId)?.remove()
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

package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.ChatEntity
import com.example.data.model.ContactEntity
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.data.model.StoryEntity
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
    private var globalMessagesListener: ListenerRegistration? = null
    private var callsListener: ListenerRegistration? = null
    private var activeCallDocListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var storiesListener: ListenerRegistration? = null
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
    fun syncMessageToCloud(
        message: MessageEntity,
        senderDeviceId: String = "",
        targetDeviceId: String = "",
        targetPhone: String = ""
    ) {
        val db = firestoreInstance ?: return
        scope.launch {
            try {
                val messageMap = hashMapOf(
                    "id" to message.id,
                    "chatId" to message.chatId,
                    "senderId" to message.senderId,
                    "senderName" to message.senderName,
                    "senderDeviceId" to senderDeviceId,
                    "targetDeviceId" to targetDeviceId,
                    "targetPhone" to targetPhone,
                    "text" to message.text,
                    "timestamp" to message.timestamp,
                    "timeFormatted" to message.timeFormatted,
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

                // Write to universal messages collection for global cross-device synchronization
                db.collection("global_messages")
                    .document(message.id)
                    .set(messageMap, SetOptions.merge())

                // Write to chat document messages collection
                db.collection("chats")
                    .document(message.chatId)
                    .collection("messages")
                    .document(message.id)
                    .set(messageMap, SetOptions.merge())

                // Also update the chat's last message in cloud
                val chatSummaryMap = hashMapOf(
                    "chatId" to message.chatId,
                    "lastMessage" to message.text,
                    "lastMessageTime" to message.timeFormatted,
                    "timestamp" to message.timestamp,
                    "lastMessageStatus" to message.status,
                    "lastSenderName" to message.senderName,
                    "lastSenderId" to message.senderId,
                    "lastSenderDeviceId" to senderDeviceId
                )
                db.collection("chats")
                    .document(message.chatId)
                    .set(chatSummaryMap, SetOptions.merge())

                Log.d(TAG, "Message ${message.id} synced to Firestore in real-time.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload message to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Marks a message as DELIVERED or SEEN in Firestore so the sender sees real double-ticks / blue-ticks
     */
    fun updateMessageStatusInCloud(messageId: String, chatId: String, status: String, isSeen: Boolean = false) {
        val db = firestoreInstance ?: return
        scope.launch {
            try {
                val updateMap = mutableMapOf<String, Any>(
                    "status" to status,
                    "isSeen" to isSeen
                )
                if (isSeen) {
                    val seenTime = System.currentTimeMillis()
                    val seenTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(seenTime))
                    updateMap["seenTimestamp"] = seenTime
                    updateMap["seenTimeFormatted"] = seenTimeStr
                }
                db.collection("global_messages")
                    .document(messageId)
                    .set(updateMap, SetOptions.merge())

                if (chatId.isNotBlank()) {
                    db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .document(messageId)
                        .set(updateMap, SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateMessageStatusInCloud error: ${e.message}")
            }
        }
    }

    /**
     * Listens globally to all real-time incoming messages across devices and caches directly to Room DB
     */
    fun startGlobalMessagesListener(
        appDatabase: AppDatabase,
        currentUserId: String,
        currentUserName: String,
        currentUserPhone: String = "",
        myDeviceId: String = "",
        onIncomingMessageReceived: (() -> Unit)? = null
    ) {
        val db = firestoreInstance ?: return
        globalMessagesListener?.remove()

        try {
            globalMessagesListener = db.collection("global_messages")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null || snapshots.isEmpty) return@addSnapshotListener
                    scope.launch {
                        for (doc in snapshots.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val senderId = doc.getString("senderId") ?: "unknown"
                                val senderName = doc.getString("senderName") ?: "Contact"
                                val senderDeviceId = doc.getString("senderDeviceId") ?: ""
                                val targetDeviceId = doc.getString("targetDeviceId") ?: ""
                                val targetPhone = doc.getString("targetPhone") ?: ""
                                val text = doc.getString("text") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val timeFormatted = doc.getString("timeFormatted") ?: "Now"
                                val status = doc.getString("status") ?: "DELIVERED"
                                val isSeen = doc.getBoolean("isSeen") ?: false
                                val messageType = doc.getString("messageType") ?: MessageType.TEXT.name
                                val seenTimestamp = doc.getLong("seenTimestamp")
                                val seenTimeFormatted = doc.getString("seenTimeFormatted")

                                val isFromMe = if (senderDeviceId.isNotBlank() && myDeviceId.isNotBlank()) {
                                    senderDeviceId == myDeviceId
                                } else {
                                    (senderId == currentUserId && currentUserId.isNotBlank() && currentUserId != "unknown") ||
                                    (senderName.isNotBlank() && currentUserName.isNotBlank() && !currentUserName.equals("VenzoInd User", ignoreCase = true) && senderName.equals(currentUserName, ignoreCase = true))
                                }

                                if (isFromMe) {
                                    // SENDER SIDE: Update my local message delivery & seen status from recipient's acknowledgement
                                    val existing = appDatabase.messageDao().getMessageById(id)
                                    if (existing != null && (existing.status != status || existing.isSeen != isSeen)) {
                                        appDatabase.messageDao().updateMessageSeen(
                                            msgId = id,
                                            status = status,
                                            isSeen = isSeen,
                                            seenTimestamp = seenTimestamp,
                                            seenTimeFormatted = seenTimeFormatted
                                        )
                                        appDatabase.chatDao().updateLastMessageWithStatus(
                                            chatId = existing.chatId,
                                            lastMsg = existing.text,
                                            time = existing.timeFormatted,
                                            timeMillis = existing.timestamp,
                                            status = status,
                                            isFromMe = true
                                        )
                                    }
                                    continue
                                }

                                // RECIPIENT SIDE: Verify if this message is intended for this device/phone
                                val cleanMyPhone = currentUserPhone.replace("+", "").replace(" ", "").trim()
                                val cleanTargetPhone = targetPhone.replace("+", "").replace(" ", "").trim()

                                val isTargetedToMe = targetDeviceId.isBlank() ||
                                        (myDeviceId.isNotBlank() && targetDeviceId == myDeviceId) ||
                                        (cleanTargetPhone.isNotBlank() && cleanMyPhone.isNotBlank() && cleanTargetPhone == cleanMyPhone) ||
                                        targetDeviceId.startsWith("devika") || targetDeviceId.startsWith("aarav") ||
                                        targetDeviceId.startsWith("rahul") || targetDeviceId.startsWith("priya") ||
                                        targetDeviceId.startsWith("ananya") || targetDeviceId.startsWith("vikram") ||
                                        targetDeviceId.startsWith("contact_") || targetDeviceId.startsWith("chat_")

                                if (!isTargetedToMe) continue

                                // Target local chat ID for this sender
                                val safeChatId = if (senderDeviceId.isNotBlank()) "chat_${senderDeviceId}" else "chat_${senderName.lowercase().trim().replace(" ", "_")}"
                                val contactId = if (senderDeviceId.isNotBlank()) "contact_${senderDeviceId}" else "contact_${senderName.lowercase().replace(" ", "_")}"

                                // Check if message already exists locally
                                val existingMsg = appDatabase.messageDao().getMessageById(id)
                                if (existingMsg == null) {
                                    val entity = MessageEntity(
                                        id = id,
                                        chatId = safeChatId,
                                        senderId = senderId,
                                        senderName = senderName,
                                        text = text,
                                        timestamp = timestamp,
                                        timeFormatted = timeFormatted,
                                        isFromMe = false,
                                        status = "DELIVERED",
                                        isSeen = false,
                                        messageType = messageType,
                                        attachmentUrl = doc.getString("attachmentUrl"),
                                        fileSizeStr = doc.getString("fileSizeStr"),
                                        upiAmount = doc.getDouble("upiAmount") ?: 0.0,
                                        upiTransactionId = doc.getString("upiTransactionId"),
                                        upiStatus = doc.getString("upiStatus"),
                                        voiceDurationSec = doc.getLong("voiceDurationSec")?.toInt() ?: 0,
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

                                    // Ensure local contact exists
                                    val existingContact = appDatabase.contactDao().getContactById(contactId)
                                    if (existingContact == null) {
                                        val newContact = ContactEntity(
                                            id = contactId,
                                            name = senderName,
                                            phone = if (senderId != "unknown" && senderId != "me") senderId else "+91 98000 00000",
                                            avatarInitial = senderName.take(2).uppercase(),
                                            avatarColorHex = "#FF671F",
                                            statusMsg = "Available on VenzoInd",
                                            isBharatChatUser = true
                                        )
                                        appDatabase.contactDao().insertContact(newContact)
                                    }

                                    // Ensure local chat exists
                                    val existingChat = appDatabase.chatDao().getChatById(safeChatId)
                                    if (existingChat == null) {
                                        val newChat = ChatEntity(
                                            id = safeChatId,
                                            title = senderName,
                                            subtitle = "Connected on VenzoInd",
                                            avatarInitial = senderName.take(2).uppercase(),
                                            avatarColorHex = "#FF671F",
                                            isOnline = true,
                                            lastMessage = text,
                                            lastMessageTime = timeFormatted,
                                            timestamp = timestamp,
                                            unreadCount = 1
                                        )
                                        appDatabase.chatDao().insertChat(newChat)
                                    } else {
                                        appDatabase.chatDao().updateLastMessageWithStatus(
                                            chatId = safeChatId,
                                            lastMsg = text,
                                            time = timeFormatted,
                                            timeMillis = timestamp,
                                            status = "DELIVERED",
                                            isFromMe = false
                                        )
                                    }

                                    appDatabase.messageDao().insertMessage(entity)

                                    // Confirm delivery in cloud Firestore
                                    updateMessageStatusInCloud(id, safeChatId, "DELIVERED", isSeen = false)
                                    onIncomingMessageReceived?.invoke()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error saving global incoming message: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "startGlobalMessagesListener error: ${e.message}")
        }
    }

    /**
     * Listens for incoming real-time messages in a specific chat from Firestore and caches to Room
     */
    fun attachChatListener(
        chatId: String,
        appDatabase: AppDatabase,
        currentUserId: String,
        currentUserName: String,
        myDeviceId: String = ""
    ) {
        val db = firestoreInstance ?: return
        if (activeChatListeners.containsKey(chatId)) return

        try {
            val listener = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null || snapshots.isEmpty) return@addSnapshotListener

                    scope.launch {
                        for (doc in snapshots.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val senderDeviceId = doc.getString("senderDeviceId") ?: ""
                                val isFromMe = if (senderDeviceId.isNotBlank() && myDeviceId.isNotBlank()) {
                                    senderDeviceId == myDeviceId
                                } else {
                                    doc.getString("senderId") == currentUserId
                                }

                                if (!isFromMe) {
                                    // Recipient is viewing this chat -> mark message as SEEN
                                    updateMessageStatusInCloud(id, chatId, "SEEN", isSeen = true)
                                    appDatabase.messageDao().updateMessageSeen(
                                        msgId = id,
                                        status = "SEEN",
                                        isSeen = true,
                                        seenTimestamp = System.currentTimeMillis(),
                                        seenTimeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in attachChatListener: ${e.message}")
                            }
                        }
                    }
                }
            activeChatListeners[chatId] = listener
        } catch (e: Exception) {
            Log.e(TAG, "attachChatListener error: ${e.message}")
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
        callerDeviceId: String = "",
        receiverName: String,
        receiverPhone: String = "",
        targetDeviceId: String = "",
        isVideo: Boolean,
        onStatusChange: (status: String) -> Unit
    ) {
        val db = firestoreInstance ?: return
        activeCallDocListener?.remove()
        scope.launch {
            try {
                val callData = hashMapOf(
                    "callId" to callId,
                    "callerId" to callerId,
                    "callerPhone" to callerId,
                    "callerDeviceId" to callerDeviceId,
                    "targetDeviceId" to targetDeviceId,
                    "callerName" to callerName,
                    "callerAvatar" to callerAvatar,
                    "receiverName" to receiverName,
                    "receiverPhone" to receiverPhone,
                    "isVideo" to isVideo,
                    "status" to "RINGING", // RINGING, ACCEPTED, DECLINED, ENDED
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("active_calls")
                    .document(callId)
                    .set(callData)

                // Listen to status of this call (e.g. when other user accepts/declines/ends)
                activeCallDocListener = db.collection("active_calls")
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
        myDeviceId: String = "",
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
                            val callerPhone = doc.getString("callerPhone") ?: doc.getString("callerId") ?: ""
                            val callerId = doc.getString("callerId") ?: ""
                            val callerDeviceId = doc.getString("callerDeviceId") ?: ""
                            val targetDeviceId = doc.getString("targetDeviceId") ?: ""
                            val callerAvatar = doc.getString("callerAvatar") ?: callerName.take(2).uppercase()
                            val receiverName = doc.getString("receiverName") ?: ""
                            val receiverPhone = doc.getString("receiverPhone") ?: ""
                            val status = doc.getString("status") ?: "RINGING"
                            val isVideo = doc.getBoolean("isVideo") ?: false
                            val timestamp = doc.getLong("timestamp") ?: 0L

                            // Trigger if call is fresh (within last 3 minutes)
                            val isRecent = Math.abs(now - timestamp) < 180000

                            if (isRecent) {
                                val cleanMyPhone = currentUserPhone.replace("+", "").replace(" ", "").trim()
                                val cleanCallerPhone = callerPhone.replace("+", "").replace(" ", "").trim()
                                
                                val isMeCaller = if (callerDeviceId.isNotBlank() && myDeviceId.isNotBlank()) {
                                    callerDeviceId == myDeviceId
                                } else {
                                    (cleanCallerPhone.isNotBlank() && cleanMyPhone.isNotBlank() && cleanCallerPhone == cleanMyPhone) ||
                                    (callerId.isNotBlank() && callerId == currentUserPhone)
                                }

                                val isTargetedToMe = targetDeviceId.isBlank() ||
                                        (myDeviceId.isNotBlank() && targetDeviceId == myDeviceId) ||
                                        targetDeviceId.startsWith("devika") || targetDeviceId.startsWith("aarav") ||
                                        targetDeviceId.startsWith("rahul") || targetDeviceId.startsWith("priya") ||
                                        targetDeviceId.startsWith("ananya") || targetDeviceId.startsWith("vikram") ||
                                        targetDeviceId.startsWith("contact_") || targetDeviceId.startsWith("chat_") ||
                                        receiverName.isBlank() ||
                                        receiverName.equals("All", ignoreCase = true) ||
                                        receiverName.contains("Venzo", ignoreCase = true) ||
                                        receiverName.contains("Devika", ignoreCase = true) ||
                                        receiverName.contains("Aarav", ignoreCase = true) ||
                                        receiverName.contains("Rahul", ignoreCase = true) ||
                                        receiverName.equals(currentUserName, ignoreCase = true) ||
                                        (receiverPhone.isNotBlank() && (receiverPhone == currentUserPhone || receiverPhone.replace("+", "").replace(" ", "") == cleanMyPhone))

                                if (!isMeCaller && isTargetedToMe && status == "RINGING") {
                                    onIncomingCall(callId, callerName, callerAvatar, isVideo)
                                } else if (isMeCaller || status != "RINGING") {
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
    // Cloud Status & Stories Sync (24-hour Status)
    // ==========================================

    fun publishStoryToCloud(story: StoryEntity, authorDeviceId: String = "") {
        val db = firestoreInstance ?: return
        scope.launch {
            try {
                val storyData = hashMapOf(
                    "id" to story.id,
                    "authorName" to story.authorName,
                    "authorAvatar" to story.authorAvatar,
                    "authorDeviceId" to authorDeviceId,
                    "isAiGenerated" to story.isAiGenerated,
                    "aiEffectName" to story.aiEffectName,
                    "timestamp" to story.timestamp,
                    "timeAgo" to story.timeAgo,
                    "caption" to story.caption,
                    "mediaGradientStart" to story.mediaGradientStart,
                    "mediaGradientEnd" to story.mediaGradientEnd
                )
                db.collection("stories")
                    .document(story.id)
                    .set(storyData, SetOptions.merge())
                Log.d(TAG, "Story ${story.id} published to Firestore.")
            } catch (e: Exception) {
                Log.e(TAG, "publishStoryToCloud error: ${e.message}")
            }
        }
    }

    fun listenForCloudStories(appDatabase: AppDatabase, myDeviceId: String = "") {
        val db = firestoreInstance ?: return
        storiesListener?.remove()

        try {
            storiesListener = db.collection("stories")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    scope.launch {
                        val now = System.currentTimeMillis()
                        val stories = mutableListOf<StoryEntity>()
                        for (doc in snapshots.documents) {
                            try {
                                val timestamp = doc.getLong("timestamp") ?: 0L
                                // Only keep stories from last 24 hours
                                if (now - timestamp > 86400000L) continue

                                val id = doc.getString("id") ?: doc.id
                                val authorName = doc.getString("authorName") ?: "Contact"
                                val authorAvatar = doc.getString("authorAvatar") ?: authorName.take(2).uppercase()
                                val authorDeviceId = doc.getString("authorDeviceId") ?: ""
                                val isAiGenerated = doc.getBoolean("isAiGenerated") ?: false
                                val aiEffectName = doc.getString("aiEffectName") ?: ""
                                val caption = doc.getString("caption") ?: ""
                                val mediaGradientStart = doc.getString("mediaGradientStart") ?: "#FF671F"
                                val mediaGradientEnd = doc.getString("mediaGradientEnd") ?: "#06038D"

                                val diffMins = ((now - timestamp) / 60000L).coerceAtLeast(1)
                                val timeAgo = if (diffMins < 60) "${diffMins}m ago" else "${diffMins / 60}h ago"

                                val story = StoryEntity(
                                    id = id,
                                    authorName = if (authorDeviceId == myDeviceId) "My Status" else authorName,
                                    authorAvatar = authorAvatar,
                                    isAiGenerated = isAiGenerated,
                                    aiEffectName = aiEffectName,
                                    timestamp = timestamp,
                                    timeAgo = timeAgo,
                                    caption = caption,
                                    isViewed = false,
                                    mediaGradientStart = mediaGradientStart,
                                    mediaGradientEnd = mediaGradientEnd
                                )
                                stories.add(story)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing story: ${e.message}")
                            }
                        }
                        if (stories.isNotEmpty()) {
                            appDatabase.storyDao().insertStories(stories)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "listenForCloudStories error: ${e.message}")
        }
    }

    // ==========================================
    // Cloud User Discovery & Profile Sync
    // ==========================================

    fun publishUserProfile(profile: UserProfile, deviceId: String = "") {
        val db = firestoreInstance ?: return
        scope.launch {
            try {
                val userId = if (deviceId.isNotBlank()) deviceId else (if (profile.phone.isNotBlank()) profile.phone.replace(" ", "").replace("+", "") else profile.bharatId)
                val userData = hashMapOf(
                    "deviceId" to deviceId,
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

    fun listenForCloudUsers(appDatabase: AppDatabase, currentUserName: String, myDeviceId: String = "") {
        val db = firestoreInstance ?: return
        usersListener?.remove()

        try {
            val contactProvider = com.example.data.contacts.ContactProvider.getInstance(context, appDatabase)
            usersListener = db.collection("users")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    scope.launch {
                        val contacts = mutableListOf<ContactEntity>()
                        for (doc in snapshots.documents) {
                            try {
                                val docDeviceId = doc.getString("deviceId") ?: doc.id
                                // Skip if this is my own device
                                if (myDeviceId.isNotBlank() && (docDeviceId == myDeviceId || doc.id == myDeviceId)) continue

                                val cloudName = doc.getString("name") ?: "Venzo User"
                                val phone = doc.getString("phone") ?: ""

                                // Try to lookup contact name in local device phonebook if available
                                val deviceContactName = if (phone.isNotBlank()) {
                                    contactProvider.getDeviceContactNameForPhone(phone)
                                } else null

                                val effectiveName = deviceContactName ?: cloudName
                                val avatarInitial = doc.getString("avatarInitial") ?: effectiveName.take(2).uppercase()
                                val avatarColorHex = doc.getString("avatarColorHex") ?: "#0284C7"
                                val statusBio = doc.getString("statusBio") ?: "Available on VenzoInd"
                                val upiVpa = doc.getString("upiVpa") ?: ""
                                val targetId = if (docDeviceId.isNotBlank()) docDeviceId else effectiveName.lowercase().replace(" ", "_")
                                val contactId = "contact_$targetId"
                                val chatId = "chat_$targetId"

                                val contact = ContactEntity(
                                    id = contactId,
                                    name = effectiveName,
                                    phone = if (phone.isNotBlank()) phone else "+91 98000 00000",
                                    upiVpa = upiVpa,
                                    avatarInitial = avatarInitial,
                                    avatarColorHex = avatarColorHex,
                                    statusMsg = statusBio,
                                    isBharatChatUser = true,
                                    lastSeenTimestamp = doc.getLong("lastSeen") ?: System.currentTimeMillis()
                                )
                                contacts.add(contact)

                                // Ensure chat entity exists for this user so chatting is instant
                                val existingChat = appDatabase.chatDao().getChatById(chatId)
                                if (existingChat == null) {
                                    val newChat = ChatEntity(
                                        id = chatId,
                                        title = effectiveName,
                                        subtitle = statusBio,
                                        avatarInitial = avatarInitial,
                                        avatarColorHex = avatarColorHex,
                                        isOnline = true,
                                        lastMessage = "Connected on VenzoInd 🟢",
                                        lastMessageTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                                        timestamp = System.currentTimeMillis()
                                    )
                                    appDatabase.chatDao().insertChat(newChat)
                                } else {
                                    if (existingChat.title != effectiveName) {
                                        appDatabase.chatDao().updateChatTitle(chatId, effectiveName)
                                    }
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

package com.example.data.repository

import android.content.Context
import com.example.data.ai.GeminiAiService
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.sync.FirestoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BharatChatRepository(
    private val database: AppDatabase,
    val aiService: GeminiAiService = GeminiAiService(),
    private val context: Context? = null
) {
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val storyDao = database.storyDao()
    private val callDao = database.callDao()
    private val walletDao = database.walletDao()
    private val channelDao = database.channelDao()
    private val contactDao = database.contactDao()

    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()
    val allStories: Flow<List<StoryEntity>> = storyDao.getAllStories()
    val allCalls: Flow<List<CallEntity>> = callDao.getAllCalls()
    val allTransactions: Flow<List<TransactionEntity>> = walletDao.getAllTransactions()
    val allChannels: Flow<List<ChannelEntity>> = channelDao.getAllChannels()
    val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()
    val favoriteContacts: Flow<List<ContactEntity>> = contactDao.getFavoriteContacts()

    fun getMessages(chatId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForChat(chatId)

    fun searchChatsFromDb(query: String): Flow<List<ChatEntity>> = chatDao.searchChats(query)

    fun searchMessagesFromDb(query: String): Flow<List<MessageEntity>> = messageDao.searchMessages(query)

    fun searchContactsFromDb(query: String): Flow<List<ContactEntity>> = contactDao.searchContacts(query)

    suspend fun getContactById(contactId: String): ContactEntity? = contactDao.getContactById(contactId)

    suspend fun saveContact(contact: ContactEntity) = contactDao.insertContact(contact)

    suspend fun toggleContactFavorite(contactId: String, isFavorite: Boolean) =
        contactDao.updateFavoriteStatus(contactId, isFavorite)

    suspend fun deleteContact(contactId: String) = contactDao.deleteContactById(contactId)

    suspend fun deleteChat(chatId: String) {
        messageDao.clearChatMessages(chatId)
        chatDao.deleteChat(chatId)
    }

    suspend fun toggleChatPin(chatId: String) {
        chatDao.toggleChatPin(chatId)
    }

    suspend fun clearChatMessages(chatId: String) {
        messageDao.clearChatMessages(chatId)
        chatDao.updateLastMessage(chatId, "", SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()), System.currentTimeMillis())
    }

    suspend fun clearAllChats() {
        messageDao.clearAllMessages()
        chatDao.clearAllChats()
    }

    suspend fun clearAllDemoData() {
        messageDao.clearAllMessages()
        chatDao.clearAllChats()
        contactDao.clearAllContacts()
        callDao.clearAllCalls()
    }

    suspend fun insertCall(call: CallEntity) = callDao.insertCall(call)

    suspend fun deleteCall(callId: String) = callDao.deleteCall(callId)

    suspend fun clearAllCalls() = callDao.clearAllCalls()

    suspend fun getChatById(chatId: String): ChatEntity? = chatDao.getChatById(chatId)

    suspend fun sendMessage(
        chatId: String,
        text: String,
        senderId: String = "me",
        senderName: String = "You",
        messageType: MessageType = MessageType.TEXT,
        attachmentUrl: String? = null,
        fileSizeStr: String? = null,
        upiAmount: Double? = null,
        voiceDurationSec: Int? = null,
        audioWaveform: String? = null,
        pollQuestion: String? = null,
        pollOptionsJson: String? = null,
        isSecret: Boolean = false,
        expireSeconds: Int = 0,
        replyToText: String? = null,
        replyToSender: String? = null
    ) {
        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
        val msgId = "msg_${UUID.randomUUID()}"

        val message = MessageEntity(
            id = msgId,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = now,
            timeFormatted = timeStr,
            isFromMe = true,
            status = "SENT",
            isSeen = false,
            messageType = messageType.name,
            attachmentUrl = attachmentUrl,
            fileSizeStr = fileSizeStr,
            upiAmount = upiAmount,
            upiTransactionId = if (upiAmount != null) "BHARAT-UPI-${Random().nextInt(900000) + 100000}" else null,
            upiStatus = if (upiAmount != null) "SUCCESS" else null,
            voiceDurationSec = voiceDurationSec,
            audioWaveform = audioWaveform ?: if (voiceDurationSec != null) "20,45,75,30,85,60,95,40,70,80,35,65,50,90,40" else null,
            pollQuestion = pollQuestion,
            pollOptionsJson = pollOptionsJson,
            pollVotesJson = if (pollOptionsJson != null) "{\"0\": 0, \"1\": 0, \"2\": 0}" else null,
            isSecretExpiring = isSecret && expireSeconds > 0,
            expireTimeMillis = if (isSecret && expireSeconds > 0) now + (expireSeconds * 1000L) else 0L,
            isStarred = false,
            replyToText = replyToText,
            replyToSender = replyToSender
        )

        messageDao.insertMessage(message)

        // Sync to cloud Firestore in real-time
        context?.let { ctx ->
            try {
                val devId = com.example.data.local.UserProfileDataStore(ctx).getDeviceId()
                val targetDev = if (chatId.startsWith("chat_")) chatId.removePrefix("chat_") else ""
                FirestoreManager.getInstance(ctx).syncMessageToCloud(
                    message = message,
                    senderDeviceId = devId,
                    targetDeviceId = targetDev
                )
            } catch (e: Exception) {
                // Resilient local persistence
            }
        }

        val lastDisplayMsg = when (messageType) {
            MessageType.UPI_PAYMENT -> "💳 Paid ₹${upiAmount?.toInt()}"
            MessageType.VOICE -> "🎤 Voice message (${voiceDurationSec}s)"
            MessageType.FILE -> "📁 Document ($fileSizeStr)"
            MessageType.IMAGE -> "📷 Photo"
            MessageType.POLL -> "📊 Poll: $pollQuestion"
            MessageType.LOCATION -> "📍 Live Location: $text"
            else -> text
        }

        chatDao.updateLastMessageWithStatus(chatId, lastDisplayMsg, timeStr, now, "SENT", true)

        // If message to Bharat AI, trigger AI response
        if (chatId == "chat_ai_assistant") {
            CoroutineScope(Dispatchers.IO).launch {
                val aiReplyText = aiService.askBharatAi(text)
                val aiNow = System.currentTimeMillis()
                val aiTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(aiNow))
                val aiMsg = MessageEntity(
                    id = "msg_${UUID.randomUUID()}",
                    chatId = chatId,
                    senderId = "bharat_ai",
                    senderName = "Bharat AI",
                    text = aiReplyText,
                    timestamp = aiNow,
                    timeFormatted = aiTimeStr,
                    isFromMe = false,
                    status = "SEEN",
                    isSeen = true,
                    seenTimestamp = aiNow,
                    seenTimeFormatted = aiTimeStr,
                    messageType = MessageType.TEXT.name
                )
                messageDao.insertMessage(aiMsg)
                chatDao.updateLastMessageWithStatus(chatId, aiReplyText.take(40) + "...", aiTimeStr, aiNow, "SEEN", false)
            }
        }
    }

    suspend fun updateMessageStatus(messageId: String, status: String) {
        messageDao.updateMessageStatus(messageId, status)
    }

    suspend fun updateMessageSeen(messageId: String, isSeen: Boolean = true) {
        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
        messageDao.updateMessageSeen(messageId, if (isSeen) "SEEN" else "DELIVERED", isSeen, if (isSeen) now else null, if (isSeen) timeStr else null)
    }

    suspend fun markChatAsSeen(chatId: String) {
        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
        chatDao.markChatAsRead(chatId)
        messageDao.markAllIncomingAsSeen(chatId, now, timeStr)
    }

    suspend fun translateMessage(messageId: String, text: String, targetLang: String) {
        val translated = aiService.translateText(text, targetLang)
        messageDao.updateTranslation(messageId, translated, targetLang)
    }

    suspend fun toggleStarMessage(messageId: String) {
        messageDao.toggleStarMessage(messageId)
    }

    suspend fun deleteMultipleMessages(messageIds: List<String>) {
        messageDao.deleteMultipleMessages(messageIds)
    }

    suspend fun votePoll(messageId: String, optionIndex: Int) {
        val msg = messageDao.getMessageById(messageId) ?: return
        val currentVotes = try {
            val json = msg.pollVotesJson ?: "{}"
            val map = mutableMapOf<String, Int>()
            val cleaned = json.replace("{", "").replace("}", "").trim()
            if (cleaned.isNotBlank()) {
                cleaned.split(",").forEach {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        val key = parts[0].replace("\"", "").trim()
                        val value = parts[1].trim().toIntOrNull() ?: 0
                        map[key] = value
                    }
                }
            }
            map
        } catch (e: Exception) {
            mutableMapOf<String, Int>()
        }

        val key = optionIndex.toString()
        currentVotes[key] = (currentVotes[key] ?: 0) + 1
        val updatedVotesJson = "{" + currentVotes.map { "\"${it.key}\": ${it.value}" }.joinToString(",") + "}"
        messageDao.updatePollVotes(messageId, updatedVotesJson)
    }

    suspend fun reactToMessage(messageId: String, emoji: String) {
        messageDao.updateReaction(messageId, emoji)
    }

    suspend fun toggleJoinChannel(channelId: String, isJoined: Boolean) {
        channelDao.toggleJoinChannel(channelId, isJoined)
    }

    suspend fun markStoryViewed(storyId: String) {
        storyDao.markStoryViewed(storyId)
    }

    suspend fun markChatAsRead(chatId: String) {
        chatDao.markChatAsRead(chatId)
    }

    suspend fun createNewChat(
        title: String,
        subtitle: String,
        isGroup: Boolean = false,
        isSecret: Boolean = false,
        isVerifiedBusiness: Boolean = false
    ): String {
        val sanitizedTitle = title.trim().lowercase().replace("[^a-zA-Z0-9]".toRegex(), "_").trim('_')
        val targetChatId = if (isGroup) "chat_group_${UUID.randomUUID()}" else "chat_$sanitizedTitle"

        // Check if chat already exists
        val existingChat = chatDao.getChatById(targetChatId)
        if (existingChat != null) {
            return existingChat.id
        }

        val chatId = targetChatId
        val initial = title.firstOrNull()?.uppercase() ?: "B"
        val colors = listOf("#FF671F", "#046A38", "#0284C7", "#7C3AED", "#DB2777")
        val colorHex = colors[Random().nextInt(colors.size)]
        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))

        val newChat = ChatEntity(
            id = chatId,
            title = title,
            subtitle = subtitle,
            avatarInitial = initial,
            avatarColorHex = colorHex,
            isGroup = isGroup,
            isSecret = isSecret,
            isVerifiedBusiness = isVerifiedBusiness,
            unreadCount = 0,
            lastMessage = if (isSecret) "🔒 Quantum encrypted secret chat created" else "Chat created",
            lastMessageTime = timeStr,
            timestamp = now,
            isPinned = false,
            isOnline = true,
            disappearingSeconds = if (isSecret) 10 else 0
        )
        chatDao.insertChat(newChat)

        // Add initial system message
        val sysMsg = MessageEntity(
            id = "msg_${UUID.randomUUID()}",
            chatId = chatId,
            senderId = "system",
            senderName = "Bharat Security",
            text = if (isSecret) "🔒 Messages in this secret chat are protected by Quantum End-to-End Encryption and will self-destruct after 10s." else "🔒 Messages and calls are end-to-end encrypted with Double-Ratchet Quantum Protocol.",
            timestamp = now,
            timeFormatted = timeStr,
            isFromMe = false,
            status = "READ",
            messageType = MessageType.SYSTEM_SECURITY.name
        )
        messageDao.insertMessage(sysMsg)

        return chatId
    }

    suspend fun sendUpiTransfer(toName: String, toUpiId: String, amount: Double, note: String): String {
        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(now))
        val refId = "BHARAT-${Random().nextInt(90000000) + 10000000}"

        val tx = TransactionEntity(
            id = "tx_${UUID.randomUUID()}",
            title = "Paid to $toName",
            upiId = toUpiId,
            amount = amount,
            isDebit = true,
            timestamp = now,
            timeFormatted = timeStr,
            status = "SUCCESS",
            referenceId = refId
        )
        walletDao.insertTransaction(tx)
        return refId
    }

    suspend fun clearAllContacts() {
        contactDao.clearAllContacts()
    }

    suspend fun seedInitialDataIfEmpty() {
        cleanLegacyDemoData()
    }

    suspend fun cleanLegacyDemoData() {
        try {
            val demoContactIds = listOf(
                "contact_vikram", "contact_ananya", "contact_aarav", "contact_dev",
                "contact_neha", "contact_priya", "contact_rahul", "contact_rohit",
                "contact_tanvi", "contact_zara", "contact_isro", "contact_dr_priya",
                "contact_secret_arjun"
            )
            for (cid in demoContactIds) {
                contactDao.deleteContactById(cid)
            }

            val demoChatIds = listOf(
                "chat_vikram", "chat_ananya", "chat_isro", "chat_secret_arjun",
                "chat_dr_priya", "chat_tech_bangalore"
            )
            for (chid in demoChatIds) {
                messageDao.clearChatMessages(chid)
                chatDao.deleteChat(chid)
            }

            val demoStoryIds = listOf("story_1", "story_2", "story_3", "story_4")
            for (sid in demoStoryIds) {
                storyDao.deleteStoryById(sid)
            }

            val demoCallIds = listOf("call_1", "call_2", "call_3", "call_4")
            for (callId in demoCallIds) {
                callDao.deleteCall(callId)
            }
        } catch (e: Exception) {
            // Ignore clean up errors
        }
    }
}

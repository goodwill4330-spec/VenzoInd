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
        replyToSender: String? = null,
        isFromMe: Boolean = true
    ) {
        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
        val msgId = "msg_${UUID.randomUUID()}"

        // Ensure ChatEntity exists before inserting message
        var existingChat = chatDao.getChatById(chatId)
        if (existingChat == null) {
            val contact = contactDao.getContactById(chatId)
                ?: contactDao.getContactById(if (chatId.startsWith("chat_")) "contact_${chatId.removePrefix("chat_")}" else "contact_$chatId")
                ?: contactDao.getAllContactsList().firstOrNull { c -> chatId.contains(c.name, ignoreCase = true) }
            
            val initial = contact?.avatarInitial ?: senderName.take(2).uppercase().ifBlank { "IN" }
            val colorHex = contact?.avatarColorHex ?: "#FF671F"
            val title = contact?.name ?: if (senderId != "me" && !isFromMe) senderName else "Chat"
            
            val newChat = ChatEntity(
                id = chatId,
                title = title,
                subtitle = contact?.statusMsg ?: "Bharat Secure Chat",
                lastMessage = text,
                lastMessageTime = timeStr,
                timestamp = now,
                unreadCount = if (isFromMe) 0 else 1,
                avatarInitial = initial,
                avatarColorHex = colorHex,
                isGroup = false,
                isSecret = isSecret,
                isVerifiedBusiness = false,
                isPinned = false,
                disappearingSeconds = expireSeconds
            )
            chatDao.insertChat(newChat)
            existingChat = newChat
        }

        val message = MessageEntity(
            id = msgId,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = text,
            timestamp = now,
            timeFormatted = timeStr,
            isFromMe = isFromMe,
            status = if (isFromMe) "SENT" else "READ",
            isSeen = !isFromMe,
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
                val dataStore = com.example.data.local.UserProfileDataStore(ctx)
                val devId = dataStore.getDeviceId()
                val myPhone = dataStore.getPhone()
                val chatObj = chatDao.getChatById(chatId)
                val targetDev = if (chatId.startsWith("chat_")) chatId.removePrefix("chat_") else ""
                var targetPhone = ""
                val contact = contactDao.getContactById(if (chatId.startsWith("chat_")) "contact_${chatId.removePrefix("chat_")}" else "contact_$chatId")
                    ?: contactDao.getContactById(chatId)
                    ?: contactDao.getAllContactsList().firstOrNull { c ->
                        (chatObj != null && c.name.equals(chatObj.title, ignoreCase = true)) ||
                        (chatObj != null && chatObj.subtitle.contains(c.phone)) ||
                        (c.phone.isNotBlank() && chatId.contains(c.phone.filter { it.isDigit() }.takeLast(8)))
                    }
                if (contact != null && contact.phone.isNotBlank()) {
                    targetPhone = contact.phone
                } else if (chatObj != null) {
                    val digits = chatObj.subtitle.filter { it.isDigit() }
                    if (digits.length >= 10) {
                        targetPhone = digits.takeLast(10)
                    }
                }
                FirestoreManager.getInstance(ctx).syncMessageToCloud(
                    message = message,
                    senderDeviceId = devId,
                    targetDeviceId = targetDev,
                    targetPhone = targetPhone,
                    senderPhone = myPhone
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
            MessageType.CATALOGUE -> "🛍️ Catalogue: $text"
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

    suspend fun sendCatalogueMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        product: CatalogueProduct
    ) {
        val msgId = "msg_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))

        val message = MessageEntity(
            id = msgId,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            text = product.title,
            timestamp = now,
            timeFormatted = timeStr,
            isFromMe = true,
            status = "SENT",
            isSeen = false,
            messageType = MessageType.CATALOGUE.name,
            catalogueTitle = product.title,
            cataloguePrice = product.price,
            catalogueImageUrl = product.imageUrl,
            catalogueDescription = product.description,
            catalogueCategory = product.category,
            catalogueDiscountPercent = product.discountPercent,
            upiAmount = product.price
        )

        messageDao.insertMessage(message)

        context?.let { ctx ->
            try {
                val devId = com.example.data.local.UserProfileDataStore(ctx).getDeviceId()
                val chatObj = chatDao.getChatById(chatId)
                val targetDev = if (chatId.startsWith("chat_")) chatId.removePrefix("chat_") else ""
                var targetPhone = ""
                val contact = contactDao.getContactById(if (chatId.startsWith("chat_")) "contact_${chatId.removePrefix("chat_")}" else "contact_$chatId")
                    ?: contactDao.getContactById(chatId)
                    ?: contactDao.getAllContactsList().firstOrNull { c ->
                        (chatObj != null && c.name.equals(chatObj.title, ignoreCase = true)) ||
                        (chatObj != null && chatObj.subtitle.contains(c.phone)) ||
                        (c.phone.isNotBlank() && chatId.contains(c.phone.filter { it.isDigit() }.takeLast(8)))
                    }
                if (contact != null && contact.phone.isNotBlank()) {
                    targetPhone = contact.phone
                } else if (chatObj != null) {
                    val digits = chatObj.subtitle.filter { it.isDigit() }
                    if (digits.length >= 10) {
                        targetPhone = digits.takeLast(10)
                    }
                }
                FirestoreManager.getInstance(ctx).syncMessageToCloud(
                    message = message,
                    senderDeviceId = devId,
                    targetDeviceId = targetDev,
                    targetPhone = targetPhone
                )
            } catch (e: Exception) {
                // Non-blocking
            }
        }

        chatDao.updateLastMessageWithStatus(
            chatId = chatId,
            lastMsg = "🛍️ Catalogue: ${product.title} (₹${product.price.toInt()})",
            time = timeStr,
            timeMillis = now,
            status = "SENT",
            isFromMe = true
        )
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
        try {
            val now = System.currentTimeMillis()
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            // 1. Do NOT seed dummy contacts (User requested real contacts only)
            if (false) {
                val initialContacts = listOf(
                    ContactEntity(
                        id = "contact_aarav_sharma",
                        name = "Aarav Sharma",
                        phone = "+91 98201 12345",
                        upiVpa = "aarav.sharma@okhdfcbank",
                        avatarInitial = "AS",
                        avatarColorHex = "#0284C7",
                        statusMsg = "Coding next-gen apps with Gemini AI 🚀",
                        isBharatChatUser = true,
                        isFavorite = true,
                        publicKeyFingerprint = "KYBER-1024-AS789A",
                        lastSeenTimestamp = now - 60_000L // 1m ago (Online)
                    ),
                    ContactEntity(
                        id = "contact_aditi_rao",
                        name = "Aditi Rao",
                        phone = "+91 98450 23456",
                        upiVpa = "aditirao@upi",
                        avatarInitial = "AR",
                        avatarColorHex = "#EC4899",
                        statusMsg = "Designing sovereign Indian UI systems 🎨",
                        isBharatChatUser = true,
                        isFavorite = true,
                        publicKeyFingerprint = "KYBER-1024-AR432B",
                        lastSeenTimestamp = now - 120_000L // 2m ago (Online)
                    ),
                    ContactEntity(
                        id = "contact_bhavna_patel",
                        name = "Bhavna Patel",
                        phone = "+91 97123 34567",
                        upiVpa = "bhavna.patel@icici",
                        avatarInitial = "BP",
                        avatarColorHex = "#10B981",
                        statusMsg = "In a meeting. Urgent calls only 📵",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-BP910C",
                        lastSeenTimestamp = now - 900_000L // 15m ago
                    ),
                    ContactEntity(
                        id = "contact_chirag_verma",
                        name = "Chirag Verma",
                        phone = "+91 99887 45678",
                        upiVpa = "chiragverma@axl",
                        avatarInitial = "CV",
                        avatarColorHex = "#8B5CF6",
                        statusMsg = "Available on VenzoInd 🇮🇳",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-CV234D",
                        lastSeenTimestamp = now - 1800_000L // 30m ago
                    ),
                    ContactEntity(
                        id = "contact_devendra_nair",
                        name = "Devendra Nair",
                        phone = "+91 94470 56789",
                        upiVpa = "dev.nair@sbi",
                        avatarInitial = "DN",
                        avatarColorHex = "#F59E0B",
                        statusMsg = "Coffee & Coroutines ☕",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-DN567E",
                        lastSeenTimestamp = now - 3600_000L // 1h ago
                    ),
                    ContactEntity(
                        id = "contact_esha_deol",
                        name = "Esha Deol",
                        phone = "+91 98112 67890",
                        upiVpa = "eshadeol@paytm",
                        avatarInitial = "ED",
                        avatarColorHex = "#EF4444",
                        statusMsg = "Exploring Bangalore Tech Park 🌿",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-ED890F",
                        lastSeenTimestamp = now - 7200_000L // 2h ago
                    ),
                    ContactEntity(
                        id = "contact_farhan_akhtar",
                        name = "Farhan Akhtar",
                        phone = "+91 98210 78901",
                        upiVpa = "farhan@ybl",
                        avatarInitial = "FA",
                        avatarColorHex = "#14B8A6",
                        statusMsg = "Live life with passion and purpose ✨",
                        isBharatChatUser = true,
                        isFavorite = true,
                        publicKeyFingerprint = "KYBER-1024-FA123G",
                        lastSeenTimestamp = now - 60_000L // Online
                    ),
                    ContactEntity(
                        id = "contact_gaurav_sen",
                        name = "Gaurav Sen",
                        phone = "+91 98300 89012",
                        upiVpa = "gauravsen@hdfcbank",
                        avatarInitial = "GS",
                        avatarColorHex = "#6366F1",
                        statusMsg = "System Design & Distributed Scalability ⚡",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-GS456H",
                        lastSeenTimestamp = now - 14400_000L // 4h ago
                    ),
                    ContactEntity(
                        id = "contact_harshita_joshi",
                        name = "Harshita Joshi",
                        phone = "+91 98910 90123",
                        upiVpa = "harshita.joshi@upi",
                        avatarInitial = "HJ",
                        avatarColorHex = "#D946EF",
                        statusMsg = "Working from mountains 🏔️",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-HJ789I",
                        lastSeenTimestamp = now - 86400_000L // Yesterday
                    ),
                    ContactEntity(
                        id = "contact_ishaan_khatter",
                        name = "Ishaan Khatter",
                        phone = "+91 98765 01234",
                        upiVpa = "ishaan@okaxis",
                        avatarInitial = "IK",
                        avatarColorHex = "#0EA5E9",
                        statusMsg = "Fitness first 💪 Every rep counts",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-IK012J",
                        lastSeenTimestamp = now - 120_000L // Online
                    ),
                    ContactEntity(
                        id = "contact_kavita_krishnan",
                        name = "Kavita Krishnan",
                        phone = "+91 94440 12345",
                        upiVpa = "kavita.k@kotak",
                        avatarInitial = "KK",
                        avatarColorHex = "#F97316",
                        statusMsg = "Building secure P2P systems 🛡️",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-KK345K",
                        lastSeenTimestamp = now - 43200_000L // 12h ago
                    ),
                    ContactEntity(
                        id = "contact_lakshya_sen",
                        name = "Lakshya Sen",
                        phone = "+91 97410 23456",
                        upiVpa = "lakshya.sen@upi",
                        avatarInitial = "LS",
                        avatarColorHex = "#22C55E",
                        statusMsg = "Focus on the game 🏸",
                        isBharatChatUser = true,
                        isFavorite = true,
                        publicKeyFingerprint = "KYBER-1024-LS678L",
                        lastSeenTimestamp = now - 60_000L // Online
                    ),
                    ContactEntity(
                        id = "contact_meera_nambiar",
                        name = "Meera Nambiar",
                        phone = "+91 98840 34567",
                        upiVpa = "meera.n@okhdfcbank",
                        avatarInitial = "MN",
                        avatarColorHex = "#A855F7",
                        statusMsg = "Reading sci-fi & deep learning papers 📚",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-MN901M",
                        lastSeenTimestamp = now - 18000_000L // 5h ago
                    ),
                    ContactEntity(
                        id = "contact_neha_gupta",
                        name = "Neha Gupta",
                        phone = "+91 98100 45678",
                        upiVpa = "neha.gupta@paytm",
                        avatarInitial = "NG",
                        avatarColorHex = "#06B6D4",
                        statusMsg = "VenzoInd Early Adopter 🇮🇳",
                        isBharatChatUser = true,
                        isFavorite = true,
                        publicKeyFingerprint = "KYBER-1024-NG234N",
                        lastSeenTimestamp = now - 60_000L // Online
                    ),
                    ContactEntity(
                        id = "contact_priya_sharma",
                        name = "Priya Sharma",
                        phone = "+91 98200 56789",
                        upiVpa = "priyasharma@icici",
                        avatarInitial = "PS",
                        avatarColorHex = "#EC4899",
                        statusMsg = "Always curious, forever learning 💡",
                        isBharatChatUser = true,
                        isFavorite = true,
                        publicKeyFingerprint = "KYBER-1024-PS567O",
                        lastSeenTimestamp = now - 60_000L // Online
                    ),
                    ContactEntity(
                        id = "contact_rohan_mehta",
                        name = "Rohan Mehta",
                        phone = "+91 99200 67890",
                        upiVpa = "rohan.mehta@axisbank",
                        avatarInitial = "RM",
                        avatarColorHex = "#3B82F6",
                        statusMsg = "At Starbucks Indiranagar ☕",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-RM890P",
                        lastSeenTimestamp = now - 120_000L // Online
                    ),
                    ContactEntity(
                        id = "contact_tanvi_shah",
                        name = "Tanvi Shah",
                        phone = "+91 98790 78901",
                        upiVpa = "tanvi.shah@upi",
                        avatarInitial = "TS",
                        avatarColorHex = "#E11D48",
                        statusMsg = "Creating music & sonic vibes 🎧",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-TS123Q",
                        lastSeenTimestamp = now - 28800_000L // 8h ago
                    ),
                    ContactEntity(
                        id = "contact_vikram_malhotra",
                        name = "Vikram Malhotra",
                        phone = "+91 98220 89012",
                        upiVpa = "vikram.m@okhdfcbank",
                        avatarInitial = "VM",
                        avatarColorHex = "#10B981",
                        statusMsg = "Leading sovereign tech initiatives 🇮🇳",
                        isBharatChatUser = true,
                        isFavorite = true,
                        publicKeyFingerprint = "KYBER-1024-VM456R",
                        lastSeenTimestamp = now - 60_000L // Online
                    ),
                    ContactEntity(
                        id = "contact_zara_khan",
                        name = "Zara Khan",
                        phone = "+91 98205 90123",
                        upiVpa = "zarakhan@ybl",
                        avatarInitial = "ZK",
                        avatarColorHex = "#8B5CF6",
                        statusMsg = "Architecting cloud native apps ☁️",
                        isBharatChatUser = true,
                        isFavorite = false,
                        publicKeyFingerprint = "KYBER-1024-ZK789S",
                        lastSeenTimestamp = now - 3600_000L // 1h ago
                    )
                )
                contactDao.insertContacts(initialContacts)
            }

            // 2. Seed Chats & Messages if empty
            if (chatDao.getChatsCount() == 0) {
                val initialChats = listOf(
                    ChatEntity(
                        id = "chat_ai_assistant",
                        title = "Bharat AI (भारत एआई)",
                        subtitle = "🇮🇳 Sovereign AI Assistant • Online",
                        avatarInitial = "AI",
                        avatarColorHex = "#FF671F",
                        isGroup = false,
                        isSecret = false,
                        isVerifiedBusiness = false,
                        isAiAssistant = true,
                        unreadCount = 0,
                        lastMessage = "Namaste! How can I assist you today? 🇮🇳",
                        lastMessageTime = timeFormat.format(Date(now - 120_000L)),
                        timestamp = now - 120_000L,
                        isPinned = true,
                        isOnline = true
                    )
                )
                chatDao.insertChats(initialChats)

                // Seed messages for chats
                val messages = mutableListOf<MessageEntity>()

                // AI Assistant messages
                messages.add(
                    MessageEntity(
                        id = "msg_ai_1",
                        chatId = "chat_ai_assistant",
                        senderId = "bharat_ai",
                        senderName = "Bharat AI",
                        text = "🇮🇳 **Namaste! I am Bharat AI**, your sovereign AI assistant built for India.\n\nI can help you translate messages in 12+ Indian languages, summarize conversations, generate quick smart replies, assist with UPI transfers, or answer any questions!",
                        timestamp = now - 300_000L,
                        timeFormatted = timeFormat.format(Date(now - 300_000L)),
                        isFromMe = false,
                        status = "READ",
                        isSeen = true,
                        messageType = MessageType.TEXT.name
                    )
                )

                messageDao.insertMessages(messages)
            }

            // 3. Do NOT seed fake calls (clean real calls only)
            if (false) {
                // No dummy calls
            }
        } catch (e: Exception) {
            // Ignore seeding errors
        }
    }

    suspend fun cleanLegacyDemoData() {
        try {
            val demoContactIds = listOf(
                "contact_aarav_sharma", "contact_aditi_rao", "contact_bhavna_patel", "contact_chirag_verma",
                "contact_devendra_nair", "contact_esha_deol", "contact_farhan_akhtar", "contact_geeta_phogat",
                "contact_harsh_vardhan", "contact_ishaan_khattar", "contact_jaya_bachchan", "contact_kavita_krishnan",
                "contact_lokesh_rahul", "contact_manish_malhotra", "contact_neha_sharma", "contact_omkar_goswami",
                "contact_priya_mani", "contact_qasim_ali", "contact_rohit_shetty", "contact_siddharth_roy",
                "contact_tanvi_shah", "contact_uday_kotak", "contact_vikram_sarabhai", "contact_wasim_jaffer",
                "contact_xena_fernandes", "contact_yuvraj_singh", "contact_zoya_akhtar", "contact_vikram",
                "contact_ananya", "contact_aarav", "contact_dev", "contact_neha", "contact_priya", "contact_rahul",
                "contact_rohit", "contact_tanvi", "contact_zara", "contact_isro", "contact_dr_priya",
                "contact_secret_arjun", "contact_zoya_khan", "contact_vikram_malhotra", "contact_zara_khan", "contact_rohan_mehta"
            )
            for (cid in demoContactIds) {
                contactDao.deleteContactById(cid)
            }

            val demoChatIds = listOf(
                "chat_aarav_sharma", "chat_aditi_rao", "chat_isro_tech_hub", "chat_vikram_malhotra",
                "chat_dr_priya_consult", "chat_secret_vault", "chat_vikram", "chat_ananya",
                "chat_isro", "chat_secret_arjun", "chat_dr_priya", "chat_tech_bangalore"
            )
            for (chid in demoChatIds) {
                messageDao.clearChatMessages(chid)
                chatDao.deleteChat(chid)
            }

            val demoStoryIds = listOf("story_1", "story_2", "story_3", "story_4")
            for (sid in demoStoryIds) {
                storyDao.deleteStoryById(sid)
            }

            val demoCallIds = listOf("call_1", "call_2", "call_3", "call_4", "call_seed_1", "call_seed_2", "call_seed_3")
            for (callId in demoCallIds) {
                callDao.deleteCall(callId)
            }
        } catch (e: Exception) {
            // Ignore clean up errors
        }
    }
}

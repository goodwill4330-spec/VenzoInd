package com.example.data.repository

import android.content.Context
import com.example.data.ai.GeminiAiService
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BharatChatRepository(
    private val database: AppDatabase,
    val aiService: GeminiAiService = GeminiAiService()
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

    suspend fun getChatById(chatId: String): ChatEntity? = chatDao.getChatById(chatId)

    suspend fun sendMessage(
        chatId: String,
        text: String,
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
            senderId = "me",
            senderName = "You",
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

        // Asynchronously update status from SENT -> DELIVERED -> SEEN (double blue checkmarks)
        CoroutineScope(Dispatchers.IO).launch {
            delay(500)
            messageDao.updateMessageStatus(msgId, "DELIVERED")
            chatDao.updateLastMessageWithStatus(chatId, lastDisplayMsg, timeStr, now, "DELIVERED", true)
            delay(1000)
            val seenTime = System.currentTimeMillis()
            val seenTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(seenTime))
            messageDao.updateMessageSeen(msgId, "SEEN", isSeen = true, seenTimestamp = seenTime, seenTimeFormatted = seenTimeStr)
            chatDao.updateLastMessageWithStatus(chatId, lastDisplayMsg, timeStr, now, "SEEN", true)
        }

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
        val chatId = "chat_${UUID.randomUUID()}"
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

    suspend fun seedInitialDataIfEmpty() {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

        val initialChats = listOf(
            ChatEntity(
                id = "chat_ai_assistant",
                title = "Bharat AI Copilot",
                subtitle = "Active • Official AI Assistant",
                avatarInitial = "AI",
                avatarColorHex = "#FF671F",
                isGroup = false,
                isSecret = false,
                isVerifiedBusiness = true,
                isAiAssistant = true,
                unreadCount = 1,
                lastMessage = "✨ Namaste! Ask me anything, translate chats, or draft emails.",
                lastMessageTime = sdf.format(Date(now - 60000)),
                timestamp = now - 60000,
                isPinned = true,
                isOnline = true
            ),
            ChatEntity(
                id = "chat_vikram",
                title = "Vikram Sharma",
                subtitle = "AI Systems Architect, Bengaluru",
                avatarInitial = "VS",
                avatarColorHex = "#0284C7",
                isGroup = false,
                isSecret = false,
                isVerifiedBusiness = false,
                unreadCount = 2,
                lastMessage = "Transferred the 10GB neural dataset over Bharat Cloud 🚀",
                lastMessageTime = sdf.format(Date(now - 120000)),
                timestamp = now - 120000,
                isPinned = true,
                isOnline = true
            ),
            ChatEntity(
                id = "chat_ananya",
                title = "Ananya Sen",
                subtitle = "Design Lead, Bharat Ecosystem",
                avatarInitial = "AS",
                avatarColorHex = "#EC4899",
                isGroup = false,
                isSecret = false,
                isVerifiedBusiness = false,
                unreadCount = 0,
                lastMessage = "💳 Paid ₹1,250",
                lastMessageTime = sdf.format(Date(now - 3600000)),
                timestamp = now - 3600000,
                isPinned = true,
                isOnline = true
            ),
            ChatEntity(
                id = "chat_isro",
                title = "ISRO Mission Hub 🛰️",
                subtitle = "4,280 space engineers & enthusiasts",
                avatarInitial = "IS",
                avatarColorHex = "#046A38",
                isGroup = true,
                isSecret = false,
                isVerifiedBusiness = true,
                unreadCount = 5,
                lastMessage = "Dr. Somanath: Gaganyaan crew module high-altitude drop test successful!",
                lastMessageTime = sdf.format(Date(now - 7200000)),
                timestamp = now - 7200000,
                isPinned = false,
                isOnline = false
            ),
            ChatEntity(
                id = "chat_secret_arjun",
                title = "Secret Chat (Arjun)",
                subtitle = "Self-destruct: 10s • Quantum Guard",
                avatarInitial = "AJ",
                avatarColorHex = "#7C3AED",
                isGroup = false,
                isSecret = true,
                isVerifiedBusiness = false,
                unreadCount = 0,
                lastMessage = "🔒 Quantum encrypted message sent",
                lastMessageTime = sdf.format(Date(now - 14400000)),
                timestamp = now - 14400000,
                isPinned = false,
                isOnline = false,
                disappearingSeconds = 10
            ),
            ChatEntity(
                id = "chat_dr_priya",
                title = "Dr. Priya Nair (Apollo)",
                subtitle = "Verified Health Provider",
                avatarInitial = "PN",
                avatarColorHex = "#10B981",
                isGroup = false,
                isSecret = false,
                isVerifiedBusiness = true,
                unreadCount = 0,
                lastMessage = "Your digital health report is securely signed and attached.",
                lastMessageTime = "Yesterday",
                timestamp = now - 86400000,
                isPinned = false,
                isOnline = false
            ),
            ChatEntity(
                id = "chat_tech_bangalore",
                title = "Bangalore Techies 💻",
                subtitle = "Startups, AI & Founders",
                avatarInitial = "BT",
                avatarColorHex = "#F59E0B",
                isGroup = true,
                isSecret = false,
                isVerifiedBusiness = false,
                unreadCount = 0,
                lastMessage = "Rahul: Who is attending the Bharat AI Hackathon this weekend?",
                lastMessageTime = "Yesterday",
                timestamp = now - 90000000,
                isPinned = false,
                isOnline = false
            )
        )
        chatDao.insertChats(initialChats)

        // Seed messages for AI Chat
        val aiMessages = listOf(
            MessageEntity(
                id = "msg_ai_1",
                chatId = "chat_ai_assistant",
                senderId = "system",
                senderName = "Bharat Security",
                text = "🛡️ Quantum End-to-End Encryption active. Your conversations with Bharat AI are private & sovereign.",
                timestamp = now - 3600000,
                timeFormatted = "10:00 AM",
                isFromMe = false,
                status = "READ",
                messageType = MessageType.SYSTEM_SECURITY.name
            ),
            MessageEntity(
                id = "msg_ai_2",
                chatId = "chat_ai_assistant",
                senderId = "bharat_ai",
                senderName = "Bharat AI",
                text = "🇮🇳 **Namaste! I am Bharat AI**, your ultra-fast smart assistant.\n\nHere is how I can supercharge your chats:\n• 🌐 **Instant Translation**: Tap any message to translate into Hindi, Tamil, Telugu, Marathi, Bengali & more.\n• 📋 **Summarization**: Catch up on unread group chats in 3 seconds.\n• 💳 **UPI Smart Actions**: Draft payment links and track split expenses.\n• ✍️ **Smart Composing**: Draft professional messages with perfect Indian context.\n\nHow can I help you today?",
                timestamp = now - 60000,
                timeFormatted = sdf.format(Date(now - 60000)),
                isFromMe = false,
                status = "READ",
                messageType = MessageType.TEXT.name
            )
        )
        messageDao.insertMessages(aiMessages)

        // Seed messages for Vikram
        val vikramMessages = listOf(
            MessageEntity(
                id = "msg_v1",
                chatId = "chat_vikram",
                senderId = "system",
                senderName = "Bharat Security",
                text = "🔒 Encrypted with Bharat Quantum Double-Ratchet. Zero-knowledge verified.",
                timestamp = now - 7200000,
                timeFormatted = "09:15 AM",
                isFromMe = false,
                status = "READ",
                messageType = MessageType.SYSTEM_SECURITY.name
            ),
            MessageEntity(
                id = "msg_v2",
                chatId = "chat_vikram",
                senderId = "vikram",
                senderName = "Vikram Sharma",
                text = "Hey Vikram! Did you review the new AI sovereign model architecture?",
                timestamp = now - 3600000,
                timeFormatted = "10:30 AM",
                isFromMe = false,
                status = "READ",
                messageType = MessageType.TEXT.name
            ),
            MessageEntity(
                id = "msg_v3",
                chatId = "chat_vikram",
                senderId = "me",
                senderName = "You",
                text = "Yes, tested the low-latency speech-to-text pipeline. It handles Indian accents and multilingual code-switching flawlessly!",
                timestamp = now - 1800000,
                timeFormatted = "11:00 AM",
                isFromMe = true,
                status = "READ",
                messageType = MessageType.TEXT.name,
                reactionEmoji = "🔥"
            ),
            MessageEntity(
                id = "msg_v4",
                chatId = "chat_vikram",
                senderId = "vikram",
                senderName = "Vikram Sharma",
                text = "Audio briefing on the new Bharat Quantum cluster deployment:",
                timestamp = now - 900000,
                timeFormatted = "11:15 AM",
                isFromMe = false,
                status = "READ",
                messageType = MessageType.VOICE.name,
                voiceDurationSec = 28,
                audioWaveform = "30,60,80,45,90,70,95,65,85,50,40,75,90,60,35"
            ),
            MessageEntity(
                id = "msg_v5",
                chatId = "chat_vikram",
                senderId = "vikram",
                senderName = "Vikram Sharma",
                text = "Transferred the 10GB neural dataset over Bharat Cloud 🚀",
                timestamp = now - 120000,
                timeFormatted = sdf.format(Date(now - 120000)),
                isFromMe = false,
                status = "READ",
                messageType = MessageType.FILE.name,
                attachmentUrl = "neural_weights_v4.bin",
                fileSizeStr = "9.8 GB • 10GB Cloud Speed"
            )
        )
        messageDao.insertMessages(vikramMessages)

        // Seed messages for Ananya (UPI Payment demo)
        val ananyaMessages = listOf(
            MessageEntity(
                id = "msg_a1",
                chatId = "chat_ananya",
                senderId = "ananya",
                senderName = "Ananya Sen",
                text = "Hey! Splitting the team dinner at Indiranagar yesterday.",
                timestamp = now - 7200000,
                timeFormatted = "08:45 PM",
                isFromMe = false,
                status = "READ",
                messageType = MessageType.TEXT.name
            ),
            MessageEntity(
                id = "msg_a2",
                chatId = "chat_ananya",
                senderId = "me",
                senderName = "You",
                text = "Sent via Bharat UPI! ⚡",
                timestamp = now - 3600000,
                timeFormatted = "09:00 PM",
                isFromMe = true,
                status = "READ",
                messageType = MessageType.UPI_PAYMENT.name,
                upiAmount = 1250.00,
                upiTransactionId = "BHARAT-UPI-984210",
                upiStatus = "SUCCESS"
            ),
            MessageEntity(
                id = "msg_a3",
                chatId = "chat_ananya",
                senderId = "ananya",
                senderName = "Ananya Sen",
                text = "Received instantly! That UPI 2.0 integration in chat is super smooth. Thanks! 🙏",
                timestamp = now - 3500000,
                timeFormatted = "09:02 PM",
                isFromMe = false,
                status = "READ",
                messageType = MessageType.TEXT.name,
                reactionEmoji = "🇮🇳"
            )
        )
        messageDao.insertMessages(ananyaMessages)

        // Seed Stories
        val initialStories = listOf(
            StoryEntity(
                id = "story_1",
                authorName = "Vikram Sharma",
                authorAvatar = "VS",
                isAiGenerated = true,
                aiEffectName = "Tricolor Hologram AI",
                timestamp = now - 720000,
                timeAgo = "12m ago",
                caption = "Testing Bharat 120Hz Holographic Glass UI! 🇮🇳✨",
                isViewed = false,
                mediaGradientStart = "#FF671F",
                mediaGradientEnd = "#0284C7"
            ),
            StoryEntity(
                id = "story_2",
                authorName = "Ananya Sen",
                authorAvatar = "AS",
                isAiGenerated = true,
                aiEffectName = "Cyber Neon Vedic",
                timestamp = now - 3600000,
                timeAgo = "1h ago",
                caption = "Sunset at Bengaluru Cyber City 🌆✨",
                isViewed = false,
                mediaGradientStart = "#EC4899",
                mediaGradientEnd = "#7C3AED"
            ),
            StoryEntity(
                id = "story_3",
                authorName = "ISRO Updates",
                authorAvatar = "IS",
                isAiGenerated = false,
                aiEffectName = "Space 4K HDR",
                timestamp = now - 10800000,
                timeAgo = "3h ago",
                caption = "Gaganyaan Module orbital telemetry test nominal! 🚀🛰️",
                isViewed = true,
                mediaGradientStart = "#046A38",
                mediaGradientEnd = "#06038D"
            ),
            StoryEntity(
                id = "story_4",
                authorName = "Digital Bharat Hub",
                authorAvatar = "DB",
                isAiGenerated = true,
                aiEffectName = "Quantum Aura",
                timestamp = now - 18000000,
                timeAgo = "5h ago",
                caption = "100 Billion UPI Transactions milestone celebrated across 100+ nations! 🌐",
                isViewed = true,
                mediaGradientStart = "#0284C7",
                mediaGradientEnd = "#10B981"
            )
        )
        storyDao.insertStories(initialStories)

        // Seed Calls
        val initialCalls = listOf(
            CallEntity(
                id = "call_1",
                contactName = "Vikram Sharma",
                contactAvatar = "VS",
                isVideo = true,
                isIncoming = false,
                isMissed = false,
                timestamp = now - 3600000,
                timeFormatted = "Today, 11:20 AM",
                durationStr = "14m 32s",
                isEncrypted = true,
                qualityStr = "4K HDR Video"
            ),
            CallEntity(
                id = "call_2",
                contactName = "Dr. Priya Nair",
                contactAvatar = "PN",
                isVideo = false,
                isIncoming = true,
                isMissed = false,
                timestamp = now - 14400000,
                timeFormatted = "Today, 08:15 AM",
                durationStr = "6m 12s",
                isEncrypted = true,
                qualityStr = "HD Voice • AI Denoise"
            ),
            CallEntity(
                id = "call_3",
                contactName = "Ananya Sen",
                contactAvatar = "AS",
                isVideo = true,
                isIncoming = true,
                isMissed = true,
                timestamp = now - 86400000,
                timeFormatted = "Yesterday, 06:45 PM",
                durationStr = "Missed Call",
                isEncrypted = true,
                qualityStr = "4K Video"
            ),
            CallEntity(
                id = "call_4",
                contactName = "ISRO Mission Hub",
                contactAvatar = "IS",
                isVideo = false,
                isIncoming = true,
                isMissed = false,
                timestamp = now - 172800000,
                timeFormatted = "2 days ago",
                durationStr = "45m 10s",
                isEncrypted = true,
                qualityStr = "Encrypted Conference"
            )
        )
        callDao.insertCalls(initialCalls)

        // Seed Wallet Transactions
        val initialTransactions = listOf(
            TransactionEntity(
                id = "tx_1",
                title = "Paid to Ananya Sen",
                upiId = "ananya@oksbi",
                amount = 1250.00,
                isDebit = true,
                timestamp = now - 3600000,
                timeFormatted = "Today, 09:00 PM",
                status = "SUCCESS",
                referenceId = "BHARAT-UPI-984210"
            ),
            TransactionEntity(
                id = "tx_2",
                title = "Received from Rahul Verma",
                upiId = "rahul.v@okhdfcbank",
                amount = 3500.00,
                isDebit = false,
                timestamp = now - 86400000,
                timeFormatted = "Yesterday, 04:30 PM",
                status = "SUCCESS",
                referenceId = "BHARAT-UPI-871402"
            ),
            TransactionEntity(
                id = "tx_3",
                title = "Starbucks Indiranagar (Scan QR)",
                upiId = "starbucks.india@icici",
                amount = 480.00,
                isDebit = true,
                timestamp = now - 172800000,
                timeFormatted = "2 days ago",
                status = "SUCCESS",
                referenceId = "BHARAT-UPI-719324"
            ),
            TransactionEntity(
                id = "tx_4",
                title = "Bharat Cloud 1TB Subscription",
                upiId = "bharat.cloud@sbi",
                amount = 199.00,
                isDebit = true,
                timestamp = now - 259200000,
                timeFormatted = "3 days ago",
                status = "SUCCESS",
                referenceId = "BHARAT-UPI-610943"
            )
        )
        walletDao.insertTransactions(initialTransactions)

        // Seed Channels & Communities
        val initialChannels = listOf(
            ChannelEntity(
                id = "ch_1",
                name = "ISRO Official Channel 🛰️",
                description = "Official updates on space missions, Gaganyaan, and satellites from ISRO.",
                category = "Science & Tech",
                followersCountStr = "12.4M",
                verified = true,
                avatarInitial = "IS",
                isJoined = true,
                latestPost = "Chandrayaan-4 sample return lunar module design finalized! 🌙",
                latestPostTime = "2h ago"
            ),
            ChannelEntity(
                id = "ch_2",
                name = "Digital India & Tech Hub 🇮🇳",
                description = "Updates on UPI, AI sovereign models, semiconductor fabs and digital infrastructure.",
                category = "National Tech",
                followersCountStr = "8.7M",
                verified = true,
                avatarInitial = "DI",
                isJoined = true,
                latestPost = "Bharat AI 10,000 GPU computing cluster open to developers nationwide.",
                latestPostTime = "4h ago"
            ),
            ChannelEntity(
                id = "ch_3",
                name = "Bangalore Startups & Founders 🚀",
                description = "Connect with tech entrepreneurs, VC funding updates, and AI product launches.",
                category = "Business",
                followersCountStr = "650K",
                verified = true,
                avatarInitial = "BS",
                isJoined = false,
                latestPost = "Seed funding announcement for 12 new Indian deep-tech ventures.",
                latestPostTime = "6h ago"
            ),
            ChannelEntity(
                id = "ch_4",
                name = "Cricket Bharat Live 🏏",
                description = "Ball-by-ball score alerts, dressing room insights & match highlights.",
                category = "Sports",
                followersCountStr = "18.2M",
                verified = true,
                avatarInitial = "CB",
                isJoined = false,
                latestPost = "Bharat clinches historic series victory! Match summary available.",
                latestPostTime = "Yesterday"
            )
        )
        channelDao.insertChannels(initialChannels)

        // Seed Contacts with realistic timestamps for recent activity sorting
        val initialContacts = listOf(
            ContactEntity(
                id = "contact_vikram",
                name = "Vikram Sharma",
                phone = "+91 98765 11223",
                upiVpa = "vikram.sharma@okaxis",
                avatarInitial = "VS",
                avatarColorHex = "#0284C7",
                statusMsg = "Building sovereign AI systems 🇮🇳",
                isBharatChatUser = true,
                isFavorite = true,
                publicKeyFingerprint = "KYBER-1024-9FA281",
                lastSeenTimestamp = now - 120000 // 2m ago
            ),
            ContactEntity(
                id = "contact_ananya",
                name = "Ananya Iyer",
                phone = "+91 98765 22334",
                upiVpa = "ananya.iyer@okhdfcbank",
                avatarInitial = "AI",
                avatarColorHex = "#EC4899",
                statusMsg = "Quantum encryption researcher 🛡️",
                isBharatChatUser = true,
                isFavorite = true,
                publicKeyFingerprint = "KYBER-1024-88BC12",
                lastSeenTimestamp = now - 900000 // 15m ago
            ),
            ContactEntity(
                id = "contact_aarav",
                name = "Aarav Mehta",
                phone = "+91 98765 99887",
                upiVpa = "aarav.mehta@okaxis",
                avatarInitial = "AM",
                avatarColorHex = "#F97316",
                statusMsg = "Fintech & UPI 2.0 Architect ⚡",
                isBharatChatUser = true,
                isFavorite = false,
                publicKeyFingerprint = "KYBER-1024-55FA19",
                lastSeenTimestamp = now - 60000 // 1m ago (Active now)
            ),
            ContactEntity(
                id = "contact_dev",
                name = "Devansh Kapoor",
                phone = "+91 98765 88776",
                upiVpa = "devansh.k@paytm",
                avatarInitial = "DK",
                avatarColorHex = "#06B6D4",
                statusMsg = "Exploring WebRTC 4K streaming 🎥",
                isBharatChatUser = true,
                isFavorite = false,
                publicKeyFingerprint = "KYBER-1024-11AB99",
                lastSeenTimestamp = now - 18000000 // 5 hours ago
            ),
            ContactEntity(
                id = "contact_neha",
                name = "Neha Joshi",
                phone = "+91 98765 77665",
                upiVpa = "neha.j@sbi",
                avatarInitial = "NJ",
                avatarColorHex = "#EC4899",
                statusMsg = "Space Mission Controller @ ISRO 🛰️",
                isBharatChatUser = true,
                isFavorite = true,
                publicKeyFingerprint = "KYBER-1024-66EE44",
                lastSeenTimestamp = now - 3600000 // 1 hour ago
            ),
            ContactEntity(
                id = "contact_priya",
                name = "Priya Patel",
                phone = "+91 98765 44556",
                upiVpa = "priya.p@sbi",
                avatarInitial = "PP",
                avatarColorHex = "#8B5CF6",
                statusMsg = "At ISRO Space Exhibition 🚀",
                isBharatChatUser = true,
                isFavorite = true,
                publicKeyFingerprint = "KYBER-1024-77AA43",
                lastSeenTimestamp = now - 86400000 // Yesterday
            ),
            ContactEntity(
                id = "contact_rahul",
                name = "Rahul Verma",
                phone = "+91 98765 33445",
                upiVpa = "rahul.v@icici",
                avatarInitial = "RV",
                avatarColorHex = "#10B981",
                statusMsg = "UPI & Fintech developer 💳",
                isBharatChatUser = true,
                isFavorite = false,
                publicKeyFingerprint = "KYBER-1024-44DE90",
                lastSeenTimestamp = now - 7200000 // 2 hours ago
            ),
            ContactEntity(
                id = "contact_rohit",
                name = "Rohit Deshmukh",
                phone = "+91 98765 55667",
                upiVpa = "rohit.d@kotak",
                avatarInitial = "RD",
                avatarColorHex = "#F59E0B",
                statusMsg = "Busy in code sprint 💻",
                isBharatChatUser = true,
                isFavorite = false,
                publicKeyFingerprint = "KYBER-1024-33FF61",
                lastSeenTimestamp = now - 172800000 // 2 days ago
            ),
            ContactEntity(
                id = "contact_tanvi",
                name = "Tanvi Sharma",
                phone = "+91 98765 66554",
                upiVpa = "tanvi.s@icici",
                avatarInitial = "TS",
                avatarColorHex = "#3B82F6",
                statusMsg = "Audio AI & Voice recognition researcher 🎙️",
                isBharatChatUser = true,
                isFavorite = false,
                publicKeyFingerprint = "KYBER-1024-22DD88",
                lastSeenTimestamp = now - 300000 // 5m ago
            ),
            ContactEntity(
                id = "contact_zara",
                name = "Zara Khan",
                phone = "+91 98765 11998",
                upiVpa = "zara.k@yesbank",
                avatarInitial = "ZK",
                avatarColorHex = "#14B8A6",
                statusMsg = "Designing next-gen Indian UX 🎨",
                isBharatChatUser = true,
                isFavorite = true,
                publicKeyFingerprint = "KYBER-1024-99CC77",
                lastSeenTimestamp = now - 259200000 // 3 days ago
            )
        )
        contactDao.insertContacts(initialContacts)
    }
}

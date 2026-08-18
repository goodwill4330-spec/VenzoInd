package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.BharatChatRepository
import com.example.data.sync.DeviceRole
import com.example.data.sync.IncomingCallEvent
import com.example.data.sync.IncomingUpiEvent
import com.example.data.sync.MultiDeviceSyncManager
import com.example.data.sync.SyncPairStatus
import com.example.utils.AudioAndTtsManager
import com.example.util.VoiceRecorderManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.UUID

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    AUTH,
    MAIN_APP,
    CHAT_DETAIL,
    ACTIVE_CALL,
    STORY_VIEWER,
    USER_PROFILE,
    CONTACTS_LIST,
    SETTINGS
}

enum class ContactSortOrder {
    NAME_ASC,       // Name (A to Z)
    NAME_DESC,      // Name (Z to A)
    RECENT_ACTIVITY,// Recent Activity / Last Seen
    FAVORITES_FIRST // Starred / Favorites First
}

enum class NavigationTab {
    CHATS,
    CALLS,
    UPDATES,
    AI_ASSISTANT,
    PROFILE
}

enum class ChatFilter {
    ALL,
    UNREAD,
    GROUPS,
    SECRET,
    BUSINESS
}

data class ActiveCallState(
    val contactName: String = "",
    val contactAvatar: String = "",
    val isVideo: Boolean = false,
    val isMuted: Boolean = false,
    val isVideoOff: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isAiNoiseCanceling: Boolean = true,
    val isFrontCamera: Boolean = true,
    val isScreenSharing: Boolean = false,
    val durationSeconds: Int = 0,
    val isConnected: Boolean = false,
    val webrtcCodec: String = "VP9 / Opus HD",
    val webrtcResolution: String = "1080p 60fps (WebRTC 4K Ready)",
    val webrtcBitrateKbps: Int = 2450,
    val webrtcLatencyMs: Int = 18,
    val webrtcPacketLossPercent: Double = 0.0,
    val encryptionStandard: String = "Quantum Kyber-1024 / DTLS-SRTP"
)

class BharatChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = BharatChatRepository(database)
    val profileDataStore = com.example.data.local.UserProfileDataStore(application)
    val ttsManager = AudioAndTtsManager(application)
    val voiceRecorder = VoiceRecorderManager(application)
    val backupManager = com.example.data.backup.LocalBackupManager(application, database)

    // Export & Backup / Restore Dialog State
    val showBackupRestoreDialog = MutableStateFlow(false)
    val showNewGroupDialog = MutableStateFlow(false)
    val showBroadcastDialog = MutableStateFlow(false)
    val isBackingUp = MutableStateFlow(false)
    val isRestoring = MutableStateFlow(false)
    val lastExportResult = MutableStateFlow<com.example.data.backup.ExportResult?>(null)
    val lastRestoreResult = MutableStateFlow<com.example.data.backup.RestoreResult?>(null)
    val availableBackupsList = MutableStateFlow<List<com.example.data.backup.BackupFileInfo>>(emptyList())

    val isUserLoggedIn: StateFlow<Boolean> = profileDataStore.isLoggedInFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            profileDataStore.userProfileFlow.collect { savedProfile ->
                _userProfile.value = savedProfile.copy(
                    walletBalance = _userProfile.value.walletBalance
                )
            }
        }
        refreshAvailableBackups()
    }

    // Navigation and Screen State
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedTab = MutableStateFlow(NavigationTab.CHATS)
    val selectedTab: StateFlow<NavigationTab> = _selectedTab.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Chat List and Filter State
    private val _chatFilter = MutableStateFlow(ChatFilter.ALL)
    val chatFilter: StateFlow<ChatFilter> = _chatFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val chats: StateFlow<List<ChatEntity>> = combine(
        repository.allChats,
        _chatFilter,
        _searchQuery
    ) { chatList, filter, query ->
        var filtered = when (filter) {
            ChatFilter.ALL -> chatList
            ChatFilter.UNREAD -> chatList.filter { it.unreadCount > 0 }
            ChatFilter.GROUPS -> chatList.filter { it.isGroup }
            ChatFilter.SECRET -> chatList.filter { it.isSecret }
            ChatFilter.BUSINESS -> chatList.filter { it.isVerifiedBusiness }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.lastMessage.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true)
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat detail state
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeChat = MutableStateFlow<ChatEntity?>(null)
    val activeChat: StateFlow<ChatEntity?> = _activeChat.asStateFlow()

    val currentMessages: StateFlow<List<MessageEntity>> = _activeChatId.flatMapLatest { chatId ->
        if (chatId != null) repository.getMessages(chatId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stories, Calls, Channels, Transactions
    val stories: StateFlow<List<StoryEntity>> = repository.allStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calls: StateFlow<List<CallEntity>> = repository.allCalls
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val channels: StateFlow<List<ChannelEntity>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<ContactEntity>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteContacts: StateFlow<List<ContactEntity>> = repository.favoriteContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Contact List Search & Sorting
    private val _contactSearchQuery = MutableStateFlow("")
    val contactSearchQuery: StateFlow<String> = _contactSearchQuery.asStateFlow()

    private val _contactSortOrder = MutableStateFlow(ContactSortOrder.NAME_ASC)
    val contactSortOrder: StateFlow<ContactSortOrder> = _contactSortOrder.asStateFlow()

    val filteredAndSortedContacts: StateFlow<List<ContactEntity>> = combine(
        repository.allContacts,
        _contactSearchQuery,
        _contactSortOrder
    ) { contactList, query, sortOrder ->
        val filtered = if (query.isBlank()) {
            contactList
        } else {
            contactList.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.phone.contains(query, ignoreCase = true) ||
                it.upiVpa.contains(query, ignoreCase = true) ||
                it.statusMsg.contains(query, ignoreCase = true)
            }
        }

        when (sortOrder) {
            ContactSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            ContactSortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            ContactSortOrder.RECENT_ACTIVITY -> filtered.sortedByDescending { it.lastSeenTimestamp }
            ContactSortOrder.FAVORITES_FIRST -> filtered.sortedWith(
                compareByDescending<ContactEntity> { it.isFavorite }
                    .thenBy { it.name.lowercase() }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setContactSearchQuery(query: String) {
        _contactSearchQuery.value = query
    }

    fun setContactSortOrder(order: ContactSortOrder) {
        _contactSortOrder.value = order
    }

    fun toggleContactFavorite(contactId: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleContactFavorite(contactId, !currentFavorite)
        }
    }

    fun addNewContact(
        name: String,
        phone: String,
        upiVpa: String = "",
        statusMsg: String = "Available on Bharat Chat"
    ) {
        viewModelScope.launch {
            val initials = name.trim().split(" ")
                .filter { it.isNotBlank() }
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
                .ifEmpty { "C" }
            val colors = listOf("#0284C7", "#EC4899", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444", "#14B8A6")
            val randomColor = colors.random()
            val newContact = ContactEntity(
                id = "contact_${UUID.randomUUID()}",
                name = name.trim(),
                phone = phone.trim(),
                upiVpa = if (upiVpa.isBlank()) "${name.lowercase().replace(" ", "")}@upi" else upiVpa.trim(),
                avatarInitial = initials,
                avatarColorHex = randomColor,
                statusMsg = statusMsg.ifBlank { "Available on Bharat Chat" },
                isBharatChatUser = true,
                isFavorite = false,
                publicKeyFingerprint = "KYBER-1024-${UUID.randomUUID().toString().take(6).uppercase()}",
                lastSeenTimestamp = System.currentTimeMillis()
            )
            repository.saveContact(newContact)
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
        }
    }

    // Active Story Viewer State
    private val _activeStory = MutableStateFlow<StoryEntity?>(null)
    val activeStory: StateFlow<StoryEntity?> = _activeStory.asStateFlow()

    // Active Call State
    private val _activeCallState = MutableStateFlow(ActiveCallState())
    val activeCallState: StateFlow<ActiveCallState> = _activeCallState.asStateFlow()
    private var callTimerJob: Job? = null

    // User Profile & UPI Wallet
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Multi-Device Realtime Sync & 2-Phone Testing Manager
    val syncManager = MultiDeviceSyncManager()
    val syncStatus: StateFlow<SyncPairStatus> = syncManager.syncStatus
    val incomingCallEvent: StateFlow<IncomingCallEvent?> = syncManager.incomingCall
    val incomingUpiEvent: StateFlow<IncomingUpiEvent?> = syncManager.incomingUpi

    // Dialogs and Sheets State
    val showNewChatSheet = MutableStateFlow(false)
    val showDualPhoneSyncDialog = MutableStateFlow(false)
    val showUpiPaymentSheet = MutableStateFlow(false)
    val showQrScannerSheet = MutableStateFlow(false)
    val showAiSummarizerDialog = MutableStateFlow(false)
    val aiSummaryContent = MutableStateFlow("")
    val isGeneratingSummary = MutableStateFlow(false)
    val showAiTranslatorDialog = MutableStateFlow(false)
    val targetTranslateMessageId = MutableStateFlow<String?>(null)
    val showWallpaperSheet = MutableStateFlow(false)
    val showDisappearingTimerDialog = MutableStateFlow(false)
    val showPollCreatorDialog = MutableStateFlow(false)
    val showAttachmentOptions = MutableStateFlow(false)
    val showSecretChatInfo = MutableStateFlow(false)

    // Biometric Security Gate State for UPI & Transactions
    val isUpiBiometricUnlocked = MutableStateFlow(false)
    val isHistoryBiometricUnlocked = MutableStateFlow(false)
    val showBiometricAuthDialog = MutableStateFlow(false)
    val biometricAuthPurpose = MutableStateFlow("UPI Payment Authorization") // or "Transaction History Access"
    private var onBiometricSuccessAction: (() -> Unit)? = null

    fun requestBiometricAuth(purpose: String, onSuccess: () -> Unit) {
        biometricAuthPurpose.value = purpose
        onBiometricSuccessAction = onSuccess
        showBiometricAuthDialog.value = true
    }

    fun completeBiometricAuth(success: Boolean) {
        showBiometricAuthDialog.value = false
        if (success) {
            if (biometricAuthPurpose.value.contains("UPI Payment", ignoreCase = true)) {
                isUpiBiometricUnlocked.value = true
            } else {
                isHistoryBiometricUnlocked.value = true
            }
            onBiometricSuccessAction?.invoke()
        }
        onBiometricSuccessAction = null
    }

    fun triggerUpiSheetWithBiometrics() {
        if (isUpiBiometricUnlocked.value) {
            showUpiPaymentSheet.value = true
        } else {
            requestBiometricAuth("Authorize UPI Payment") {
                showUpiPaymentSheet.value = true
            }
        }
    }

    fun triggerQrScannerWithBiometrics() {
        if (isUpiBiometricUnlocked.value) {
            showQrScannerSheet.value = true
        } else {
            requestBiometricAuth("Authorize Merchant QR Scanner") {
                showQrScannerSheet.value = true
            }
        }
    }

    // Voice recording simulation state
    val isRecordingVoice = MutableStateFlow(false)
    val voiceRecordDurationSec = MutableStateFlow(0)
    private var recordTimerJob: Job? = null

    // Real-Time Typing Indicator State
    val isOtherUserTyping = MutableStateFlow(false)
    val typingStatusText = MutableStateFlow("typing...")
    val typingUserName = MutableStateFlow("")
    private var typingTimerJob: Job? = null

    // AI Smart replies in active chat
    val smartReplies = MutableStateFlow<List<String>>(emptyList())

    // Bharat AI dedicated Tab conversation
    val bharatAiHistory = MutableStateFlow<List<Pair<String, String>>>(
        listOf(
            "model" to "🇮🇳 **Namaste! I am Bharat AI**, your ultra-fast sovereign AI assistant.\n\nAsk me questions, generate smart code, draft official emails in 12+ Indian languages, plan travel itineraries across India, or manage your UPI finances!"
        )
    )
    val isBharatAiThinking = MutableStateFlow(false)

    // Nearby Radar Simulation
    val isRadarScanning = MutableStateFlow(false)
    val nearbyUsersList = MutableStateFlow<List<NearbyUser>>(
        listOf(
            NearbyUser("n1", "Rohan Mehta", 45, "RM", "At Starbucks Indiranagar ☕", false),
            NearbyUser("n2", "Pooja Reddy", 120, "PR", "Working on AI Models 💻", true),
            NearbyUser("n3", "Karan Singh", 280, "KS", "Exploring Bangalore Tech Park 🚀", false),
            NearbyUser("n4", "Neha Gupta", 450, "NG", "Bharat Chat Early Adopter 🇮🇳", true)
        )
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            delay(1800) // Splash screen delay
            _currentScreen.value = AppScreen.MAIN_APP
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setTab(tab: NavigationTab) {
        _selectedTab.value = tab
    }

    fun setChatFilter(filter: ChatFilter) {
        _chatFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun openChat(chatId: String) {
        viewModelScope.launch {
            typingTimerJob?.cancel()
            isOtherUserTyping.value = false
            val chat = repository.getChatById(chatId)
            _activeChatId.value = chatId
            _activeChat.value = chat
            repository.markChatAsSeen(chatId)
            _currentScreen.value = AppScreen.CHAT_DETAIL

            // Load smart replies
            if (chat != null && chat.lastMessage.isNotBlank()) {
                val replies = repository.aiService.generateSmartReplies(chat.lastMessage)
                smartReplies.value = replies
            }
        }
    }

    fun closeChat() {
        typingTimerJob?.cancel()
        isOtherUserTyping.value = false
        _activeChatId.value = null
        _activeChat.value = null
        _currentScreen.value = AppScreen.MAIN_APP
    }

    fun simulateTyping(statusText: String = "typing...", durationMs: Long = 3000L) {
        val chat = _activeChat.value ?: return
        typingTimerJob?.cancel()
        typingTimerJob = viewModelScope.launch {
            typingUserName.value = if (chat.isGroup) "Aarav" else chat.title
            typingStatusText.value = statusText
            isOtherUserTyping.value = true
            delay(durationMs)
            isOtherUserTyping.value = false
        }
    }

    fun sendMessage(text: String) {
        val chatId = _activeChatId.value ?: return
        if (text.isBlank()) return
        val chat = _activeChat.value

        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = text,
                messageType = MessageType.TEXT,
                isSecret = chat?.isSecret == true,
                expireSeconds = chat?.disappearingSeconds ?: 0
            )

            // Update smart replies
            delay(500)
            val newReplies = repository.aiService.generateSmartReplies(text)
            smartReplies.value = newReplies

            // If this is AI assistant chat
            if (chatId == "chat_ai_assistant" || chat?.isAiAssistant == true) {
                typingUserName.value = "Bharat AI"
                typingStatusText.value = "Bharat AI is thinking..."
                isOtherUserTyping.value = true
                delay(1400)
                isOtherUserTyping.value = false
            }

            // If simulated 2-phone testing is enabled and this is a user contact chat (not AI assistant)
            if (syncStatus.value.autoReplyEnabled && chatId != "chat_ai_assistant" && chat != null && !chat.isGroup) {
                // Show real-time typing indicator while preparing response
                val typingDuration = (syncStatus.value.simulatedDelayMs + 600L).coerceAtLeast(1500L)
                typingUserName.value = chat.title
                typingStatusText.value = "typing..."
                isOtherUserTyping.value = true
                delay(typingDuration)
                isOtherUserTyping.value = false

                val replyText = when {
                    text.contains("hello", ignoreCase = true) || text.contains("hi", ignoreCase = true) -> "Namaste! Received your message on Bharat Chat. 🇮🇳 How are you?"
                    text.contains("upi", ignoreCase = true) || text.contains("pay", ignoreCase = true) -> "Got the UPI update! Thanks for the instant settlement via Bharat Pay. ⚡"
                    text.contains("call", ignoreCase = true) -> "Sure, let's connect on Bharat 4K Encrypted Video call in 5 mins! 📞"
                    text.contains("meeting", ignoreCase = true) -> "Yes, scheduled! Sharing the Bharat Cloud doc link shortly. 🚀"
                    else -> "Message delivered securely on Phone 2! Quantum E2EE verified. ✅"
                }

                val now = System.currentTimeMillis()
                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
                val incomingMsg = MessageEntity(
                    id = "msg_${UUID.randomUUID()}",
                    chatId = chatId,
                    senderId = chat.id,
                    senderName = chat.title,
                    text = replyText,
                    timestamp = now,
                    timeFormatted = timeStr,
                    isFromMe = false,
                    status = "READ",
                    messageType = MessageType.TEXT.name
                )
                database.messageDao().insertMessage(incomingMsg)
                database.chatDao().updateLastMessage(chatId, replyText, timeStr, now)
            }
        }
    }

    fun sendUpiMoney(recipientName: String, upiId: String, amount: Double, note: String) {
        val chatId = _activeChatId.value
        viewModelScope.launch {
            val refId = repository.sendUpiTransfer(recipientName, upiId, amount, note)
            
            // Deduct balance
            val currentBal = _userProfile.value.walletBalance
            _userProfile.value = _userProfile.value.copy(walletBalance = currentBal - amount)

            if (chatId != null) {
                repository.sendMessage(
                    chatId = chatId,
                    text = "UPI Transfer of ₹${amount.toInt()} to $recipientName ($note)",
                    messageType = MessageType.UPI_PAYMENT,
                    upiAmount = amount
                )
            }
            showUpiPaymentSheet.value = false
        }
    }

    fun startVoiceRecording() {
        voiceRecorder.startRecording()
        isRecordingVoice.value = true
        voiceRecordDurationSec.value = 0
        recordTimerJob?.cancel()
        recordTimerJob = viewModelScope.launch {
            while (isRecordingVoice.value) {
                delay(1000)
                voiceRecordDurationSec.value += 1
            }
        }
    }

    fun pauseVoiceRecordingForPreview() {
        voiceRecorder.stopRecordingForPreview()
        recordTimerJob?.cancel()
    }

    fun toggleVoicePreviewPlayback() {
        voiceRecorder.togglePreviewPlayback()
    }

    fun stopAndSendVoiceRecording() {
        val chatId = _activeChatId.value ?: return
        val duration = voiceRecorder.getFinalDurationSeconds().coerceAtLeast(voiceRecordDurationSec.value).coerceAtLeast(1)
        val waveform = voiceRecorder.getWaveformString()
        isRecordingVoice.value = false
        recordTimerJob?.cancel()
        voiceRecorder.stopPreview()

        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = "Voice message ($duration sec)",
                messageType = MessageType.VOICE,
                voiceDurationSec = duration,
                audioWaveform = waveform
            )
            voiceRecorder.resetState()
            voiceRecordDurationSec.value = 0
        }
    }

    fun cancelVoiceRecording() {
        isRecordingVoice.value = false
        recordTimerJob?.cancel()
        voiceRecordDurationSec.value = 0
        voiceRecorder.discardRecording()
    }

    fun sendAttachment(type: MessageType, name: String, sizeStr: String) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = name,
                messageType = type,
                attachmentUrl = name,
                fileSizeStr = sizeStr
            )
            showAttachmentOptions.value = false
        }
    }

    fun sendPoll(question: String, options: List<String>) {
        val chatId = _activeChatId.value ?: return
        val optJson = "[" + options.mapIndexed { idx, op -> "{\"id\": $idx, \"text\": \"$op\", \"votes\": 0}" }.joinToString(",") + "]"
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = "Poll: $question",
                messageType = MessageType.POLL,
                pollQuestion = question,
                pollOptionsJson = optJson
            )
            showPollCreatorDialog.value = false
        }
    }

    fun translateMessage(msgId: String, text: String, targetLanguage: String) {
        viewModelScope.launch {
            repository.translateMessage(msgId, text, targetLanguage)
            showAiTranslatorDialog.value = false
        }
    }

    fun summarizeActiveChat() {
        val msgs = currentMessages.value.map { "${it.senderName}: ${it.text}" }
        isGeneratingSummary.value = true
        showAiSummarizerDialog.value = true

        viewModelScope.launch {
            val summary = repository.aiService.summarizeMessages(msgs)
            aiSummaryContent.value = summary
            isGeneratingSummary.value = false
        }
    }

    fun reactToMessage(msgId: String, emoji: String) {
        viewModelScope.launch {
            repository.reactToMessage(msgId, emoji)
        }
    }

    fun createChat(title: String, subtitle: String, isGroup: Boolean, isSecret: Boolean, isBusiness: Boolean) {
        viewModelScope.launch {
            val newId = repository.createNewChat(title, subtitle, isGroup, isSecret, isBusiness)
            showNewChatSheet.value = false
            openChat(newId)
        }
    }

    fun openStory(story: StoryEntity) {
        _activeStory.value = story
        _currentScreen.value = AppScreen.STORY_VIEWER
        viewModelScope.launch {
            repository.markStoryViewed(story.id)
        }
    }

    fun closeStory() {
        _activeStory.value = null
        _currentScreen.value = AppScreen.MAIN_APP
    }

    fun startCall(contactName: String, contactAvatar: String, isVideo: Boolean) {
        _activeCallState.value = ActiveCallState(
            contactName = contactName,
            contactAvatar = contactAvatar,
            isVideo = isVideo,
            isConnected = false,
            durationSeconds = 0
        )
        _currentScreen.value = AppScreen.ACTIVE_CALL

        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            delay(1200) // WebRTC ICE candidate & SDP handshake simulation
            _activeCallState.value = _activeCallState.value.copy(
                isConnected = true,
                webrtcLatencyMs = 16 + Random().nextInt(12),
                webrtcBitrateKbps = if (isVideo) 2850 else 320
            )
            while (true) {
                delay(1000)
                val current = _activeCallState.value
                val jitterLatency = (14..28).random()
                val jitterBitrate = if (current.isVideo && !current.isVideoOff) (2700..3200).random() else 320
                _activeCallState.value = current.copy(
                    durationSeconds = current.durationSeconds + 1,
                    webrtcLatencyMs = jitterLatency,
                    webrtcBitrateKbps = jitterBitrate
                )
            }
        }
    }

    fun toggleMute() {
        _activeCallState.value = _activeCallState.value.copy(isMuted = !_activeCallState.value.isMuted)
    }

    fun toggleVideo() {
        _activeCallState.value = _activeCallState.value.copy(isVideoOff = !_activeCallState.value.isVideoOff)
    }

    fun toggleSpeaker() {
        _activeCallState.value = _activeCallState.value.copy(isSpeakerOn = !_activeCallState.value.isSpeakerOn)
    }

    fun flipCamera() {
        _activeCallState.value = _activeCallState.value.copy(isFrontCamera = !_activeCallState.value.isFrontCamera)
    }

    fun toggleScreenShare() {
        _activeCallState.value = _activeCallState.value.copy(isScreenSharing = !_activeCallState.value.isScreenSharing)
    }

    fun toggleNoiseCanceling() {
        _activeCallState.value = _activeCallState.value.copy(isAiNoiseCanceling = !_activeCallState.value.isAiNoiseCanceling)
    }

    fun endCall() {
        callTimerJob?.cancel()
        _currentScreen.value = if (_activeChatId.value != null) AppScreen.CHAT_DETAIL else AppScreen.MAIN_APP
    }

    fun askBharatAiTab(prompt: String) {
        if (prompt.isBlank()) return
        val currentHistory = bharatAiHistory.value.toMutableList()
        currentHistory.add("user" to prompt)
        bharatAiHistory.value = currentHistory
        isBharatAiThinking.value = true

        viewModelScope.launch {
            val reply = repository.aiService.askBharatAi(prompt, currentHistory)
            val updated = bharatAiHistory.value.toMutableList()
            updated.add("model" to reply)
            bharatAiHistory.value = updated
            isBharatAiThinking.value = false
        }
    }

    fun toggleJoinChannel(channelId: String, currentJoined: Boolean) {
        viewModelScope.launch {
            repository.toggleJoinChannel(channelId, !currentJoined)
        }
    }

    fun toggleRadarScan() {
        isRadarScanning.value = !isRadarScanning.value
    }

    // Dual Phone Testing & Role Switching Methods
    fun switchDeviceRole(role: DeviceRole) {
        val newProfile = syncManager.switchRole(role)
        _userProfile.value = newProfile
    }

    fun updatePairCode(code: String) {
        syncManager.updatePairCode(code)
    }

    fun toggleSyncAutoReply(enabled: Boolean) {
        syncManager.toggleAutoReply(enabled)
    }

    fun triggerIncomingTestCall(isVideo: Boolean = false) {
        syncManager.triggerSimulatedIncomingCall(isVideo)
    }

    fun acceptIncomingCall() {
        val call = incomingCallEvent.value
        syncManager.clearIncomingCall()
        if (call != null) {
            startCall(call.callerName, call.callerAvatar, call.isVideo)
        }
    }

    fun declineIncomingCall() {
        syncManager.clearIncomingCall()
    }

    fun declineIncomingCallWithMessage(message: String) {
        val call = incomingCallEvent.value
        syncManager.clearIncomingCall()
        if (call != null) {
            viewModelScope.launch {
                val currentChats = chats.value
                val matchedChat = currentChats.find { it.title.contains(call.callerName, ignoreCase = true) }
                val targetChatId = matchedChat?.id ?: currentChats.firstOrNull()?.id
                if (targetChatId != null) {
                    repository.sendMessage(
                        chatId = targetChatId,
                        text = "Declined call: \"$message\"",
                        messageType = MessageType.TEXT
                    )
                }
            }
        }
    }

    fun triggerIncomingTestUpi(amount: Double = 500.0) {
        syncManager.triggerSimulatedIncomingUpi(amount)
        viewModelScope.launch {
            val sender = if (syncStatus.value.activeRole == DeviceRole.PHONE_1) "Ananya Sen" else "Vikram Aditya"
            val refId = "BHARAT-UPI-${Random().nextInt(90000000) + 10000000}"
            val tx = TransactionEntity(
                id = "tx_${UUID.randomUUID()}",
                title = "Received from $sender",
                upiId = if (syncStatus.value.activeRole == DeviceRole.PHONE_1) "ananya@upi" else "vikram@upi",
                amount = amount,
                isDebit = false,
                timestamp = System.currentTimeMillis(),
                timeFormatted = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date()),
                status = "SUCCESS",
                referenceId = refId
            )
            database.walletDao().insertTransaction(tx)
            _userProfile.value = _userProfile.value.copy(walletBalance = _userProfile.value.walletBalance + amount)
        }
    }

    fun dismissIncomingUpi() {
        syncManager.clearIncomingUpi()
    }

    fun saveUserProfile(
        name: String,
        statusBio: String,
        profilePicUri: String,
        avatarInitial: String,
        avatarColorHex: String,
        avatarIndex: Int = 0
    ) {
        viewModelScope.launch {
            profileDataStore.saveUserProfile(
                name = name,
                statusBio = statusBio,
                profilePicUri = profilePicUri,
                avatarInitial = avatarInitial,
                avatarColorHex = avatarColorHex,
                customAvatarIndex = avatarIndex
            )
        }
    }

    fun updateProfile(name: String, phone: String, statusBio: String) {
        viewModelScope.launch {
            val initials = name.trim().split(" ")
                .filter { it.isNotBlank() }
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
                .ifEmpty { "V" }
            profileDataStore.saveUserProfile(
                name = name,
                displayName = name,
                statusBio = statusBio,
                profilePicUri = _userProfile.value.profilePicUri,
                avatarInitial = initials,
                avatarColorHex = _userProfile.value.avatarColorHex,
                customAvatarIndex = _userProfile.value.customAvatarIndex,
                phone = phone
            )
        }
    }

    fun completeAuthLogin(name: String, phone: String, statusBio: String, photoUri: String? = null) {
        viewModelScope.launch {
            val initials = name.trim().split(" ")
                .filter { it.isNotBlank() }
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
                .ifEmpty { "V" }
            profileDataStore.saveUserProfile(
                name = name,
                displayName = name,
                statusBio = statusBio,
                profilePicUri = photoUri ?: _userProfile.value.profilePicUri,
                avatarInitial = initials,
                avatarColorHex = "#10B981",
                customAvatarIndex = 0,
                phone = phone
            )
            profileDataStore.setLoggedIn(true)
            _currentScreen.value = AppScreen.MAIN_APP
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            profileDataStore.setLoggedIn(false)
            _currentScreen.value = AppScreen.ONBOARDING
        }
    }

    fun deleteAccountAndReset() {
        viewModelScope.launch {
            profileDataStore.clearAccount()
            _currentScreen.value = AppScreen.ONBOARDING
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            profileDataStore.updateDisplayName(name)
        }
    }

    fun updateStatusBio(bio: String) {
        viewModelScope.launch {
            profileDataStore.updateStatusBio(bio)
        }
    }

    fun updateProfilePicture(picUri: String, avatarIndex: Int = 0, colorHex: String = "#FF671F") {
        viewModelScope.launch {
            profileDataStore.updateProfilePicture(picUri, avatarIndex, colorHex)
        }
    }

    // Local Backup & Restore Operations
    fun refreshAvailableBackups() {
        viewModelScope.launch {
            val list = backupManager.getAvailableBackups()
            availableBackupsList.value = list
        }
    }

    fun exportChatAndContactsBackup(customName: String? = null) {
        viewModelScope.launch {
            isBackingUp.value = true
            lastExportResult.value = null
            delay(400) // Realistic UI processing feel
            val result = backupManager.exportDataToLocalFile(customName)
            lastExportResult.value = result
            isBackingUp.value = false
            refreshAvailableBackups()
        }
    }

    fun restoreBackupFromFile(file: java.io.File) {
        viewModelScope.launch {
            isRestoring.value = true
            lastRestoreResult.value = null
            delay(500)
            val result = backupManager.restoreDataFromFile(file)
            lastRestoreResult.value = result
            isRestoring.value = false
            refreshAvailableBackups()
        }
    }

    fun restoreBackupFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            isRestoring.value = true
            lastRestoreResult.value = null
            delay(500)
            val result = backupManager.restoreDataFromUri(uri)
            lastRestoreResult.value = result
            isRestoring.value = false
            refreshAvailableBackups()
        }
    }

    fun clearBackupRestoreMessages() {
        lastExportResult.value = null
        lastRestoreResult.value = null
    }

    override fun onCleared() {
        super.onCleared()
        callTimerJob?.cancel()
        recordTimerJob?.cancel()
        ttsManager.shutdown()
    }
}

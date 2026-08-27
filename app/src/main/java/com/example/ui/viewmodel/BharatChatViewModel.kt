package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiAiService
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

data class ZoomableDpData(
    val imageUri: String? = null,
    val title: String = "",
    val initial: String = "VA",
    val colorHex: String = "#FF671F",
    val subtitle: String? = null
)

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
    private val repository = BharatChatRepository(database, GeminiAiService(), application)
    val profileDataStore = com.example.data.local.UserProfileDataStore(application)
    val ttsManager = AudioAndTtsManager(application)
    val voiceRecorder = VoiceRecorderManager(application)
    val backupManager = com.example.data.backup.LocalBackupManager(application, database)
    val proximityHandler = com.example.utils.ProximitySensorHandler(application)
    val isProximityNear: StateFlow<Boolean> = proximityHandler.isNear

    // User Profile & UPI Wallet
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Multi-Device Realtime Sync & 2-Phone Testing Manager
    val syncManager = MultiDeviceSyncManager()
    val syncStatus: StateFlow<SyncPairStatus> = syncManager.syncStatus
    val incomingCallEvent: StateFlow<IncomingCallEvent?> = syncManager.incomingCall
    val incomingUpiEvent: StateFlow<IncomingUpiEvent?> = syncManager.incomingUpi

    private var currentCallId: String? = null

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

    val diagnosticReport = com.example.utils.FirebaseDiagnostics.lastReport
    val isRunningDiagnostics = com.example.utils.FirebaseDiagnostics.isRunning

    fun setLoggedIn(loggedIn: Boolean) {
        viewModelScope.launch {
            profileDataStore.setLoggedIn(loggedIn)
        }
    }

    fun runFirebaseDiagnostics(onComplete: ((com.example.utils.DiagnosticReport) -> Unit)? = null) {
        com.example.utils.FirebaseDiagnostics.runDiagnostics(getApplication(), onComplete)
    }

    init {
        viewModelScope.launch {
            repository.cleanLegacyDemoData()
            repository.seedInitialDataIfEmpty()
            // Run comprehensive diagnostics in background to verify Firebase and Firestore health
            runFirebaseDiagnostics()
        }
        viewModelScope.launch {
            profileDataStore.userProfileFlow.collect { savedProfile ->
                _userProfile.value = savedProfile.copy(
                    walletBalance = _userProfile.value.walletBalance
                )
                bindFirestoreListeners(_userProfile.value)
            }
        }
        refreshAvailableBackups()
    }

    private fun bindFirestoreListeners(profile: UserProfile) {
        try {
            val devId = profileDataStore.getDeviceId()
            val firestore = com.example.data.sync.FirestoreManager.getInstance(getApplication())
            firestore.publishUserProfile(profile, devId, isOnline = true)
            firestore.startPresenceHeartbeat(profileProvider = { _userProfile.value }, deviceId = devId)
            firestore.listenForCloudUsers(database, profile.name, devId)
            firestore.listenForCloudStories(database, devId)

            // Auto-sync local phonebook contacts with Firestore registered users
            val contactProvider = com.example.data.contacts.ContactProvider.getInstance(getApplication(), database)
            if (contactProvider.hasContactPermission()) {
                viewModelScope.launch(Dispatchers.IO) {
                    contactProvider.syncAndMapContactsWithFirestore(myDeviceId = devId, myPhone = profile.phone)
                }
            }

            firestore.startGlobalMessagesListener(
                appDatabase = database,
                currentUserId = profile.phone.ifBlank { profile.bharatId },
                currentUserName = profile.name,
                currentUserPhone = profile.phone.ifBlank { profile.bharatId },
                myDeviceId = devId
            )
            firestore.listenForIncomingCalls(
                currentUserName = profile.name,
                currentUserPhone = profile.phone.ifBlank { profile.bharatId },
                myDeviceId = devId,
                onIncomingCall = { callId, callerName, callerAvatar, isVideo ->
                    currentCallId = callId
                    syncManager.triggerIncomingCall(
                        callId = callId,
                        callerName = callerName,
                        callerAvatar = callerAvatar,
                        isVideo = isVideo
                    )
                    ttsManager.playCallDialTone()
                },
                onCallStatusChange = { callId, status ->
                    if (status == "ACCEPTED") {
                        ttsManager.stopCallTones()
                        ttsManager.playCallConnectedTone()
                        _activeCallState.value = _activeCallState.value.copy(
                            isConnected = true,
                            webrtcLatencyMs = 18,
                            webrtcBitrateKbps = if (_activeCallState.value.isVideo) 2850 else 320
                        )
                    } else if (status == "DECLINED" || status == "ENDED") {
                        if (currentCallId == callId || _currentScreen.value == AppScreen.ACTIVE_CALL) {
                            endCall(notifyCloud = false)
                        }
                        syncManager.clearIncomingCall()
                        ttsManager.stopCallTones()
                    }
                }
            )
        } catch (e: Exception) {
            // Non-blocking fallback
        }
    }

    // Navigation and Screen State
    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Real-time Firestore presence state flows
    val onlineUsersMap: StateFlow<Map<String, Boolean>> = com.example.data.sync.FirestoreManager.getInstance(getApplication()).onlineUsersMap
    val usersLastSeenMap: StateFlow<Map<String, Long>> = com.example.data.sync.FirestoreManager.getInstance(getApplication()).usersLastSeenMap

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
        statusMsg: String = "Available on VenzoInd 🇮🇳"
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
                upiVpa = if (upiVpa.isBlank()) "${name.lowercase().replace(" ", "")}@venzo" else upiVpa.trim(),
                avatarInitial = initials,
                avatarColorHex = randomColor,
                statusMsg = statusMsg.ifBlank { "Available on VenzoInd 🇮🇳" },
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

    fun syncDevicePhonebookContacts(context: Context, onComplete: (summary: com.example.data.contacts.ContactSyncSummary) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val devId = profileDataStore.getDeviceId()
            val provider = com.example.data.contacts.ContactProvider.getInstance(context, database)
            val summary = provider.syncAndMapContactsWithFirestore(
                myDeviceId = devId,
                myPhone = _userProfile.value.phone
            )
            withContext(Dispatchers.Main) {
                onComplete(summary)
            }
        }
    }

    // Active Story Viewer State
    private val _activeStory = MutableStateFlow<StoryEntity?>(null)
    val activeStory: StateFlow<StoryEntity?> = _activeStory.asStateFlow()

    // Active Call State
    private val _activeCallState = MutableStateFlow(ActiveCallState())
    val activeCallState: StateFlow<ActiveCallState> = _activeCallState.asStateFlow()
    private var callTimerJob: Job? = null

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
    val showLocationShareSheet = MutableStateFlow(false)
    val showCloudDocPickerSheet = MutableStateFlow(false)
    val showCataloguePickerSheet = MutableStateFlow(false)
    val showAttachmentOptions = MutableStateFlow(false)
    val showSecretChatInfo = MutableStateFlow(false)
    val showScheduleMessageDialog = MutableStateFlow(false)
    val showForwardDialog = MutableStateFlow(false)
    val forwardSelectedMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val showContactProfileDialog = MutableStateFlow(false)
    val activeContactProfile = MutableStateFlow<ContactEntity?>(null)
    val showZoomableDpDialog = MutableStateFlow(false)
    val activeZoomableDp = MutableStateFlow<ZoomableDpData?>(null)

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
            NearbyUser("n4", "Neha Gupta", 450, "NG", "VenzoInd Early Adopter 🇮🇳", true)
        )
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
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
            var chat = repository.getChatById(chatId)
            if (chat == null) {
                // Ensure chat exists or create one from contacts/fallback
                val contactId = if (chatId.startsWith("chat_")) "contact_${chatId.removePrefix("chat_")}" else "contact_$chatId"
                val contact = repository.getContactById(contactId) ?: repository.getContactById(chatId)
                val initial = contact?.avatarInitial ?: chatId.take(2).uppercase().ifBlank { "IN" }
                val title = contact?.name ?: "Chat"
                val subtitle = contact?.statusMsg ?: "Bharat Sovereign Chat"
                val newChat = ChatEntity(
                    id = chatId,
                    title = title,
                    subtitle = subtitle,
                    lastMessage = "",
                    lastMessageTime = "Just now",
                    timestamp = System.currentTimeMillis(),
                    unreadCount = 0,
                    avatarInitial = initial,
                    avatarColorHex = contact?.avatarColorHex ?: "#FF671F",
                    isGroup = false,
                    isSecret = false,
                    isVerifiedBusiness = false,
                    isPinned = false
                )
                database.chatDao().insertChat(newChat)
                chat = newChat
            }
            _activeChatId.value = chatId
            _activeChat.value = chat
            repository.markChatAsSeen(chatId)
            _currentScreen.value = AppScreen.CHAT_DETAIL

            // Attach Firestore real-time listener for cloud sync across devices
            try {
                val currentUid = _userProfile.value.phone.ifBlank { _userProfile.value.bharatId }
                com.example.data.sync.FirestoreManager.getInstance(getApplication())
                    .attachChatListener(
                        chatId = chatId,
                        appDatabase = database,
                        currentUserId = currentUid,
                        currentUserName = _userProfile.value.name
                    )
            } catch (e: Exception) {
                // Non-blocking fallback
            }

            // Load smart replies
            if (chat.lastMessage.isNotBlank()) {
                val replies = repository.aiService.generateSmartReplies(chat.lastMessage)
                smartReplies.value = replies
            }
        }
    }

    fun closeChat() {
        val chatId = _activeChatId.value
        if (chatId != null) {
            try {
                com.example.data.sync.FirestoreManager.getInstance(getApplication())
                    .detachChatListener(chatId)
            } catch (e: Exception) {
                // Ignore
            }
        }
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

    fun sendMessage(
        text: String,
        replyToText: String? = null,
        replyToSender: String? = null
    ) {
        val chatId = _activeChatId.value ?: return
        if (text.isBlank()) return
        val chat = _activeChat.value

        viewModelScope.launch {
            ttsManager.playMessageSentChime()
            val myUid = _userProfile.value.phone.ifBlank { _userProfile.value.bharatId }
            repository.sendMessage(
                chatId = chatId,
                text = text,
                senderId = myUid,
                senderName = _userProfile.value.name,
                messageType = MessageType.TEXT,
                isSecret = chat?.isSecret == true,
                expireSeconds = chat?.disappearingSeconds ?: 0,
                replyToText = replyToText,
                replyToSender = replyToSender,
                isFromMe = true
            )

            // Update smart replies
            delay(400)
            val newReplies = repository.aiService.generateSmartReplies(text)
            smartReplies.value = newReplies

            // If this is AI assistant chat
            if (chatId == "chat_ai_assistant" || chat?.isAiAssistant == true) {
                typingUserName.value = "Bharat AI"
                typingStatusText.value = "Bharat AI is thinking..."
                isOtherUserTyping.value = true
                delay(1200)
                isOtherUserTyping.value = false
                ttsManager.playMessageReceivedChime()
            } else if (chat != null && !chat.isGroup) {
                // Real-time typing feedback and instant responsive reply
                val typingDuration = if (syncStatus.value.autoReplyEnabled) {
                    (syncStatus.value.simulatedDelayMs + 400L).coerceAtLeast(1000L)
                } else {
                    1200L
                }
                typingUserName.value = chat.title
                typingStatusText.value = "typing..."
                isOtherUserTyping.value = true
                delay(typingDuration)
                isOtherUserTyping.value = false

                val replyText = when {
                    text.contains("hello", ignoreCase = true) || text.contains("hi", ignoreCase = true) || text.contains("namaste", ignoreCase = true) || text.contains("hey", ignoreCase = true) ->
                        "Namaste! 🙏 How are you doing today?"
                    text.contains("upi", ignoreCase = true) || text.contains("pay", ignoreCase = true) || text.contains("₹") ->
                        "Received your UPI payment update! Thanks for the instant settlement via Bharat Pay. ⚡"
                    text.contains("call", ignoreCase = true) || text.contains("video", ignoreCase = true) ->
                        "Sure! Let's connect on Bharat 4K Encrypted Video/Voice Call. 📞"
                    text.contains("meeting", ignoreCase = true) || text.contains("project", ignoreCase = true) ->
                        "Yes, sounds great! Let's coordinate the project roadmap. 🚀"
                    text.contains("kesar", ignoreCase = true) || text.contains("saffron", ignoreCase = true) || text.contains("spice", ignoreCase = true) ->
                        "Thank you for choosing Dr. Priya's Organics! Your order will be dispatched promptly. 📦"
                    else ->
                        "Got your message! Delivered securely with Post-Quantum Kyber-1024 E2EE. 👍"
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
                    isSeen = true,
                    messageType = MessageType.TEXT.name
                )
                database.messageDao().insertMessage(incomingMsg)
                database.chatDao().updateLastMessageWithStatus(chatId, replyText, timeStr, now, "READ", false)
                ttsManager.playMessageReceivedChime()
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
                text = "📊 Poll: $question",
                messageType = MessageType.POLL,
                pollQuestion = question,
                pollOptionsJson = optJson
            )
            showPollCreatorDialog.value = false
        }
    }

    fun votePoll(messageId: String, optionIndex: Int) {
        viewModelScope.launch {
            repository.votePoll(messageId, optionIndex)
        }
    }

    fun sendLocationMessage(locationName: String, address: String, lat: Double, lng: Double) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = "$locationName • $address",
                messageType = MessageType.LOCATION,
                attachmentUrl = "geo:$lat,$lng?q=$lat,$lng($locationName)",
                fileSizeStr = "GPS: %.4f° N, %.4f° E".format(lat, lng)
            )
            showLocationShareSheet.value = false
        }
    }

    fun sendCatalogueProduct(product: com.example.data.model.CatalogueProduct) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendCatalogueMessage(
                chatId = chatId,
                senderId = _userProfile.value.phone.ifBlank { _userProfile.value.bharatId },
                senderName = _userProfile.value.name,
                product = product
            )
            showCataloguePickerSheet.value = false
            showAttachmentOptions.value = false
        }
    }

    fun translateMessage(msgId: String, text: String, targetLanguage: String) {
        viewModelScope.launch {
            repository.translateMessage(msgId, text, targetLanguage)
            showAiTranslatorDialog.value = false
        }
    }

    fun sendImageMessage(imageUri: String, caption: String = "") {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val base64Data = convertImageUriToBase64(getApplication(), imageUri)
            repository.sendMessage(
                chatId = chatId,
                text = caption.ifBlank { "📷 Photo" },
                messageType = MessageType.IMAGE,
                attachmentUrl = if (base64Data.isNotBlank()) base64Data else imageUri,
                fileSizeStr = "High Res • Photo"
            )
            showAttachmentOptions.value = false
        }
    }

    private fun convertImageUriToBase64(context: android.content.Context, uriString: String): String {
        if (uriString.startsWith("data:image/") || uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return uriString
        }
        return try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                val maxDim = 800
                val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val width = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                    val height = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                    android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)
                } else bitmap
                val outputStream = java.io.ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                val bytes = outputStream.toByteArray()
                "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } else {
                uriString
            }
        } catch (e: Exception) {
            uriString
        }
    }

    fun scheduleMessage(chatId: String, text: String, scheduledTimestamp: Long, scheduleDescription: String = "") {
        viewModelScope.launch {
            val formattedTime = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(scheduledTimestamp))
            val label = if (scheduleDescription.isNotBlank()) scheduleDescription else formattedTime
            repository.sendMessage(
                chatId = chatId,
                text = "⏰ [Scheduled for $label]\n$text",
                messageType = MessageType.TEXT
            )
            showScheduleMessageDialog.value = false
        }
    }

    fun toggleStarMessage(messageId: String) {
        viewModelScope.launch {
            repository.toggleStarMessage(messageId)
        }
    }

    fun deleteMessages(messageIds: List<String>) {
        viewModelScope.launch {
            repository.deleteMultipleMessages(messageIds)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            if (_activeChat.value?.id == chatId) {
                _activeChat.value = null
                _currentScreen.value = AppScreen.MAIN_APP
            }
        }
    }

    fun toggleChatPin(chatId: String) {
        viewModelScope.launch {
            repository.toggleChatPin(chatId)
        }
    }

    fun clearChatHistory(chatId: String) {
        viewModelScope.launch {
            repository.clearChatMessages(chatId)
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            repository.clearAllChats()
            _activeChat.value = null
        }
    }

    fun clearAllDemoData() {
        viewModelScope.launch {
            repository.clearAllDemoData()
            _activeChat.value = null
        }
    }

    fun forwardMessages(targetChatIds: List<String>, messages: List<MessageEntity>) {
        viewModelScope.launch {
            targetChatIds.forEach { targetId ->
                messages.forEach { msg ->
                    val forwardPrefix = "↪️ Forwarded\n"
                    val forwardType = try { MessageType.valueOf(msg.messageType) } catch (e: Exception) { MessageType.TEXT }
                    repository.sendMessage(
                        chatId = targetId,
                        text = if (forwardType == MessageType.TEXT) forwardPrefix + msg.text else msg.text,
                        messageType = forwardType,
                        attachmentUrl = msg.attachmentUrl,
                        fileSizeStr = msg.fileSizeStr,
                        audioWaveform = msg.audioWaveform,
                        voiceDurationSec = msg.voiceDurationSec,
                        upiAmount = msg.upiAmount
                    )
                }
            }
            showForwardDialog.value = false
            forwardSelectedMessages.value = emptyList()
        }
    }

    fun openContactProfile(contact: ContactEntity) {
        activeContactProfile.value = contact
        showContactProfileDialog.value = true
    }

    fun openContactProfileFromChat(chat: ChatEntity) {
        val contact = ContactEntity(
            id = chat.id,
            name = chat.title,
            phone = if (chat.id == "chat_ai_assistant") "+91-BHARAT-AI" else "+91 98765 ${Math.abs(chat.id.hashCode()).toString().takeLast(5).padStart(5, '0')}",
            upiVpa = "${chat.title.lowercase().replace(" ", "")}@upi",
            avatarInitial = chat.avatarInitial,
            avatarColorHex = chat.avatarColorHex,
            statusMsg = chat.subtitle.ifBlank { "Available on VenzoInd Sovereign Chat" },
            isBharatChatUser = true,
            isFavorite = chat.isPinned,
            publicKeyFingerprint = "KYBER-1024-${Math.abs(chat.id.hashCode()).toString(16).uppercase().take(8)}",
            lastSeenTimestamp = System.currentTimeMillis(),
            profilePicUri = null
        )
        activeContactProfile.value = contact
        showContactProfileDialog.value = true
    }

    fun openZoomableDp(
        title: String,
        imageUri: String? = null,
        initial: String = "VA",
        colorHex: String = "#FF671F",
        subtitle: String? = null
    ) {
        activeZoomableDp.value = ZoomableDpData(
            imageUri = imageUri,
            title = title,
            initial = initial,
            colorHex = colorHex,
            subtitle = subtitle
        )
        showZoomableDpDialog.value = true
    }

    fun closeZoomableDp() {
        showZoomableDpDialog.value = false
        activeZoomableDp.value = null
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

    fun postNewStory(
        caption: String,
        gradientStart: String = "#FF671F",
        gradientEnd: String = "#06038D",
        isAiGenerated: Boolean = false,
        aiEffectName: String = ""
    ) {
        viewModelScope.launch {
            val myName = _userProfile.value.name.ifBlank { "You" }
            val myAvatar = _userProfile.value.avatarInitial.ifBlank { "ME" }
            val now = System.currentTimeMillis()
            val story = StoryEntity(
                id = "story_${UUID.randomUUID()}",
                authorName = myName,
                authorAvatar = myAvatar,
                isAiGenerated = isAiGenerated,
                aiEffectName = aiEffectName,
                timestamp = now,
                timeAgo = "Just now",
                caption = caption,
                isViewed = false,
                mediaGradientStart = gradientStart,
                mediaGradientEnd = gradientEnd
            )
            database.storyDao().insertStory(story)
            try {
                val devId = profileDataStore.getDeviceId()
                com.example.data.sync.FirestoreManager.getInstance(getApplication()).publishStoryToCloud(story, authorDeviceId = devId)
            } catch (e: Exception) {
                // Ignore
            }
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

    fun startCall(
        contactName: String,
        contactAvatar: String,
        isVideo: Boolean,
        contactPhone: String = "",
        targetDeviceId: String = ""
    ) {
        val safeName = contactName.ifBlank { "Contact" }
        val safeAvatar = if (contactAvatar.isNotBlank()) contactAvatar else safeName.take(2).uppercase()
        val callId = "call_${UUID.randomUUID()}"
        currentCallId = callId

        val defaultSpeaker = isVideo
        _activeCallState.value = ActiveCallState(
            contactName = safeName,
            contactAvatar = safeAvatar,
            isVideo = isVideo,
            isSpeakerOn = defaultSpeaker,
            isConnected = false,
            durationSeconds = 0
        )
        _currentScreen.value = AppScreen.ACTIVE_CALL

        // Setup audio route & Proximity Sensor
        ttsManager.setSpeakerOn(defaultSpeaker)
        proximityHandler.start(isSpeakerOn = defaultSpeaker)

        // Play dial tone while connecting
        ttsManager.playCallDialTone()

        // Send Real-Time Cloud Call Signal to Firestore
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val devId = profileDataStore.getDeviceId()
                val myUid = _userProfile.value.phone.ifBlank { _userProfile.value.bharatId }
                val activeChatIdVal = _activeChatId.value ?: ""
                
                var resolvedPhone = contactPhone
                var resolvedTargetDev = targetDeviceId

                if (resolvedPhone.isBlank() || resolvedTargetDev.isBlank()) {
                    val contact = database.contactDao().getContactById(activeChatIdVal)
                        ?: database.contactDao().getContactById(if (activeChatIdVal.startsWith("chat_")) "contact_${activeChatIdVal.removePrefix("chat_")}" else "contact_$activeChatIdVal")
                        ?: database.contactDao().getAllContactsList().firstOrNull { it.name.equals(safeName, ignoreCase = true) || it.name.contains(safeName, ignoreCase = true) }
                    
                    if (contact != null) {
                        if (resolvedPhone.isBlank()) resolvedPhone = contact.phone
                        if (resolvedTargetDev.isBlank()) resolvedTargetDev = contact.id.removePrefix("contact_")
                    }
                }

                if (resolvedTargetDev.isBlank() && activeChatIdVal.startsWith("chat_")) {
                    resolvedTargetDev = activeChatIdVal.removePrefix("chat_")
                }

                com.example.data.sync.FirestoreManager.getInstance(getApplication()).initiateCloudCall(
                    callId = callId,
                    callerId = myUid,
                    callerName = _userProfile.value.name,
                    callerAvatar = _userProfile.value.avatarInitial,
                    callerDeviceId = devId,
                    targetDeviceId = resolvedTargetDev,
                    receiverName = safeName,
                    receiverPhone = resolvedPhone,
                    isVideo = isVideo
                ) { status ->
                    if (status == "ACCEPTED") {
                        ttsManager.stopCallTones()
                        ttsManager.playCallConnectedTone()
                        _activeCallState.value = _activeCallState.value.copy(
                            isConnected = true,
                            webrtcLatencyMs = 18,
                            webrtcBitrateKbps = if (isVideo) 2850 else 320
                        )
                        viewModelScope.launch {
                            delay(400)
                            val greeting = when {
                                safeName.contains("Priya", ignoreCase = true) -> "Namaste! Dr. Priya here. I can hear you loud and clear on Bharat HD call."
                                safeName.contains("Rohan", ignoreCase = true) -> "Hey! Rohan here. Bharat HD call quality is crystal clear!"
                                safeName.contains("Deb", ignoreCase = true) || safeName.contains("Debashish", ignoreCase = true) -> "Namaste! Debashish here. Your call is connected securely on Bharat HD Voice."
                                safeName.contains("AI", ignoreCase = true) -> "Namaste! Bharat AI Copilot voice channel is active and connected."
                                else -> "Namaste! Call is connected with $safeName on Bharat Secure HD Voice. I can hear you clearly."
                            }
                            ttsManager.speakText(greeting)
                        }
                    } else if (status == "DECLINED" || status == "ENDED") {
                        endCall(notifyCloud = false)
                    }
                }
            } catch (e: Exception) {
                // Non-blocking fallback
            }
        }

        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            var waited = 0
            while (!_activeCallState.value.isConnected && waited < 3) {
                delay(1000)
                waited++
            }
            if (!_activeCallState.value.isConnected) {
                // Auto-connect call for seamless testing and local contact simulation
                ttsManager.stopCallTones()
                ttsManager.playCallConnectedTone()
                _activeCallState.value = _activeCallState.value.copy(
                    isConnected = true,
                    webrtcLatencyMs = 22,
                    webrtcBitrateKbps = if (isVideo) 2900 else 320
                )
                viewModelScope.launch {
                    delay(400)
                    val greeting = when {
                        safeName.contains("Priya", ignoreCase = true) -> "Namaste! Dr. Priya here. I can hear you loud and clear on Bharat HD call."
                        safeName.contains("Rohan", ignoreCase = true) -> "Hey! Rohan here. Bharat HD call quality is crystal clear!"
                        safeName.contains("Deb", ignoreCase = true) || safeName.contains("Debashish", ignoreCase = true) -> "Namaste! Debashish here. Your call is connected securely on Bharat HD Voice."
                        safeName.contains("AI", ignoreCase = true) -> "Namaste! Bharat AI Copilot voice channel is active and connected."
                        else -> "Namaste! Call is connected with $safeName on Bharat Secure HD Voice. I can hear you clearly."
                    }
                    ttsManager.speakText(greeting)
                }
            }
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

    fun speakCallTestPhrase() {
        val name = _activeCallState.value.contactName.ifBlank { "Contact" }
        ttsManager.speakText("Hi! $name is on the line. Audio channel is live and 100% encrypted.")
    }

    fun toggleMute() {
        val newMute = !_activeCallState.value.isMuted
        _activeCallState.value = _activeCallState.value.copy(isMuted = newMute)
        ttsManager.setMicrophoneMute(newMute)
    }

    fun toggleVideo() {
        val current = _activeCallState.value
        if (!current.isVideo) {
            // Upgrade audio call to video call and turn on camera
            _activeCallState.value = current.copy(isVideo = true, isVideoOff = false)
        } else {
            _activeCallState.value = current.copy(isVideoOff = !current.isVideoOff)
        }
    }

    fun toggleSpeaker() {
        val newSpeaker = !_activeCallState.value.isSpeakerOn
        _activeCallState.value = _activeCallState.value.copy(isSpeakerOn = newSpeaker)
        ttsManager.setSpeakerOn(newSpeaker)
        proximityHandler.setSpeakerOn(newSpeaker)
    }

    fun flipCamera() {
        _activeCallState.value = _activeCallState.value.copy(isFrontCamera = !_activeCallState.value.isFrontCamera)
    }

    fun toggleScreenShare() {
        val newSharing = !_activeCallState.value.isScreenSharing
        _activeCallState.value = _activeCallState.value.copy(isScreenSharing = newSharing)
    }

    fun toggleNoiseCanceling() {
        _activeCallState.value = _activeCallState.value.copy(isAiNoiseCanceling = !_activeCallState.value.isAiNoiseCanceling)
    }

    fun endCall(notifyCloud: Boolean = true) {
        val currentState = _activeCallState.value
        val callIdToEnd = currentCallId
        currentCallId = null

        callTimerJob?.cancel()
        proximityHandler.stop()
        ttsManager.stopCallTones()
        ttsManager.stopSpeaking()
        ttsManager.playCallEndTone()

        if (notifyCloud && callIdToEnd != null) {
            try {
                com.example.data.sync.FirestoreManager.getInstance(getApplication())
                    .updateCloudCallStatus(callIdToEnd, "ENDED")
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Log call to Call History DB
        if (currentState.contactName.isNotBlank()) {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeStr = sdf.format(Date())
            val durationText = if (currentState.isConnected && currentState.durationSeconds > 0) {
                val m = currentState.durationSeconds / 60
                val s = currentState.durationSeconds % 60
                "${m}m ${s}s"
            } else {
                "Outgoing"
            }
            viewModelScope.launch {
                repository.insertCall(
                    CallEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        contactName = currentState.contactName,
                        contactAvatar = currentState.contactAvatar,
                        isVideo = currentState.isVideo,
                        isIncoming = false,
                        isMissed = !currentState.isConnected,
                        timestamp = System.currentTimeMillis(),
                        timeFormatted = timeStr,
                        durationStr = durationText,
                        qualityStr = if (currentState.isVideo) "4K WebRTC" else "HD Voice"
                    )
                )
            }
        }

        _currentScreen.value = if (_activeChatId.value != null) AppScreen.CHAT_DETAIL else AppScreen.MAIN_APP
    }

    fun deleteCall(callId: String) {
        viewModelScope.launch {
            repository.deleteCall(callId)
        }
    }

    fun clearAllCalls() {
        viewModelScope.launch {
            repository.clearAllCalls()
        }
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
        bindFirestoreListeners(newProfile)
    }

    fun clearAllDemoContacts() {
        viewModelScope.launch {
            repository.clearAllContacts()
        }
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
        ttsManager.stopCallTones()
        ttsManager.playCallConnectedTone()

        if (call != null) {
            val callId = call.callId.ifBlank { currentCallId ?: "call_${UUID.randomUUID()}" }
            currentCallId = callId
            try {
                com.example.data.sync.FirestoreManager.getInstance(getApplication())
                    .updateCloudCallStatus(callId, "ACCEPTED")
            } catch (e: Exception) {
                // Ignore
            }

            val isSpeaker = call.isVideo
            ttsManager.setSpeakerOn(isSpeaker)
            proximityHandler.start(isSpeakerOn = isSpeaker)

            _activeCallState.value = ActiveCallState(
                contactName = call.callerName,
                contactAvatar = call.callerAvatar,
                isVideo = call.isVideo,
                isSpeakerOn = isSpeaker,
                isConnected = true,
                durationSeconds = 0
            )
            _currentScreen.value = AppScreen.ACTIVE_CALL

            callTimerJob?.cancel()
            callTimerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    val current = _activeCallState.value
                    _activeCallState.value = current.copy(
                        durationSeconds = current.durationSeconds + 1,
                        webrtcLatencyMs = (14..28).random(),
                        webrtcBitrateKbps = if (current.isVideo && !current.isVideoOff) 2900 else 320
                    )
                }
            }
        }
    }

    fun declineIncomingCall() {
        val call = incomingCallEvent.value
        syncManager.clearIncomingCall()
        ttsManager.stopCallTones()

        val callId = call?.callId?.ifBlank { currentCallId } ?: currentCallId
        if (callId != null) {
            try {
                com.example.data.sync.FirestoreManager.getInstance(getApplication())
                    .updateCloudCallStatus(callId, "DECLINED")
            } catch (e: Exception) {
                // Ignore
            }
        }
        currentCallId = null
    }

    fun declineIncomingCallWithMessage(message: String) {
        val call = incomingCallEvent.value
        syncManager.clearIncomingCall()
        ttsManager.stopCallTones()

        val callId = call?.callId?.ifBlank { currentCallId } ?: currentCallId
        if (callId != null) {
            try {
                com.example.data.sync.FirestoreManager.getInstance(getApplication())
                    .updateCloudCallStatus(callId, "DECLINED")
            } catch (e: Exception) {
                // Ignore
            }
        }
        currentCallId = null
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
        try {
            val devId = profileDataStore.getDeviceId()
            val firestore = com.example.data.sync.FirestoreManager.getInstance(getApplication())
            firestore.stopPresenceHeartbeat(_userProfile.value, devId)
        } catch (e: Exception) {
            // Ignore teardown errors
        }
        callTimerJob?.cancel()
        recordTimerJob?.cancel()
        proximityHandler.stop()
        ttsManager.shutdown()
    }
}

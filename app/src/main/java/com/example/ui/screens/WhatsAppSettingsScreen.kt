package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.ChatTransferDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.TricolorGlowPill
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BharatChatViewModel

enum class SettingsSubScreen {
    MAIN,
    ACCOUNT,
    PRIVACY,
    CHATS,
    NOTIFICATIONS,
    STORAGE,
    LANGUAGE,
    HELP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppSettingsScreen(
    viewModel: BharatChatViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bColors = LocalBharatColors.current

    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // Settings state
    var readReceiptsEnabled by remember { mutableStateOf(true) }
    var enterIsSend by remember { mutableStateOf(false) }
    var mediaVisibility by remember { mutableStateOf(true) }
    var highPriorityNotifs by remember { mutableStateOf(true) }
    var conversationTones by remember { mutableStateOf(true) }
    var lessDataForCalls by remember { mutableStateOf(false) }
    var hdUploadQuality by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("English (device's language)") }
    var twoStepEnabled by remember { mutableStateOf(false) }
    var biometricLockEnabled by remember { mutableStateOf(false) }
    var disappearingTimer by remember { mutableStateOf("Off") }
    var lastSeenPrivacy by remember { mutableStateOf("Everyone") }
    var profilePhotoPrivacy by remember { mutableStateOf("Everyone") }
    var selectedFontSize by remember { mutableStateOf("Medium") }
    var selectedThemeMode by remember { mutableStateOf(if (isDark) "Dark" else "Light") }

    // Dialog and Sheet states
    var showQrDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showLastSeenDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showClearChatsDialog by remember { mutableStateOf(false) }
    var showClearAllDemoDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showTwoStepDialog by remember { mutableStateOf(false) }
    var twoStepPinInput by remember { mutableStateOf("") }
    var showStorageDetailsSheet by remember { mutableStateOf(false) }
    var showChatTransferDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentSubScreen) {
                            SettingsSubScreen.MAIN -> "Settings"
                            SettingsSubScreen.ACCOUNT -> "Account"
                            SettingsSubScreen.PRIVACY -> "Privacy"
                            SettingsSubScreen.CHATS -> "Chats"
                            SettingsSubScreen.NOTIFICATIONS -> "Notifications"
                            SettingsSubScreen.STORAGE -> "Storage and data"
                            SettingsSubScreen.LANGUAGE -> "App language"
                            SettingsSubScreen.HELP -> "Help"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = bColors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentSubScreen == SettingsSubScreen.MAIN) {
                                onBackClick()
                            } else {
                                currentSubScreen = SettingsSubScreen.MAIN
                            }
                        },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = bColors.textPrimary
                        )
                    }
                },
                actions = {
                    if (currentSubScreen == SettingsSubScreen.MAIN) {
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Search settings", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = bColors.textPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (bColors.isDark) DarkSurface else LightSurface
                )
            )
        },
        containerColor = if (bColors.isDark) DarkBackground else LightBackground,
        modifier = modifier.fillMaxSize().testTag("whatsapp_settings_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentSubScreen,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "settings_nav"
            ) { subScreen ->
                when (subScreen) {
                    SettingsSubScreen.MAIN -> {
                        MainSettingsContent(
                            userProfile = userProfile,
                            bColors = bColors,
                            isDark = isDark,
                            selectedLanguage = selectedLanguage,
                            onProfileClick = { viewModel.navigateTo(AppScreen.USER_PROFILE) },
                            onQrClick = { showQrDialog = true },
                            onAccountClick = { currentSubScreen = SettingsSubScreen.ACCOUNT },
                            onPrivacyClick = { currentSubScreen = SettingsSubScreen.PRIVACY },
                            onAvatarClick = { viewModel.navigateTo(AppScreen.USER_PROFILE) },
                            onChatsClick = { currentSubScreen = SettingsSubScreen.CHATS },
                            onNotificationsClick = { currentSubScreen = SettingsSubScreen.NOTIFICATIONS },
                            onStorageClick = { currentSubScreen = SettingsSubScreen.STORAGE },
                            onLanguageClick = { showLanguageSheet = true },
                            onHelpClick = { currentSubScreen = SettingsSubScreen.HELP },
                            onInviteClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Let's chat on VenzoInd! It's a fast, simple, and secure quantum-encrypted messaging app. Download at https://venzoind.app"
                                    )
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Invite friends to VenzoInd")
                                context.startActivity(shareIntent)
                            }
                        )
                    }

                    SettingsSubScreen.ACCOUNT -> {
                        AccountSettingsContent(
                            bColors = bColors,
                            twoStepEnabled = twoStepEnabled,
                            onTwoStepClick = { showTwoStepDialog = true },
                            onLogoutClick = {
                                viewModel.logoutUser()
                                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteAccountClick = { showDeleteAccountDialog = true }
                        )
                    }

                    SettingsSubScreen.PRIVACY -> {
                        PrivacySettingsContent(
                            bColors = bColors,
                            lastSeen = lastSeenPrivacy,
                            profilePhoto = profilePhotoPrivacy,
                            readReceipts = readReceiptsEnabled,
                            onReadReceiptsChange = { readReceiptsEnabled = it },
                            disappearingTimer = disappearingTimer,
                            biometricLock = biometricLockEnabled,
                            onBiometricLockChange = { biometricLockEnabled = it },
                            onLastSeenClick = { showLastSeenDialog = true },
                            onDisappearingClick = { showDisappearingDialog = true }
                        )
                    }

                    SettingsSubScreen.CHATS -> {
                        ChatsSettingsContent(
                            bColors = bColors,
                            themeMode = selectedThemeMode,
                            enterIsSend = enterIsSend,
                            onEnterIsSendChange = { enterIsSend = it },
                            mediaVisibility = mediaVisibility,
                            onMediaVisibilityChange = { mediaVisibility = it },
                            fontSize = selectedFontSize,
                            onThemeClick = { showThemeDialog = true },
                            onBackupClick = { viewModel.showBackupRestoreDialog.value = true },
                            onTransferChatsClick = { showChatTransferDialog = true },
                            onClearChatsClick = { showClearChatsDialog = true },
                            onClearDemoDataClick = { showClearAllDemoDialog = true }
                        )
                    }

                    SettingsSubScreen.NOTIFICATIONS -> {
                        NotificationsSettingsContent(
                            bColors = bColors,
                            conversationTones = conversationTones,
                            onConversationTonesChange = { conversationTones = it },
                            highPriority = highPriorityNotifs,
                            onHighPriorityChange = { highPriorityNotifs = it }
                        )
                    }

                    SettingsSubScreen.STORAGE -> {
                        StorageSettingsContent(
                            bColors = bColors,
                            lessDataForCalls = lessDataForCalls,
                            onLessDataChange = { lessDataForCalls = it },
                            hdUploadQuality = hdUploadQuality,
                            onHdQualityChange = { hdUploadQuality = it },
                            onManageStorageClick = { showStorageDetailsSheet = true }
                        )
                    }

                    SettingsSubScreen.LANGUAGE -> {
                        LanguageSettingsContent(
                            bColors = bColors,
                            selectedLanguage = selectedLanguage,
                            onLanguageSelected = {
                                selectedLanguage = it
                                currentSubScreen = SettingsSubScreen.MAIN
                                Toast.makeText(context, "Language set to $it", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    SettingsSubScreen.HELP -> {
                        HelpSettingsContent(
                            bColors = bColors
                        )
                    }
                }
            }
        }

        // --- Dialogs & Sheets ---

        if (showQrDialog) {
            PersonalQrDialog(
                userProfile = userProfile,
                onDismiss = { showQrDialog = false }
            )
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose theme", fontWeight = FontWeight.Bold, color = bColors.textPrimary) },
                text = {
                    Column {
                        listOf("System default", "Light", "Dark").forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedThemeMode = mode
                                        if (mode == "Dark" && !isDark) viewModel.toggleTheme()
                                        if (mode == "Light" && isDark) viewModel.toggleTheme()
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedThemeMode == mode,
                                    onClick = {
                                        selectedThemeMode = mode
                                        if (mode == "Dark" && !isDark) viewModel.toggleTheme()
                                        if (mode == "Light" && isDark) viewModel.toggleTheme()
                                        showThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = BharatGreenLight)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(mode, fontSize = 15.sp, color = bColors.textPrimary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("CANCEL", color = BharatGreenLight, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface
            )
        }

        if (showLastSeenDialog) {
            AlertDialog(
                onDismissRequest = { showLastSeenDialog = false },
                title = { Text("Who can see my last seen & online", fontWeight = FontWeight.Bold, color = bColors.textPrimary) },
                text = {
                    Column {
                        listOf("Everyone", "My contacts", "My contacts except...", "Nobody").forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        lastSeenPrivacy = opt
                                        showLastSeenDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = lastSeenPrivacy == opt,
                                    onClick = {
                                        lastSeenPrivacy = opt
                                        showLastSeenDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = BharatGreenLight)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(opt, fontSize = 14.sp, color = bColors.textPrimary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLastSeenDialog = false }) {
                        Text("CANCEL", color = BharatGreenLight, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface
            )
        }

        if (showDisappearingDialog) {
            AlertDialog(
                onDismissRequest = { showDisappearingDialog = false },
                title = { Text("Default message timer", fontWeight = FontWeight.Bold, color = bColors.textPrimary) },
                text = {
                    Column {
                        Text(
                            text = "Start new chats with disappearing messages set to your timer.",
                            fontSize = 13.sp,
                            color = bColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("24 hours", "7 days", "90 days", "Off").forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        disappearingTimer = opt
                                        showDisappearingDialog = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = disappearingTimer == opt,
                                    onClick = {
                                        disappearingTimer = opt
                                        showDisappearingDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = BharatGreenLight)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(opt, fontSize = 14.sp, color = bColors.textPrimary)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDisappearingDialog = false }) {
                        Text("DONE", color = BharatGreenLight, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface
            )
        }

        if (showLanguageSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                LanguageSelectionSheetContent(
                    bColors = bColors,
                    selectedLanguage = selectedLanguage,
                    onSelect = {
                        selectedLanguage = it
                        showLanguageSheet = false
                        Toast.makeText(context, "Language set to $it", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        if (showTwoStepDialog) {
            AlertDialog(
                onDismissRequest = { showTwoStepDialog = false },
                title = { Text("Two-Step Verification", fontWeight = FontWeight.Bold, color = bColors.textPrimary) },
                text = {
                    Column {
                        Text(
                            text = "Enter a 6-digit PIN that you'll be asked for when you register your phone number again with VenzoInd.",
                            fontSize = 13.sp,
                            color = bColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = twoStepPinInput,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) twoStepPinInput = it },
                            placeholder = { Text("••••••") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (twoStepPinInput.length == 6) {
                                twoStepEnabled = true
                                showTwoStepDialog = false
                                Toast.makeText(context, "Two-step verification enabled!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter 6 digits", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight)
                    ) {
                        Text("ENABLE PIN", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTwoStepDialog = false }) {
                        Text("CANCEL", color = bColors.textSecondary)
                    }
                },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface
            )
        }

        if (showClearChatsDialog) {
            AlertDialog(
                onDismissRequest = { showClearChatsDialog = false },
                title = { Text("Clear all chats?", fontWeight = FontWeight.Bold, color = bColors.textPrimary) },
                text = {
                    Text(
                        "All messages and conversation histories will be permanently deleted from this device.",
                        color = bColors.textSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearChatsDialog = false
                            viewModel.clearAllChats()
                            Toast.makeText(context, "All chats & messages deleted", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("CLEAR CHATS", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearChatsDialog = false }) {
                        Text("CANCEL", color = bColors.textSecondary)
                    }
                },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface
            )
        }

        if (showClearAllDemoDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDemoDialog = false },
                title = { Text("Remove Demo Names & Data?", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
                text = {
                    Text(
                        "This will remove all demo contacts (Vikram, Ananya, ISRO, Dr. Priya, Techies, etc.) and conversations from your app.",
                        color = bColors.textSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearAllDemoDialog = false
                            viewModel.clearAllDemoData()
                            Toast.makeText(context, "All demo data removed successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("REMOVE DEMO DATA", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDemoDialog = false }) {
                        Text("CANCEL", color = bColors.textSecondary)
                    }
                },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface
            )
        }

        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                title = { Text("Delete this account?", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
                text = {
                    Text(
                        "Deleting your account will:\n• Delete your account info and profile photo\n• Delete you from all VenzoInd groups\n• Delete your message backup",
                        color = bColors.textSecondary,
                        fontSize = 13.5.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteAccountDialog = false
                            viewModel.deleteAccountAndReset()
                            Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("DELETE ACCOUNT", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text("CANCEL", color = bColors.textSecondary)
                    }
                },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface
            )
        }

        if (showChatTransferDialog) {
            ChatTransferDialog(
                viewModel = viewModel,
                onDismiss = { showChatTransferDialog = false }
            )
        }

        if (showStorageDetailsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showStorageDetailsSheet = false },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text("Manage storage", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = bColors.textPrimary)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("1.4 GB used of 128 GB device storage", fontSize = 13.sp, color = bColors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 0.12f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = BharatGreenLight,
                        trackColor = Color(0x3364748B)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Chats storage breakdown:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = bColors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "Quantum India Devs" to "450 MB",
                        "VenzoInd Official Channel" to "320 MB",
                        "Aarav Sharma" to "140 MB",
                        "Priya Patel" to "85 MB"
                    ).forEach { (chat, size) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(chat, fontSize = 14.sp, color = bColors.textPrimary)
                            Text(size, fontSize = 13.sp, color = BharatElectricCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// MAIN SETTINGS LIST
// ---------------------------------------------------------------------
@Composable
private fun MainSettingsContent(
    userProfile: com.example.data.model.UserProfile,
    bColors: BharatExtendedColors,
    isDark: Boolean,
    selectedLanguage: String,
    onProfileClick: () -> Unit,
    onQrClick: () -> Unit,
    onAccountClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onChatsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onStorageClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onHelpClick: () -> Unit,
    onInviteClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // WhatsApp Top Profile Header Item (Matching screenshot: circular photo, name, status bubble pill, QR icon)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() }
                    .testTag("settings_profile_header"),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val customColor = try {
                        Color(android.graphics.Color.parseColor(userProfile.avatarColorHex))
                    } catch (e: Exception) {
                        Color(0xFF64748B)
                    }

                    // Avatar (Circular)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (userProfile.profilePicUri.isNotBlank()) Color(0xFFE2E8F0)
                                else Color(0xFF64748B)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfile.profilePicUri.isNotBlank()) {
                            AsyncImage(
                                model = userProfile.profilePicUri,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = userProfile.avatarInitial.ifBlank { "VA" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile.name.ifBlank { "VenzoInd User" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = bColors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Status pill capsule (matching screenshot)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (bColors.isDark) Color(0xFF1F2C34) else Color(0xFFF0F2F5),
                            border = BorderStroke(0.8.dp, if (bColors.isDark) Color(0xFF2A3942) else Color(0xFFE9EDEF)),
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                        ) {
                            Text(
                                text = if (userProfile.statusBio.isNotBlank()) userProfile.statusBio else "Hey there! I am using VenzoInd.",
                                fontSize = 12.5.sp,
                                color = bColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // QR Code Icon
                    IconButton(
                        onClick = onQrClick,
                        modifier = Modifier.testTag("settings_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "My QR Code",
                            tint = if (bColors.isDark) Color(0xFF8696A0) else Color(0xFF111B21),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = if (bColors.isDark) Color(0xFF1F2C34) else Color(0xFFF0F2F5),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Meta / Sovereign Verified Badge Row
        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Verified,
                title = "Meta Verified",
                subtitle = "Get a verified badge and other benefits",
                onClick = { onProfileClick() },
                bColors = bColors,
                testTag = "settings_meta_verified_row"
            )
        }

        // WhatsApp Navigation Rows
        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Key,
                title = "Account",
                subtitle = "Security notifications, change number",
                onClick = onAccountClick,
                bColors = bColors,
                testTag = "settings_account_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Lock,
                title = "Privacy",
                subtitle = "Blocked accounts, disappearing messages",
                onClick = onPrivacyClick,
                bColors = bColors,
                testTag = "settings_privacy_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.ContactPage,
                title = "Lists",
                subtitle = "Manage people and groups",
                onClick = onAvatarClick,
                bColors = bColors,
                testTag = "settings_lists_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Chat,
                title = "Chats",
                subtitle = "Theme, wallpapers, chat history",
                onClick = onChatsClick,
                bColors = bColors,
                testTag = "settings_chats_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Palette,
                title = "Appearance",
                subtitle = "Chat theme, app icon, app theme",
                onClick = onChatsClick,
                bColors = bColors,
                testTag = "settings_appearance_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Notifications,
                title = "Notifications",
                subtitle = "Message, group & call tones",
                onClick = onNotificationsClick,
                bColors = bColors,
                testTag = "settings_notifications_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.DataUsage,
                title = "Storage and data",
                subtitle = "Network usage, auto-download",
                onClick = onStorageClick,
                bColors = bColors,
                testTag = "settings_storage_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Link,
                title = "Facebook & Instagram",
                subtitle = "Connect to reach more customers",
                onClick = { onInviteClick() },
                bColors = bColors,
                testTag = "settings_meta_link_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.AccessibilityNew,
                title = "Accessibility",
                subtitle = "Increase contrast, animation",
                onClick = { onHelpClick() },
                bColors = bColors,
                testTag = "settings_accessibility_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Language,
                title = "App language",
                subtitle = selectedLanguage,
                onClick = onLanguageClick,
                bColors = bColors,
                testTag = "settings_language_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.HelpOutline,
                title = "Help and feedback",
                subtitle = "Help center, contact us, privacy policy",
                onClick = onHelpClick,
                bColors = bColors,
                testTag = "settings_help_row"
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.GroupAdd,
                title = "Invite a contact",
                subtitle = "",
                onClick = onInviteClick,
                bColors = bColors,
                testTag = "settings_invite_row"
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "from",
                    fontSize = 11.sp,
                    color = bColors.textMuted
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = null,
                        tint = BharatGreenLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "VenzoInd Technologies",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = bColors.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Version 2.5.0 (E2EE Signal Protocol)",
                    fontSize = 10.5.sp,
                    color = bColors.textMuted
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// ---------------------------------------------------------------------
// SUB-SCREENS
// ---------------------------------------------------------------------

@Composable
private fun AccountSettingsContent(
    bColors: BharatExtendedColors,
    twoStepEnabled: Boolean,
    onTwoStepClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Security,
                title = "Security notifications",
                subtitle = "Get notified when security code changes",
                onClick = { Toast.makeText(context, "Security notifications active", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Fingerprint,
                title = "Passkeys",
                subtitle = "Create a passkey for secure and easy login",
                onClick = { Toast.makeText(context, "Passkey setup initiated", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Email,
                title = "Email address",
                subtitle = "user@venzoind.com",
                onClick = { Toast.makeText(context, "Email verification linked", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Pin,
                title = "Two-step verification",
                subtitle = if (twoStepEnabled) "Enabled (6-digit PIN active)" else "Add extra security with a PIN",
                onClick = onTwoStepClick,
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.PhoneAndroid,
                title = "Change number",
                subtitle = "Migrate account info, groups & settings",
                onClick = { Toast.makeText(context, "Change number flow", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Description,
                title = "Request account info",
                subtitle = "Download sovereign data & E2EE report",
                onClick = { Toast.makeText(context, "Report requested. Ready in 3 days.", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.ExitToApp,
                title = "Log out",
                subtitle = "Switch account or sign in with another phone",
                onClick = onLogoutClick,
                bColors = bColors,
                tint = BharatSaffron
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.DeleteOutline,
                title = "Delete my account",
                subtitle = "Erase all personal data, chats and groups",
                onClick = onDeleteAccountClick,
                bColors = bColors,
                tint = Color(0xFFEF4444)
            )
        }
    }
}

@Composable
private fun PrivacySettingsContent(
    bColors: BharatExtendedColors,
    lastSeen: String,
    profilePhoto: String,
    readReceipts: Boolean,
    onReadReceiptsChange: (Boolean) -> Unit,
    disappearingTimer: String,
    biometricLock: Boolean,
    onBiometricLockChange: (Boolean) -> Unit,
    onLastSeenClick: () -> Unit,
    onDisappearingClick: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "Who can see my personal info",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BharatGreenLight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Visibility,
                title = "Last seen and online",
                subtitle = lastSeen,
                onClick = onLastSeenClick,
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.AccountCircle,
                title = "Profile photo",
                subtitle = profilePhoto,
                onClick = onLastSeenClick,
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Info,
                title = "About",
                subtitle = "Everyone",
                onClick = onLastSeenClick,
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.DonutLarge,
                title = "Status",
                subtitle = "My contacts",
                onClick = { Toast.makeText(context, "Status privacy settings", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )

            // Read receipts switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReadReceiptsChange(!readReceipts) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Read receipts", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = bColors.textPrimary)
                    Text(
                        "If turned off, you won't send or receive read receipts (blue ticks).",
                        fontSize = 12.sp,
                        color = bColors.textSecondary
                    )
                }
                Switch(
                    checked = readReceipts,
                    onCheckedChange = onReadReceiptsChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = BharatWhite, checkedTrackColor = BharatGreenLight)
                )
            }

            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Disappearing messages",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BharatGreenLight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            WhatsAppSettingRow(
                icon = Icons.Outlined.Timer,
                title = "Default message timer",
                subtitle = disappearingTimer,
                onClick = onDisappearingClick,
                bColors = bColors
            )

            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Security & App lock",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BharatGreenLight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBiometricLockChange(!biometricLock) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("App lock (Biometrics / Fingerprint)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = bColors.textPrimary)
                    Text(
                        "Require biometric authentication to unlock VenzoInd.",
                        fontSize = 12.sp,
                        color = bColors.textSecondary
                    )
                }
                Switch(
                    checked = biometricLock,
                    onCheckedChange = onBiometricLockChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = BharatWhite, checkedTrackColor = BharatGreenLight)
                )
            }

            WhatsAppSettingRow(
                icon = Icons.Outlined.Block,
                title = "Blocked contacts",
                subtitle = "None",
                onClick = { Toast.makeText(context, "Manage blocked contacts", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Shield,
                title = "Advanced",
                subtitle = "Protect IP address in calls, disable link previews",
                onClick = { Toast.makeText(context, "Advanced privacy options active", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
        }
    }
}

@Composable
private fun ChatsSettingsContent(
    bColors: BharatExtendedColors,
    themeMode: String,
    enterIsSend: Boolean,
    onEnterIsSendChange: (Boolean) -> Unit,
    mediaVisibility: Boolean,
    onMediaVisibilityChange: (Boolean) -> Unit,
    fontSize: String,
    onThemeClick: () -> Unit,
    onBackupClick: () -> Unit,
    onTransferChatsClick: () -> Unit,
    onClearChatsClick: () -> Unit,
    onClearDemoDataClick: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "Display",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BharatGreenLight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Brightness4,
                title = "Theme",
                subtitle = themeMode,
                onClick = onThemeClick,
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Wallpaper,
                title = "Wallpaper",
                subtitle = "Change chat background wallpaper",
                onClick = { Toast.makeText(context, "Wallpaper selector opened", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )

            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Chat settings",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BharatGreenLight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Enter is send
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEnterIsSendChange(!enterIsSend) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enter is send", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = bColors.textPrimary)
                    Text("Enter key will send your message", fontSize = 12.sp, color = bColors.textSecondary)
                }
                Switch(
                    checked = enterIsSend,
                    onCheckedChange = onEnterIsSendChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = BharatWhite, checkedTrackColor = BharatGreenLight)
                )
            }

            // Media visibility
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMediaVisibilityChange(!mediaVisibility) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Media visibility", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = bColors.textPrimary)
                    Text("Show newly downloaded media in your device's gallery", fontSize = 12.sp, color = bColors.textSecondary)
                }
                Switch(
                    checked = mediaVisibility,
                    onCheckedChange = onMediaVisibilityChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = BharatWhite, checkedTrackColor = BharatGreenLight)
                )
            }

            WhatsAppSettingRow(
                icon = Icons.Outlined.FormatSize,
                title = "Font size",
                subtitle = fontSize,
                onClick = { Toast.makeText(context, "Font size is $fontSize", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )

            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

            WhatsAppSettingRow(
                icon = Icons.Outlined.CloudUpload,
                title = "Chat backup",
                subtitle = "Google Drive & E2EE Cloud Backup",
                onClick = onBackupClick,
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.PhoneForwarded,
                title = "Transfer chats",
                subtitle = "Transfer chats to another phone seamlessly",
                onClick = onTransferChatsClick,
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.History,
                title = "Chat history",
                subtitle = "Export chat, clear all chats, delete all chats",
                onClick = onClearChatsClick,
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.DeleteSweep,
                title = "Remove demo names & contacts",
                subtitle = "Delete all sample contacts, chats, and calls",
                onClick = onClearDemoDataClick,
                bColors = bColors
            )
        }
    }
}

@Composable
private fun NotificationsSettingsContent(
    bColors: BharatExtendedColors,
    conversationTones: Boolean,
    onConversationTonesChange: (Boolean) -> Unit,
    highPriority: Boolean,
    onHighPriorityChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // Conversation tones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConversationTonesChange(!conversationTones) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Conversation tones", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = bColors.textPrimary)
                    Text("Play sounds for incoming and outgoing messages", fontSize = 12.sp, color = bColors.textSecondary)
                }
                Switch(
                    checked = conversationTones,
                    onCheckedChange = onConversationTonesChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = BharatWhite, checkedTrackColor = BharatGreenLight)
                )
            }

            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Messages",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BharatGreenLight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            WhatsAppSettingRow(
                icon = Icons.Outlined.MusicNote,
                title = "Notification tone",
                subtitle = "Default (VenzoInd Chime)",
                onClick = { Toast.makeText(context, "Notification tone selected", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Vibration,
                title = "Vibrate",
                subtitle = "Default",
                onClick = { Toast.makeText(context, "Vibrate set to Default", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Lightbulb,
                title = "Light",
                subtitle = "White",
                onClick = { Toast.makeText(context, "LED Light set to White", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHighPriorityChange(!highPriority) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("High priority notifications", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = bColors.textPrimary)
                    Text("Show previews of notifications at the top of the screen", fontSize = 12.sp, color = bColors.textSecondary)
                }
                Switch(
                    checked = highPriority,
                    onCheckedChange = onHighPriorityChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = BharatWhite, checkedTrackColor = BharatGreenLight)
                )
            }

            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Calls",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BharatGreenLight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            WhatsAppSettingRow(
                icon = Icons.Outlined.RingVolume,
                title = "Ringtone",
                subtitle = "Default (VenzoInd Quantum Ring)",
                onClick = { Toast.makeText(context, "Call ringtone updated", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Vibration,
                title = "Vibrate",
                subtitle = "Default",
                onClick = { Toast.makeText(context, "Call vibrate updated", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
        }
    }
}

@Composable
private fun StorageSettingsContent(
    bColors: BharatExtendedColors,
    lessDataForCalls: Boolean,
    onLessDataChange: (Boolean) -> Unit,
    hdUploadQuality: Boolean,
    onHdQualityChange: (Boolean) -> Unit,
    onManageStorageClick: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.Folder,
                title = "Manage storage",
                subtitle = "1.4 GB used (Clean large files & forwards)",
                onClick = onManageStorageClick,
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.NetworkCheck,
                title = "Network usage",
                subtitle = "4.8 GB sent • 12.1 GB received",
                onClick = { Toast.makeText(context, "Network stats detailed", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLessDataChange(!lessDataForCalls) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use less data for calls", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = bColors.textPrimary)
                    Text("Reduces data consumed during voice and video calls", fontSize = 12.sp, color = bColors.textSecondary)
                }
                Switch(
                    checked = lessDataForCalls,
                    onCheckedChange = onLessDataChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = BharatWhite, checkedTrackColor = BharatGreenLight)
                )
            }

            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Media auto-download",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BharatGreenLight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            WhatsAppSettingRow(
                icon = Icons.Outlined.CellTower,
                title = "When using mobile data",
                subtitle = "Photos, Documents",
                onClick = { Toast.makeText(context, "Configured mobile data auto-download", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Wifi,
                title = "When connected on Wi-Fi",
                subtitle = "All media (Photos, Audio, Video, Docs)",
                onClick = { Toast.makeText(context, "Configured Wi-Fi auto-download", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Flight,
                title = "When roaming",
                subtitle = "No media",
                onClick = { Toast.makeText(context, "Configured roaming auto-download", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )

            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Media upload quality",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BharatGreenLight,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHdQualityChange(!hdUploadQuality) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("HD photo and video quality", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = bColors.textPrimary)
                    Text("HD media is clearer but consumes more bandwidth", fontSize = 12.sp, color = bColors.textSecondary)
                }
                Switch(
                    checked = hdUploadQuality,
                    onCheckedChange = onHdQualityChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = BharatWhite, checkedTrackColor = BharatGreenLight)
                )
            }
        }
    }
}

@Composable
private fun LanguageSettingsContent(
    bColors: BharatExtendedColors,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val languages = listOf(
        "English (device's language)",
        "हिन्दी (Hindi)",
        "Español (Spanish)",
        "Français (French)",
        "Deutsch (German)",
        "Português (Portuguese)",
        "العربية (Arabic)",
        "বাংলা (Bengali)",
        "தமிழ் (Tamil)",
        "తెలుగు (Telugu)",
        "मराठी (Marathi)",
        "ગુજરાતી (Gujarati)",
        "Bahasa Indonesia (Indonesian)",
        "Русский (Russian)",
        "日本語 (Japanese)"
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(languages.size) { index ->
            val lang = languages[index]
            val isSelected = selectedLanguage == lang
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLanguageSelected(lang) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onLanguageSelected(lang) },
                    colors = RadioButtonDefaults.colors(selectedColor = BharatGreenLight)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = lang,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) BharatGreenLight else bColors.textPrimary
                )
            }
            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.3f))
        }
    }
}

@Composable
private fun LanguageSelectionSheetContent(
    bColors: BharatExtendedColors,
    selectedLanguage: String,
    onSelect: (String) -> Unit
) {
    val languages = listOf(
        "English (device's language)",
        "हिन्दी (Hindi)",
        "Español (Spanish)",
        "Français (French)",
        "Deutsch (German)",
        "Português (Portuguese)",
        "العربية (Arabic)",
        "বাংলা (Bengali)",
        "தமிழ் (Tamil)",
        "తెలుగు (Telugu)",
        "Bahasa Indonesia",
        "Русский (Russian)",
        "日本語 (Japanese)"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "App language",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = bColors.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
            items(languages.size) { index ->
                val lang = languages[index]
                val isSelected = selectedLanguage == lang
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(lang) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelect(lang) },
                        colors = RadioButtonDefaults.colors(selectedColor = BharatGreenLight)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = lang,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) BharatGreenLight else bColors.textPrimary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HelpSettingsContent(
    bColors: BharatExtendedColors
) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            WhatsAppSettingRow(
                icon = Icons.Outlined.HelpCenter,
                title = "Help center",
                subtitle = "Get help, contact support, FAQs",
                onClick = { Toast.makeText(context, "Opening VenzoInd Help Center", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.ContactSupport,
                title = "Contact us",
                subtitle = "Questions? Reach 24/7 sovereign support team",
                onClick = { Toast.makeText(context, "Connecting to Support AI", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Policy,
                title = "Terms and Privacy Policy",
                subtitle = "Double Ratchet Signal Protocol Terms",
                onClick = { Toast.makeText(context, "Terms & Sovereign Privacy Policy", Toast.LENGTH_SHORT).show() },
                bColors = bColors
            )
            WhatsAppSettingRow(
                icon = Icons.Outlined.Info,
                title = "App info",
                subtitle = "VenzoInd version 2.5.0 for Android",
                onClick = { Toast.makeText(context, "VenzoInd v2.5.0 • End-to-End Encrypted", Toast.LENGTH_LONG).show() },
                bColors = bColors
            )
        }
    }
}

// ---------------------------------------------------------------------
// WHATSAPP SETTING ROW COMPONENT
// ---------------------------------------------------------------------
@Composable
private fun WhatsAppSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    bColors: BharatExtendedColors,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    testTag: String = ""
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint ?: bColors.textSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = tint ?: bColors.textPrimary
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = bColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// PERSONAL QR CODE DIALOG
// ---------------------------------------------------------------------
@Composable
private fun PersonalQrDialog(
    userProfile: com.example.data.model.UserProfile,
    onDismiss: () -> Unit
) {
    val bColors = LocalBharatColors.current
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("My QR Code", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = bColors.textPrimary)
                Text("Your QR code is private. People can scan it to chat with you on VenzoInd.", fontSize = 12.sp, color = bColors.textSecondary, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // QR Mock Container
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, BharatGreenLight),
                    modifier = Modifier.size(200.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "QR Code",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(170.dp)
                        )
                        Surface(
                            shape = CircleShape,
                            color = BharatGreenLight,
                            modifier = Modifier.size(36.dp),
                            border = BorderStroke(2.dp, Color.White)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userProfile.avatarInitial.ifBlank { "V" }.take(1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = userProfile.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = bColors.textPrimary
                )
                Text(
                    text = userProfile.phone.ifBlank { "+91 98765 43210" },
                    fontSize = 13.sp,
                    color = bColors.textSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    try {
                        val shareText = "Scan to chat with me on VenzoInd Bharat Sovereign Messenger! 🇮🇳\nName: ${userProfile.name}\nPhone: ${userProfile.phone.ifBlank { "+91 98765 43210" }}\nUPI ID: ${userProfile.upiId.ifBlank { "venzo@upi" }}\nPost-Quantum Kyber-1024 Encrypted"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Connect with ${userProfile.name} on VenzoInd")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Share My QR Contact")
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open share: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SHARE QR CODE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = bColors.textSecondary)
            }
        },
        containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface
    )
}

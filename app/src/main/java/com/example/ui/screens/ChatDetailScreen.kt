package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatEntity
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.ui.components.*
import com.example.ui.dialogs.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import com.example.util.AudioRecordingState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val activeChat by viewModel.activeChat.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val smartReplies by viewModel.smartReplies.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val voiceRecordSec by viewModel.voiceRecordDurationSec.collectAsState()
    val voiceRecordingData by viewModel.voiceRecorder.recordingData.collectAsState()
    val isOtherUserTyping by viewModel.isOtherUserTyping.collectAsState()
    val typingStatusText by viewModel.typingStatusText.collectAsState()
    val typingUserName by viewModel.typingUserName.collectAsState()
    val bColors = LocalBharatColors.current

    val showUpiPay by viewModel.showUpiPaymentSheet.collectAsState()
    val showAiSummary by viewModel.showAiSummarizerDialog.collectAsState()
    val showAiTranslate by viewModel.showAiTranslatorDialog.collectAsState()
    val showAttachments by viewModel.showAttachmentOptions.collectAsState()
    val showDisappearing by viewModel.showDisappearingTimerDialog.collectAsState()
    val showWallpaper by viewModel.showWallpaperSheet.collectAsState()
    val showPollCreator by viewModel.showPollCreatorDialog.collectAsState()
    val showSecretInfo by viewModel.showSecretChatInfo.collectAsState()
    val showBiometricDialog by viewModel.showBiometricAuthDialog.collectAsState()
    val biometricPurpose by viewModel.biometricAuthPurpose.collectAsState()

    val showScheduleMessage by viewModel.showScheduleMessageDialog.collectAsState()
    val showLocationShare by viewModel.showLocationShareSheet.collectAsState()
    val showCloudDocPicker by viewModel.showCloudDocPickerSheet.collectAsState()
    val showForwardDialog by viewModel.showForwardDialog.collectAsState()
    val forwardMessagesList by viewModel.forwardSelectedMessages.collectAsState()
    val showContactProfile by viewModel.showContactProfileDialog.collectAsState()
    val activeContactProfile by viewModel.activeContactProfile.collectAsState()
    val showZoomableDp by viewModel.showZoomableDpDialog.collectAsState()
    val activeZoomableDp by viewModel.activeZoomableDp.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var showDeleteChatConfirmDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirmDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val selectedMessages = remember { mutableStateListOf<MessageEntity>() }
    val isSelectionMode by remember { derivedStateOf { selectedMessages.isNotEmpty() } }

    var selectedMessageForActions by remember { mutableStateOf<MessageEntity?>(null) }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.sendImageMessage(it.toString())
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Sovereign_Doc.pdf"
            viewModel.sendAttachment(MessageType.FILE, fileName, "5.4 MB • Encrypted Cloud")
            android.widget.Toast.makeText(context, "Attached document: $fileName", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.sendAttachment(MessageType.IMAGE, "camera_capture.jpg", "8.2 MB • High Res")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Unable to launch camera", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Camera permission is required to capture photos", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val safeLaunchCamera = {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Unable to launch camera", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording()
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required for voice notes", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val safeStartVoiceRecording = {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startVoiceRecording()
        } else {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (activeChat == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BharatSaffron)
        }
        return
    }

    val chat = activeChat!!

    Scaffold(
        topBar = {
            Surface(
                color = if (bColors.isDark) DarkSurface.copy(alpha = 0.95f) else LightSurface.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, bColors.glassBorder)
            ) {
                if (isSelectionMode) {
                    // Multi-Select Action Bar (Copy, Forward, Tag/Star, Delete, Select All)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { selectedMessages.clear() },
                                modifier = Modifier.testTag("clear_selection_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Selection", tint = bColors.textPrimary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${selectedMessages.size} selected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BharatElectricCyan
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Tag / Star Action
                            IconButton(
                                onClick = {
                                    selectedMessages.forEach { msg ->
                                        viewModel.toggleStarMessage(msg.id)
                                    }
                                    android.widget.Toast.makeText(context, "Starred / Tagged selected ⭐", android.widget.Toast.LENGTH_SHORT).show()
                                    selectedMessages.clear()
                                },
                                modifier = Modifier.testTag("star_selected_messages_button")
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Tag/Star", tint = GoldAccent)
                            }

                            // Copy to Clipboard Action
                            IconButton(
                                onClick = {
                                    val textToCopy = selectedMessages.joinToString("\n") { it.text }
                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                    android.widget.Toast.makeText(context, "Copied ${selectedMessages.size} message(s)", android.widget.Toast.LENGTH_SHORT).show()
                                    selectedMessages.clear()
                                },
                                modifier = Modifier.testTag("copy_selected_messages_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BharatElectricCyan)
                            }

                            // Forward Action
                            IconButton(
                                onClick = {
                                    viewModel.forwardSelectedMessages.value = selectedMessages.toList()
                                    viewModel.showForwardDialog.value = true
                                    selectedMessages.clear()
                                },
                                modifier = Modifier.testTag("forward_selected_messages_button")
                            ) {
                                Icon(Icons.Default.Forward, contentDescription = "Forward", tint = BharatGreenLight)
                            }

                            // Delete Action
                            IconButton(
                                onClick = {
                                    val count = selectedMessages.size
                                    viewModel.deleteMessages(selectedMessages.map { it.id })
                                    android.widget.Toast.makeText(context, "Deleted $count message(s)", android.widget.Toast.LENGTH_SHORT).show()
                                    selectedMessages.clear()
                                },
                                modifier = Modifier.testTag("delete_selected_messages_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
                            }

                            // Select All Action
                            IconButton(
                                onClick = {
                                    if (selectedMessages.size == messages.size) {
                                        selectedMessages.clear()
                                    } else {
                                        selectedMessages.clear()
                                        selectedMessages.addAll(messages)
                                    }
                                },
                                modifier = Modifier.testTag("select_all_messages_button")
                            ) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = bColors.textSecondary)
                            }
                        }
                    }
                } else {
                    // Standard Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.closeChat() },
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = bColors.textPrimary
                            )
                        }

                        // Avatar with Zoom Tap trigger & optional typing halo
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clickable {
                                    viewModel.openZoomableDp(
                                        title = chat.title,
                                        initial = chat.avatarInitial,
                                        colorHex = chat.avatarColorHex,
                                        subtitle = chat.subtitle
                                    )
                                }
                                .testTag("chat_header_avatar_zoom")
                        ) {
                            if (isOtherUserTyping) {
                                val infiniteTransition = rememberInfiniteTransition(label = "avatar_typing_halo")
                                val haloAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 0.85f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(700, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "halo_alpha"
                                )
                                val haloColor = if (chat.isSecret) SecretChatPink else if (chat.isAiAssistant) BharatElectricCyan else BharatGreenLight
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, haloColor.copy(alpha = haloAlpha), CircleShape)
                                )
                            }

                            StatusRingAvatar(
                                initial = chat.avatarInitial,
                                avatarColorHex = chat.avatarColorHex,
                                size = 40.dp,
                                isOnline = chat.isOnline,
                                isAiBot = chat.isAiAssistant
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Contact Info
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.showSecretChatInfo.value = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = chat.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (chat.isSecret) SecretChatPink else bColors.textPrimary,
                                    maxLines = 1
                                )
                                if (chat.isVerifiedBusiness) {
                                    VerifiedBadge(isBusiness = true)
                                } else if (chat.isAiAssistant) {
                                    VerifiedBadge(isBusiness = false)
                                }
                            }

                            AnimatedContent(
                                targetState = isOtherUserTyping,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                                },
                                label = "typing_status_anim"
                            ) { typingActive ->
                                if (typingActive) {
                                    val indicatorColor = if (chat.isSecret) SecretChatPink else if (chat.isAiAssistant) BharatElectricCyan else BharatGreenLight
                                    PulsingTypingIndicator(
                                        text = if (chat.isGroup && typingUserName.isNotBlank()) "$typingUserName is $typingStatusText" else typingStatusText,
                                        color = indicatorColor
                                    )
                                } else {
                                    Text(
                                        text = if (chat.isSecret) "🔒 Self-destruct 10s • Quantum Encrypted" else if (chat.isOnline) "Active now" else chat.subtitle,
                                        fontSize = 11.sp,
                                        color = if (chat.isSecret) SecretChatPink else if (chat.isOnline) OnlineGreen else bColors.textMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Call Actions
                        IconButton(
                            onClick = { viewModel.startCall(chat.title, chat.avatarInitial, isVideo = false) },
                            modifier = Modifier.testTag("voice_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "HD Voice Call",
                                tint = BharatGreenLight,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.startCall(chat.title, chat.avatarInitial, isVideo = true) },
                            modifier = Modifier.testTag("video_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "4K Video Call",
                                tint = BharatElectricCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // More Menu
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.testTag("chat_more_options_button")) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = bColors.textSecondary
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🖼️ View Full Profile (Zoom DP)", color = BharatElectricCyan) },
                                    leadingIcon = { Icon(Icons.Default.ZoomIn, null, tint = BharatElectricCyan) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.openZoomableDp(
                                            title = chat.title,
                                            initial = chat.avatarInitial,
                                            colorHex = chat.avatarColorHex,
                                            subtitle = chat.subtitle
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("⏱️ Schedule Message", color = BharatSaffron) },
                                    leadingIcon = { Icon(Icons.Default.Schedule, null, tint = BharatSaffron) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.showScheduleMessageDialog.value = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("💬 Test Real-Time Typing", color = BharatGreenLight) },
                                    leadingIcon = { Icon(Icons.Default.Keyboard, null, tint = BharatGreenLight) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.simulateTyping()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("✨ Summarize with AI", color = BharatSaffronLight) },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = BharatSaffron) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.summarizeActiveChat()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🌐 Live Translation", color = bColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Translate, null, tint = BharatElectricCyan) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.showAiTranslatorDialog.value = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("💳 Send UPI Money", color = bColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, tint = BharatGreenLight) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.triggerUpiSheetWithBiometrics()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🔒 Disappearing Timer", color = bColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Timer, null, tint = SecretChatPink) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.showDisappearingTimerDialog.value = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🎨 Chat Wallpaper", color = bColors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Wallpaper, null, tint = bColors.textSecondary) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.showWallpaperSheet.value = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🧹 Clear History", color = BharatSaffron) },
                                    leadingIcon = { Icon(Icons.Outlined.CleaningServices, null, tint = BharatSaffron) },
                                    onClick = {
                                        showMenu = false
                                        showClearHistoryConfirmDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🗑️ Delete Chat", color = Color(0xFFEF4444)) },
                                    leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = Color(0xFFEF4444)) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteChatConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (bColors.isDark) DarkSurface.copy(alpha = 0.95f) else LightSurface.copy(alpha = 0.95f))
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // AI Smart Replies Row
                if (smartReplies.isNotEmpty() && !isRecordingVoice) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(smartReplies) { reply ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BharatNavy.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Brush.horizontalGradient(listOf(BharatSaffron, BharatElectricCyan))
                                ),
                                modifier = Modifier
                                    .clickable {
                                        viewModel.sendMessage(reply)
                                    }
                                    .testTag("smart_reply_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = BharatSaffron,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = reply,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BharatWhite
                                    )
                                }
                            }
                        }
                    }
                }

                // Voice Recording Live Bar with Live Waveform, Preview & Send vs Normal Input
                if (isRecordingVoice || voiceRecordingData.state != AudioRecordingState.IDLE) {
                    val isPreviewReady = voiceRecordingData.state == AudioRecordingState.RECORDED || voiceRecordingData.state == AudioRecordingState.PLAYING_PREVIEW
                    val durationVal = if (isPreviewReady) voiceRecordingData.durationSeconds else voiceRecordSec
                    val durationFormatted = "00:${durationVal.toString().padStart(2, '0')}"

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("voice_recording_panel"),
                        shape = RoundedCornerShape(28.dp),
                        backgroundColor = if (isPreviewReady) {
                            if (bColors.isDark) Color(0xCC0F172A) else Color(0xEEF8FAFC)
                        } else {
                            if (bColors.isDark) Color(0xCC1E1B4B) else Color(0xEEFDF2F8)
                        },
                        borderColor = if (isPreviewReady) BharatElectricCyan.copy(alpha = 0.5f) else SecretChatPink.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left section: Status icon / Preview Play button & timer
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isPreviewReady) {
                                    IconButton(
                                        onClick = { viewModel.toggleVoicePreviewPlayback() },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(BharatElectricCyan)
                                            .testTag("voice_preview_play_button")
                                    ) {
                                        Icon(
                                            imageVector = if (voiceRecordingData.isPlayingPreview) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play Preview",
                                            tint = Color(0xFF0F172A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(RoseError)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = if (isPreviewReady) "Preview ($durationFormatted)" else "Recording $durationFormatted",
                                    color = if (isPreviewReady) BharatElectricCyan else SecretChatPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Live Waveform Visualizer
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    val amps = voiceRecordingData.amplitudes
                                    val waveformStr = if (amps.isNotEmpty()) amps.joinToString(",") else "20,40,70,30,85,60,90,40,65,80"
                                    AudioWaveformVisualizer(
                                        waveformStr = waveformStr,
                                        isPlaying = !isPreviewReady || voiceRecordingData.isPlayingPreview,
                                        activeColor = if (isPreviewReady) BharatElectricCyan else SecretChatPink,
                                        inactiveColor = Color(0x4464748B)
                                    )
                                }
                            }

                            // Right action buttons: Pause/Review, Delete, and Send
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Delete/Discard button
                                IconButton(
                                    onClick = { viewModel.cancelVoiceRecording() },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .testTag("voice_recording_discard_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Discard",
                                        tint = RoseError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Pause for preview button (if currently recording)
                                if (!isPreviewReady) {
                                    IconButton(
                                        onClick = { viewModel.pauseVoiceRecordingForPreview() },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(BharatNavyLight.copy(alpha = 0.5f))
                                            .testTag("voice_recording_pause_preview_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Pause,
                                            contentDescription = "Review",
                                            tint = BharatElectricCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Send Audio Note button
                                IconButton(
                                    onClick = { viewModel.stopAndSendVoiceRecording() },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(BharatGreenLight, BharatGreenDark)
                                            )
                                        )
                                        .testTag("voice_recording_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Voice Note",
                                        tint = BharatWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Replying To Message Banner
                        if (replyingToMessage != null) {
                            val replyMsg = replyingToMessage!!
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = if (bColors.isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BharatElectricCyan.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(BharatElectricCyan)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Replying to ${replyMsg.senderName}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = BharatElectricCyan
                                            )
                                            Text(
                                                text = if (replyMsg.messageType == MessageType.IMAGE.name) "📷 Photo"
                                                       else if (replyMsg.messageType == MessageType.UPI_PAYMENT.name) "💳 UPI Transfer ₹${replyMsg.upiAmount?.toInt()}"
                                                       else replyMsg.text.take(60),
                                                fontSize = 11.5.sp,
                                                color = bColors.textSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { replyingToMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Cancel reply",
                                            tint = bColors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Attachment Button (+)
                            IconButton(
                                onClick = { viewModel.showAttachmentOptions.value = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("attachment_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Attach",
                                    tint = BharatSaffron,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Quick Gallery Button
                            IconButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("quick_gallery_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = "Gallery",
                                    tint = BharatElectricCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Quick Camera Button
                            IconButton(
                                onClick = { safeLaunchCamera() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("quick_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Camera",
                                    tint = BharatElectricCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // UPI Quick Pay icon
                            IconButton(
                                onClick = { viewModel.triggerUpiSheetWithBiometrics() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("upi_pay_quick_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyRupee,
                                    contentDescription = "UPI Pay",
                                    tint = BharatGreenLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Input Field
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp, max = 100.dp),
                                shape = RoundedCornerShape(22.dp),
                                color = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        placeholder = {
                                            Text(
                                                if (chat.isSecret) "🔒 Encrypted secret..." else "Message in English, हिंदी...",
                                                color = bColors.textMuted,
                                                fontSize = 13.sp
                                            )
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedTextColor = bColors.textPrimary,
                                            unfocusedTextColor = bColors.textPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("chat_message_input")
                                    )
                                }
                            }

                            // Mic or Send Button
                            if (inputText.isBlank()) {
                                IconButton(
                                    onClick = { safeStartVoiceRecording() },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(BharatNavyLight)
                                        .testTag("voice_record_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Record Voice",
                                        tint = BharatWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        val text = inputText
                                        val repText = replyingToMessage?.text
                                        val repSender = replyingToMessage?.senderName
                                        inputText = ""
                                        replyingToMessage = null
                                        viewModel.sendMessage(
                                            text = text,
                                            replyToText = repText,
                                            replyToSender = repSender
                                        )
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(BharatSaffron, BharatSaffronLight)
                                            )
                                        )
                                        .testTag("send_message_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = BharatWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    if (bColors.isDark) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF0A0F1E), Color(0xFF060913), DarkBackground)
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))
                        )
                    }
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AI Smart Toolbar Banner inside chat
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = BharatNavy.copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BharatSaffronLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Copilot Active",
                                    color = BharatWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BharatSaffron.copy(alpha = 0.2f),
                                    modifier = Modifier.clickable { viewModel.summarizeActiveChat() }
                                ) {
                                    Text(
                                        text = "⚡ Summarize",
                                        color = BharatSaffronLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BharatElectricCyan.copy(alpha = 0.2f),
                                    modifier = Modifier.clickable { viewModel.showAiTranslatorDialog.value = true }
                                ) {
                                    Text(
                                        text = "🌐 Translate",
                                        color = BharatElectricCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Messages
                items(messages, key = { it.id }) { message ->
                    val isSelected = selectedMessages.any { it.id == message.id }
                    MessageBubble(
                        message = message,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) {
                                    selectedMessages.removeAll { it.id == message.id }
                                } else {
                                    selectedMessages.add(message)
                                }
                            } else {
                                selectedMessageForActions = message
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                selectedMessages.add(message)
                            } else {
                                if (isSelected) {
                                    selectedMessages.removeAll { it.id == message.id }
                                } else {
                                    selectedMessages.add(message)
                                }
                            }
                        },
                        onTranslate = {
                            viewModel.targetTranslateMessageId.value = message.id
                            viewModel.showAiTranslatorDialog.value = true
                        },
                        onPlayTts = {
                            viewModel.ttsManager.speakText(message.translatedText ?: message.text)
                        },
                        onVotePoll = { optionIdx ->
                            viewModel.votePoll(message.id, optionIdx)
                        }
                    )
                }
            }
        }
    }

    // Context Menu Bottom Sheet on Message Click/Tap
    if (selectedMessageForActions != null) {
        val targetMsg = selectedMessageForActions!!
        MessageActionBottomSheet(
            message = targetMsg,
            onDismiss = { selectedMessageForActions = null },
            onReaction = { emoji ->
                viewModel.reactToMessage(targetMsg.id, emoji)
                selectedMessageForActions = null
            },
            onReply = {
                replyingToMessage = targetMsg
                selectedMessageForActions = null
            },
            onCopy = {
                clipboardManager.setText(AnnotatedString(targetMsg.text))
                android.widget.Toast.makeText(context, "Message copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                selectedMessageForActions = null
            },
            onForward = {
                viewModel.forwardSelectedMessages.value = listOf(targetMsg)
                viewModel.showForwardDialog.value = true
                selectedMessageForActions = null
            },
            onStar = {
                viewModel.toggleStarMessage(targetMsg.id)
                android.widget.Toast.makeText(
                    context,
                    if (targetMsg.isStarred) "Removed star" else "Starred message ⭐",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                selectedMessageForActions = null
            },
            onDelete = {
                viewModel.deleteMessages(listOf(targetMsg.id))
                android.widget.Toast.makeText(context, "Message deleted 🗑️", android.widget.Toast.LENGTH_SHORT).show()
                selectedMessageForActions = null
            },
            onTranslate = {
                viewModel.targetTranslateMessageId.value = targetMsg.id
                viewModel.showAiTranslatorDialog.value = true
                selectedMessageForActions = null
            },
            onPlayTts = {
                viewModel.ttsManager.speakText(targetMsg.translatedText ?: targetMsg.text)
                selectedMessageForActions = null
            },
            onSelect = {
                selectedMessages.add(targetMsg)
                selectedMessageForActions = null
            }
        )
    }

    if (showUpiPay) {
        UpiPaymentBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.showUpiPaymentSheet.value = false }
        )
    }

    if (showAiSummary) {
        AiSummarizerDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showAiSummarizerDialog.value = false }
        )
    }

    if (showAiTranslate) {
        AiTranslatorDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showAiTranslatorDialog.value = false }
        )
    }

    if (showAttachments) {
        AttachmentOptionsBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.showAttachmentOptions.value = false },
            onPickGallery = { galleryLauncher.launch("image/*") },
            onTakePhoto = { safeLaunchCamera() },
            onRecordAudio = { safeStartVoiceRecording() },
            onPickDocument = { docPickerLauncher.launch("*/*") }
        )
    }

    if (showLocationShare) {
        LocationShareBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.showLocationShareSheet.value = false }
        )
    }

    if (showCloudDocPicker) {
        CloudDocPickerBottomSheet(
            viewModel = viewModel,
            onPickSystemFile = { docPickerLauncher.launch("*/*") },
            onDismiss = { viewModel.showCloudDocPickerSheet.value = false }
        )
    }

    if (showScheduleMessage) {
        ScheduleMessageDialog(
            chatId = chat.id,
            onSchedule = { text, time, label ->
                viewModel.scheduleMessage(chat.id, text, time, label)
            },
            onDismiss = { viewModel.showScheduleMessageDialog.value = false }
        )
    }

    if (showForwardDialog) {
        ForwardMessageDialog(
            viewModel = viewModel,
            messages = forwardMessagesList,
            onDismiss = { viewModel.showForwardDialog.value = false }
        )
    }

    if (showContactProfile && activeContactProfile != null) {
        ContactProfileDialog(
            viewModel = viewModel,
            contact = activeContactProfile!!,
            onDismiss = { viewModel.showContactProfileDialog.value = false }
        )
    }

    if (showZoomableDp && activeZoomableDp != null) {
        com.example.ui.components.ZoomableProfilePicDialog(
            title = activeZoomableDp!!.title,
            subtitle = activeZoomableDp!!.subtitle,
            avatarInitial = activeZoomableDp!!.initial,
            avatarColorHex = activeZoomableDp!!.colorHex,
            imageUri = activeZoomableDp!!.imageUri,
            onDismiss = { viewModel.closeZoomableDp() }
        )
    }

    if (showDisappearing) {
        DisappearingMessagesDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showDisappearingTimerDialog.value = false }
        )
    }

    if (showWallpaper) {
        ChatWallpaperBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.showWallpaperSheet.value = false }
        )
    }

    if (showPollCreator) {
        PollCreatorDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showPollCreatorDialog.value = false }
        )
    }

    if (showSecretInfo) {
        SecretChatInfoDialog(
            onDismiss = { viewModel.showSecretChatInfo.value = false }
        )
    }

    if (showBiometricDialog) {
        BiometricAuthDialog(
            purpose = biometricPurpose,
            onSuccess = { viewModel.completeBiometricAuth(true) },
            onDismiss = { viewModel.completeBiometricAuth(false) }
        )
    }

    if (showDeleteChatConfirmDialog && activeChat != null) {
        AlertDialog(
            onDismissRequest = { showDeleteChatConfirmDialog = false },
            title = { Text("Delete conversation with ${activeChat!!.title}?", fontWeight = FontWeight.Bold, color = bColors.textPrimary) },
            text = { Text("This chat and all its messages will be permanently deleted from this device.", color = bColors.textSecondary, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteChatConfirmDialog = false
                        viewModel.deleteChat(activeChat!!.id)
                        android.widget.Toast.makeText(context, "Chat deleted", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatConfirmDialog = false }) {
                    Text("CANCEL", color = bColors.textSecondary)
                }
            },
            containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
        )
    }

    if (showClearHistoryConfirmDialog && activeChat != null) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirmDialog = false },
            title = { Text("Clear chat history?", fontWeight = FontWeight.Bold, color = bColors.textPrimary) },
            text = { Text("All messages in this chat will be deleted.", color = bColors.textSecondary, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearHistoryConfirmDialog = false
                        viewModel.clearChatHistory(activeChat!!.id)
                        android.widget.Toast.makeText(context, "History cleared", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron)
                ) {
                    Text("CLEAR", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirmDialog = false }) {
                    Text("CANCEL", color = bColors.textSecondary)
                }
            },
            containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionBottomSheet(
    message: MessageEntity,
    onDismiss: () -> Unit,
    onReaction: (String) -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    onTranslate: () -> Unit,
    onPlayTts: () -> Unit,
    onSelect: () -> Unit
) {
    val bColors = LocalBharatColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(bColors.textMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Quick Emoji Reactions
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (bColors.isDark) DarkSurface else LightSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("❤️", "👍", "😂", "😮", "🙏", "🔥", "🚀", "🇮🇳").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 26.sp,
                            modifier = Modifier
                                .clickable { onReaction(emoji) }
                                .testTag("context_reaction_$emoji")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Message Preview Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (bColors.isDark) DarkSurface.copy(alpha = 0.7f) else LightSurface.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, bColors.glassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BharatElectricCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = message.senderName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = BharatElectricCyan
                        )
                        Text(
                            text = if (message.messageType == MessageType.IMAGE.name) "📷 Photo attachment"
                                   else if (message.messageType == MessageType.UPI_PAYMENT.name) "💳 UPI Transfer ₹${message.upiAmount?.toInt()}"
                                   else message.text,
                            fontSize = 12.sp,
                            color = bColors.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Items Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Reply Action ("Recent with message")
                ContextActionButton(
                    icon = Icons.Default.Reply,
                    label = "Reply",
                    tint = BharatElectricCyan,
                    onClick = onReply,
                    testTag = "context_reply_button"
                )

                // Copy Action
                ContextActionButton(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy",
                    tint = BharatElectricCyan,
                    onClick = onCopy,
                    testTag = "context_copy_button"
                )

                // Forward Action
                ContextActionButton(
                    icon = Icons.Default.Forward,
                    label = "Forward",
                    tint = BharatGreenLight,
                    onClick = onForward,
                    testTag = "context_forward_button"
                )

                // Tag / Star Action
                ContextActionButton(
                    icon = if (message.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                    label = if (message.isStarred) "Unstar" else "Tag/Star",
                    tint = GoldAccent,
                    onClick = onStar,
                    testTag = "context_star_button"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Items Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Read Aloud Action
                ContextActionButton(
                    icon = Icons.Outlined.VolumeUp,
                    label = "Read Aloud",
                    tint = BharatSaffronLight,
                    onClick = onPlayTts,
                    testTag = "context_tts_button"
                )

                // Translate Action
                ContextActionButton(
                    icon = Icons.Outlined.Translate,
                    label = "Translate",
                    tint = BharatElectricCyan,
                    onClick = onTranslate,
                    testTag = "context_translate_button"
                )

                // Multi-select Action
                ContextActionButton(
                    icon = Icons.Default.CheckCircleOutline,
                    label = "Select",
                    tint = bColors.textPrimary,
                    onClick = onSelect,
                    testTag = "context_select_button"
                )

                // Delete Action
                ContextActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    tint = RoseError,
                    onClick = onDelete,
                    testTag = "context_delete_button"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ContextActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val bColors = LocalBharatColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 10.dp)
            .testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.15f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = bColors.textPrimary
        )
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTranslate: () -> Unit,
    onPlayTts: () -> Unit,
    onVotePoll: ((Int) -> Unit)? = null
) {
    val isMe = message.isFromMe
    val bColors = LocalBharatColors.current
    val localContext = androidx.compose.ui.platform.LocalContext.current

    if (message.messageType == MessageType.SYSTEM_SECURITY.name) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            QuantumShieldBadge(text = message.text)
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .then(
                if (isSelected) Modifier.background(BharatElectricCyan.copy(alpha = 0.15f))
                else Modifier
            ),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected && !isMe) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = BharatElectricCyan,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(20.dp)
            )
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMe) 18.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 18.dp
                ),
                color = if (isMe) {
                    if (message.isSecretExpiring) Color(0xFF831843)
                    else Color(0xFF1E3A8A)
                } else {
                    if (bColors.isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                },
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) BharatElectricCyan
                    else if (isMe) {
                        if (message.isSecretExpiring) SecretChatPink.copy(alpha = 0.5f)
                        else BharatElectricCyan.copy(alpha = 0.35f)
                    } else {
                        bColors.glassBorder
                    }
                ),
                modifier = Modifier
                    .clickable { onClick() }
                    .testTag("message_bubble_${message.id}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Sender name if not me
                    if (!isMe && message.senderName != "You") {
                        Text(
                            text = message.senderName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (message.senderId == "bharat_ai") BharatSaffronLight else BharatElectricCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Quoted Reply Box (if replying to another message)
                    if (message.replyToText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMe) Color(0x33000000) else bColors.glassBorder.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, BharatElectricCyan.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(BharatElectricCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = message.replyToSender ?: "Message",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = BharatElectricCyan
                                    )
                                    Text(
                                        text = message.replyToText,
                                        fontSize = 11.sp,
                                        color = if (isMe) BharatWhite.copy(alpha = 0.8f) else bColors.textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Image / Photo Bubble
                    if (message.messageType == MessageType.IMAGE.name) {
                        Column(modifier = Modifier.padding(bottom = 4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Black.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                            ) {
                                AsyncImage(
                                    model = message.attachmentUrl ?: "https://picsum.photos/400/300",
                                    contentDescription = "Photo message",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            if (message.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.text,
                                    fontSize = 14.sp,
                                    color = if (isMe) BharatWhite else bColors.textPrimary
                                )
                            }
                        }
                    }

                    // UPI Payment Card Bubble
                    else if (message.messageType == MessageType.UPI_PAYMENT.name) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x335F259F),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66A855F7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Bharat UPI Transfer",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = GoldAccent
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${message.upiAmount?.toInt() ?: 500}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BharatWhite
                                )
                                Text(
                                    text = "Ref: ${message.upiTransactionId ?: "BHARAT-UPI-SUCCESS"}",
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }

                    // Voice Audio Note Bubble
                    else if (message.messageType == MessageType.VOICE.name) {
                        var isPlaying by remember { mutableStateOf(false) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    isPlaying = !isPlaying
                                    if (isPlaying) onPlayTts()
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(BharatSaffron)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = BharatWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            AudioWaveformVisualizer(
                                waveformStr = message.audioWaveform,
                                isPlaying = isPlaying,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${message.voiceDurationSec ?: 15}s",
                                fontSize = 11.sp,
                                color = bColors.textSecondary
                            )
                        }
                    }

                    // 10GB File Attachment Card
                    else if (message.messageType == MessageType.FILE.name) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BharatNavyLight.copy(alpha = 0.3f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BharatElectricCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = BharatElectricCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.attachmentUrl ?: "sovereign_dataset.bin",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = BharatWhite,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = message.fileSizeStr ?: "9.8 GB • Bharat Sovereign Cloud",
                                        fontSize = 10.5.sp,
                                        color = BharatGreenLight
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = BharatWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Interactive Poll Message Bubble
                    else if (message.messageType == MessageType.POLL.name) {
                        val pollQuestion = message.pollQuestion ?: message.text.removePrefix("📊 Poll: ")
                        val optionsList = remember(message.pollOptionsJson) {
                            val list = mutableListOf<String>()
                            try {
                                val json = message.pollOptionsJson ?: ""
                                val regex = "\"text\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                                regex.findAll(json).forEach { match ->
                                    list.add(match.groupValues[1])
                                }
                            } catch (e: Exception) {}
                            if (list.isEmpty()) listOf("Option 1", "Option 2") else list
                        }

                        val votesMap = remember(message.pollVotesJson) {
                            val map = mutableMapOf<Int, Int>()
                            try {
                                val json = message.pollVotesJson ?: "{}"
                                val cleaned = json.replace("{", "").replace("}", "").trim()
                                if (cleaned.isNotBlank()) {
                                    cleaned.split(",").forEach { item ->
                                        val parts = item.split(":")
                                        if (parts.size == 2) {
                                            val key = parts[0].replace("\"", "").trim().toIntOrNull() ?: 0
                                            val value = parts[1].trim().toIntOrNull() ?: 0
                                            map[key] = value
                                        }
                                    }
                                }
                            } catch (e: Exception) {}
                            map
                        }

                        val totalVotes = remember(votesMap) { votesMap.values.sum().coerceAtLeast(1) }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x22A855F7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66A855F7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Poll,
                                        contentDescription = null,
                                        tint = Color(0xFFA855F7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Interactive Poll",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFA855F7)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = pollQuestion,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = if (isMe) BharatWhite else bColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                optionsList.forEachIndexed { idx, optText ->
                                    val votes = votesMap[idx] ?: 0
                                    val pct = ((votes.toFloat() / totalVotes.toFloat()) * 100).toInt()

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (bColors.isDark) Color(0x442D124D) else Color(0x22A855F7),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0x55A855F7)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable {
                                                onVotePoll?.invoke(idx)
                                            }
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction = (votes.toFloat() / totalVotes.toFloat()).coerceIn(0.05f, 1f))
                                                    .height(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0x44A855F7))
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = optText,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    color = if (isMe) BharatWhite else bColors.textPrimary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = "$votes ($pct%)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFFA855F7)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap option to vote • $totalVotes total votes",
                                    fontSize = 10.sp,
                                    color = bColors.textSecondary
                                )
                            }
                        }
                    }

                    // Live Location Bubble
                    else if (message.messageType == MessageType.LOCATION.name) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x22F59E0B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66F59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Live NavIC GPS Location",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0F1E36),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(30.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = message.text.substringBefore(" • "),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = BharatWhite,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.fileSizeStr ?: "Live GPS Coordinates",
                                    fontSize = 11.sp,
                                    color = BharatGreenLight
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val geoUri = message.attachmentUrl ?: "geo:28.6139,77.2090"
                                            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(geoUri))
                                            mapIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                            localContext.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(localContext, "Opening GPS Map...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B))
                                ) {
                                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Live Directions", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Standard Text Message
                    else {
                        Text(
                            text = message.text,
                            fontSize = 14.5.sp,
                            color = if (isMe) BharatWhite else bColors.textPrimary,
                            lineHeight = 20.sp
                        )
                    }

                    // Translation Card if translated
                    if (message.translatedText != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x3300F0FF),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, BharatElectricCyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = BharatElectricCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = message.translatedText,
                                    fontSize = 12.5.sp,
                                    color = BharatElectricCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Time & Status Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        // Starred indicator
                        if (message.isStarred) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Starred",
                                tint = GoldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Translation trigger icon
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = "Translate",
                            tint = bColors.textMuted,
                            modifier = Modifier
                                .size(12.dp)
                                .clickable { onTranslate() }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // TTS Audio trigger
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = bColors.textMuted,
                            modifier = Modifier
                                .size(12.dp)
                                .clickable { onPlayTts() }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = message.timeFormatted,
                            fontSize = 10.sp,
                            color = if (isMe) Color(0xCCFFFFFF) else bColors.textMuted
                        )

                        if (isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            MessageStatusIndicator(
                                status = message.status,
                                isSeen = message.isSeen || message.status.equals("SEEN", ignoreCase = true) || message.status.equals("READ", ignoreCase = true),
                                isSecret = message.isSecretExpiring
                            )
                        }
                    }
                }
            }

            // Attached reaction badge
            if (message.reactionEmoji != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder),
                    modifier = Modifier.offset(y = (-8).dp, x = if (isMe) (-8).dp else 8.dp)
                ) {
                    Text(
                        text = message.reactionEmoji,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (isSelected && isMe) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = BharatElectricCyan,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(20.dp)
            )
        }
    }
}

package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showMenu by remember { mutableStateOf(false) }
    var selectedMessageForReaction by remember { mutableStateOf<MessageEntity?>(null) }

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

                    // Avatar with optional typing halo
                    Box(contentAlignment = Alignment.Center) {
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
                                text = { Text("⏱️ Disappearing Timer", color = bColors.textPrimary) },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Attachment Button (+)
                        IconButton(
                            onClick = { viewModel.showAttachmentOptions.value = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("attachment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = "Attach",
                                tint = BharatSaffron,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // UPI Quick Pay icon
                        IconButton(
                            onClick = { viewModel.triggerUpiSheetWithBiometrics() },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("upi_pay_quick_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = "UPI Pay",
                                tint = BharatGreenLight,
                                modifier = Modifier.size(24.dp)
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
                                    .padding(horizontal = 14.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            if (chat.isSecret) "🔒 Encrypted secret message..." else "Message in English, हिंदी, தமிழ்...",
                                            color = bColors.textMuted,
                                            fontSize = 13.5.sp
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
                                onClick = { viewModel.startVoiceRecording() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(BharatNavyLight)
                                    .testTag("voice_record_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record Voice",
                                    tint = BharatWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val text = inputText
                                    inputText = ""
                                    viewModel.sendMessage(text)
                                },
                                modifier = Modifier
                                    .size(44.dp)
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
                    MessageBubble(
                        message = message,
                        onLongClick = { selectedMessageForReaction = message },
                        onTranslate = {
                            viewModel.targetTranslateMessageId.value = message.id
                            viewModel.showAiTranslatorDialog.value = true
                        },
                        onPlayTts = {
                            viewModel.ttsManager.speakText(message.translatedText ?: message.text)
                        }
                    )
                }
            }

            // Quick Emoji Reaction Bar (if message selected)
            if (selectedMessageForReaction != null) {
                val targetMsg = selectedMessageForReaction!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { selectedMessageForReaction = null },
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        shape = RoundedCornerShape(28.dp),
                        backgroundColor = DarkSurfaceElevated
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            listOf("🇮🇳", "❤️", "🔥", "🚀", "🤖", "👍", "🙏").forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 24.sp,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.reactToMessage(targetMsg.id, emoji)
                                            selectedMessageForReaction = null
                                        }
                                        .testTag("reaction_emoji_$emoji")
                                )
                            }
                        }
                    }
                }
            }
        }
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
            onDismiss = { viewModel.showAttachmentOptions.value = false }
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
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    onLongClick: () -> Unit,
    onTranslate: () -> Unit,
    onPlayTts: () -> Unit
) {
    val isMe = message.isFromMe
    val bColors = LocalBharatColors.current

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
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp)
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
                    1.dp,
                    if (isMe) {
                        if (message.isSecretExpiring) SecretChatPink.copy(alpha = 0.5f)
                        else BharatElectricCyan.copy(alpha = 0.35f)
                    } else {
                        bColors.glassBorder
                    }
                ),
                modifier = Modifier
                    .clickable { onLongClick() }
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

                    // UPI Payment Card Bubble
                    if (message.messageType == MessageType.UPI_PAYMENT.name) {
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
    }
}

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.ui.components.MessageStatusIndicator
import com.example.ui.components.StatusRingAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ChatScreen Composable
 * A modern, production-grade Chat Interface featuring:
 * 1. Deep Glassmorphic Surfaces with blur, frost layers, and translucent elevation.
 * 2. Vibrant Tri-Color Theme (Saffron #FF671F, Pure Crystal Frost #FFFFFF, Emerald #10B981).
 * 3. Reactive Messages List with animations, reactions, timestamps, and delivery indicators.
 * 4. Floating Glassmorphic Input Dock with attachments, smart reply chips, and dynamic send interaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    recipientName: String = "Vikram Sharma",
    recipientStatus: String = "Online • Post-Quantum Encrypted",
    recipientAvatarInitial: String = "VS",
    recipientAvatarColorHex: String = "#FF671F",
    viewModel: BharatChatViewModel? = null,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bColors = LocalBharatColors.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val e2eeTooltipState = rememberTooltipState(isPersistent = false)
    var showE2eeDialog by remember { mutableStateOf(false) }

    // Interactive Local Message State (Synchronized with ViewModel if available)
    val vmActiveChat by viewModel?.activeChat?.collectAsState() ?: remember { mutableStateOf(null) }
    val vmMessages by viewModel?.currentMessages?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    var localInputText by remember { mutableStateOf("") }
    var localMessages by remember {
        mutableStateOf(
            listOf(
                MessageEntity(
                    id = "msg_1",
                    chatId = "default_chat",
                    senderId = "recipient",
                    senderName = recipientName,
                    text = "Namaste! Welcome to Venzora sovereign chat. 🇮🇳",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 15,
                    timeFormatted = "10:15 AM",
                    isFromMe = false,
                    status = "READ",
                    reactionEmoji = "🇮🇳"
                ),
                MessageEntity(
                    id = "msg_2",
                    chatId = "default_chat",
                    senderId = "me",
                    senderName = "Me",
                    text = "Hello! Love the high-security Kyber-1024 encryption and glassmorphic UI.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 12,
                    timeFormatted = "10:18 AM",
                    isFromMe = true,
                    status = "READ",
                    reactionEmoji = "🔥"
                ),
                MessageEntity(
                    id = "msg_3",
                    chatId = "default_chat",
                    senderId = "recipient",
                    senderName = recipientName,
                    text = "Everything runs in high speed with instant zero-lag messaging, UPI integrations, and smart AI summaries.",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 5,
                    timeFormatted = "10:25 AM",
                    isFromMe = false,
                    status = "READ"
                ),
                MessageEntity(
                    id = "msg_4",
                    chatId = "default_chat",
                    senderId = "recipient",
                    senderName = recipientName,
                    text = "Try sending a message or tap one of the smart reply chips below! 🚀",
                    timestamp = System.currentTimeMillis() - 1000 * 60 * 1,
                    timeFormatted = "10:29 AM",
                    isFromMe = false,
                    status = "DELIVERED"
                )
            )
        )
    }

    val displayMessages = if (vmMessages.isNotEmpty()) vmMessages else localMessages
    val title = vmActiveChat?.title ?: recipientName
    val subtitle = if (vmActiveChat?.isOnline == true) "Online • Kyber E2EE" else recipientStatus

    val smartReplies = listOf(
        "Namaste 🙏",
        "Got it! 👍",
        "Sounds great! 🚀",
        "Send UPI ₹",
        "On my way! ⚡"
    )

    // Auto-scroll on new message
    LaunchedEffect(displayMessages.size) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tri_color_ambient")
    val ambientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (bColors.isDark) DarkBackground else LightBackground)
            .testTag("chat_screen_container")
    ) {
        // --- Ambient Tri-Color Glow Orbs Background ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BharatSaffron.copy(alpha = if (bColors.isDark) 0.14f else 0.08f),
                            Color.Transparent
                        ),
                        radius = 800f,
                        center = androidx.compose.ui.geometry.Offset(200f + ambientOffset * 0.2f, 150f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BharatGreenLight.copy(alpha = if (bColors.isDark) 0.12f else 0.07f),
                            Color.Transparent
                        ),
                        radius = 700f,
                        center = androidx.compose.ui.geometry.Offset(800f - ambientOffset * 0.15f, 1400f)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Glassmorphic Header with Tricolor accent bar
                Column {
                    Surface(
                        color = if (bColors.isDark) DarkSurface.copy(alpha = 0.88f) else LightSurface.copy(alpha = 0.90f),
                        tonalElevation = 6.dp,
                        border = BorderStroke(1.dp, bColors.glassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (viewModel != null) {
                                        viewModel.closeChat()
                                    } else {
                                        onBackClick()
                                    }
                                },
                                modifier = Modifier.testTag("chat_screen_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = bColors.textPrimary
                                )
                            }

                            StatusRingAvatar(
                                initial = vmActiveChat?.avatarInitial ?: recipientAvatarInitial,
                                avatarColorHex = vmActiveChat?.avatarColorHex ?: recipientAvatarColorHex,
                                size = 42.dp,
                                isOnline = true,
                                onClick = {}
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = bColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(BharatGreenLight)
                                    )
                                }
                                Text(
                                    text = subtitle,
                                    fontSize = 11.5.sp,
                                    color = BharatGreenLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // E2EE Lock Indicator with Tooltip
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = {
                                    PlainTooltip(
                                        shape = RoundedCornerShape(10.dp),
                                        containerColor = if (bColors.isDark) Color(0xFF0F172A) else Color(0xFF1E293B),
                                        contentColor = Color(0xFF38BDF8)
                                    ) {
                                        Text(
                                            "🔒 End-to-End Encrypted (Kyber-1024)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                },
                                state = e2eeTooltipState
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BharatGreenLight.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, BharatGreenLight.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .clickable {
                                            coroutineScope.launch { e2eeTooltipState.show() }
                                            showE2eeDialog = true
                                        }
                                        .testTag("chat_screen_e2ee_lock_indicator")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Encrypted",
                                            tint = BharatGreenLight,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "E2EE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BharatGreenLight
                                        )
                                    }
                                }
                            }

                            // Quick Action Icons
                            IconButton(
                                onClick = {},
                                modifier = Modifier.testTag("chat_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Voice Call",
                                    tint = BharatSaffron,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {},
                                modifier = Modifier.testTag("chat_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video Call",
                                    tint = BharatGreenLight,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Tri-color Gradient Divider Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BharatSaffron, BharatWhite, BharatGreenLight)
                                )
                            )
                    )
                }
            },
            bottomBar = {
                // --- Glassmorphic Floating Input Dock ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // Smart Replies Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        items(smartReplies) { reply ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (bColors.isDark) Color(0x331E293B) else Color(0x22CBD5E1),
                                border = BorderStroke(1.dp, bColors.glassBorder),
                                modifier = Modifier
                                    .clickable {
                                        localInputText = reply
                                    }
                                    .testTag("smart_reply_$reply")
                            ) {
                                Text(
                                    text = reply,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = bColors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Frosted Glass Text Input Capsule
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = if (bColors.isDark) DarkSurfaceElevated.copy(alpha = 0.85f) else LightSurface.copy(alpha = 0.92f),
                        border = BorderStroke(
                            1.2.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    BharatSaffron.copy(alpha = 0.6f),
                                    BharatWhite.copy(alpha = 0.4f),
                                    BharatGreenLight.copy(alpha = 0.6f)
                                )
                            )
                        ),
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(26.dp), spotColor = BharatSaffron.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EmojiEmotions,
                                    contentDescription = "Emojis",
                                    tint = BharatSaffron,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach",
                                    tint = bColors.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Text Input Area
                            TextField(
                                value = localInputText,
                                onValueChange = { localInputText = it },
                                placeholder = {
                                    Text(
                                        text = "Type an encrypted message...",
                                        fontSize = 13.5.sp,
                                        color = bColors.textMuted
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = BharatSaffron,
                                    focusedTextColor = bColors.textPrimary,
                                    unfocusedTextColor = bColors.textPrimary
                                ),
                                maxLines = 4,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field")
                            )

                            // Dynamic Send / Voice Button
                            val isTextEmpty = localInputText.isBlank()

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isTextEmpty) {
                                            Brush.linearGradient(listOf(BharatGreenLight, BharatGreen))
                                        } else {
                                            Brush.linearGradient(listOf(BharatSaffron, BharatSaffronLight))
                                        }
                                    )
                                    .clickable {
                                        if (!isTextEmpty) {
                                            val textToSend = localInputText.trim()
                                            if (viewModel != null && vmActiveChat != null) {
                                                viewModel.sendMessage(text = textToSend)
                                            } else {
                                                val now = System.currentTimeMillis()
                                                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
                                                localMessages = localMessages + MessageEntity(
                                                    id = "msg_${System.currentTimeMillis()}",
                                                    chatId = "default_chat",
                                                    senderId = "me",
                                                    senderName = "Me",
                                                    text = textToSend,
                                                    timestamp = now,
                                                    timeFormatted = timeStr,
                                                    isFromMe = true,
                                                    status = "SENT"
                                                )
                                            }
                                            localInputText = ""
                                        }
                                    }
                                    .testTag("chat_send_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTextEmpty) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send,
                                    contentDescription = if (isTextEmpty) "Voice Note" else "Send",
                                    tint = BharatWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            // --- Messages List ---
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 8.dp,
                    bottom = paddingValues.calculateBottomPadding() + 12.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_messages_list")
            ) {
                // E2EE Info Capsule
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (bColors.isDark) Color(0x330D1527) else Color(0x33F1F5F9),
                            border = BorderStroke(1.dp, bColors.glassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = BharatGreenLight,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Post-Quantum 1024-bit End-to-End Encrypted",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = bColors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Messages
                items(displayMessages, key = { it.id }) { msg ->
                    val isMe = msg.isFromMe || msg.senderId == "me"
                    GlassMessageBubble(
                        message = msg,
                        isMe = isMe
                    )
                }
            }
        }

        if (showE2eeDialog) {
            AlertDialog(
                onDismissRequest = { showE2eeDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = null,
                            tint = BharatGreenLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "End-to-End Encrypted",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = bColors.textPrimary
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Messages and calls with $title are secured with Post-Quantum Kyber-1024 cryptography.",
                            fontSize = 13.5.sp,
                            color = bColors.textPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (bColors.isDark) Color(0x331E293B) else Color(0x1564748B),
                            border = BorderStroke(1.dp, bColors.glassBorder),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🔐 Protocol: CRYSTALS-Kyber-1024 Sovereign", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BharatGreenLight)
                                Text("Safety Fingerprint: 894F-902B-12CE-VNZ8", fontSize = 11.sp, color = BharatElectricCyan)
                                Text("No one outside of this chat can read or listen to your messages.", fontSize = 11.sp, color = bColors.textSecondary)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showE2eeDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight)
                    ) {
                        Text("VERIFIED & SECURE", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showE2eeDialog = false }) {
                        Text("CLOSE", color = bColors.textSecondary)
                    }
                },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
            )
        }
    }
}

/**
 * Glassmorphic Message Bubble
 */
@Composable
fun GlassMessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val bColors = LocalBharatColors.current
    val formattedTime = remember(message.timeFormatted, message.timestamp) {
        if (message.timeFormatted.isNotBlank()) message.timeFormatted
        else SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    val bubbleBackground = if (isMe) {
        Brush.linearGradient(
            listOf(
                BharatSaffron.copy(alpha = 0.92f),
                BharatSaffronDark.copy(alpha = 0.95f)
            )
        )
    } else {
        if (bColors.isDark) {
            Brush.linearGradient(
                listOf(
                    DarkSurfaceElevated.copy(alpha = 0.88f),
                    DarkCardBg.copy(alpha = 0.85f)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    LightSurface.copy(alpha = 0.95f),
                    Color(0xFFF1F5F9).copy(alpha = 0.92f)
                )
            )
        }
    }

    val borderStroke = if (isMe) {
        BorderStroke(1.dp, BharatSaffronLight.copy(alpha = 0.6f))
    } else {
        BorderStroke(1.dp, bColors.glassBorder)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            border = borderStroke,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
            color = Color.Transparent,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .background(bubbleBackground, bubbleShape)
                .testTag("message_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Sender name for group/received
                if (!isMe && message.senderName.isNotBlank() && message.senderName != "Me") {
                    Text(
                        text = message.senderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatGreenLight,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Message Text
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (isMe) BharatWhite else bColors.textPrimary,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Time and Status
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = if (isMe) BharatWhite.copy(alpha = 0.75f) else bColors.textMuted
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

        // Reaction chips if present
        if (!message.reactionEmoji.isNullOrBlank()) {
            Surface(
                shape = CircleShape,
                color = if (bColors.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                border = BorderStroke(0.8.dp, bColors.glassBorder),
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = message.reactionEmoji,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

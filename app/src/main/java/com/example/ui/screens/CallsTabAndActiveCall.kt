package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.CallEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.LiveCameraVideoPreview
import com.example.ui.components.QuantumShieldBadge
import com.example.ui.components.StatusRingAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel

@Composable
fun CallsTab(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val calls by viewModel.calls.collectAsState()
    val bColors = LocalBharatColors.current
    var showDirectCallDialog by remember { mutableStateOf(false) }
    var directCallNameOrPhone by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (bColors.isDark) DarkBackground else LightBackground)
            .padding(horizontal = 16.dp)
    ) {
        // Quick Call Header Banner & Incoming Call Test Controls
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(18.dp),
            backgroundColor = BharatNavy.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(BharatElectricCyan, BharatGreenLight))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = BharatWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HD Voice & 4K Video Calling",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = BharatWhite
                            )
                            Text(
                                text = "AI Noise Cancelling • Quantum Encrypted",
                                fontSize = 11.5.sp,
                                color = BharatElectricCyan
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Simulation Action Buttons for Testing Incoming Call Overlay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BharatGreenLight.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BharatGreenLight.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.triggerIncomingTestCall(isVideo = false) }
                            .testTag("test_incoming_voice_call_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Test Voice Call",
                                tint = BharatGreenLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Test Voice Call",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatGreenLight
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BharatElectricCyan.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BharatElectricCyan.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.triggerIncomingTestCall(isVideo = true) }
                            .testTag("test_incoming_video_call_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Test Video Call",
                                tint = BharatElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Test Video Call",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatElectricCyan
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recent Calls",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = bColors.textPrimary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { showDirectCallDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dialpad,
                        contentDescription = null,
                        tint = BharatSaffron,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Dial",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatSaffron
                    )
                }

                TextButton(
                    onClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.CONTACTS_LIST) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = null,
                        tint = BharatElectricCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Phonebook",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatElectricCyan
                    )
                }
            }
        }

        if (calls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BharatNavyLight.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneCallback,
                            contentDescription = null,
                            tint = BharatGreenLight,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No recent calls",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = bColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap below to start an HD audio or 4K video call",
                        fontSize = 13.sp,
                        color = bColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.CONTACTS_LIST) },
                        colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start a Call (कॉल करें)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(calls, key = { it.id }) { call ->
                    CallItemRow(
                        call = call,
                        onCallClick = {
                            viewModel.startCall(call.contactName, call.contactAvatar, call.isVideo)
                        },
                        onDeleteClick = {
                            viewModel.deleteCall(call.id)
                        }
                    )
                }
            }
        }
    }

    if (showDirectCallDialog) {
        AlertDialog(
            onDismissRequest = { showDirectCallDialog = false },
            title = {
                Text("Direct Call / Dial (डायरेक्ट कॉल)", fontWeight = FontWeight.Bold, color = bColors.textPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter contact name or phone number to start an instant call:", fontSize = 13.sp, color = bColors.textSecondary)
                    OutlinedTextField(
                        value = directCallNameOrPhone,
                        onValueChange = { directCallNameOrPhone = it },
                        placeholder = { Text("e.g. Rahul, +91 9876543210") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val target = directCallNameOrPhone.ifBlank { "Unknown User" }
                            showDirectCallDialog = false
                            viewModel.startCall(target, target.take(2).uppercase(), isVideo = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight)
                    ) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voice Call")
                    }
                    Button(
                        onClick = {
                            val target = directCallNameOrPhone.ifBlank { "Unknown User" }
                            showDirectCallDialog = false
                            viewModel.startCall(target, target.take(2).uppercase(), isVideo = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BharatElectricCyan)
                    ) {
                        Icon(Icons.Default.Videocam, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Video")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectCallDialog = false }) {
                    Text("Cancel", color = bColors.textSecondary)
                }
            },
            containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
        )
    }
}

@Composable
fun CallItemRow(
    call: CallEntity,
    onCallClick: () -> Unit,
    onDeleteClick: () -> Unit = {}
) {
    val bColors = LocalBharatColors.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("call_item_${call.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        onClick = onCallClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusRingAvatar(
                initial = call.contactAvatar,
                avatarColorHex = "#0284C7",
                size = 46.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.contactName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = if (call.isMissed) RoseError else bColors.textPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (call.isIncoming) {
                            if (call.isMissed) Icons.Default.CallMissed else Icons.Default.CallReceived
                        } else {
                            Icons.Default.CallMade
                        },
                        contentDescription = null,
                        tint = if (call.isMissed) RoseError else BharatGreenLight,
                        modifier = Modifier.size(14.dp)
                    )

                    Text(
                        text = "${call.timeFormatted} • ${call.durationStr}",
                        fontSize = 12.sp,
                        color = bColors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = call.qualityStr,
                    fontSize = 10.5.sp,
                    color = BharatElectricCyan,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onCallClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BharatNavyLight.copy(alpha = 0.3f))
                    .testTag("call_action_button")
            ) {
                Icon(
                    imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = if (call.isVideo) BharatElectricCyan else BharatGreenLight,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_call_log_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Log",
                    tint = bColors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ActiveCallScreen(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val callState by viewModel.activeCallState.collectAsState()
    val isProximityNear by viewModel.isProximityNear.collectAsState()
    val bColors = LocalBharatColors.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.proximityHandler.stop()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "webrtc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "call_avatar_pulse"
    )

    val min = callState.durationSeconds / 60
    val sec = callState.durationSeconds % 60
    val timerStr = "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"

    var showStatsOverlay by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        if (callState.isVideo && !callState.isVideoOff) {
            // Main Remote Video Stream Canvas (Simulated 4K / 1080p WebRTC stream)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0B192C),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    )
            ) {
                // Video simulation pattern / grid
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(BharatSaffron, BharatElectricCyan, BharatGreenLight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = callState.contactAvatar,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = BharatWhite
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = callState.contactName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatWhite
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BharatGreenLight)
                        )
                        Text(
                            text = if (callState.isScreenSharing) "Live Screen Share Active (WebRTC)" else "WebRTC P2P Ultra-HD Stream Active",
                            fontSize = 12.sp,
                            color = BharatElectricCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // PiP (Picture-in-Picture) Local Camera Preview
                GlassCard(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 60.dp, end = 16.dp)
                        .size(width = 110.dp, height = 150.dp)
                        .testTag("pip_camera_preview"),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = Color(0xCC0F172A),
                    borderColor = BharatElectricCyan.copy(alpha = 0.6f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Real hardware camera preview feed
                        LiveCameraVideoPreview(
                            isFrontCamera = callState.isFrontCamera,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Camera flip mini badge button
                        IconButton(
                            onClick = { viewModel.flipCamera() },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA000000))
                                .testTag("flip_camera_pip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Flip Camera",
                                tint = BharatWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Camera badge indicator
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x99000000)
                        ) {
                            Text(
                                text = if (callState.isFrontCamera) "Front" else "Back",
                                fontSize = 9.sp,
                                color = BharatElectricCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // HD Voice Call / Video Off Mode Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = callState.contactName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = BharatWhite
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (!callState.isConnected) "Calling • Ringing..." else timerStr,
                    fontSize = 15.sp,
                    color = if (callState.isConnected) BharatGreenLight else BharatElectricCyan,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(40.dp))

                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(if (!callState.isConnected) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    BharatSaffron.copy(alpha = 0.4f),
                                    BharatElectricCyan.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            3.dp,
                            Brush.sweepGradient(listOf(BharatSaffron, BharatWhite, BharatGreenLight, BharatSaffron)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = callState.contactAvatar,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatWhite
                    )
                }
            }
        }

        // Top WebRTC Stats & Status Overlay Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.endCall() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x990F172A))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Minimize / Back",
                            tint = BharatWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x990F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BharatElectricCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (callState.isConnected) BharatGreenLight else BharatSaffron)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (callState.isConnected) "WebRTC HD • ${callState.webrtcLatencyMs}ms" else "Calling...",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatWhite
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0x990F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BharatSaffron.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { showStatsOverlay = !showStatsOverlay }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Stats",
                                tint = BharatSaffron,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showStatsOverlay) "HUD On" else "HUD Off",
                                fontSize = 11.sp,
                                color = BharatWhite
                            )
                        }
                    }
                }
            }

            // WebRTC Stream Performance HUD Card
            if (showStatsOverlay && callState.isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = Color(0xBF0F172A),
                    borderColor = Color(0x4438BDF8)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CODEC: ${callState.webrtcCodec}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatElectricCyan
                            )
                            Text(
                                text = "RES: ${callState.webrtcResolution}",
                                fontSize = 10.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "BITRATE: ${callState.webrtcBitrateKbps} kbps",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatGreenLight
                            )
                            Text(
                                text = "LATENCY: ${callState.webrtcLatencyMs}ms (0% loss)",
                                fontSize = 10.sp,
                                color = BharatSaffronLight
                            )
                        }
                    }
                }
            }
        }

        // Bottom Call Action Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Secondary toolbar (Noise cancel & Screen share)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (callState.isAiNoiseCanceling) BharatGreenLight.copy(alpha = 0.25f) else Color(0x661E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (callState.isAiNoiseCanceling) BharatGreenLight else Color.Gray),
                    modifier = Modifier.clickable { viewModel.toggleNoiseCanceling() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (callState.isAiNoiseCanceling) BharatGreenLight else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (callState.isAiNoiseCanceling) "AI Audio Filter: ON" else "AI Filter: OFF",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BharatWhite
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (callState.isScreenSharing) BharatElectricCyan.copy(alpha = 0.25f) else Color(0x661E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (callState.isScreenSharing) BharatElectricCyan else Color.Gray),
                    modifier = Modifier.clickable { viewModel.toggleScreenShare() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenShare,
                            contentDescription = null,
                            tint = if (callState.isScreenSharing) BharatElectricCyan else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (callState.isScreenSharing) "Sharing Screen" else "Share Screen",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BharatWhite
                        )
                    }
                }
            }

            // Primary control button grid
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                backgroundColor = Color(0xE60F172A),
                borderColor = Color(0x3364748B)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (callState.isMuted) RoseError else Color(0xFF1E293B))
                            .testTag("toggle_mute_button")
                    ) {
                        Icon(
                            imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = BharatWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Video toggle
                    IconButton(
                        onClick = { viewModel.toggleVideo() },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (callState.isVideoOff) Color(0xFF475569) else BharatNavyLight)
                            .testTag("toggle_video_button")
                    ) {
                        Icon(
                            imageVector = if (callState.isVideoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "Video",
                            tint = BharatWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Flip camera
                    IconButton(
                        onClick = { viewModel.flipCamera() },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("flip_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Flip Camera",
                            tint = BharatElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Speaker
                    IconButton(
                        onClick = { viewModel.toggleSpeaker() },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (callState.isSpeakerOn) BharatElectricCyan.copy(alpha = 0.3f) else Color(0xFF1E293B))
                            .testTag("toggle_speaker_button")
                    ) {
                        Icon(
                            imageVector = if (callState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Speaker",
                            tint = if (callState.isSpeakerOn) BharatElectricCyan else BharatWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // End Call
                    IconButton(
                        onClick = { viewModel.endCall() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(RoseError)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = BharatWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Proximity Sensor Screen-Off & Anti-Accidental Touch Overlay
        // Turns off display / renders black screen when phone is held to ear (speaker OFF)
        if (isProximityNear && !callState.isSpeakerOn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(999f)
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        // Consumes all touch events to prevent accidental ear/cheek touches
                    }
                    .testTag("proximity_screen_off_overlay")
            )
        }
    }
}

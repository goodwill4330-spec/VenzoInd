package com.example.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay

/**
 * CallScreen Composable utilizing Google Accompanist Permissions Library
 * to request and manage Microphone access (android.Manifest.permission.RECORD_AUDIO)
 * for HD Voice and Sovereign Encrypted Calling.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CallScreen(
    viewModel: BharatChatViewModel? = null,
    contactName: String = "Bharat Contact",
    contactAvatar: String = "BC",
    isVideo: Boolean = false,
    onEndCall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 1. Accompanist Permission State for RECORD_AUDIO
    val micPermissionState = rememberPermissionState(
        permission = Manifest.permission.RECORD_AUDIO
    )

    // Call state variables
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(isVideo) }
    var callDurationSec by remember { mutableIntStateOf(0) }
    var isCallActive by remember { mutableStateOf(false) }

    // Auto-increment timer once call connects
    LaunchedEffect(isCallActive) {
        if (isCallActive) {
            while (true) {
                delay(1000L)
                callDurationSec++
            }
        }
    }

    // Auto-connect call after 2 seconds
    LaunchedEffect(Unit) {
        delay(2000L)
        isCallActive = true
    }

    // Pulse animation for avatar & audio activity
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val min = callDurationSec / 60
    val sec = callDurationSec % 60
    val timerStr = "${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("call_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Sovereign Encryption Header & HD Audio Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x2210B981),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BharatGreenLight.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = BharatGreenLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Kyber-1024 Quantum Encrypted • HD Voice",
                            color = BharatGreenLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Caller Avatar with audio pulse animation
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(150.dp)
                ) {
                    // Outer audio activity aura
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(if (isCallActive && !isMuted) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        BharatGreenLight.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Inner Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(BharatSaffron, BharatSaffronDark)
                                )
                            )
                            .border(3.dp, Color(0x44FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contactAvatar.take(2).uppercase(),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = contactName,
                    color = BharatWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (!isCallActive) "Ringing (Securing P2P channel)..." else "HD Call • $timerStr",
                    color = if (!isCallActive) BharatSaffron else BharatGreenLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Middle Section: Accompanist Permission Handling Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (!micPermissionState.status.isGranted) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0x33F59E0B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66F59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MicOff,
                                contentDescription = "Microphone Required",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (micPermissionState.status.shouldShowRationale) {
                                    "VenzoInd requires microphone access for real-time crystal-clear HD audio calling between devices."
                                } else {
                                    "Microphone permission is needed for HD voice calling."
                                },
                                color = BharatWhite,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { micPermissionState.launchPermissionRequest() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("grant_mic_permission_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Grant Mic Permission",
                                    color = Color.Black,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    // Microphone permission granted indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x15FFFFFF))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color(0xFFEF4444) else BharatGreenLight)
                        )
                        Text(
                            text = if (isMuted) "Microphone Muted" else "HD Microphone Active (48kHz Opus)",
                            color = if (isMuted) Color(0xFFEF4444) else BharatWhite,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Bottom Section: Call Action Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Secondary Controls Row (Mute, Speaker, Video)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute Button
                    CallActionButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Unmute" else "Mute",
                        isActive = isMuted,
                        activeColor = Color(0xFFEF4444),
                        onClick = {
                            if (!micPermissionState.status.isGranted) {
                                micPermissionState.launchPermissionRequest()
                            } else {
                                isMuted = !isMuted
                            }
                        },
                        testTag = "call_btn_mute"
                    )

                    // Speakerphone Toggle
                    CallActionButton(
                        icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Outlined.VolumeUp,
                        label = if (isSpeakerOn) "Speaker ON" else "Speaker",
                        isActive = isSpeakerOn,
                        activeColor = BharatElectricCyan,
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        testTag = "call_btn_speaker"
                    )

                    // Video Toggle
                    CallActionButton(
                        icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = if (isVideoEnabled) "Video ON" else "Video",
                        isActive = isVideoEnabled,
                        activeColor = BharatGreenLight,
                        onClick = { isVideoEnabled = !isVideoEnabled },
                        testTag = "call_btn_video"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Large Red End Call Button
                IconButton(
                    onClick = {
                        viewModel?.endCall()
                        onEndCall()
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor.copy(alpha = 0.25f) else Color(0x22FFFFFF))
                .border(
                    width = 1.dp,
                    color = if (isActive) activeColor else Color(0x33FFFFFF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else BharatWhite,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isActive) activeColor else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

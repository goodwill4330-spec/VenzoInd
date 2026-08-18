package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.sync.IncomingCallEvent
import com.example.ui.theme.*

/**
 * A clean, modern Incoming Call Overlay UI with:
 * - Animated caller avatar with pulsating acoustic rings
 * - Prominent contact name, phone/handle, and call type status badge
 * - Large, accessible Material-icon buttons for 'Accept' (green) and 'Decline' (red)
 * - Quick response actions (e.g. quick decline message)
 */
@Composable
fun IncomingCallOverlay(
    callEvent: IncomingCallEvent,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onQuickMessage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bColors = LocalBharatColors.current

    // Pulsating animation for call ripple waves & accept button
    val infiniteTransition = rememberInfiniteTransition(label = "incoming_call_ring")
    val ringScale1 by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale_1"
    )
    val ringAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha_1"
    )

    val ringScale2 by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale_2"
    )
    val ringAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, delayMillis = 400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha_2"
    )

    val acceptPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "accept_pulse"
    )

    var showQuickMessageMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        // Semi-transparent darkened backdrop with soft blur/gradient feel
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC050B14))
                .clickable(enabled = false) {}
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Main Overlay Glass Card
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .testTag("incoming_call_overlay_card"),
                shape = RoundedCornerShape(32.dp),
                color = if (bColors.isDark) Color(0xFF0F172A).copy(alpha = 0.94f) else Color(0xFF1E293B).copy(alpha = 0.96f),
                tonalElevation = 16.dp,
                shadowElevation = 24.dp,
                border = BorderStroke(
                    1.2.dp,
                    Brush.verticalGradient(
                        listOf(
                            BharatSaffron.copy(alpha = 0.8f),
                            Color(0x33FFFFFF),
                            BharatGreenLight.copy(alpha = 0.8f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Status Badge: Call Type & Security Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x22FFFFFF))
                            .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (callEvent.isVideo) Icons.Default.Videocam else Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = if (callEvent.isVideo) BharatElectricCyan else BharatGreenLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (callEvent.isVideo) "INCOMING 4K VIDEO CALL" else "INCOMING HD VOICE CALL",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = BharatWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = BharatElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Avatar with concentric pulsing rings
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Pulsing Ring 2
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(ringScale2)
                                .clip(CircleShape)
                                .background(
                                    if (callEvent.isVideo) BharatElectricCyan.copy(alpha = ringAlpha2)
                                    else BharatGreenLight.copy(alpha = ringAlpha2)
                                )
                        )

                        // Outer Pulsing Ring 1
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(ringScale1)
                                .clip(CircleShape)
                                .background(
                                    if (callEvent.isVideo) BharatElectricCyan.copy(alpha = ringAlpha1)
                                    else BharatGreenLight.copy(alpha = ringAlpha1)
                                )
                        )

                        // Core Avatar Surface
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            BharatSaffron,
                                            Color(0xFF1E293B),
                                            BharatGreenLight
                                        )
                                    )
                                )
                                .border(
                                    3.dp,
                                    Brush.sweepGradient(
                                        listOf(
                                            BharatSaffron,
                                            BharatWhite,
                                            BharatGreenLight,
                                            BharatElectricCyan,
                                            BharatSaffron
                                        )
                                    ),
                                    CircleShape
                                )
                                .testTag("incoming_call_avatar"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = callEvent.callerAvatar.ifBlank { "VA" },
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = BharatWhite
                            )
                        }

                        // Small bottom-right calling indicator badge
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = (-24).dp, y = (-12).dp)
                                .clip(CircleShape)
                                .background(BharatGreenLight)
                                .border(2.dp, Color(0xFF0F172A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (callEvent.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = null,
                                tint = BharatWhite,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Caller Information
                    Text(
                        text = callEvent.callerName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BharatWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("incoming_call_contact_name")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = callEvent.callerPhone,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = BharatElectricCyan,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BharatGreenLight)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ringing • Quantum Shield Level 5",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Secondary Quick Actions (e.g., Quick Message Reply)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x1FFFFFFF),
                            border = BorderStroke(0.8.dp, Color(0x2EFFFFFF)),
                            modifier = Modifier.clickable {
                                showQuickMessageMenu = !showQuickMessageMenu
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Quick Reply",
                                    tint = BharatWhite.copy(alpha = 0.85f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Reply with Message",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BharatWhite.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    if (showQuickMessageMenu) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "Can't talk now. What's up?",
                                "I'll call you right back!",
                                "In a meeting, please text."
                            ).forEach { msg ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x33000000),
                                    border = BorderStroke(0.5.dp, Color(0x22FFFFFF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onQuickMessage?.invoke(msg)
                                            onDecline()
                                        }
                                ) {
                                    Text(
                                        text = msg,
                                        fontSize = 11.5.sp,
                                        color = BharatWhite,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Primary Action Buttons: 'Accept' and 'Decline' with Material Icons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. DECLINE BUTTON (Red with Material CallEnd icon)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.testTag("decline_incoming_call_container")
                        ) {
                            IconButton(
                                onClick = onDecline,
                                modifier = Modifier
                                    .size(68.dp)
                                    .shadow(12.dp, CircleShape, spotColor = Color(0xFFEF4444))
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFFF87171),
                                                Color(0xFFDC2626)
                                            )
                                        )
                                    )
                                    .border(2.dp, Color(0xFFFF8A8A), CircleShape)
                                    .testTag("decline_incoming_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Decline Call",
                                    tint = BharatWhite,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Decline",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5)
                            )
                        }

                        // 2. ACCEPT BUTTON (Green with Material Call icon & pulsing invitation scale)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.testTag("accept_incoming_call_container")
                        ) {
                            IconButton(
                                onClick = onAccept,
                                modifier = Modifier
                                    .scale(acceptPulseScale)
                                    .size(68.dp)
                                    .shadow(14.dp, CircleShape, spotColor = Color(0xFF10B981))
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF34D399),
                                                Color(0xFF059669)
                                            )
                                        )
                                    )
                                    .border(2.dp, Color(0xFF6EE7B7), CircleShape)
                                    .testTag("accept_incoming_call_button")
                            ) {
                                Icon(
                                    imageVector = if (callEvent.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                    contentDescription = "Accept Call",
                                    tint = BharatWhite,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Accept",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }
                }
            }
        }
    }
}

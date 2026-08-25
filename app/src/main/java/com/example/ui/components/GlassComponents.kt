package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*

enum class ContactPresence {
    ONLINE,
    AWAY,
    BUSY,
    OFFLINE
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val bColors = LocalBharatColors.current
    val bg = backgroundColor ?: if (bColors.isDark) DarkCardBg.copy(alpha = 0.85f) else LightCardBg.copy(alpha = 0.92f)
    val border = borderColor ?: bColors.glassBorder

    Surface(
        modifier = modifier
            .then(
                if (onLongClick != null && onClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = bg,
        tonalElevation = elevation,
        border = BorderStroke(1.dp, border)
    ) {
        Column(content = content)
    }
}

@Composable
fun StatusRingAvatar(
    initial: String,
    avatarColorHex: String = "#FF671F",
    imageUri: String? = null,
    size: Dp = 52.dp,
    hasStory: Boolean = false,
    isStoryViewed: Boolean = false,
    isOnline: Boolean = false,
    presence: ContactPresence? = null,
    isAiBot: Boolean = false,
    onClick: () -> Unit = {}
) {
    val bColors = LocalBharatColors.current
    val avatarBg = try {
        Color(android.graphics.Color.parseColor(avatarColorHex))
    } catch (e: Exception) {
        BharatSaffron
    }

    val effectivePresence = presence ?: if (isOnline) ContactPresence.ONLINE else ContactPresence.OFFLINE

    // Pulsing effect for active online status
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Story ring
        if (hasStory) {
            val ringBrush = if (isStoryViewed) {
                Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF475569)))
            } else {
                Brush.sweepGradient(listOf(BharatSaffron, BharatElectricCyan, BharatGreenLight, BharatSaffron))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.5.dp, ringBrush, CircleShape)
            )
        }

        // Inner Avatar Box / Image
        val innerSize = if (hasStory) size - 7.dp else size
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .background(
                    if (isAiBot) Brush.linearGradient(listOf(BharatSaffron, BharatNavyLight, BharatGreenDark))
                    else Brush.linearGradient(listOf(avatarBg, avatarBg.copy(alpha = 0.75f)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isAiBot) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Bharat AI",
                    tint = BharatWhite,
                    modifier = Modifier.size(size * 0.5f)
                )
            } else if (!imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Profile Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Text(
                    text = initial.take(2).uppercase().ifEmpty { "C" },
                    color = BharatWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp
                )
            }
        }

        // Presence Status Indicator Badge / Dot
        if (effectivePresence != ContactPresence.OFFLINE) {
            val (statusColor, showPulse) = when (effectivePresence) {
                ContactPresence.ONLINE -> OnlineGreen to true
                ContactPresence.AWAY -> Color(0xFFF59E0B) to false
                ContactPresence.BUSY -> Color(0xFFEF4444) to false
                ContactPresence.OFFLINE -> Color(0xFF94A3B8) to false
            }

            Box(
                modifier = Modifier
                    .size(size * 0.32f)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                if (showPulse) {
                    Box(
                        modifier = Modifier
                            .size(size * 0.32f * pulseScale)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = pulseAlpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(size * 0.28f)
                        .clip(CircleShape)
                        .background(statusColor)
                        .border(2.dp, if (bColors.isDark) DarkBackground else LightBackground, CircleShape)
                )
            }
        }
    }
}

@Composable
fun TricolorGlowPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = BharatNavy.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(BharatSaffron, BharatWhite, BharatGreenLight)))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BharatSaffronLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BharatWhite
            )
        }
    }
}

@Composable
fun VerifiedBadge(
    modifier: Modifier = Modifier,
    isBusiness: Boolean = false
) {
    val tint = if (isBusiness) BharatGreenLight else BharatElectricCyan
    Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = "Verified",
        tint = tint,
        modifier = modifier.size(15.dp)
    )
}

@Composable
fun QuantumShieldBadge(
    modifier: Modifier = Modifier,
    text: String = "Quantum Encrypted"
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0x2210B981),
        border = BorderStroke(0.8.dp, Color(0x5510B981))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = BharatGreenLight,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = text,
                color = BharatGreenLight,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AudioWaveformVisualizer(
    waveformStr: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = BharatSaffron,
    inactiveColor: Color = Color(0x6694A3B8)
) {
    val defaultHeights = listOf(25, 50, 80, 40, 95, 60, 85, 30, 70, 90, 45, 65, 80, 55, 35)
    val heights = remember(waveformStr) {
        waveformStr?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: defaultHeights
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_anim"
    )

    Row(
        modifier = modifier.height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        heights.forEachIndexed { index, heightPercent ->
            val isCurrentBarActive = if (isPlaying) {
                (index.toFloat() / heights.size) <= animatedProgress
            } else {
                index < heights.size / 2
            }

            val barHeight = (28 * (heightPercent.coerceIn(15, 100) / 100f)).dp

            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isCurrentBarActive) activeColor else inactiveColor)
            )
        }
    }
}

@Composable
fun MessageStatusIndicator(
    status: String,
    modifier: Modifier = Modifier,
    isSeen: Boolean = false,
    isSecret: Boolean = false
) {
    if (isSecret) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Encrypted Secret Message",
            tint = SecretChatPink,
            modifier = modifier
                .size(13.dp)
                .testTag("message_status_secret")
        )
        return
    }

    if (isSeen || status.equals("SEEN", ignoreCase = true) || status.equals("READ", ignoreCase = true)) {
        Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = "Message Seen (Double Blue Checks)",
            tint = BharatElectricCyan,
            modifier = modifier
                .size(15.dp)
                .testTag("message_status_seen")
        )
        return
    }

    when (status.uppercase().trim()) {
        "SENDING" -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Message Sending",
                tint = Color(0x9994A3B8),
                modifier = modifier
                    .size(13.dp)
                    .testTag("message_status_sending")
            )
        }
        "SENT" -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Message Sent (Single Check)",
                tint = Color(0xCCB0BEC5),
                modifier = modifier
                    .size(14.dp)
                    .testTag("message_status_sent")
            )
        }
        "DELIVERED" -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Message Delivered (Double Grey Checks)",
                tint = Color(0xCCB0BEC5),
                modifier = modifier
                    .size(15.dp)
                    .testTag("message_status_delivered")
            )
        }
        else -> {
            // Default fallback
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Message Sent",
                tint = Color(0xCCB0BEC5),
                modifier = modifier
                    .size(14.dp)
                    .testTag("message_status_sent")
            )
        }
    }
}

@Composable
fun PulsingTypingIndicator(
    text: String = "typing...",
    color: Color = BharatGreenLight,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_pulsing")
    
    // Subtle glow/alpha pulse
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )

    // Pulsing dot scale
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    Row(
        modifier = modifier.testTag("header_typing_indicator"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Glowing status beacon dot
        Box(
            modifier = Modifier
                .size((7 * dotScale).dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alphaPulse))
        )

        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = color.copy(alpha = alphaPulse)
        )

        // 3 Animated Dancing Dots
        TypingDotsIndicator(
            color = color,
            dotSize = 3.5.dp
        )
    }
}

@Composable
fun TypingDotsIndicator(
    color: Color = BharatGreenLight,
    dotSize: Dp = 3.5.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")

    @Composable
    fun animateDotOffset(delayMillis: Int): State<Float> {
        return infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -3.5f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0f at 0
                    0f at delayMillis
                    -3.5f at (delayMillis + 250) using FastOutSlowInEasing
                    0f at (delayMillis + 500) using FastOutSlowInEasing
                    0f at 1000
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot_offset_$delayMillis"
        )
    }

    val dot1Offset by animateDotOffset(0)
    val dot2Offset by animateDotOffset(160)
    val dot3Offset by animateDotOffset(320)

    Row(
        modifier = modifier.padding(start = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .offset(y = dot1Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .offset(y = dot2Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .offset(y = dot3Offset.dp)
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
    }
}


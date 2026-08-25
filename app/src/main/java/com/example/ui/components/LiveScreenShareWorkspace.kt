package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class ScreenShareMode {
    APP_MIRROR,
    WHITEBOARD,
    PRESENTATION_SLIDES
}

data class DrawPoint(val offset: Offset, val color: Color, val strokeWidth: Float)

/**
 * Live Screen Sharing Workspace component that renders interactive device screen mirroring,
 * collaborative real-time whiteboard canvas with drawing tools, and presentation slide deck.
 */
@Composable
fun LiveScreenShareWorkspace(
    contactName: String,
    onStopSharing: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMode by remember { mutableStateOf(ScreenShareMode.APP_MIRROR) }
    var selectedColor by remember { mutableStateOf(BharatElectricCyan) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }
    val drawPaths = remember { mutableStateListOf<List<DrawPoint>>() }
    var currentPath = remember { mutableStateListOf<DrawPoint>() }
    var currentSlideIndex by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "screenshare_pulse")
    val livePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .testTag("live_screen_share_workspace")
    ) {
        // --- SCREEN SHARING CONTENT BY MODE ---
        when (currentMode) {
            ScreenShareMode.APP_MIRROR -> {
                AppMirrorStreamView(contactName = contactName)
            }
            ScreenShareMode.WHITEBOARD -> {
                WhiteboardCanvasView(
                    drawPaths = drawPaths,
                    currentPath = currentPath,
                    selectedColor = selectedColor,
                    strokeWidth = strokeWidth,
                    onDrawPoint = { pt -> currentPath.add(pt) },
                    onPathEnd = {
                        if (currentPath.isNotEmpty()) {
                            drawPaths.add(currentPath.toList())
                            currentPath.clear()
                        }
                    },
                    onClear = {
                        drawPaths.clear()
                        currentPath.clear()
                    },
                    onColorChange = { selectedColor = it }
                )
            }
            ScreenShareMode.PRESENTATION_SLIDES -> {
                PresentationSlidesView(
                    currentSlideIndex = currentSlideIndex,
                    onSlideChange = { currentSlideIndex = it }
                )
            }
        }

        // --- TOP FLOATING SCREEN SHARE STATUS & MODE TOOLBAR ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, start = 12.dp, end = 12.dp)
        ) {
            // Live Status Banner & Stop Button
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xDD0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, BharatElectricCyan.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(RoseError.copy(alpha = livePulseAlpha))
                        )

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SCREEN SHARING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BharatElectricCyan,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "1080p 60fps",
                                    fontSize = 10.sp,
                                    color = BharatGreenLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Sharing screen with $contactName",
                                fontSize = 10.sp,
                                color = BharatWhite.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Button(
                        onClick = onStopSharing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("stop_screen_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopScreenShare,
                            contentDescription = "Stop Sharing",
                            tint = BharatWhite,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Stop",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BharatWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mode Selector Chips: [App Mirror | Whiteboard | Slides]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScreenShareMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    val title = when (mode) {
                        ScreenShareMode.APP_MIRROR -> "📱 App Mirror"
                        ScreenShareMode.WHITEBOARD -> "🎨 Whiteboard"
                        ScreenShareMode.PRESENTATION_SLIDES -> "📊 Slides"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) BharatElectricCyan.copy(alpha = 0.3f) else Color(0xAA0F172A),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) BharatElectricCyan else Color(0x3364748B)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { currentMode = mode }
                            .testTag("screenshare_mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BharatElectricCyan else BharatWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-definition simulated App Mirror screen stream with live chat preview,
 * interactive message bubble, and streaming indicators.
 */
@Composable
private fun AppMirrorStreamView(contactName: String) {
    val transition = rememberInfiniteTransition(label = "cursor_blink")
    val typingDot by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "typing_dot"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 115.dp, bottom = 160.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Device Mockup Container
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = Color(0xF00A0F1D),
            borderColor = BharatElectricCyan.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Mock App Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BharatSaffron),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "V",
                                fontWeight = FontWeight.Bold,
                                color = BharatWhite,
                                fontSize = 16.sp
                            )
                        }
                        Column {
                            Text(
                                text = "VenzoInd Live Mirror",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatWhite
                            )
                            Text(
                                text = "WebRTC Stream 1080p • Encrypted",
                                fontSize = 9.5.sp,
                                color = BharatElectricCyan
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BharatGreenLight.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BharatGreenLight)
                    ) {
                        Text(
                            text = "CASTING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BharatGreenLight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = Color(0x3364748B)
                )

                // Shared Screen Dynamic Content Cards
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Shared Workspace Presentation card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF131D31),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4438BDF8))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CastConnected,
                                    contentDescription = null,
                                    tint = BharatElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Active Screen Broadcast",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BharatElectricCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sharing high-framerate visuals, documents, and code directly with $contactName without quality degradation.",
                                fontSize = 10.5.sp,
                                color = BharatWhite.copy(alpha = 0.8f),
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Live Simulated Chat Stream in App Mirror
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3364748B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Message 1
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BharatSaffron.copy(alpha = 0.25f),
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    Text(
                                        text = "Sharing architecture blueprint & realtime telemetry...",
                                        fontSize = 11.sp,
                                        color = BharatWhite,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                // Message 2
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BharatElectricCyan.copy(alpha = 0.25f),
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .align(Alignment.End)
                                ) {
                                    Text(
                                        text = "Crystal clear 60fps feed received over P2P mesh!",
                                        fontSize = 11.sp,
                                        color = BharatWhite,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            // Simulated Active Typing Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(BharatElectricCyan.copy(alpha = typingDot))
                                )
                                Text(
                                    text = "Live pointer & cursor synced...",
                                    fontSize = 10.sp,
                                    color = BharatElectricCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Collaborative Whiteboard where the caller and participant can draw in real-time.
 */
@Composable
private fun WhiteboardCanvasView(
    drawPaths: List<List<DrawPoint>>,
    currentPath: List<DrawPoint>,
    selectedColor: Color,
    strokeWidth: Float,
    onDrawPoint: (DrawPoint) -> Unit,
    onPathEnd: () -> Unit,
    onClear: () -> Unit,
    onColorChange: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 115.dp, bottom = 160.dp, start = 14.dp, end = 14.dp)
    ) {
        // Whiteboard Canvas Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A))
                .border(1.5.dp, BharatElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .pointerInput(selectedColor, strokeWidth) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onDrawPoint(DrawPoint(offset, selectedColor, strokeWidth))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            onDrawPoint(DrawPoint(change.position, selectedColor, strokeWidth))
                        },
                        onDragEnd = { onPathEnd() },
                        onDragCancel = { onPathEnd() }
                    )
                }
                .testTag("whiteboard_touch_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw completed lines
                drawPaths.forEach { pathPoints ->
                    if (pathPoints.size > 1) {
                        for (i in 0 until pathPoints.size - 1) {
                            val p1 = pathPoints[i]
                            val p2 = pathPoints[i + 1]
                            drawLine(
                                color = p1.color,
                                start = p1.offset,
                                end = p2.offset,
                                strokeWidth = p1.strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Draw active continuous line
                if (currentPath.size > 1) {
                    for (i in 0 until currentPath.size - 1) {
                        val p1 = currentPath[i]
                        val p2 = currentPath[i + 1]
                        drawLine(
                            color = p1.color,
                            start = p1.offset,
                            end = p2.offset,
                            strokeWidth = p1.strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            if (drawPaths.isEmpty() && currentPath.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = null,
                        tint = BharatElectricCyan.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Touch & Drag anywhere to Draw on Whiteboard",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BharatWhite.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Whiteboard Tool Palette (Color selection & Clear)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xDD1E293B))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Color choices
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val colors = listOf(
                    BharatElectricCyan,
                    BharatSaffron,
                    BharatGreenLight,
                    BharatWhite,
                    RoseError
                )

                colors.forEach { color ->
                    val isColorSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isColorSelected) 3.dp else 1.dp,
                                color = if (isColorSelected) BharatWhite else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorChange(color) }
                    )
                }
            }

            // Clear Canvas button
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Slide deck presentation view with interactive Next / Previous slide controls.
 */
@Composable
private fun PresentationSlidesView(
    currentSlideIndex: Int,
    onSlideChange: (Int) -> Unit
) {
    val slides = listOf(
        SlideContent(
            title = "🇮🇳 VenzoInd P2P Architecture",
            subtitle = "Zero-Latency WebRTC Ultra-HD Audio & Video Mesh",
            bulletPoints = listOf(
                "Direct peer-to-peer encryption with post-quantum key exchange",
                "Adaptive bitrate streaming with sub-25ms jitter buffer",
                "Hardware accelerated multi-camera front & back capture",
                "Built-in offline fallback & mesh synchronization"
            ),
            accentColor = BharatSaffron
        ),
        SlideContent(
            title = "🛡️ Quantum-Shield Encryption",
            subtitle = "Military-grade End-to-End Privacy Matrix",
            bulletPoints = listOf(
                "Kyber-1024 Post-Quantum Key Encapsulation Mechanism",
                "AES-256-GCM symmetric payload stream cipher",
                "Decentralized identity verification with biometric auth",
                "Strict zero-telemetry, on-device local storage"
            ),
            accentColor = BharatElectricCyan
        ),
        SlideContent(
            title = "⚡ Multi-Lens Camera & PiP Engine",
            subtitle = "Seamless Gesture-Driven Screen & Camera Integration",
            bulletPoints = listOf(
                "Instant Front ↔ Back Camera switching without stream drop",
                "Tap-to-toggle full screen & floating Picture-in-Picture window",
                "Integrated Accompanist runtime permission negotiation",
                "Concurrent screen presentation with live camera overlay"
            ),
            accentColor = BharatGreenLight
        )
    )

    val slide = slides[currentSlideIndex.coerceIn(0, slides.size - 1)]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 115.dp, bottom = 160.dp, start = 14.dp, end = 14.dp)
    ) {
        // Slide Canvas Card
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color(0xF00F172A),
            borderColor = slide.accentColor.copy(alpha = 0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = slide.accentColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, slide.accentColor)
                        ) {
                            Text(
                                text = "SLIDE ${currentSlideIndex + 1} OF ${slides.size}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = slide.accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Slideshow,
                            contentDescription = null,
                            tint = slide.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = slide.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatWhite
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = slide.subtitle,
                        fontSize = 11.5.sp,
                        color = slide.accentColor,
                        fontWeight = FontWeight.Medium
                    )

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color(0x3364748B)
                    )

                    // Bullet points
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        slide.bulletPoints.forEach { point ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 5.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(slide.accentColor)
                                )
                                Text(
                                    text = point,
                                    fontSize = 12.sp,
                                    color = BharatWhite.copy(alpha = 0.85f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Slide Navigation Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { if (currentSlideIndex > 0) onSlideChange(currentSlideIndex - 1) },
                        enabled = currentSlideIndex > 0,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (currentSlideIndex > 0) Color(0x991E293B) else Color(0x331E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous Slide",
                            tint = if (currentSlideIndex > 0) BharatWhite else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Slide indicators dots
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        slides.indices.forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == currentSlideIndex) 16.dp else 8.dp, 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (idx == currentSlideIndex) slide.accentColor else Color(0x4464748B)
                                    )
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (currentSlideIndex < slides.size - 1) onSlideChange(currentSlideIndex + 1) },
                        enabled = currentSlideIndex < slides.size - 1,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (currentSlideIndex < slides.size - 1) Color(0x991E293B) else Color(0x331E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Next Slide",
                            tint = if (currentSlideIndex < slides.size - 1) BharatWhite else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class SlideContent(
    val title: String,
    val subtitle: String,
    val bulletPoints: List<String>,
    val accentColor: Color
)

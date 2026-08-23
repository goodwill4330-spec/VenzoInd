package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.*

/**
 * Full-screen zoomable DP and image viewer with pinch-to-zoom, double-tap zoom, and pan gestures.
 */
@Composable
fun ZoomableProfilePicDialog(
    title: String,
    subtitle: String? = "",
    avatarInitial: String = "VA",
    avatarColorHex: String = "#FF671F",
    imageUri: String? = null,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(durationMillis = 200),
        label = "dp_zoom_anim"
    )

    val avatarBg = try {
        Color(android.graphics.Color.parseColor(avatarColorHex))
    } catch (e: Exception) {
        BharatSaffron
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .testTag("zoomable_dp_dialog")
        ) {
            // Top Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .testTag("close_zoom_dp_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                color = Color(0xCCFFFFFF),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Reset zoom button
                    if (scale > 1.05f) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .testTag("reset_zoom_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "Reset Zoom",
                                tint = BharatElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Zoom in quick button
                    IconButton(
                        onClick = {
                            if (scale < 2.5f) {
                                scale = 2.5f
                            } else {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .testTag("toggle_zoom_button")
                    ) {
                        Icon(
                            imageVector = if (scale > 1.1f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                            contentDescription = "Toggle Zoom",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Central Zoomable Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 80.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxOffset = (scale - 1f) * 400f
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                    y = (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(
                                    3.dp,
                                    Brush.sweepGradient(
                                        listOf(BharatSaffron, BharatWhite, BharatGreenLight, BharatSaffron)
                                    ),
                                    CircleShape
                                ),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(avatarBg, Color(0xFF0F172A), BharatGreenLight)
                                    )
                                )
                                .border(
                                    3.dp,
                                    Brush.sweepGradient(
                                        listOf(BharatSaffron, BharatWhite, BharatGreenLight, BharatSaffron)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = avatarInitial.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 92.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Bottom Hint Bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0x66000000),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pinch,
                        contentDescription = null,
                        tint = BharatElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (scale > 1.05f) "Zoom: ${(scale * 100).toInt()}% • Drag to pan" else "Pinch or double tap to zoom DP",
                        color = Color(0xEEFFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

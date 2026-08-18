package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BharatGreenLight
import com.example.ui.theme.DarkBackground

@Composable
fun VenzoraLogoEmblem(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val emeraldNeon = Color(0xFF22C55E)
    val emeraldGlow = Color(0xFF10B981)
    val deepSlate = Color(0xFF0A1814)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(deepSlate)
            .border(
                (size * 0.045f).coerceAtLeast(1.dp),
                Brush.sweepGradient(
                    listOf(
                        emeraldNeon,
                        Color(0xFF86EFAC),
                        Color.White,
                        emeraldGlow,
                        emeraldNeon
                    )
                ),
                RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner circle accent
        Box(
            modifier = Modifier
                .size(size * 0.78f)
                .clip(CircleShape)
                .border(1.dp, emeraldNeon.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Stylized 'V' monogram with chat dot accent
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "V",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.52f).sp
                )
                Box(
                    modifier = Modifier
                        .size((size * 0.16f).coerceAtLeast(3.dp))
                        .offset(x = (-2).dp, y = (-size * 0.18f))
                        .clip(CircleShape)
                        .background(emeraldNeon)
                )
            }
        }
    }
}

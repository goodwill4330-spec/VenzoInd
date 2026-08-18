package com.example.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.components.TricolorGlowPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel

@Composable
fun BharatAiTab(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.bharatAiHistory.collectAsState()
    val isThinking by viewModel.isBharatAiThinking.collectAsState()
    val bColors = LocalBharatColors.current

    var userPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    val samplePrompts = listOf(
        "Translate 'Meeting rescheduled to 4 PM' to Hindi, Tamil & Telugu",
        "Explain Quantum End-to-End Encryption in VenzoInd",
        "Draft a formal request to transfer ₹5,000 via UPI",
        "Summarize today's top Indian tech & space news"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (bColors.isDark) DarkBackground else LightBackground)
    ) {
        // AI Hero Banner
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = BharatNavy.copy(alpha = 0.35f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(BharatSaffron, BharatWhite, BharatGreenLight, BharatElectricCyan, BharatSaffron)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DarkBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Bharat AI",
                            tint = BharatSaffronLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "VenzoInd AI Copilot",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = BharatWhite
                        )
                        TricolorGlowPill(text = "Sovereign AI")
                    }
                    Text(
                        text = "12+ Indian Languages • 100% On-Device & Cloud Privacy",
                        fontSize = 11.sp,
                        color = BharatElectricCyan
                    )
                }
            }
        }

        // Suggestions Carousel
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(samplePrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder),
                    modifier = Modifier
                        .clickable {
                            viewModel.askBharatAiTab(prompt)
                        }
                        .testTag("ai_prompt_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = BharatSaffron,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = prompt.take(35) + "...",
                            fontSize = 11.5.sp,
                            color = bColors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Message stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(history) { (role, text) ->
                val isUser = role == "user"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isUser) {
                            Color(0xFF1E3A8A)
                        } else {
                            if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isUser) BharatElectricCyan.copy(alpha = 0.4f) else bColors.glassBorder
                        ),
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!isUser) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = BharatSaffronLight,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Bharat AI",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BharatSaffronLight
                                        )
                                    }

                                    // TTS Speaker
                                    IconButton(
                                        onClick = { viewModel.ttsManager.speakText(text) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.VolumeUp,
                                            contentDescription = "Read Aloud",
                                            tint = BharatElectricCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Text(
                                text = text,
                                fontSize = 13.5.sp,
                                color = if (isUser) BharatWhite else bColors.textPrimary,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = BharatSaffron,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Bharat AI is thinking...",
                                    fontSize = 12.sp,
                                    color = BharatElectricCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom input
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(26.dp),
            color = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = {
                        Text(
                            "Ask Bharat AI in any Indian language...",
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
                        .testTag("ai_tab_input_field")
                )

                IconButton(
                    onClick = {
                        val prompt = userPrompt
                        userPrompt = ""
                        viewModel.askBharatAiTab(prompt)
                    },
                    enabled = userPrompt.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (userPrompt.isNotBlank()) Brush.linearGradient(listOf(BharatSaffron, BharatSaffronLight))
                            else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                        )
                        .testTag("send_ai_prompt_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = BharatWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelEntity
import com.example.data.model.StoryEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusRingAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel

@Composable
fun UpdatesTab(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val stories by viewModel.stories.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val bColors = LocalBharatColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(if (bColors.isDark) DarkBackground else LightBackground),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Status & Stories Section
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "Status & Stories",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = bColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // My Status Add Card
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clickable { }
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(BharatNavyLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ME",
                                        color = BharatWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(BharatSaffron)
                                        .border(2.dp, DarkBackground, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = BharatWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "My Status",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = bColors.textPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "+ AI Effect",
                                fontSize = 9.5.sp,
                                color = BharatElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Contact Stories
                    items(stories, key = { it.id }) { story ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clickable { viewModel.openStory(story) }
                                .testTag("story_bubble_${story.id}")
                        ) {
                            StatusRingAvatar(
                                initial = story.authorAvatar,
                                size = 56.dp,
                                hasStory = true,
                                isStoryViewed = story.isViewed,
                                onClick = { viewModel.openStory(story) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = story.authorName.split(" ").firstOrNull() ?: "",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = bColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = story.timeAgo,
                                fontSize = 9.5.sp,
                                color = bColors.textMuted
                            )
                        }
                    }
                }
            }
        }

        // Channels & Communities Section Header
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Communities & Channels",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = bColors.textPrimary
                    )
                    Text(
                        text = "Verified broadcasts across India",
                        fontSize = 12.sp,
                        color = bColors.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = BharatSaffron.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BharatSaffron.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Explore",
                        color = BharatSaffronLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Channels List
        items(channels, key = { it.id }) { channel ->
            ChannelItemCard(
                channel = channel,
                onToggleJoin = { viewModel.toggleJoinChannel(channel.id, channel.isJoined) }
            )
        }
    }
}

@Composable
fun ChannelItemCard(
    channel: ChannelEntity,
    onToggleJoin: () -> Unit
) {
    val bColors = LocalBharatColors.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("channel_item_${channel.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(BharatNavyLight, BharatGreenDark))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = channel.avatarInitial,
                        color = BharatWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = channel.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = bColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (channel.verified) {
                            VerifiedBadge()
                        }
                    }

                    Text(
                        text = "${channel.followersCountStr} subscribers • ${channel.category}",
                        fontSize = 11.5.sp,
                        color = bColors.textSecondary
                    )
                }

                Button(
                    onClick = onToggleJoin,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (channel.isJoined) Color(0x3364748B) else BharatSaffron
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (channel.isJoined) "Following" else "Follow",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatWhite
                    )
                }
            }

            if (channel.latestPost.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = BharatElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = channel.latestPost,
                            fontSize = 12.sp,
                            color = bColors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoryViewerScreen(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val activeStory by viewModel.activeStory.collectAsState()

    if (activeStory == null) {
        return
    }

    val story = activeStory!!
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(story.id) {
        val startTime = System.currentTimeMillis()
        val duration = 5000L
        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
            kotlinx.coroutines.delay(50)
        }
        viewModel.closeStory()
    }

    val startColor = try {
        Color(android.graphics.Color.parseColor(story.mediaGradientStart))
    } catch (e: Exception) {
        BharatSaffron
    }
    val endColor = try {
        Color(android.graphics.Color.parseColor(story.mediaGradientEnd))
    } catch (e: Exception) {
        BharatNavy
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Story Canvas Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(startColor, endColor, Color.Black))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                if (story.isAiGenerated) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BharatElectricCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BharatElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = story.aiEffectName,
                                color = BharatElectricCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Text(
                    text = story.caption,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = BharatWhite,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Top progress bar & Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = BharatWhite,
                trackColor = Color.White.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.closeStory() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = BharatWhite
                    )
                }

                StatusRingAvatar(initial = story.authorAvatar, size = 36.dp)

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = story.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BharatWhite
                    )
                    Text(
                        text = story.timeAgo,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = { viewModel.closeStory() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BharatWhite
                    )
                }
            }
        }

        // Bottom Reply Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(23.dp),
                color = Color.Black.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reply with AI reaction...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }

            listOf("🔥", "❤️", "🇮🇳").forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                    modifier = Modifier.clickable {
                        viewModel.closeStory()
                    }
                )
            }
        }
    }
}

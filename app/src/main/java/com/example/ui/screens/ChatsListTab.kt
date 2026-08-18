package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import com.example.ui.viewmodel.ChatFilter

@Composable
fun ChatsListTab(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val chats by viewModel.chats.collectAsState()
    val activeFilter by viewModel.chatFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val bColors = LocalBharatColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (bColors.isDark) DarkBackground else LightBackground)
    ) {
        // Search bar in Glass style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(24.dp),
                color = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = bColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Search chats, AI, messages...",
                                color = bColors.textMuted,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
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
                            .testTag("chat_search_input")
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = bColors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Filter Pills Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ChatFilter.values()) { filter ->
                val isSelected = filter == activeFilter
                val label = when (filter) {
                    ChatFilter.ALL -> "All"
                    ChatFilter.UNREAD -> "Unread"
                    ChatFilter.GROUPS -> "Groups"
                    ChatFilter.SECRET -> "🔒 Secret"
                    ChatFilter.BUSINESS -> "🏢 Business"
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) {
                        if (filter == ChatFilter.SECRET) SecretChatPink else BharatSaffron
                    } else {
                        if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color.Transparent else bColors.glassBorder
                    ),
                    modifier = Modifier
                        .clickable { viewModel.setChatFilter(filter) }
                        .testTag("filter_${filter.name.lowercase()}")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) BharatWhite else bColors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Chat list
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = bColors.textMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No conversations found",
                        color = bColors.textSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap the + button to start a secure chat",
                        color = bColors.textMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(chats, key = { it.id }) { chat ->
                    ChatItemRow(
                        chat = chat,
                        onClick = { viewModel.openChat(chat.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatItemRow(
    chat: ChatEntity,
    onClick: () -> Unit
) {
    val bColors = LocalBharatColors.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_item_${chat.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Status Ring
            StatusRingAvatar(
                initial = chat.avatarInitial,
                avatarColorHex = chat.avatarColorHex,
                size = 52.dp,
                hasStory = chat.id != "chat_ai_assistant",
                isStoryViewed = false,
                isOnline = chat.isOnline,
                isAiBot = chat.isAiAssistant,
                onClick = onClick
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (chat.isSecret) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secret",
                            tint = SecretChatPink,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Text(
                        text = chat.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (chat.isSecret) SecretChatPink else bColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (chat.isVerifiedBusiness) {
                        VerifiedBadge(isBusiness = true)
                    } else if (chat.isAiAssistant) {
                        VerifiedBadge(isBusiness = false)
                    }

                    if (chat.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = BharatSaffron,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (chat.unreadCount == 0 && chat.lastMessage.isNotBlank()) {
                        MessageStatusIndicator(
                            status = chat.lastMessageStatus,
                            isSeen = chat.lastMessageStatus.equals("SEEN", ignoreCase = true) || chat.lastMessageStatus.equals("READ", ignoreCase = true),
                            isSecret = chat.isSecret
                        )
                    }

                    Text(
                        text = chat.lastMessage.ifBlank { chat.subtitle },
                        color = if (chat.unreadCount > 0) bColors.textPrimary else bColors.textSecondary,
                        fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Time and Unread pill
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = chat.lastMessageTime,
                    fontSize = 11.sp,
                    color = if (chat.unreadCount > 0) BharatSaffron else bColors.textMuted,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                )

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BharatSaffron, BharatSaffronLight)
                                )
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = BharatWhite,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (chat.isSecret && chat.disappearingSeconds > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SecretChatPink.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, SecretChatPink.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${chat.disappearingSeconds}s",
                            color = SecretChatPink,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

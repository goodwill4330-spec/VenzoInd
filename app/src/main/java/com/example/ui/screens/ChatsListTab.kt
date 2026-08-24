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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListTab(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val chats by viewModel.chats.collectAsState()
    val activeFilter by viewModel.chatFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val bColors = LocalBharatColors.current
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedChatForActions by remember { mutableStateOf<ChatEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<ChatEntity?>(null) }
    var showClearMessagesConfirmDialog by remember { mutableStateOf<ChatEntity?>(null) }

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
                        color = bColors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All demo names & chats have been removed.\nTap the + button to start a new chat.",
                        color = bColors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        onClick = { viewModel.openChat(chat.id) },
                        onLongClick = { selectedChatForActions = chat },
                        onAvatarClick = { viewModel.openContactProfileFromChat(chat) }
                    )
                }
            }
        }
    }

    // Long-press BottomSheet Options Menu
    if (selectedChatForActions != null) {
        val activeSelected = selectedChatForActions!!
        ModalBottomSheet(
            onDismissRequest = { selectedChatForActions = null },
            containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    StatusRingAvatar(
                        initial = activeSelected.avatarInitial,
                        avatarColorHex = activeSelected.avatarColorHex,
                        size = 46.dp,
                        isAiBot = activeSelected.isAiAssistant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = activeSelected.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = bColors.textPrimary
                        )
                        Text(
                            text = activeSelected.subtitle.ifBlank { "Options" },
                            fontSize = 13.sp,
                            color = bColors.textSecondary
                        )
                    }
                }

                HorizontalDivider(color = bColors.glassBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Pin / Unpin
                ListItem(
                    headlineContent = {
                        Text(
                            if (activeSelected.isPinned) "Unpin Chat (अनपिन करें)" else "Pin to Top (ऊपर पिन करें)",
                            color = bColors.textPrimary
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = if (activeSelected.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = BharatSaffron
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.toggleChatPin(activeSelected.id)
                        selectedChatForActions = null
                    }
                )

                // View Contact Info
                ListItem(
                    headlineContent = { Text("Contact Info (प्रोफ़ाइल देखें)", color = bColors.textPrimary) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = BharatElectricCyan
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.openContactProfileFromChat(activeSelected)
                        selectedChatForActions = null
                    }
                )

                // Clear Messages
                ListItem(
                    headlineContent = { Text("Clear Messages (मैसेज साफ करें)", color = bColors.textPrimary) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.CleaningServices,
                            contentDescription = null,
                            tint = BharatSaffron
                        )
                    },
                    modifier = Modifier.clickable {
                        showClearMessagesConfirmDialog = activeSelected
                        selectedChatForActions = null
                    }
                )

                // Delete Chat (Red)
                ListItem(
                    headlineContent = {
                        Text("Delete Chat (चैट हटाएं)", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFEF4444)
                        )
                    },
                    modifier = Modifier.clickable {
                        showDeleteConfirmDialog = activeSelected
                        selectedChatForActions = null
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Delete Chat Confirmation Dialog
    if (showDeleteConfirmDialog != null) {
        val target = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    text = "Delete Chat with ${target.title}?",
                    fontWeight = FontWeight.Bold,
                    color = bColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "All messages and media with ${target.title} will be permanently removed from this device.",
                    color = bColors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChat(target.id)
                        showDeleteConfirmDialog = null
                        android.widget.Toast.makeText(context, "${target.title} removed", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("DELETE (हटाएं)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("CANCEL", color = bColors.textSecondary)
                }
            },
            containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
        )
    }

    // Clear Messages Confirmation Dialog
    if (showClearMessagesConfirmDialog != null) {
        val target = showClearMessagesConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showClearMessagesConfirmDialog = null },
            title = {
                Text(
                    text = "Clear all messages in ${target.title}?",
                    fontWeight = FontWeight.Bold,
                    color = bColors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Messages will be cleared. The conversation will remain in your chat list.",
                    color = bColors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChatHistory(target.id)
                        showClearMessagesConfirmDialog = null
                        android.widget.Toast.makeText(context, "Messages cleared", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron)
                ) {
                    Text("CLEAR", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMessagesConfirmDialog = null }) {
                    Text("CANCEL", color = bColors.textSecondary)
                }
            },
            containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
        )
    }
}

@Composable
fun ChatItemRow(
    chat: ChatEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onAvatarClick: () -> Unit = onClick
) {
    val bColors = LocalBharatColors.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_item_${chat.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Status Ring (tap avatar opens contact profile & zoomable DP)
            StatusRingAvatar(
                initial = chat.avatarInitial,
                avatarColorHex = chat.avatarColorHex,
                size = 52.dp,
                hasStory = chat.id != "chat_ai_assistant",
                isStoryViewed = false,
                isOnline = chat.isOnline,
                isAiBot = chat.isAiAssistant,
                onClick = onAvatarClick
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

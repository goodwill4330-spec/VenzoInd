package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import com.example.data.model.ContactEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.QuantumShieldBadge
import com.example.ui.components.StatusRingAvatar
import com.example.ui.components.TricolorGlowPill
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BharatChatViewModel
import com.example.ui.viewmodel.ContactSortOrder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsListScreen(
    viewModel: BharatChatViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bColors = LocalBharatColors.current
    val searchQuery by viewModel.contactSearchQuery.collectAsState()
    val sortOrder by viewModel.contactSortOrder.collectAsState()
    val contacts by viewModel.filteredAndSortedContacts.collectAsState()
    val allContacts by viewModel.contacts.collectAsState()
    val showBackupRestore by viewModel.showBackupRestoreDialog.collectAsState()

    val context = LocalContext.current
    var isSyncingPhonebook by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isSyncingPhonebook = true
            viewModel.syncDevicePhonebookContacts(context) { summary ->
                isSyncingPhonebook = false
                Toast.makeText(
                    context,
                    "Synced ${summary.totalDeviceContacts} contacts (${summary.matchedCount} on VenzoInd)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(context, "Permission to read contacts was denied", Toast.LENGTH_SHORT).show()
        }
    }

    var showAddContactDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = if (bColors.isDark) DarkSurface.copy(alpha = 0.95f) else LightSurface.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, bColors.glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("contacts_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = bColors.textPrimary
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Contacts",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = bColors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TricolorGlowPill(text = "E2EE")
                            }
                            Text(
                                text = "${allContacts.size} sovereign verified contacts",
                                fontSize = 11.5.sp,
                                color = bColors.textSecondary
                            )
                        }
                    }

                    // Top Action Buttons (Sync from Phonebook, Backup/Restore & Add Contact)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.READ_CONTACTS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    isSyncingPhonebook = true
                                    viewModel.syncDevicePhonebookContacts(context) { summary ->
                                        isSyncingPhonebook = false
                                        Toast.makeText(
                                            context,
                                            "Synced ${summary.totalDeviceContacts} contacts (${summary.matchedCount} on VenzoInd)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BharatElectricCyan.copy(alpha = 0.2f))
                                .testTag("contacts_sync_phonebook_button")
                        ) {
                            if (isSyncingPhonebook) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = BharatElectricCyan,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Mobile Contacts",
                                    tint = BharatElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.showBackupRestoreDialog.value = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BharatGreenLight.copy(alpha = 0.2f))
                                .testTag("contacts_backup_restore_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Export / Restore Contacts",
                                tint = BharatGreenLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showAddContactDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BharatSaffron.copy(alpha = 0.2f))
                                .testTag("add_contact_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add Contact",
                                tint = BharatSaffron,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddContactDialog = true },
                containerColor = BharatSaffron,
                contentColor = BharatWhite,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_contact")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Contact"
                    )
                    Text("New Contact", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (bColors.isDark) DarkBackground else LightBackground)
                .padding(innerPadding)
        ) {
            // Search Bar Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (bColors.isDark) Color(0x331E293B) else Color(0x1564748B),
                border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BharatElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setContactSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = "Search by name, phone, or UPI ID...",
                                fontSize = 13.5.sp,
                                color = bColors.textMuted
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
                            .testTag("contact_search_bar")
                    )

                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.setContactSearchQuery("") },
                            modifier = Modifier.size(28.dp).testTag("clear_contact_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Search",
                                tint = bColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Sync Phonebook Contacts Prompt Card (if permission not yet granted)
            val hasContactPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasContactPerm) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = BharatElectricCyan.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BharatElectricCyan.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(BharatElectricCyan.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    tint = BharatElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Sync Device Phonebook",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = bColors.textPrimary
                                )
                                Text(
                                    text = "Match device numbers to display registered names",
                                    fontSize = 11.sp,
                                    color = bColors.textSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BharatElectricCyan),
                            modifier = Modifier.testTag("grant_contacts_permission_btn")
                        ) {
                            Text(
                                text = "Allow",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Sorting Controls Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = BharatSaffron,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sort Contacts by:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = bColors.textSecondary
                        )
                    }

                    Text(
                        text = "${contacts.size} found",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BharatElectricCyan
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Sorting Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SortChip(
                            label = "Name (A-Z)",
                            icon = Icons.Default.SortByAlpha,
                            isSelected = sortOrder == ContactSortOrder.NAME_ASC,
                            onClick = { viewModel.setContactSortOrder(ContactSortOrder.NAME_ASC) },
                            testTag = "sort_chip_name_asc"
                        )
                    }

                    item {
                        SortChip(
                            label = "Recent Activity ⏱️",
                            icon = Icons.Default.Schedule,
                            isSelected = sortOrder == ContactSortOrder.RECENT_ACTIVITY,
                            onClick = { viewModel.setContactSortOrder(ContactSortOrder.RECENT_ACTIVITY) },
                            testTag = "sort_chip_recent_activity"
                        )
                    }

                    item {
                        SortChip(
                            label = "Favorites ⭐",
                            icon = Icons.Default.Star,
                            isSelected = sortOrder == ContactSortOrder.FAVORITES_FIRST,
                            onClick = { viewModel.setContactSortOrder(ContactSortOrder.FAVORITES_FIRST) },
                            testTag = "sort_chip_favorites"
                        )
                    }

                    item {
                        SortChip(
                            label = "Name (Z-A)",
                            icon = Icons.Default.SortByAlpha,
                            isSelected = sortOrder == ContactSortOrder.NAME_DESC,
                            onClick = { viewModel.setContactSortOrder(ContactSortOrder.NAME_DESC) },
                            testTag = "sort_chip_name_desc"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Contacts List
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(BharatNavyLight.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = BharatElectricCyan,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Text(
                            text = if (searchQuery.isNotBlank()) "No contacts found matching \"$searchQuery\"" else "No contacts available",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = bColors.textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching by a different name, phone number, or UPI address." else "Tap the '+' button below to add your first contact.",
                            fontSize = 12.5.sp,
                            color = bColors.textSecondary,
                            textAlign = TextAlign.Center
                        )

                        if (searchQuery.isNotBlank()) {
                            Button(
                                onClick = { viewModel.setContactSearchQuery("") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BharatElectricCyan),
                                modifier = Modifier.testTag("empty_clear_search_button")
                            ) {
                                Text("Clear Search", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        ContactItemCard(
                            contact = contact,
                            onChatClick = {
                                viewModel.createChat(
                                    title = contact.name,
                                    subtitle = contact.phone,
                                    isGroup = false,
                                    isSecret = false,
                                    isBusiness = false
                                )
                            },
                            onCallClick = { isVideo ->
                                viewModel.startCall(
                                    contactName = contact.name,
                                    contactAvatar = contact.avatarInitial,
                                    isVideo = isVideo,
                                    contactPhone = contact.phone
                                )
                            },
                            onUpiClick = {
                                viewModel.showUpiPaymentSheet.value = true
                            },
                            onToggleFavorite = {
                                viewModel.toggleContactFavorite(contact.id, contact.isFavorite)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onAddContact = { name, phone, upi, status ->
                viewModel.addNewContact(name, phone, upi, status)
                showAddContactDialog = false
            }
        )
    }

    if (showBackupRestore) {
        com.example.ui.components.BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showBackupRestoreDialog.value = false }
        )
    }
}

@Composable
fun SortChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bColors = LocalBharatColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) BharatSaffron else if (bColors.isDark) Color(0x331E293B) else Color(0x22CBD5E1),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) BharatSaffron else bColors.glassBorder
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) BharatWhite else bColors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) BharatWhite else bColors.textPrimary
            )
        }
    }
}

@Composable
fun ContactItemCard(
    contact: ContactEntity,
    onChatClick: () -> Unit,
    onCallClick: (isVideo: Boolean) -> Unit,
    onUpiClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val bColors = LocalBharatColors.current
    val recentActivityText = remember(contact.lastSeenTimestamp) {
        formatRecentActivity(contact.lastSeenTimestamp)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_card_${contact.id}"),
        shape = RoundedCornerShape(18.dp),
        onClick = onChatClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                StatusRingAvatar(
                    initial = contact.avatarInitial,
                    avatarColorHex = contact.avatarColorHex,
                    size = 46.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = bColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (contact.isBharatChatUser) {
                            VerifiedBadge()
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = contact.phone,
                        fontSize = 12.sp,
                        color = bColors.textSecondary
                    )

                    if (contact.statusMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = contact.statusMsg,
                            fontSize = 11.sp,
                            color = bColors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Favorite Star Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(34.dp).testTag("fav_btn_${contact.id}")
                ) {
                    Icon(
                        imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (contact.isFavorite) Color(0xFFFACC15) else bColors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom detail row: Recent activity pill & Quick Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Recent Activity Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (recentActivityText.contains("now")) BharatGreenLight.copy(alpha = 0.15f) else if (bColors.isDark) Color(0x331E293B) else Color(0x22CBD5E1),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        if (recentActivityText.contains("now")) BharatGreenLight.copy(alpha = 0.5f) else bColors.glassBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (recentActivityText.contains("now")) BharatGreenLight else bColors.textMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = recentActivityText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (recentActivityText.contains("now")) BharatGreenLight else bColors.textSecondary
                        )
                    }
                }

                // Quick Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Chat Action
                    IconButton(
                        onClick = onChatClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BharatSaffron.copy(alpha = 0.18f))
                            .testTag("contact_chat_action_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat",
                            tint = BharatSaffron,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Voice Call Action
                    IconButton(
                        onClick = { onCallClick(false) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BharatGreenLight.copy(alpha = 0.18f))
                            .testTag("contact_voice_call_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Voice Call",
                            tint = BharatGreenLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Video Call Action
                    IconButton(
                        onClick = { onCallClick(true) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(BharatElectricCyan.copy(alpha = 0.18f))
                            .testTag("contact_video_call_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = BharatElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // UPI Transfer Action
                    IconButton(
                        onClick = onUpiClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFA855F7).copy(alpha = 0.18f))
                            .testTag("contact_upi_pay_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = "UPI Transfer",
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onAddContact: (name: String, phone: String, upi: String, status: String) -> Unit
) {
    val bColors = LocalBharatColors.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var upi by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bColors.isDark) Color(0xF50B132B) else Color(0xFAF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BharatSaffron.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = BharatSaffron,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Add New Contact",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = bColors.textPrimary
                        )
                        Text(
                            text = "Encrypted Bharat Directory",
                            fontSize = 11.5.sp,
                            color = BharatElectricCyan
                        )
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = RoseError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Contact Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMsg = null },
                    label = { Text("Phone Number (+91) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_phone_input")
                )

                OutlinedTextField(
                    value = upi,
                    onValueChange = { upi = it },
                    label = { Text("UPI VPA ID (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_upi_input")
                )

                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Status / Bio (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_status_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = bColors.textSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMsg = "Please enter contact name"
                            } else if (phone.isBlank()) {
                                errorMsg = "Please enter phone number"
                            } else {
                                onAddContact(name, phone, upi, status)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron),
                        modifier = Modifier.testTag("add_contact_submit_button")
                    ) {
                        Text("Save Contact", fontWeight = FontWeight.Bold, color = BharatWhite)
                    }
                }
            }
        }
    }
}

fun formatRecentActivity(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val diffMins = diffMs / (1000 * 60)
    val diffHours = diffMs / (1000 * 60 * 60)
    val diffDays = diffMs / (1000 * 60 * 60 * 24)

    return when {
        diffMins <= 2 -> "Active just now 🟢"
        diffMins < 60 -> "${diffMins}m ago"
        diffHours < 24 -> "${diffHours}h ago"
        diffDays == 1L -> "Yesterday"
        diffDays < 7 -> "${diffDays}d ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

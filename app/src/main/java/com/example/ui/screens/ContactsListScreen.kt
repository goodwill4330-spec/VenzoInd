package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.components.ContactPresence
import com.example.ui.components.GlassCard
import com.example.ui.components.QuantumShieldBadge
import com.example.ui.components.StatusRingAvatar
import com.example.ui.components.TricolorGlowPill
import com.example.ui.components.VerifiedBadge
import com.example.ui.components.VenzoIndLinkedBadge
import com.example.ui.components.VenzoIndInviteBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BharatChatViewModel
import com.example.ui.viewmodel.ContactSortOrder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ContactQuickFilter {
    ALL,
    ONLINE,
    FAVORITES,
    RECENT,
    VERIFIED
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val onlineUsersMap by viewModel.onlineUsersMap.collectAsState()
    val usersLastSeenMap by viewModel.usersLastSeenMap.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var isSyncingPhonebook by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(ContactQuickFilter.ALL) }
    var selectedContactForOptions by remember { mutableStateOf<ContactEntity?>(null) }
    var touchedAlphabetLetter by remember { mutableStateOf<String?>(null) }

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

    // Computed real-time presence mapping
    fun getPresence(contact: ContactEntity): ContactPresence {
        // Unregistered device contacts from phonebook must NEVER show green online dot
        if (!contact.isBharatChatUser) {
            return ContactPresence.OFFLINE
        }

        val targetDevId = contact.id.removePrefix("contact_")
        val cleanPhone = contact.phone.filter { it.isDigit() }.takeLast(10)

        val inMemoryOnline: Boolean? = onlineUsersMap[targetDevId]
            ?: onlineUsersMap[contact.phone]
            ?: (if (cleanPhone.isNotBlank()) onlineUsersMap[cleanPhone] else null)

        val lastSeen: Long = usersLastSeenMap[targetDevId]
            ?: usersLastSeenMap[contact.phone]
            ?: (if (cleanPhone.isNotBlank()) usersLastSeenMap[cleanPhone] else null)
            ?: contact.lastSeenTimestamp

        if (lastSeen <= 0L && inMemoryOnline != true) {
            return ContactPresence.OFFLINE
        }

        val diffMs = System.currentTimeMillis() - lastSeen
        return when {
            inMemoryOnline == true && diffMs <= 120_000L -> ContactPresence.ONLINE
            diffMs <= 90_000L -> ContactPresence.ONLINE
            diffMs <= 600_000L -> ContactPresence.AWAY
            contact.statusMsg.contains("Busy", ignoreCase = true) ||
            contact.statusMsg.contains("Meeting", ignoreCase = true) -> ContactPresence.BUSY
            else -> ContactPresence.OFFLINE
        }
    }

    // Filter contacts based on active quick filter chip
    val displayedContacts = remember(contacts, activeFilter, onlineUsersMap, usersLastSeenMap) {
        when (activeFilter) {
            ContactQuickFilter.ALL -> contacts
            ContactQuickFilter.ONLINE -> contacts.filter { contact ->
                getPresence(contact) == ContactPresence.ONLINE
            }
            ContactQuickFilter.FAVORITES -> contacts.filter { it.isFavorite }
            ContactQuickFilter.RECENT -> contacts.sortedByDescending { contact ->
                val targetDevId = contact.id.removePrefix("contact_")
                val cleanPhone = contact.phone.filter { it.isDigit() }.takeLast(10)
                val lastSeen: Long = usersLastSeenMap[targetDevId]
                    ?: usersLastSeenMap[contact.phone]
                    ?: (if (cleanPhone.isNotBlank()) usersLastSeenMap[cleanPhone] else null)
                    ?: contact.lastSeenTimestamp
                lastSeen
            }
            ContactQuickFilter.VERIFIED -> contacts.filter { it.isBharatChatUser }
        }
    }

    // Group contacts alphabetically if A-Z sort order is active
    val groupedContacts = remember(displayedContacts, sortOrder, activeFilter, searchQuery) {
        if (searchQuery.isBlank() && (sortOrder == ContactSortOrder.NAME_ASC || sortOrder == ContactSortOrder.FAVORITES_FIRST)) {
            displayedContacts.groupBy { contact ->
                val firstChar = contact.name.trim().firstOrNull()?.uppercaseChar() ?: '#'
                if (firstChar in 'A'..'Z') firstChar.toString() else "#"
            }
        } else {
            null
        }
    }

    // Alphabet index list for side scroller rail
    val alphabetLetters = remember(groupedContacts) {
        groupedContacts?.keys?.sorted() ?: emptyList()
    }

    // Show "Scroll to Top" button when scrolled down
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    // Real-time online count
    val onlineCount = remember(allContacts, onlineUsersMap, usersLastSeenMap) {
        allContacts.count { getPresence(it) == ContactPresence.ONLINE }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = if (bColors.isDark) DarkSurface.copy(alpha = 0.95f) else LightSurface.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                border = BorderStroke(0.5.dp, bColors.glassBorder)
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
                                text = "${allContacts.size} sovereign contacts • $onlineCount online 🟢",
                                fontSize = 11.5.sp,
                                color = bColors.textSecondary
                            )
                        }
                    }

                    // Top Action Buttons (Sync, Backup/Restore, Add Contact)
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Scroll to top button
                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        containerColor = if (bColors.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = BharatElectricCyan,
                        shape = CircleShape,
                        modifier = Modifier.testTag("fab_scroll_to_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Scroll to top",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Add Contact FAB
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (bColors.isDark) DarkBackground else LightBackground)
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (bColors.isDark) Color(0x331E293B) else Color(0x1564748B),
                    border = BorderStroke(1.dp, bColors.glassBorder)
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
                                    text = "Search by name, status, phone, or UPI...",
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

                // Quick Filter Chips Row (All, Online, Favorites, Recent, Verified)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            label = "All (${allContacts.size})",
                            icon = Icons.Default.People,
                            isSelected = activeFilter == ContactQuickFilter.ALL,
                            onClick = { activeFilter = ContactQuickFilter.ALL },
                            testTag = "filter_chip_all"
                        )
                    }

                    item {
                        FilterChip(
                            label = "Online 🟢 ($onlineCount)",
                            icon = Icons.Default.RadioButtonChecked,
                            isSelected = activeFilter == ContactQuickFilter.ONLINE,
                            onClick = { activeFilter = ContactQuickFilter.ONLINE },
                            testTag = "filter_chip_online"
                        )
                    }

                    item {
                        val favCount = allContacts.count { it.isFavorite }
                        FilterChip(
                            label = "Favorites ⭐ ($favCount)",
                            icon = Icons.Default.Star,
                            isSelected = activeFilter == ContactQuickFilter.FAVORITES,
                            onClick = { activeFilter = ContactQuickFilter.FAVORITES },
                            testTag = "filter_chip_favorites"
                        )
                    }

                    item {
                        FilterChip(
                            label = "Recent ⏱️",
                            icon = Icons.Default.Schedule,
                            isSelected = activeFilter == ContactQuickFilter.RECENT,
                            onClick = { activeFilter = ContactQuickFilter.RECENT },
                            testTag = "filter_chip_recent"
                        )
                    }

                    item {
                        FilterChip(
                            label = "Verified 🇮🇳",
                            icon = Icons.Default.Shield,
                            isSelected = activeFilter == ContactQuickFilter.VERIFIED,
                            onClick = { activeFilter = ContactQuickFilter.VERIFIED },
                            testTag = "filter_chip_verified"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Contact List / Empty State
                if (displayedContacts.isEmpty()) {
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
                                text = if (searchQuery.isNotBlank()) "No contacts found matching \"$searchQuery\"" else "No contacts under this filter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = bColors.textPrimary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (searchQuery.isNotBlank()) "Try searching by a different name, status, or phone." else "Try choosing 'All' or tap the '+' button to add a new contact.",
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
                    // Smooth Scrolling Contact List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = if (alphabetLetters.isNotEmpty() && searchQuery.isBlank()) 28.dp else 0.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (groupedContacts != null && searchQuery.isBlank()) {
                            groupedContacts.forEach { (letter, contactList) ->
                                stickyHeader(key = "header_$letter") {
                                    Surface(
                                        color = if (bColors.isDark) Color(0xF00F172A) else Color(0xF0F8FAFC),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.5.dp, bColors.glassBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = letter,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = BharatSaffron
                                            )
                                            Text(
                                                text = "${contactList.size}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = bColors.textMuted
                                            )
                                        }
                                    }
                                }

                                items(
                                    items = contactList,
                                    key = { it.id }
                                ) { contact ->
                                    val presence = getPresence(contact)
                                    ContactItemCard(
                                        contact = contact,
                                        presence = presence,
                                        onChatClick = {
                                            viewModel.createChat(
                                                title = contact.name,
                                                subtitle = contact.phone,
                                                isGroup = false,
                                                isSecret = false,
                                                isBusiness = false
                                            )
                                        },
                                        onAvatarClick = {
                                            viewModel.openZoomableDp(
                                                title = contact.name,
                                                imageUri = contact.profilePicUri?.takeIf { it.isNotBlank() },
                                                initial = contact.avatarInitial,
                                                colorHex = contact.avatarColorHex,
                                                subtitle = contact.phone
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
                                        },
                                        onLongClick = {
                                            selectedContactForOptions = contact
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        } else {
                            items(
                                items = displayedContacts,
                                key = { it.id }
                            ) { contact ->
                                val presence = getPresence(contact)
                                ContactItemCard(
                                    contact = contact,
                                    presence = presence,
                                    onChatClick = {
                                        viewModel.createChat(
                                            title = contact.name,
                                            subtitle = contact.phone,
                                            isGroup = false,
                                            isSecret = false,
                                            isBusiness = false
                                        )
                                    },
                                    onAvatarClick = {
                                        viewModel.openZoomableDp(
                                            title = contact.name,
                                            imageUri = contact.profilePicUri?.takeIf { it.isNotBlank() },
                                            initial = contact.avatarInitial,
                                            colorHex = contact.avatarColorHex,
                                            subtitle = contact.phone
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
                                    },
                                    onLongClick = {
                                        selectedContactForOptions = contact
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }

            // Floating Alphabet Side Scroller Index Rail
            if (alphabetLetters.isNotEmpty() && searchQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 60.dp, bottom = 60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (bColors.isDark) Color(0x660F172A) else Color(0x44CBD5E1))
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxHeight(0.7f)
                    ) {
                        alphabetLetters.forEach { letter ->
                            val isSelected = touchedAlphabetLetter == letter
                            Text(
                                text = letter,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelected) BharatSaffron else bColors.textSecondary,
                                modifier = Modifier
                                    .clickable {
                                        touchedAlphabetLetter = letter
                                        coroutineScope.launch {
                                            // Compute item target index
                                            var targetIndex = 0
                                            groupedContacts?.forEach { (grpLetter, list) ->
                                                if (grpLetter == letter) {
                                                    listState.animateScrollToItem(targetIndex)
                                                    return@launch
                                                }
                                                targetIndex += list.size + 1 // +1 for header
                                            }
                                        }
                                    }
                                    .padding(vertical = 1.dp, horizontal = 3.dp)
                            )
                        }
                    }
                }
            }

            // Magnifier Bubble for Touched Letter
            if (touchedAlphabetLetter != null) {
                LaunchedEffect(touchedAlphabetLetter) {
                    kotlinx.coroutines.delay(1000)
                    touchedAlphabetLetter = null
                }

                Surface(
                    shape = CircleShape,
                    color = BharatSaffron,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = touchedAlphabetLetter ?: "",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = BharatWhite
                        )
                    }
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onAddContact = { name, phone, upi, status ->
                viewModel.addNewContact(name, phone, upi, status)
                showAddContactDialog = false
            }
        )
    }

    // Contact Options Bottom Sheet (Long Press)
    if (selectedContactForOptions != null) {
        val selContact = selectedContactForOptions!!
        ContactActionSheet(
            contact = selContact,
            onDismiss = { selectedContactForOptions = null },
            onChatClick = {
                selectedContactForOptions = null
                viewModel.createChat(
                    title = selContact.name,
                    subtitle = selContact.phone,
                    isGroup = false,
                    isSecret = false,
                    isBusiness = false
                )
            },
            onVoiceCallClick = {
                selectedContactForOptions = null
                viewModel.startCall(
                    contactName = selContact.name,
                    contactAvatar = selContact.avatarInitial,
                    isVideo = false,
                    contactPhone = selContact.phone
                )
            },
            onVideoCallClick = {
                selectedContactForOptions = null
                viewModel.startCall(
                    contactName = selContact.name,
                    contactAvatar = selContact.avatarInitial,
                    isVideo = true,
                    contactPhone = selContact.phone
                )
            },
            onToggleFavorite = {
                viewModel.toggleContactFavorite(selContact.id, selContact.isFavorite)
                selectedContactForOptions = null
            },
            onDeleteContact = {
                viewModel.deleteContact(selContact.id)
                selectedContactForOptions = null
                Toast.makeText(context, "Contact removed", Toast.LENGTH_SHORT).show()
            },
            onOpenProfile = {
                selectedContactForOptions = null
                viewModel.openContactProfile(selContact)
            }
        )
    }

    // Backup & Restore Dialog
    if (showBackupRestore) {
        com.example.ui.components.BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showBackupRestoreDialog.value = false }
        )
    }
}

@Composable
fun FilterChip(
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
        border = BorderStroke(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItemCard(
    contact: ContactEntity,
    presence: ContactPresence,
    onChatClick: () -> Unit,
    onAvatarClick: () -> Unit = {},
    onCallClick: (isVideo: Boolean) -> Unit,
    onUpiClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bColors = LocalBharatColors.current
    val isOnlineNow = presence == ContactPresence.ONLINE
    val recentActivityText = remember(contact.lastSeenTimestamp, isOnlineNow) {
        formatRecentActivity(contact.lastSeenTimestamp, isOnline = isOnlineNow)
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("contact_card_${contact.id}"),
        shape = RoundedCornerShape(18.dp),
        onClick = onChatClick,
        onLongClick = onLongClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Image / Initials + Real-Time Status Ring & Dot
                StatusRingAvatar(
                    initial = contact.avatarInitial,
                    avatarColorHex = contact.avatarColorHex,
                    imageUri = contact.profilePicUri?.takeIf { it.isNotBlank() },
                    presence = presence,
                    size = 48.dp,
                    onClick = onAvatarClick
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Name, Bio / Status, Phone
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            VenzoIndLinkedBadge()
                        } else {
                            VenzoIndInviteBadge()
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Status Message / Bio
                    if (contact.statusMsg.isNotBlank()) {
                        Text(
                            text = contact.statusMsg,
                            fontSize = 11.5.sp,
                            color = if (presence == ContactPresence.ONLINE) BharatGreenLight else bColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(1.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = contact.phone,
                            fontSize = 11.sp,
                            color = bColors.textMuted
                        )

                        if (contact.upiVpa.isNotBlank()) {
                            Text(
                                text = "• ${contact.upiVpa}",
                                fontSize = 10.5.sp,
                                color = Color(0xFFA855F7),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom detail row: Presence pill & Quick Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Presence / Last Seen Badge
                val isOnlineNow = presence == ContactPresence.ONLINE
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOnlineNow) BharatGreenLight.copy(alpha = 0.15f)
                    else if (bColors.isDark) Color(0x331E293B) else Color(0x22CBD5E1),
                    border = BorderStroke(
                        0.8.dp,
                        if (isOnlineNow) BharatGreenLight.copy(alpha = 0.5f) else bColors.glassBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when (presence) {
                                        ContactPresence.ONLINE -> OnlineGreen
                                        ContactPresence.AWAY -> Color(0xFFF59E0B)
                                        ContactPresence.BUSY -> Color(0xFFEF4444)
                                        ContactPresence.OFFLINE -> Color(0xFF94A3B8)
                                    }
                                )
                        )
                        Text(
                            text = recentActivityText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOnlineNow) BharatGreenLight else bColors.textSecondary
                        )
                    }
                }

                // Quick Action Buttons (Chat, Voice Call, Video Call, UPI)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Chat Action
                    IconButton(
                        onClick = onChatClick,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(BharatSaffron.copy(alpha = 0.18f))
                            .testTag("contact_chat_action_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat",
                            tint = BharatSaffron,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Voice Call Action
                    IconButton(
                        onClick = { onCallClick(false) },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(BharatGreenLight.copy(alpha = 0.18f))
                            .testTag("contact_voice_call_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Voice Call",
                            tint = BharatGreenLight,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Video Call Action
                    IconButton(
                        onClick = { onCallClick(true) },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(BharatElectricCyan.copy(alpha = 0.18f))
                            .testTag("contact_video_call_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = BharatElectricCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // UPI Transfer Action
                    IconButton(
                        onClick = onUpiClick,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFA855F7).copy(alpha = 0.18f))
                            .testTag("contact_upi_pay_${contact.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = "UPI Transfer",
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactActionSheet(
    contact: ContactEntity,
    onDismiss: () -> Unit,
    onChatClick: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteContact: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val bColors = LocalBharatColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bColors.isDark) Color(0xF00F172A) else Color(0xFAF8FAFC),
            border = BorderStroke(1.dp, bColors.glassBorder),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Contact Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusRingAvatar(
                        initial = contact.avatarInitial,
                        avatarColorHex = contact.avatarColorHex,
                        imageUri = contact.profilePicUri,
                        size = 52.dp
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = contact.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = bColors.textPrimary
                            )
                            if (contact.isBharatChatUser) {
                                VenzoIndLinkedBadge()
                            } else {
                                VenzoIndInviteBadge()
                            }
                        }
                        Text(
                            text = contact.phone,
                            fontSize = 12.sp,
                            color = bColors.textSecondary
                        )
                        if (contact.statusMsg.isNotBlank()) {
                            Text(
                                text = contact.statusMsg,
                                fontSize = 11.5.sp,
                                color = BharatElectricCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = bColors.glassBorder)

                // Action Items
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProfile() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = BharatElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("View Full Profile & Kyber Key", color = bColors.textPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChatClick() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = BharatSaffron,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Send Encrypted Message", color = bColors.textPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVoiceCallClick() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = BharatGreenLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Voice Call (Ultra-HD Opus)", color = bColors.textPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVideoCallClick() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = BharatElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Video Call (WebRTC 1080p)", color = bColors.textPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFavorite() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (contact.isFavorite) Icons.Default.StarBorder else Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFACC15),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        if (contact.isFavorite) "Remove from Favorites" else "Add to Favorites ⭐",
                        color = bColors.textPrimary,
                        fontSize = 14.sp
                    )
                }

                // Share Contact Details Row
                val ctx = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val vcardText = "VenzoInd Contact:\nName: ${contact.name}\nPhone: ${contact.phone}\nUPI: ${contact.upiId}\nStatus: ${contact.status}\nKyber Encrypted ID: ${contact.id}"
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Contact: ${contact.name}")
                                    putExtra(android.content.Intent.EXTRA_TEXT, vcardText)
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Contact via")
                                shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                ctx.startActivity(shareIntent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(ctx, "Could not share contact: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = BharatElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Share Contact (vCard / Details)", color = bColors.textPrimary, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeleteContact() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = RoseError,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Delete Contact", color = RoseError, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
            border = BorderStroke(1.dp, bColors.glassBorder),
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
                            text = "Encrypted Sovereign Directory",
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

fun formatRecentActivity(timestamp: Long, isOnline: Boolean = false): String {
    if (isOnline) return "Online now 🟢"
    if (timestamp <= 0L) return "Offline"
    val diffMs = System.currentTimeMillis() - timestamp
    val diffMins = diffMs / (1000 * 60)
    val diffHours = diffMs / (1000 * 60 * 60)
    val diffDays = diffMs / (1000 * 60 * 60 * 24)

    return when {
        diffMins <= 1 -> "Online just now"
        diffMins < 60 -> "Last seen ${diffMins}m ago"
        diffHours < 24 -> "Last seen ${diffHours}h ago"
        diffDays == 1L -> "Last seen yesterday"
        diffDays < 7 -> "Last seen ${diffDays}d ago"
        else -> "Last seen " + SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

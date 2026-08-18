package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MessageType
import com.example.ui.components.IncomingCallOverlay
import com.example.ui.components.GlassCard
import com.example.ui.components.QuantumShieldBadge
import com.example.ui.components.TricolorGlowPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatBottomSheet(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val bColors = LocalBharatColors.current
    var chatTitle by remember { mutableStateOf("") }
    var chatSubtitle by remember { mutableStateOf("") }
    var isSecret by remember { mutableStateOf(false) }
    var isGroup by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "New Conversation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = bColors.textPrimary
                )
                if (isSecret) {
                    QuantumShieldBadge(text = "Quantum Encrypted")
                }
            }

            // Chat Type Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isSecret && !isGroup) BharatSaffron else Color(0x2264748B),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isSecret = false; isGroup = false }
                        .testTag("type_direct_chat")
                ) {
                    Text(
                        text = "Direct Chat",
                        color = BharatWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSecret) SecretChatPink else Color(0x2264748B),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isSecret = true; isGroup = false }
                        .testTag("type_secret_chat")
                ) {
                    Text(
                        text = "🔒 Secret Chat",
                        color = BharatWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGroup) BharatElectricCyan else Color(0x2264748B),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isGroup = true; isSecret = false }
                        .testTag("type_group_chat")
                ) {
                    Text(
                        text = "👥 Group",
                        color = if (isGroup) DarkBackground else BharatWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Input Fields
            OutlinedTextField(
                value = chatTitle,
                onValueChange = { chatTitle = it },
                label = { Text(if (isGroup) "Group Name" else "Contact Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_chat_name_input")
            )

            OutlinedTextField(
                value = chatSubtitle,
                onValueChange = { chatSubtitle = it },
                label = { Text("Subtitle / Designation / Mobile") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_chat_subtitle_input")
            )

            // Saved Device Contacts with Search and Sorting
            val savedContacts by viewModel.filteredAndSortedContacts.collectAsState()
            val sortOrder by viewModel.contactSortOrder.collectAsState()
            val contactQuery by viewModel.contactSearchQuery.collectAsState()

            if (!isGroup) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Saved Phonebook Contacts",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = bColors.textSecondary
                        )

                        TextButton(
                            onClick = {
                                onDismiss()
                                viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.CONTACTS_LIST)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "View All (${viewModel.contacts.value.size}) →",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatElectricCyan
                            )
                        }
                    }

                    // Mini Search for contacts in sheet
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (bColors.isDark) Color(0x331E293B) else Color(0x1564748B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = BharatElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TextField(
                                value = contactQuery,
                                onValueChange = { viewModel.setContactSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        "Filter contacts...",
                                        fontSize = 12.sp,
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
                                    .testTag("sheet_contact_search")
                            )
                            if (contactQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.setContactSearchQuery("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = bColors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Mini Sorting Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (sortOrder == com.example.ui.viewmodel.ContactSortOrder.NAME_ASC) BharatSaffron else if (bColors.isDark) Color(0x221E293B) else Color(0x1564748B),
                            modifier = Modifier.clickable {
                                viewModel.setContactSortOrder(com.example.ui.viewmodel.ContactSortOrder.NAME_ASC)
                            }
                        ) {
                            Text(
                                text = "🔤 Name (A-Z)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sortOrder == com.example.ui.viewmodel.ContactSortOrder.NAME_ASC) BharatWhite else bColors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (sortOrder == com.example.ui.viewmodel.ContactSortOrder.RECENT_ACTIVITY) BharatSaffron else if (bColors.isDark) Color(0x221E293B) else Color(0x1564748B),
                            modifier = Modifier.clickable {
                                viewModel.setContactSortOrder(com.example.ui.viewmodel.ContactSortOrder.RECENT_ACTIVITY)
                            }
                        ) {
                            Text(
                                text = "⏱️ Recent Activity",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sortOrder == com.example.ui.viewmodel.ContactSortOrder.RECENT_ACTIVITY) BharatWhite else bColors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (sortOrder == com.example.ui.viewmodel.ContactSortOrder.FAVORITES_FIRST) BharatSaffron else if (bColors.isDark) Color(0x221E293B) else Color(0x1564748B),
                            modifier = Modifier.clickable {
                                viewModel.setContactSortOrder(com.example.ui.viewmodel.ContactSortOrder.FAVORITES_FIRST)
                            }
                        ) {
                            Text(
                                text = "⭐ Favorites",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sortOrder == com.example.ui.viewmodel.ContactSortOrder.FAVORITES_FIRST) BharatWhite else bColors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (savedContacts.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(savedContacts) { contact ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (bColors.isDark) Color(0x331E293B) else Color(0x1564748B),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder),
                                    modifier = Modifier.clickable {
                                        chatTitle = contact.name
                                        chatSubtitle = contact.phone
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(BharatSaffron),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = contact.avatarInitial,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BharatWhite
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = contact.name,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = bColors.textPrimary
                                            )
                                            Text(
                                                text = contact.phone,
                                                fontSize = 9.5.sp,
                                                color = bColors.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (chatTitle.isNotBlank()) {
                        viewModel.createChat(
                            title = chatTitle,
                            subtitle = chatSubtitle.ifBlank { if (isSecret) "Quantum Secret Chat" else "Active on Bharat Chat" },
                            isGroup = isGroup,
                            isSecret = isSecret,
                            isBusiness = false
                        )
                    }
                },
                enabled = chatTitle.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_new_chat_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSecret) SecretChatPink else BharatSaffron
                )
            ) {
                Text(
                    text = if (isSecret) "Start Quantum Secret Chat" else "Start Chat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiPaymentBottomSheet(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val bColors = LocalBharatColors.current
    val activeChat by viewModel.activeChat.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val defaultRecipientName = activeChat?.title ?: "Ananya Sen"
    val defaultUpiId = if (activeChat != null && !activeChat!!.isAiAssistant) {
        val sanitized = activeChat!!.title.lowercase().replace(" ", "")
        "$sanitized@bharatupi"
    } else {
        "ananya@bharatupi"
    }

    var recipientName by remember { mutableStateOf(defaultRecipientName) }
    var upiId by remember { mutableStateOf(defaultUpiId) }
    var amountStr by remember { mutableStateOf("500") }
    var noteStr by remember { mutableStateOf("Dinner split 🍽️") }
    var isProcessing by remember { mutableStateOf(false) }

    // Preset suggested VPAs for easy switching
    val suggestedVpas = listOf(
        "ananya@bharatupi" to "Ananya Sen",
        "rahul@bharatupi" to "Rahul Sharma",
        "vikram@bharatupi" to "Vikram Malhotra",
        "store@bharatupi" to "Bharat Mart"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        // High-fidelity Glassmorphic Container with Tricolor cyber gradient glow border
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
            color = if (bColors.isDark) Color(0xF20B132B) else Color(0xFAF8FAFC),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        BharatSaffron.copy(alpha = 0.7f),
                        BharatElectricCyan.copy(alpha = 0.6f),
                        BharatGreenLight.copy(alpha = 0.7f)
                    )
                )
            ),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Glass Drag Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(44.dp)
                        .height(4.5.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(BharatSaffron, BharatElectricCyan, BharatGreenLight)
                            )
                        )
                )

                // Glassmorphic Header with NPCI & Quantum Security Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(BharatSaffron, BharatGreenLight)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = "UPI",
                                tint = BharatWhite,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Instant UPI Transfer",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp,
                                    color = bColors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TricolorGlowPill(text = "NPCI 2.0")
                            }
                            Text(
                                text = "Zero-fee Instant Quantum E2EE Settlement ⚡",
                                fontSize = 11.sp,
                                color = BharatGreenLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (bColors.isDark) Color(0x3364748B) else Color(0x22000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = bColors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Balance preview glass tile
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (bColors.isDark) Color(0x331E293B) else Color(0x22CBD5E1),
                    border = androidx.compose.foundation.BorderStroke(1.dp, bColors.glassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = BharatElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bharat Wallet Balance",
                                fontSize = 12.sp,
                                color = bColors.textSecondary
                            )
                        }
                        Text(
                            text = "₹${"%,.2f".format(userProfile.walletBalance)}",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BharatGreenLight
                        )
                    }
                }

                // Recipient Name & Recipient VPA Input Fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        label = { Text("Recipient Name", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = BharatSaffron,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upi_recipient_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BharatSaffron,
                            unfocusedBorderColor = bColors.glassBorder,
                            focusedLabelColor = BharatSaffron,
                            focusedTextColor = bColors.textPrimary,
                            unfocusedTextColor = bColors.textPrimary,
                            focusedContainerColor = if (bColors.isDark) Color(0x221E293B) else Color(0x11000000),
                            unfocusedContainerColor = if (bColors.isDark) Color(0x151E293B) else Color(0x08000000)
                        )
                    )

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it.trim().lowercase() },
                        label = { Text("Recipient VPA", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AlternateEmail,
                                contentDescription = null,
                                tint = BharatElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("upi_vpa_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BharatElectricCyan,
                            unfocusedBorderColor = bColors.glassBorder,
                            focusedLabelColor = BharatElectricCyan,
                            focusedTextColor = bColors.textPrimary,
                            unfocusedTextColor = bColors.textPrimary,
                            focusedContainerColor = if (bColors.isDark) Color(0x221E293B) else Color(0x11000000),
                            unfocusedContainerColor = if (bColors.isDark) Color(0x151E293B) else Color(0x08000000)
                        )
                    )
                }

                // Quick Frequent VPAs Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick:",
                        fontSize = 11.sp,
                        color = bColors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    suggestedVpas.forEach { (vpa, name) ->
                        val isSelected = upiId == vpa
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BharatElectricCyan.copy(alpha = 0.2f) else if (bColors.isDark) Color(0x1F334155) else Color(0x1564748B),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BharatElectricCyan else Color.Transparent
                            ),
                            modifier = Modifier.clickable {
                                upiId = vpa
                                recipientName = name
                            }
                        ) {
                            Text(
                                text = name.split(" ").first(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BharatElectricCyan else bColors.textSecondary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Amount Field with Big Rupee Indicator & High Contrast Glass Finish
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Transfer Amount (INR)", fontSize = 12.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp, end = 4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BharatGreenLight.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BharatGreenLight
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upi_amount_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BharatGreenLight,
                        unfocusedBorderColor = bColors.glassBorder,
                        focusedLabelColor = BharatGreenLight,
                        focusedTextColor = bColors.textPrimary,
                        unfocusedTextColor = bColors.textPrimary,
                        focusedContainerColor = if (bColors.isDark) Color(0x2210B981) else Color(0x0C10B981),
                        unfocusedContainerColor = if (bColors.isDark) Color(0x1210B981) else Color(0x0510B981)
                    )
                )

                // Quick Amount Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("100", "250", "500", "1000", "2000").forEach { quickAmount ->
                        val isSelected = amountStr == quickAmount
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BharatGreenLight else if (bColors.isDark) Color(0x2210B981) else Color(0x1510B981),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) BharatGreenLight else Color(0x4410B981)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { amountStr = quickAmount }
                        ) {
                            Text(
                                text = "₹$quickAmount",
                                color = if (isSelected) BharatWhite else BharatGreenLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp)
                            )
                        }
                    }
                }

                // Payment Note Field
                OutlinedTextField(
                    value = noteStr,
                    onValueChange = { noteStr = it },
                    label = { Text("Transfer Note / Purpose", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = BharatSaffron,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BharatSaffron,
                        unfocusedBorderColor = bColors.glassBorder,
                        focusedLabelColor = BharatSaffron,
                        focusedTextColor = bColors.textPrimary,
                        unfocusedTextColor = bColors.textPrimary,
                        focusedContainerColor = if (bColors.isDark) Color(0x221E293B) else Color(0x11000000),
                        unfocusedContainerColor = if (bColors.isDark) Color(0x151E293B) else Color(0x08000000)
                    )
                )

                // Transfer Button
                val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
                val canTransfer = parsedAmount > 0 && parsedAmount <= userProfile.walletBalance && !isProcessing && upiId.isNotBlank()

                Button(
                    onClick = {
                        if (canTransfer) {
                            isProcessing = true
                            viewModel.sendUpiMoney(recipientName, upiId, parsedAmount, noteStr)
                            onDismiss()
                        }
                    },
                    enabled = canTransfer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_upi_transfer_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BharatGreenLight,
                        disabledContainerColor = Color(0x4410B981)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = BharatWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (parsedAmount > userProfile.walletBalance) "Insufficient Wallet Balance" else "Pay ₹${if (amountStr.isBlank()) "0" else amountStr} via Quantum UPI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BharatWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = DarkSurfaceElevated
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan Bharat QR",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = BharatWhite
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulated Scanner Viewfinder
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F172A))
                        .border(
                            2.dp,
                            Brush.sweepGradient(listOf(BharatSaffron, BharatWhite, BharatGreen, BharatSaffron)),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = BharatElectricCyan,
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Point camera at any UPI QR or Bharat Chat Web login screen",
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun AiSummarizerDialog(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val summary by viewModel.aiSummaryContent.collectAsState()
    val isGenerating by viewModel.isGeneratingSummary.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = DarkSurfaceElevated
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BharatSaffronLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Chat Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BharatWhite
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isGenerating) {
                    CircularProgressIndicator(color = BharatSaffron)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analyzing sovereign chat with Gemini...",
                        fontSize = 12.sp,
                        color = BharatElectricCyan
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3364748B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = summary.ifBlank { "No recent messages to summarize." },
                            fontSize = 13.sp,
                            color = TextPrimaryDark,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.ttsManager.speakText(summary)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BharatNavyLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.VolumeUp, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Read Aloud", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiTranslatorDialog(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "Hindi" to "हिंदी",
        "Tamil" to "தமிழ்",
        "Telugu" to "తెలుగు",
        "Marathi" to "मराठी",
        "Bengali" to "বাংলা",
        "Gujarati" to "ગુજરાતી",
        "Kannada" to "ಕನ್ನಡ",
        "English" to "English"
    )

    val targetMsgId by viewModel.targetTranslateMessageId.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val targetMessage = messages.find { it.id == targetMsgId } ?: messages.lastOrNull()

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = DarkSurfaceElevated
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = BharatElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Indian Translation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BharatWhite
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = TextSecondaryDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select Indian language to translate instantly with AI:",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                languages.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (engName, nativeName) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3364748B)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (targetMessage != null) {
                                            viewModel.translateMessage(targetMessage.id, targetMessage.text, engName)
                                        }
                                    }
                                    .testTag("translate_lang_$engName")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = nativeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = BharatSaffronLight
                                    )
                                    Text(
                                        text = engName,
                                        fontSize = 10.5.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentOptionsBottomSheet(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val bColors = LocalBharatColors.current

    val attachmentItems = listOf(
        Triple("10GB Cloud File", Icons.Default.InsertDriveFile, BharatElectricCyan),
        Triple("4K Camera Photo", Icons.Default.PhotoCamera, BharatSaffron),
        Triple("UPI Instant Pay", Icons.Default.CurrencyRupee, BharatGreenLight),
        Triple("Interactive Poll", Icons.Default.Poll, Color(0xFFA855F7)),
        Triple("Live Location", Icons.Default.LocationOn, Color(0xFFF59E0B)),
        Triple("Audio Recording", Icons.Default.Mic, Color(0xFFEC4899))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Share & Actions",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = bColors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            attachmentItems.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    row.forEach { (label, icon, color) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(90.dp)
                                .clickable {
                                    when (label) {
                                        "10GB Cloud File" -> viewModel.sendAttachment(MessageType.FILE, "Sovereign_Project_Doc.pdf", "9.4 GB • Cloud Transfer")
                                        "4K Camera Photo" -> viewModel.sendAttachment(MessageType.IMAGE, "photo_4k_hdr.jpg", "14.2 MB")
                                        "UPI Instant Pay" -> {
                                            viewModel.showAttachmentOptions.value = false
                                            viewModel.showUpiPaymentSheet.value = true
                                        }
                                        "Interactive Poll" -> {
                                            viewModel.showAttachmentOptions.value = false
                                            viewModel.showPollCreatorDialog.value = true
                                        }
                                        else -> viewModel.sendAttachment(MessageType.FILE, "sovereign_location.gpx", "1.2 MB")
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.2f))
                                    .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = bColors.textPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun DualPhoneTestingDialog(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val bColors = LocalBharatColors.current
    var pairCodeInput by remember { mutableStateOf(syncStatus.pairCode) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BharatSaffron.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = BharatSaffron,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "2-Phone Testing Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = bColors.textPrimary
                            )
                            Text(
                                text = "Dual-Device Realtime Sync",
                                fontSize = 11.5.sp,
                                color = BharatElectricCyan
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = bColors.textSecondary)
                    }
                }

                // Active Phone Role Selector
                Text(
                    text = "Active Device Identity:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = bColors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPhone1 = syncStatus.activeRole == com.example.data.sync.DeviceRole.PHONE_1
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPhone1) BharatSaffron else Color(0x2264748B),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.switchDeviceRole(com.example.data.sync.DeviceRole.PHONE_1)
                            }
                            .testTag("select_phone_1")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📱 Phone 1",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BharatWhite
                            )
                            Text(
                                text = "Vikram Aditya",
                                fontSize = 10.5.sp,
                                color = BharatWhite.copy(alpha = 0.9f)
                            )
                        }
                    }

                    val isPhone2 = syncStatus.activeRole == com.example.data.sync.DeviceRole.PHONE_2
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isPhone2) BharatGreenLight else Color(0x2264748B),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.switchDeviceRole(com.example.data.sync.DeviceRole.PHONE_2)
                            }
                            .testTag("select_phone_2")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📱 Phone 2",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BharatWhite
                            )
                            Text(
                                text = "Ananya Sen",
                                fontSize = 10.5.sp,
                                color = BharatWhite.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // 4-Digit Sync Pairing Code
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3364748B))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pairing Sync Code:",
                                fontSize = 11.5.sp,
                                color = TextSecondaryDark
                            )
                            TricolorGlowPill(text = "Live Linked")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = pairCodeInput,
                                onValueChange = {
                                    pairCodeInput = it
                                    viewModel.updatePairCode(it)
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = BharatWhite,
                                    unfocusedTextColor = BharatWhite
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.updatePairCode(pairCodeInput) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BharatElectricCyan)
                            ) {
                                Text("Set", color = DarkBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Testing Triggers
                Text(
                    text = "Quick Cross-Phone Actions:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = bColors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.triggerIncomingTestCall(isVideo = false)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight),
                        modifier = Modifier.weight(1f).testTag("trigger_test_call_button")
                    ) {
                        Icon(Icons.Default.PhoneCallback, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call Me", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.triggerIncomingTestUpi(500.0)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron),
                        modifier = Modifier.weight(1f).testTag("trigger_test_upi_button")
                    ) {
                        Icon(Icons.Default.CurrencyRupee, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send ₹500", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Auto Reply Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1564748B))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Reply from Phone 2",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = bColors.textPrimary
                        )
                        Text(
                            text = "Simulates real-time double ticks & answers",
                            fontSize = 10.5.sp,
                            color = bColors.textSecondary
                        )
                    }
                    Switch(
                        checked = syncStatus.autoReplyEnabled,
                        onCheckedChange = { viewModel.toggleSyncAutoReply(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun IncomingCallDialog(
    callEvent: com.example.data.sync.IncomingCallEvent,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onQuickMessage: ((String) -> Unit)? = null
) {
    IncomingCallOverlay(
        callEvent = callEvent,
        onAccept = onAccept,
        onDecline = onDecline,
        onQuickMessage = onQuickMessage
    )
}

@Composable
fun IncomingUpiDialog(
    upiEvent: com.example.data.sync.IncomingUpiEvent,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = DarkSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(BharatGreenLight.copy(alpha = 0.2f))
                        .border(1.5.dp, BharatGreenLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyRupee,
                        contentDescription = null,
                        tint = BharatGreenLight,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Money Received! ⚡",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BharatWhite
                )

                Text(
                    text = "+₹${upiEvent.amount.toInt()}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BharatGreenLight
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "From: ${upiEvent.senderName} (${upiEvent.upiVpa})",
                            fontSize = 12.sp,
                            color = BharatElectricCyan,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Note: ${upiEvent.note}",
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DisappearingMessagesDialog(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val bColors = LocalBharatColors.current
    val timerOptions = listOf("Off", "24 Hours", "7 Days", "90 Days")
    var selectedOption by remember { mutableStateOf("24 Hours") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = BharatSaffron,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Disappearing Messages",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = bColors.textPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = bColors.textSecondary)
                    }
                }

                Text(
                    text = "For added quantum privacy, new messages will vanish from all devices after the set time.",
                    fontSize = 12.5.sp,
                    color = bColors.textSecondary,
                    lineHeight = 18.sp
                )

                timerOptions.forEach { option ->
                    val isSelected = selectedOption == option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) BharatSaffron.copy(alpha = 0.2f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) BharatSaffron else bColors.glassBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BharatSaffron else bColors.textPrimary,
                                fontSize = 14.sp
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = BharatSaffron,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Setting", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWallpaperBottomSheet(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val bColors = LocalBharatColors.current
    val wallpapers = listOf(
        "Default Mesh" to Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))),
        "Saffron Glow" to Brush.linearGradient(listOf(Color(0xFF2C1304), Color(0xFF1E293B))),
        "Vedic Indigo" to Brush.linearGradient(listOf(Color(0xFF0B192C), Color(0xFF1E3E62))),
        "Quantum Emerald" to Brush.linearGradient(listOf(Color(0xFF06281E), Color(0xFF0F172A)))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Chat Themes & Wallpaper",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = bColors.textPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                wallpapers.forEach { (name, gradient) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onDismiss() }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(gradient)
                                .border(1.dp, bColors.glassBorder, RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            color = bColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PollCreatorDialog(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val bColors = LocalBharatColors.current
    var question by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Create Interactive Poll",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = bColors.textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = bColors.textSecondary)
                    }
                }

                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Ask a question...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = opt1,
                    onValueChange = { opt1 = it },
                    label = { Text("Option 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = opt2,
                    onValueChange = { opt2 = it },
                    label = { Text("Option 2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (question.isNotBlank()) {
                            viewModel.sendMessage("📊 Poll: $question\n1. ${opt1.ifBlank { "Yes" }}\n2. ${opt2.ifBlank { "No" }}")
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Poll to Chat", fontWeight = FontWeight.Bold, color = BharatWhite)
                }
            }
        }
    }
}

@Composable
fun SecretChatInfoDialog(
    onDismiss: () -> Unit
) {
    val bColors = LocalBharatColors.current

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BharatElectricCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = BharatElectricCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Quantum Secret Chat",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = bColors.textPrimary
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "🔒 End-to-End Quantum Kyber-1024 encryption",
                        "🚫 No server backups or cloud caching",
                        "⏳ Ephemeral self-destruct timers supported",
                        "🛡️ Screenshot detection notification enabled"
                    ).forEach { feature ->
                        Text(
                            text = feature,
                            fontSize = 12.5.sp,
                            color = bColors.textSecondary
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BharatElectricCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            }
        }
    }
}

@Composable
fun BiometricAuthDialog(
    purpose: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val bColors = LocalBharatColors.current
    var isAuthenticating by remember { mutableStateOf(false) }
    var authMode by remember { mutableStateOf("FINGERPRINT") } // "FINGERPRINT" or "FACE"
    var pinFallbackInput by remember { mutableStateOf("") }
    var showPinFallback by remember { mutableStateOf(false) }
    var authErrorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = if (bColors.isDark) Color(0xF50B132B) else Color(0xFAF8FAFC),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(listOf(BharatSaffron, BharatElectricCyan, BharatGreenLight))
            ),
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon with pulsing scanner animation
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (authMode == "FACE") BharatElectricCyan.copy(alpha = 0.25f) else BharatGreenLight.copy(alpha = 0.25f),
                                    BharatSaffron.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(
                            2.dp,
                            if (authMode == "FACE") BharatElectricCyan else BharatGreenLight,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (authMode == "FACE") Icons.Default.Face else Icons.Default.Fingerprint,
                        contentDescription = "Biometric Sensor",
                        tint = if (authMode == "FACE") BharatElectricCyan else BharatGreenLight,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (showPinFallback) "Enter Security PIN" else "Biometric Security",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = bColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = purpose,
                        fontSize = 13.sp,
                        color = BharatElectricCyan,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "VenzoInd Quantum Secure Biometric Vault 🛡️",
                        fontSize = 11.sp,
                        color = bColors.textMuted
                    )
                }

                if (authErrorMsg != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RoseError.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseError.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = authErrorMsg ?: "",
                            color = RoseError,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (!showPinFallback) {
                    // Biometric sensor touch target simulation
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                isAuthenticating = true
                                authErrorMsg = null
                                // Simulate prompt validation success
                                onSuccess()
                            }
                            .testTag("biometric_sensor_tap_target"),
                        shape = RoundedCornerShape(18.dp),
                        color = if (bColors.isDark) Color(0x331E293B) else Color(0x1564748B),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (authMode == "FACE") BharatElectricCyan.copy(alpha = 0.6f) else BharatGreenLight.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (authMode == "FACE") "👤 Looking for Face..." else "👆 Touch the Fingerprint Sensor",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = bColors.textPrimary
                            )
                            Text(
                                text = "Tap here to simulate successful biometric sensor scan",
                                fontSize = 11.5.sp,
                                color = bColors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Mode Switcher (Fingerprint vs Face Unlock)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { authMode = if (authMode == "FINGERPRINT") "FACE" else "FINGERPRINT" },
                            colors = ButtonDefaults.textButtonColors(contentColor = BharatSaffron)
                        ) {
                            Icon(
                                imageVector = if (authMode == "FINGERPRINT") Icons.Default.Face else Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (authMode == "FINGERPRINT") "Switch to Face Unlock" else "Switch to Fingerprint",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // PIN Fallback field
                    OutlinedTextField(
                        value = pinFallbackInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinFallbackInput = it },
                        label = { Text("6-Digit Security UPI PIN") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("biometric_pin_fallback_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BharatElectricCyan,
                            unfocusedBorderColor = bColors.glassBorder,
                            focusedTextColor = bColors.textPrimary,
                            unfocusedTextColor = bColors.textPrimary
                        )
                    )

                    Button(
                        onClick = {
                            if (pinFallbackInput.length >= 4) {
                                onSuccess()
                            } else {
                                authErrorMsg = "Please enter valid 4-6 digit PIN"
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify PIN", fontWeight = FontWeight.Bold, color = DarkBackground)
                    }
                }

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showPinFallback = !showPinFallback },
                        colors = ButtonDefaults.textButtonColors(contentColor = bColors.textSecondary)
                    ) {
                        Text(
                            text = if (showPinFallback) "Use Biometrics" else "Use Device PIN",
                            fontSize = 12.sp
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = RoseError)
                    ) {
                        Text("Cancel", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TricolorGlowPill
import com.example.ui.dialogs.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import com.example.ui.viewmodel.NavigationTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeScreen(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val bColors = LocalBharatColors.current

    // Dialog state collectors
    val showNewChat by viewModel.showNewChatSheet.collectAsState()
    val showUpiPay by viewModel.showUpiPaymentSheet.collectAsState()
    val showQrScanner by viewModel.showQrScannerSheet.collectAsState()
    val showAiSummary by viewModel.showAiSummarizerDialog.collectAsState()
    val showAiTranslate by viewModel.showAiTranslatorDialog.collectAsState()
    val showAttachments by viewModel.showAttachmentOptions.collectAsState()
    val showBiometricDialog by viewModel.showBiometricAuthDialog.collectAsState()
    val biometricPurpose by viewModel.biometricAuthPurpose.collectAsState()
    val incomingCall by viewModel.incomingCallEvent.collectAsState()
    val incomingUpi by viewModel.incomingUpiEvent.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val showBackupRestore by viewModel.showBackupRestoreDialog.collectAsState()
    val showContactProfile by viewModel.showContactProfileDialog.collectAsState()
    val activeContactProfile by viewModel.activeContactProfile.collectAsState()
    val showZoomableDp by viewModel.showZoomableDpDialog.collectAsState()
    val activeZoomableDp by viewModel.activeZoomableDp.collectAsState()
    var showWhatsAppMenu by remember { mutableStateOf(false) }

    Scaffold(
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Logo & App Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.components.VenzoraLogoEmblem(size = 36.dp)

                        Column {
                            Text(
                                text = "VENZOIND",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 1.2.sp,
                                color = bColors.textPrimary
                            )
                            Text(
                                text = "CONNECT BEYOND LIMITS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = Color(0xFF22C55E)
                            )
                        }
                    }

                    // Top Action Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.CONTACTS_LIST) },
                            modifier = Modifier.testTag("top_contacts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = "Contacts",
                                tint = BharatSaffronLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.showQrScannerSheet.value = true },
                            modifier = Modifier.testTag("top_qr_scanner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = BharatElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.showUpiPaymentSheet.value = true },
                            modifier = Modifier.testTag("top_upi_pay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = "UPI Transfer",
                                tint = BharatGreenLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("top_theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDark) BharatSaffronLight else BharatNavy,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // WhatsApp 3-dots overflow menu
                        Box {
                            IconButton(
                                onClick = { showWhatsAppMenu = true },
                                modifier = Modifier.testTag("top_overflow_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = bColors.textPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showWhatsAppMenu,
                                onDismissRequest = { showWhatsAppMenu = false },
                                modifier = Modifier.background(if (bColors.isDark) DarkSurfaceElevated else LightSurface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("New group", color = bColors.textPrimary, fontSize = 14.5.sp) },
                                    onClick = {
                                        showWhatsAppMenu = false
                                        viewModel.showNewGroupDialog.value = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = BharatGreenLight, modifier = Modifier.size(20.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("New broadcast", color = bColors.textPrimary, fontSize = 14.5.sp) },
                                    onClick = {
                                        showWhatsAppMenu = false
                                        viewModel.showBroadcastDialog.value = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = BharatSaffron, modifier = Modifier.size(20.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Linked devices", color = bColors.textPrimary, fontSize = 14.5.sp) },
                                    onClick = {
                                        showWhatsAppMenu = false
                                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.SETTINGS)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null, tint = BharatElectricCyan, modifier = Modifier.size(20.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Starred messages", color = bColors.textPrimary, fontSize = 14.5.sp) },
                                    onClick = {
                                        showWhatsAppMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Payments", color = bColors.textPrimary, fontSize = 14.5.sp) },
                                    onClick = {
                                        showWhatsAppMenu = false
                                        viewModel.showUpiPaymentSheet.value = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = BharatGreenLight, modifier = Modifier.size(20.dp)) }
                                )
                                HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.5f))
                                DropdownMenuItem(
                                    text = { Text("Settings", color = bColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp) },
                                    onClick = {
                                        showWhatsAppMenu = false
                                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.SETTINGS)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = BharatGreenLight, modifier = Modifier.size(20.dp)) },
                                    modifier = Modifier.testTag("menu_settings_item")
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = if (bColors.isDark) DarkSurface.copy(alpha = 0.96f) else LightSurface.copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, bColors.glassBorder)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    val tabs = listOf(
                        Triple(NavigationTab.CHATS, "Chats", Icons.Default.ChatBubbleOutline to Icons.Filled.Chat),
                        Triple(NavigationTab.CALLS, "Calls", Icons.Default.Phone to Icons.Filled.Phone),
                        Triple(NavigationTab.UPDATES, "Updates", Icons.Default.DynamicFeed to Icons.Filled.DynamicFeed),
                        Triple(NavigationTab.AI_ASSISTANT, "Bharat AI", Icons.Outlined.AutoAwesome to Icons.Filled.AutoAwesome),
                        Triple(NavigationTab.PROFILE, "Profile", Icons.Outlined.Person to Icons.Filled.Person)
                    )

                    tabs.forEach { (tab, label, icons) ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setTab(tab) },
                            icon = {
                                if (tab == NavigationTab.AI_ASSISTANT && isSelected) {
                                    Icon(
                                        imageVector = icons.second,
                                        contentDescription = label,
                                        tint = BharatSaffron,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) icons.second else icons.first,
                                        contentDescription = label,
                                        tint = if (isSelected) BharatSaffron else bColors.textSecondary
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) BharatSaffron else bColors.textSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = if (tab == NavigationTab.AI_ASSISTANT) BharatSaffron.copy(alpha = 0.18f) else BharatSaffron.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == NavigationTab.CHATS || selectedTab == NavigationTab.UPDATES) {
                FloatingActionButton(
                    onClick = { viewModel.showNewChatSheet.value = true },
                    containerColor = BharatSaffron,
                    contentColor = BharatWhite,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("fab_new_chat")
                ) {
                    Icon(
                        imageVector = if (selectedTab == NavigationTab.UPDATES) Icons.Default.CameraAlt else Icons.Default.Add,
                        contentDescription = "New Action",
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (selectedTab == NavigationTab.CALLS) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.CONTACTS_LIST) },
                    containerColor = BharatGreenLight,
                    contentColor = BharatWhite,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("fab_new_call")
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Start New Call",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.CHATS -> ChatsListTab(viewModel = viewModel)
                NavigationTab.CALLS -> CallsTab(viewModel = viewModel)
                NavigationTab.UPDATES -> UpdatesTab(viewModel = viewModel)
                NavigationTab.AI_ASSISTANT -> BharatAiTab(viewModel = viewModel)
                NavigationTab.PROFILE -> ProfileWalletTab(viewModel = viewModel)
            }
        }
    }

    // Modal Bottom Sheets and Dialogs
    if (showNewChat) {
        NewChatBottomSheet(viewModel = viewModel, onDismiss = { viewModel.showNewChatSheet.value = false })
    }

    if (showUpiPay) {
        UpiPaymentBottomSheet(viewModel = viewModel, onDismiss = { viewModel.showUpiPaymentSheet.value = false })
    }

    if (showQrScanner) {
        QrScannerDialog(onDismiss = { viewModel.showQrScannerSheet.value = false })
    }

    if (showAiSummary) {
        AiSummarizerDialog(viewModel = viewModel, onDismiss = { viewModel.showAiSummarizerDialog.value = false })
    }

    if (showAiTranslate) {
        AiTranslatorDialog(viewModel = viewModel, onDismiss = { viewModel.showAiTranslatorDialog.value = false })
    }

    if (showAttachments) {
        AttachmentOptionsBottomSheet(viewModel = viewModel, onDismiss = { viewModel.showAttachmentOptions.value = false })
    }

    if (showBiometricDialog) {
        BiometricAuthDialog(
            purpose = biometricPurpose,
            onSuccess = { viewModel.completeBiometricAuth(true) },
            onDismiss = { viewModel.completeBiometricAuth(false) }
        )
    }

    if (showBackupRestore) {
        com.example.ui.components.BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showBackupRestoreDialog.value = false }
        )
    }

    if (showContactProfile && activeContactProfile != null) {
        ContactProfileDialog(
            viewModel = viewModel,
            contact = activeContactProfile!!,
            onDismiss = { viewModel.showContactProfileDialog.value = false }
        )
    }

    if (showZoomableDp && activeZoomableDp != null) {
        com.example.ui.components.ZoomableProfilePicDialog(
            title = activeZoomableDp!!.title,
            subtitle = activeZoomableDp!!.subtitle,
            avatarInitial = activeZoomableDp!!.initial,
            avatarColorHex = activeZoomableDp!!.colorHex,
            imageUri = activeZoomableDp!!.imageUri,
            onDismiss = { viewModel.closeZoomableDp() }
        )
    }

    incomingCall?.let { callEvent ->
        IncomingCallDialog(
            callEvent = callEvent,
            onAccept = { viewModel.acceptIncomingCall() },
            onDecline = { viewModel.declineIncomingCall() },
            onQuickMessage = { message -> viewModel.declineIncomingCallWithMessage(message) }
        )
    }

    incomingUpi?.let { upiEvent ->
        IncomingUpiDialog(
            upiEvent = upiEvent,
            onDismiss = { viewModel.dismissIncomingUpi() }
        )
    }
}

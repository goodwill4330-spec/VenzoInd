package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.TransactionEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.QuantumShieldBadge
import com.example.ui.components.TricolorGlowPill
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel

@Composable
fun ProfileWalletTab(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val isRadarScanning by viewModel.isRadarScanning.collectAsState()
    val nearbyUsers by viewModel.nearbyUsersList.collectAsState()
    val isHistoryUnlocked by viewModel.isHistoryBiometricUnlocked.collectAsState()
    val bColors = LocalBharatColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(if (bColors.isDark) DarkBackground else LightBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.USER_PROFILE)
                    }
                    .testTag("user_profile_card"),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val customColor = try {
                            Color(android.graphics.Color.parseColor(userProfile.avatarColorHex))
                        } catch (e: Exception) {
                            BharatSaffron
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    if (userProfile.profilePicUri.isNotBlank()) {
                                        Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                                    } else {
                                        Brush.linearGradient(
                                            listOf(customColor, Color(0xFF0F172A), BharatGreenLight)
                                        )
                                    }
                                )
                                .border(
                                    2.dp,
                                    Brush.sweepGradient(listOf(BharatSaffron, BharatWhite, BharatGreenLight, BharatSaffron)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userProfile.profilePicUri.isNotBlank()) {
                                AsyncImage(
                                    model = userProfile.profilePicUri,
                                    contentDescription = "User DP",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = userProfile.avatarInitial.ifBlank { "VA" },
                                    color = BharatWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userProfile.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = bColors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                VerifiedBadge()
                            }
                            Text(
                                text = "${userProfile.bharatId} • ${userProfile.phone}",
                                fontSize = 12.sp,
                                color = bColors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            QuantumShieldBadge(text = "Quantum Shield Level 5")
                        }

                        // Edit Profile action icon
                        IconButton(
                            onClick = { viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.USER_PROFILE) },
                            modifier = Modifier.testTag("edit_profile_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = BharatSaffron
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDark) BharatSaffronLight else BharatNavy
                            )
                        }
                    }

                    if (userProfile.statusBio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (bColors.isDark) Color(0x331E293B) else Color(0x22CBD5E1),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, bColors.glassBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = BharatGreenLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = userProfile.statusBio,
                                    fontSize = 11.5.sp,
                                    color = bColors.textSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        // Futuristic Holographic UPI Wallet Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(listOf(BharatSaffron, BharatElectricCyan, BharatGreenLight))
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF020617))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VENZOIND UPI WALLET",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = GoldAccent
                                )
                            }
                            TricolorGlowPill(text = "NPCI Verified")
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Total Available Balance",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )

                        Text(
                            text = "₹${String.format("%,.2f", userProfile.walletBalance)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BharatWhite
                        )

                        Text(
                            text = "VPA: ${userProfile.upiVpa}",
                            fontSize = 11.5.sp,
                            color = BharatElectricCyan
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { viewModel.triggerUpiSheetWithBiometrics() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wallet_send_money_button")
                            ) {
                                Text("⚡ Send", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { viewModel.triggerQrScannerWithBiometrics() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wallet_scan_qr_button")
                            ) {
                                Text("📷 Scan QR", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Nearby Users Radar Simulation Section
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Nearby Bharat Radar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = bColors.textPrimary
                            )
                            Text(
                                text = "Discover encrypted contacts within 500m",
                                fontSize = 11.5.sp,
                                color = bColors.textSecondary
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleRadarScan() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRadarScanning) RoseError else BharatElectricCyan
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (isRadarScanning) "Stop" else "Scan",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRadarScanning) BharatWhite else DarkBackground
                            )
                        }
                    }

                    if (isRadarScanning) {
                        Spacer(modifier = Modifier.height(14.dp))
                        nearbyUsers.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BharatNavyLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.avatarInitial,
                                        color = BharatWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = bColors.textPrimary
                                    )
                                    Text(
                                        text = "${user.distanceMeters}m away • ${user.statusMsg}",
                                        fontSize = 11.sp,
                                        color = bColors.textSecondary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BharatSaffron.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable {
                                        viewModel.createChat(user.name, user.statusMsg, false, false, false)
                                    }
                                ) {
                                    Text(
                                        text = "Connect",
                                        color = BharatSaffronLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Encrypted Contacts & Phonebook Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.CONTACTS_LIST)
                    }
                    .testTag("profile_contacts_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(BharatSaffron.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = BharatSaffron,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Encrypted Phonebook & Contacts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = bColors.textPrimary
                            )
                            Text(
                                text = "Search, sort, and manage verified contacts",
                                fontSize = 11.5.sp,
                                color = bColors.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = bColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Multi-Device Sync & Cloud Backup Meter
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cloud Storage & Multi-Device Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = bColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Used: 1.4 GB / 100 GB (10GB file transfers)",
                            fontSize = 12.sp,
                            color = bColors.textSecondary
                        )
                        Text(
                            text = "3 Devices Active",
                            fontSize = 11.5.sp,
                            color = BharatGreenLight,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { 0.014f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BharatElectricCyan,
                        trackColor = Color(0x3364748B)
                    )
                }
            }
        }

        // Export Chat History & Contacts Backup Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.showBackupRestoreDialog.value = true
                    }
                    .testTag("profile_backup_restore_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(BharatSaffron, BharatGreenLight))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = BharatWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Export & Restore Data",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = bColors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TricolorGlowPill(text = "BACKUP")
                            }
                            Text(
                                text = "Export chat history & contacts to local file or restore",
                                fontSize = 11.5.sp,
                                color = bColors.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = bColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // WhatsApp App Settings Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.SETTINGS)
                    }
                    .testTag("profile_whatsapp_settings_nav_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(BharatGreenLight, Color(0xFF059669)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = BharatWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "All App Settings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = bColors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TricolorGlowPill(text = "FULL SETTINGS")
                            }
                            Text(
                                text = "Account, Privacy, Chats, Notifications, Storage & Language",
                                fontSize = 11.5.sp,
                                color = bColors.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = bColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Profile Settings & DP Customization Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.navigateTo(com.example.ui.viewmodel.AppScreen.USER_PROFILE)
                    }
                    .testTag("profile_settings_nav_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(BharatElectricCyan, Color(0xFF8B5CF6)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = BharatWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Profile & DP Settings",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = bColors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TricolorGlowPill(text = "DP / BIO")
                            }
                            Text(
                                text = "Change display picture, avatar preset, name & status",
                                fontSize = 11.5.sp,
                                color = bColors.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = bColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Security Lock & Biometrics Settings Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.requestBiometricAuth("Configure Security & Biometric Lock") {}
                    }
                    .testTag("profile_security_lock_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF047857)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = BharatWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Security Lock & Biometrics",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = bColors.textPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TricolorGlowPill(text = "ACTIVE")
                            }
                            Text(
                                text = "Fingerprint / Face ID lock for UPI & Chat Privacy",
                                fontSize = 11.5.sp,
                                color = bColors.textSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = BharatGreenLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Recent UPI Transactions History with Biometric Protection
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recent UPI Transactions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = bColors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (!isHistoryUnlocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = BharatSaffron,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Unlocked",
                            tint = BharatGreenLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (!isHistoryUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BharatSaffron.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BharatSaffron.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            viewModel.requestBiometricAuth("Unlock UPI Transaction History") {}
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = BharatSaffron,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Unlock",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatSaffron
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = { viewModel.isHistoryBiometricUnlocked.value = false },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Lock", fontSize = 12.sp, color = bColors.textSecondary)
                    }
                }
            }
        }

        if (!isHistoryUnlocked) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.requestBiometricAuth("Unlock UPI Transaction History") {}
                        },
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = if (bColors.isDark) Color(0x221E293B) else Color(0x1564748B)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(BharatSaffron.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Lock",
                                tint = BharatSaffron,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Transaction History Protected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = bColors.textPrimary
                        )
                        Text(
                            text = "Biometric authentication (Fingerprint or Face Unlock) required to view financial records",
                            fontSize = 11.5.sp,
                            color = bColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                viewModel.requestBiometricAuth("Unlock UPI Transaction History") {}
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron),
                            modifier = Modifier.testTag("unlock_transaction_history_button")
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Authenticate with Biometrics", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                TransactionItemRow(tx = tx)
            }
        }
    }
}

@Composable
fun TransactionItemRow(tx: TransactionEntity) {
    val bColors = LocalBharatColors.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (tx.isDebit) Color(0x22F43F5E) else Color(0x2210B981)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (tx.isDebit) Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (tx.isDebit) RoseError else BharatGreenLight,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = bColors.textPrimary
                )
                Text(
                    text = "${tx.timeFormatted} • ${tx.upiId}",
                    fontSize = 11.sp,
                    color = bColors.textSecondary
                )
            }

            Text(
                text = "${if (tx.isDebit) "-" else "+"}₹${tx.amount.toInt()}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (tx.isDebit) RoseError else BharatGreenLight
            )
        }
    }
}

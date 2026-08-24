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
    val isDark by viewModel.isDarkTheme.collectAsState()
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
                                text = "Fingerprint / Face ID lock for App & Chat Privacy",
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
    }
}

package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.UserProfileDataStore
import com.example.data.model.UserProfile
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Avatar Presets for VenzoInd Profile
 */
data class VenzoAvatarPreset(
    val id: Int,
    val label: String,
    val emoji: String,
    val gradientColors: List<Color>
)

val VENZO_AVATAR_PRESETS = listOf(
    VenzoAvatarPreset(0, "Monogram", "⚡", listOf(BharatSaffron, BharatNavyLight, BharatGreenLight)),
    VenzoAvatarPreset(1, "Cyber Ninja", "🥷", listOf(Color(0xFF0F172A), Color(0xFF334155), Color(0xFF10B981))),
    VenzoAvatarPreset(2, "AI Maverick", "🤖", listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899))),
    VenzoAvatarPreset(3, "Quantum Guru", "🧙‍♂️", listOf(Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFF3B82F6))),
    VenzoAvatarPreset(4, "Solar Phoenix", "🦅", listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFFBBF24))),
    VenzoAvatarPreset(5, "Zen Master", "🧘", listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857))),
    VenzoAvatarPreset(6, "Cosmic Astro", "🚀", listOf(Color(0xFF6366F1), Color(0xFF4F46E5), Color(0xFF4338CA))),
    VenzoAvatarPreset(7, "Royal Tiger", "🐅", listOf(BharatSaffron, Color(0xFFDC2626), Color(0xFF991B1B)))
)

val VENZO_ACCENT_COLORS = listOf(
    "#FF671F" to "Saffron",
    "#10B981" to "Emerald",
    "#06B6D4" to "Cyan",
    "#3B82F6" to "Electric Blue",
    "#8B5CF6" to "Royal Purple",
    "#EC4899" to "Pink Glow",
    "#F59E0B" to "Amber",
    "#1E293B" to "Midnight"
)

val VENZO_STATUS_PRESETS = listOf(
    "⚡ Available & Encrypted on VenzoInd",
    "🚀 Living in the moment | Building the future",
    "🇮🇳 Sovereign & Quantum Protected",
    "🎧 In deep focus session",
    "⚡ Ultra-fast messaging on VenzoInd",
    "☕ Busy exploring ideas",
    "🌙 Away / Do Not Disturb"
)

/**
 * Premium Glassmorphic User Profile Component
 * Provides complete view of account details and settings management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileGlassComponent(
    viewModel: BharatChatViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bColors = LocalBharatColors.current
    val isDark by viewModel.isDarkTheme.collectAsState()

    val dataStore = remember { UserProfileDataStore(context) }
    val dataStoreProfile by dataStore.userProfileFlow.collectAsState(initial = UserProfile())
    val vmProfile by viewModel.userProfile.collectAsState()
    val activeProfile = vmProfile ?: dataStoreProfile

    // Local editable states
    var displayName by remember(activeProfile.name) { mutableStateOf(activeProfile.name) }
    var statusBio by remember(activeProfile.statusBio) { mutableStateOf(activeProfile.statusBio) }
    var selectedAvatarIndex by remember(activeProfile.customAvatarIndex) { mutableIntStateOf(activeProfile.customAvatarIndex) }
    var selectedColorHex by remember(activeProfile.avatarColorHex) { mutableStateOf(activeProfile.avatarColorHex) }
    var profilePicUri by remember(activeProfile.profilePicUri) { mutableStateOf(activeProfile.profilePicUri) }

    // Settings switch states
    var isBiometricEnabled by remember { mutableStateOf(true) }
    var isReadReceiptsEnabled by remember { mutableStateOf(true) }
    var isScreenSecurityEnabled by remember { mutableStateOf(false) }
    var isAutoDownloadEnabled by remember { mutableStateOf(true) }
    var isHighDefUploadEnabled by remember { mutableStateOf(true) }
    var isTtsEnabled by remember { mutableStateOf(false) }
    var isPushNotificationEnabled by remember { mutableStateOf(true) }

    var isSaving by remember { mutableStateOf(false) }
    var showSavedNotification by remember { mutableStateOf(false) }
    var showAvatarPickerSheet by remember { mutableStateOf(false) }
    var showCameraCaptureDialog by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            profilePicUri = pickedUri.toString()
            selectedAvatarIndex = -1
            showAvatarPickerSheet = false
            Toast.makeText(context, "Profile photo updated", Toast.LENGTH_SHORT).show()
        }
    }

    val avatarInitial = remember(displayName) {
        if (displayName.isNotBlank()) {
            val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
            } else {
                parts[0].take(2).uppercase()
            }
        } else "VI"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_profile_glass_component"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. HERO GLASS PROFILE CARD ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = if (bColors.isDark) Color(0x331E293B) else Color(0x33F8FAFC),
            borderColor = bColors.glassBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TricolorGlowPill(text = "VENZOIND SOVEREIGN")

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BharatGreenLight.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, BharatGreenLight.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(BharatGreenLight)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "ONLINE • E2EE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatGreenLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Container with Glowing Halo
                Box(
                    modifier = Modifier.size(108.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val currentPreset = VENZO_AVATAR_PRESETS.getOrElse(selectedAvatarIndex) { VENZO_AVATAR_PRESETS[0] }
                    val customColor = try {
                        Color(android.graphics.Color.parseColor(selectedColorHex))
                    } catch (e: Exception) {
                        BharatSaffron
                    }

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(
                                if (profilePicUri.isNotBlank()) {
                                    Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                                } else if (selectedAvatarIndex == 0) {
                                    Brush.linearGradient(listOf(customColor, Color(0xFF0F172A), BharatGreenLight))
                                } else {
                                    Brush.linearGradient(currentPreset.gradientColors)
                                }
                            )
                            .border(
                                2.5.dp,
                                Brush.sweepGradient(
                                    listOf(BharatSaffron, BharatWhite, BharatGreenLight, BharatElectricCyan, BharatSaffron)
                                ),
                                CircleShape
                            )
                            .clickable {
                                viewModel.openZoomableDp(
                                    title = displayName,
                                    imageUri = profilePicUri.ifBlank { null },
                                    initial = avatarInitial,
                                    colorHex = selectedColorHex,
                                    subtitle = statusBio
                                )
                            }
                            .testTag("glass_profile_avatar_preview"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePicUri.isNotBlank()) {
                            AsyncImage(
                                model = profilePicUri,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (selectedAvatarIndex == 0) {
                            Text(
                                text = avatarInitial,
                                color = BharatWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 32.sp
                            )
                        } else {
                            Text(
                                text = currentPreset.emoji,
                                fontSize = 40.sp
                            )
                        }
                    }

                    // Floating camera quick button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(BharatSaffron)
                            .border(2.dp, BharatWhite, CircleShape)
                            .clickable { showAvatarPickerSheet = true }
                            .testTag("glass_profile_camera_fab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change DP",
                            tint = BharatWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Display Name & Verified Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (displayName.isNotBlank()) displayName else "VenzoInd User",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = bColors.textPrimary
                    )
                    VerifiedBadge()
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Username & Phone Info
                Text(
                    text = "${activeProfile.bharatId} • ${activeProfile.phone}",
                    fontSize = 12.5.sp,
                    color = bColors.textSecondary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Status Bio Quote
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (bColors.isDark) Color(0x221E293B) else Color(0x150F172A),
                    border = BorderStroke(0.6.dp, bColors.glassBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "\"$statusBio\"",
                        fontSize = 12.sp,
                        color = bColors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Action Bar: Camera, Gallery, QR Code, Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCameraCaptureDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BharatSaffron.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = BharatSaffron.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = BharatSaffron, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BharatSaffron)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BharatElectricCyan.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = BharatElectricCyan.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = BharatElectricCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BharatElectricCyan)
                    }

                    OutlinedButton(
                        onClick = { showQrDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BharatGreenLight.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = BharatGreenLight.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = BharatGreenLight, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("My QR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BharatGreenLight)
                    }
                }
            }
        }

        // --- 2. SOVEREIGN ACCOUNT STATISTICS GRID ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccountStatGlassCard(
                title = "MESSAGES",
                value = "1,840+",
                subtitle = "Kyber-1024 E2EE",
                icon = Icons.Default.Lock,
                accentColor = BharatGreenLight,
                modifier = Modifier.weight(1f)
            )
            AccountStatGlassCard(
                title = "VAULT STORAGE",
                value = "1.4 GB",
                subtitle = "Of 10 GB Free",
                icon = Icons.Default.CloudQueue,
                accentColor = BharatElectricCyan,
                modifier = Modifier.weight(1f)
            )
            AccountStatGlassCard(
                title = "DEVICES",
                value = "3 Active",
                subtitle = "Multi-Device Sync",
                icon = Icons.Default.Devices,
                accentColor = BharatSaffron,
                modifier = Modifier.weight(1f)
            )
        }

        // --- 3. EDIT PROFILE & PERSONAL INFO (GLASS CARD) ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = if (bColors.isDark) Color(0x281E293B) else Color(0x30FFFFFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCOUNT DETAILS",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BharatSaffron,
                        letterSpacing = 1.2.sp
                    )

                    Button(
                        onClick = {
                            if (displayName.isBlank()) {
                                Toast.makeText(context, "Display name cannot be blank", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSaving = true
                            coroutineScope.launch {
                                viewModel.saveUserProfile(
                                    name = displayName.trim(),
                                    statusBio = statusBio.trim(),
                                    profilePicUri = profilePicUri,
                                    avatarInitial = avatarInitial,
                                    avatarColorHex = selectedColorHex,
                                    avatarIndex = selectedAvatarIndex
                                )
                                delay(300)
                                isSaving = false
                                showSavedNotification = true
                                Toast.makeText(context, "Profile details saved successfully!", Toast.LENGTH_SHORT).show()
                                delay(2000)
                                showSavedNotification = false
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("glass_save_profile_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = BharatWhite, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, tint = BharatWhite, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BharatWhite)
                        }
                    }
                }

                // Display Name Field
                Column {
                    Text("Display Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = bColors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { if (it.length <= 40) displayName = it },
                        singleLine = true,
                        placeholder = { Text("Your full name", color = bColors.textMuted) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BharatSaffron) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BharatSaffron,
                            unfocusedBorderColor = bColors.glassBorder,
                            focusedTextColor = bColors.textPrimary,
                            unfocusedTextColor = bColors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("glass_display_name_input")
                    )
                }

                // Status Bio Field
                Column {
                    Text("Status Bio / About", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = bColors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = statusBio,
                        onValueChange = { if (it.length <= 120) statusBio = it },
                        minLines = 2,
                        maxLines = 3,
                        placeholder = { Text("What's on your mind?", color = bColors.textMuted) },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = BharatGreenLight) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BharatGreenLight,
                            unfocusedBorderColor = bColors.glassBorder,
                            focusedTextColor = bColors.textPrimary,
                            unfocusedTextColor = bColors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("glass_status_bio_input")
                    )
                }

                // Quick Status Preset Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(VENZO_STATUS_PRESETS) { chip ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (bColors.isDark) Color(0x331E293B) else Color(0x22E2E8F0),
                            border = BorderStroke(0.8.dp, bColors.glassBorder),
                            modifier = Modifier.clickable { statusBio = chip }
                        ) {
                            Text(
                                text = chip,
                                fontSize = 11.sp,
                                color = bColors.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // VenzoInd ID & Phone Read-only info row
                HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.5f))

                AccountDetailRow(
                    label = "VenzoInd Sovereign ID",
                    value = activeProfile.bharatId,
                    icon = Icons.Default.AlternateEmail,
                    iconTint = BharatSaffron,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("VenzoInd ID", activeProfile.bharatId))
                        Toast.makeText(context, "VenzoInd ID copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                AccountDetailRow(
                    label = "VenzoInd UPI VPA",
                    value = activeProfile.upiVpa,
                    icon = Icons.Default.Payment,
                    iconTint = BharatGreenLight,
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("UPI VPA", activeProfile.upiVpa))
                        Toast.makeText(context, "UPI VPA copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                AccountDetailRow(
                    label = "Phone Number",
                    value = activeProfile.phone,
                    icon = Icons.Default.Phone,
                    iconTint = BharatElectricCyan
                )

                AccountDetailRow(
                    label = "Quantum Security Fingerprint",
                    value = "KYBER-1024-DEF78A • SHA256",
                    icon = Icons.Default.Shield,
                    iconTint = GoldAccent
                )
            }
        }

        // --- 4. MANAGE SETTINGS (GLASSMORPHIC SECTIONS) ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = if (bColors.isDark) Color(0x281E293B) else Color(0x30FFFFFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "MANAGE SETTINGS & PRIVACY",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BharatElectricCyan,
                    letterSpacing = 1.2.sp
                )

                // Theme Mode Switch
                SettingToggleRow(
                    title = "Dark Theme",
                    subtitle = if (isDark) "Active • Sovereign Cyber OLED Dark" else "Active • Cyber Light Canvas",
                    icon = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                    iconTint = BharatSaffron,
                    isChecked = isDark,
                    onCheckedChange = { viewModel.toggleTheme() }
                )

                // Biometric App Lock Switch
                SettingToggleRow(
                    title = "Biometric App Lock",
                    subtitle = "Require fingerprint or face ID to open VenzoInd",
                    icon = Icons.Default.Fingerprint,
                    iconTint = BharatGreenLight,
                    isChecked = isBiometricEnabled,
                    onCheckedChange = {
                        isBiometricEnabled = it
                        Toast.makeText(context, if (it) "Biometric Lock Enabled" else "Biometric Lock Disabled", Toast.LENGTH_SHORT).show()
                    }
                )

                // Read Receipts Switch
                SettingToggleRow(
                    title = "Read Receipts",
                    subtitle = "Show blue checkmarks when messages are read",
                    icon = Icons.Default.DoneAll,
                    iconTint = BharatElectricCyan,
                    isChecked = isReadReceiptsEnabled,
                    onCheckedChange = { isReadReceiptsEnabled = it }
                )

                // Anti-Screenshot Screen Security
                SettingToggleRow(
                    title = "Screen Security",
                    subtitle = "Block screenshots and screen recording in chats",
                    icon = Icons.Default.Security,
                    iconTint = BharatSaffron,
                    isChecked = isScreenSecurityEnabled,
                    onCheckedChange = {
                        isScreenSecurityEnabled = it
                        Toast.makeText(context, if (it) "Anti-screenshot vault enabled" else "Screen capture allowed", Toast.LENGTH_SHORT).show()
                    }
                )

                // Auto-Download Media
                SettingToggleRow(
                    title = "Auto-Download Media",
                    subtitle = "Save photos and voice notes on Wi-Fi",
                    icon = Icons.Default.CloudDownload,
                    iconTint = BharatGreenLight,
                    isChecked = isAutoDownloadEnabled,
                    onCheckedChange = { isAutoDownloadEnabled = it }
                )

                // High-Def 4K Media Uploads
                SettingToggleRow(
                    title = "High-Definition 4K Uploads",
                    subtitle = "Send uncompressed lossless media up to 10GB",
                    icon = Icons.Default.Hd,
                    iconTint = GoldAccent,
                    isChecked = isHighDefUploadEnabled,
                    onCheckedChange = { isHighDefUploadEnabled = it }
                )

                // Push Notifications
                SettingToggleRow(
                    title = "Push Notifications",
                    subtitle = "Alerts for incoming calls and new messages",
                    icon = Icons.Default.NotificationsActive,
                    iconTint = BharatSaffron,
                    isChecked = isPushNotificationEnabled,
                    onCheckedChange = { isPushNotificationEnabled = it }
                )

                // Text-to-Speech Readout
                SettingToggleRow(
                    title = "AI Voice Text-to-Speech",
                    subtitle = "Read out incoming messages aloud when tapped",
                    icon = Icons.Default.RecordVoiceOver,
                    iconTint = BharatElectricCyan,
                    isChecked = isTtsEnabled,
                    onCheckedChange = { isTtsEnabled = it }
                )
            }
        }

        // --- 5. DATA & BACKUP ACTIONS ---
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = if (bColors.isDark) Color(0x281E293B) else Color(0x30FFFFFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "DATA & CLOUD BACKUP",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BharatGreenLight,
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.showBackupRestoreDialog.value = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BharatGreen),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = BharatWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup Chats", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BharatWhite)
                    }

                    OutlinedButton(
                        onClick = { viewModel.showBackupRestoreDialog.value = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BharatElectricCyan.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = BharatElectricCyan.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = BharatElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BharatElectricCyan)
                    }
                }
            }
        }

        // --- 6. MORE SETTINGS SHORTCUT BUTTON ---
        if (onNavigateToSettings != null) {
            Button(
                onClick = onNavigateToSettings,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, bColors.glassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = BharatSaffronLight, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Full VenzoInd Settings", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = BharatWhite)
            }
        }
    }

    // Camera Capture Dialog
    if (showCameraCaptureDialog) {
        ProfileCameraCaptureDialog(
            onDismiss = { showCameraCaptureDialog = false },
            onPhotoConfirmed = { capturedUri, _ ->
                showCameraCaptureDialog = false
                profilePicUri = capturedUri.toString()
                selectedAvatarIndex = -1
                Toast.makeText(context, "Captured new profile photo!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Avatar Selection Bottom Sheet
    if (showAvatarPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarPickerSheet = false },
            containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurfaceElevated,
            dragHandle = { BottomSheetDefaults.DragHandle(color = bColors.textMuted) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose Avatar Style",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = bColors.textPrimary
                )

                // Avatar Presets Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AVATAR PRESETS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = bColors.textMuted
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(VENZO_AVATAR_PRESETS) { preset ->
                            val isSelected = selectedAvatarIndex == preset.id && profilePicUri.isBlank()
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        selectedAvatarIndex = preset.id
                                        profilePicUri = ""
                                        showAvatarPickerSheet = false
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(preset.gradientColors))
                                        .border(
                                            if (isSelected) 2.5.dp else 1.dp,
                                            if (isSelected) BharatWhite else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (preset.id == 0) {
                                        Text(avatarInitial, color = BharatWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    } else {
                                        Text(preset.emoji, fontSize = 24.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    preset.label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) BharatSaffron else bColors.textSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Accent Color Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COLOR ACCENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = bColors.textMuted
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(VENZO_ACCENT_COLORS) { (hex, name) ->
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            val col = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { BharatSaffron }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        if (isSelected) 2.5.dp else 1.dp,
                                        if (isSelected) BharatWhite else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable {
                                        selectedColorHex = hex
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = BharatWhite, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // QR Code Dialog
    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("Close", color = BharatSaffron, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("VenzoInd UPI & Identity QR", fontWeight = FontWeight.Bold, color = bColors.textPrimary)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BharatWhite,
                        modifier = Modifier.size(180.dp).padding(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "QR Code",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(160.dp)
                            )
                        }
                    }

                    Text(
                        text = activeProfile.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = bColors.textPrimary
                    )

                    Text(
                        text = "UPI: ${activeProfile.upiVpa}",
                        fontSize = 12.sp,
                        color = BharatGreenLight,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Scan with any UPI app or VenzoInd Scanner to connect and pay instantly.",
                        fontSize = 11.sp,
                        color = bColors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
    }
}

@Composable
private fun AccountStatGlassCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val bColors = LocalBharatColors.current

    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        backgroundColor = if (bColors.isDark) Color(0x281E293B) else Color(0x30FFFFFF)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = bColors.textPrimary
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = bColors.textMuted,
                letterSpacing = 0.5.sp
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AccountDetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    onCopy: (() -> Unit)? = null
) {
    val bColors = LocalBharatColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onCopy != null) Modifier.clickable { onCopy() } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = bColors.textMuted
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = bColors.textPrimary
            )
        }

        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = bColors.textMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val bColors = LocalBharatColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = bColors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = bColors.textMuted
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BharatWhite,
                checkedTrackColor = BharatSaffron,
                uncheckedThumbColor = bColors.textMuted,
                uncheckedTrackColor = if (bColors.isDark) Color(0x331E293B) else Color(0x22CBD5E1)
            ),
            modifier = Modifier.scale(0.85f)
        )
    }
}

private fun saveProfileBitmapToCache(context: Context, bitmap: android.graphics.Bitmap): Uri {
    val imagesDir = java.io.File(context.cacheDir, "profile_photos")
    if (!imagesDir.exists()) imagesDir.mkdirs()
    val imageFile = java.io.File(imagesDir, "dp_${System.currentTimeMillis()}.jpg")
    java.io.FileOutputStream(imageFile).use { out ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)
    }
    return Uri.fromFile(imageFile)
}

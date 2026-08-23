package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.UserProfileDataStore
import com.example.data.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.ProfileCameraCaptureDialog
import com.example.ui.components.QuantumShieldBadge
import com.example.ui.components.TricolorGlowPill
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Avatar Preset data for quick visual selection
 */
data class AvatarPreset(
    val id: Int,
    val label: String,
    val emoji: String,
    val gradientColors: List<Color>
)

val AVATAR_PRESETS = listOf(
    AvatarPreset(0, "Monogram", "⚡", listOf(BharatSaffron, BharatNavyLight, BharatGreenLight)),
    AvatarPreset(1, "Cyber Ninja", "🥷", listOf(Color(0xFF0F172A), Color(0xFF334155), Color(0xFF10B981))),
    AvatarPreset(2, "AI Maverick", "🤖", listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899))),
    AvatarPreset(3, "Quantum Guru", "🧙‍♂️", listOf(Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFF3B82F6))),
    AvatarPreset(4, "Solar Phoenix", "🦅", listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFFBBF24))),
    AvatarPreset(5, "Zen Master", "🧘", listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857))),
    AvatarPreset(6, "Cosmic Astro", "🚀", listOf(Color(0xFF6366F1), Color(0xFF4F46E5), Color(0xFF4338CA))),
    AvatarPreset(7, "Royal Tiger", "🐅", listOf(BharatSaffron, Color(0xFFDC2626), Color(0xFF991B1B)))
)

val AVATAR_COLORS = listOf(
    "#FF671F" to "Saffron",
    "#10B981" to "Emerald",
    "#3B82F6" to "Electric Blue",
    "#8B5CF6" to "Royal Purple",
    "#EC4899" to "Pink Glow",
    "#06B6D4" to "Cyan",
    "#F59E0B" to "Amber",
    "#1E293B" to "Midnight Slate"
)

val STATUS_BIO_CHIPS = listOf(
    "⚡ Available & Encrypted",
    "🚀 Living in the moment | Building the future",
    "🇮🇳 Sovereign & Quantum Protected",
    "🎧 In a deep focus session",
    "⚡ Ultra-fast messaging on VenzoInd",
    "☕ Busy exploring ideas",
    "🌙 Away / Do Not Disturb"
)

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
    val imagesDir = File(context.cacheDir, "profile_photos")
    if (!imagesDir.exists()) imagesDir.mkdirs()
    val imageFile = File(imagesDir, "dp_camera_${System.currentTimeMillis()}.jpg")
    FileOutputStream(imageFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return Uri.fromFile(imageFile)
}

private fun copyUriToLocalCache(context: Context, sourceUri: Uri): Uri {
    return try {
        val imagesDir = File(context.cacheDir, "profile_photos")
        if (!imagesDir.exists()) imagesDir.mkdirs()
        val destFile = File(imagesDir, "dp_gallery_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        Uri.fromFile(destFile)
    } catch (e: Exception) {
        sourceUri
    }
}

/**
 * UserProfile Screen Composable
 * Enables users to:
 * 1. Set / edit their Display Name
 * 2. Select profile avatar preset / custom photo / color theme
 * 3. Set custom Status Bio with quick suggestions
 * 4. Persist data reliably to Jetpack DataStore preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: BharatChatViewModel? = null,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bColors = LocalBharatColors.current

    // Local DataStore instance for direct or fallback use
    val dataStore = remember { UserProfileDataStore(context) }
    val dataStoreProfile by dataStore.userProfileFlow.collectAsState(initial = UserProfile())

    // Profile State (initialized from ViewModel if available, or directly from DataStore)
    val vmProfile by viewModel?.userProfile?.collectAsState() ?: remember { mutableStateOf(null) }
    val activeProfile = vmProfile ?: dataStoreProfile

    var displayName by remember(activeProfile.name) { mutableStateOf(activeProfile.name) }
    var statusBio by remember(activeProfile.statusBio) { mutableStateOf(activeProfile.statusBio) }
    var selectedAvatarIndex by remember(activeProfile.customAvatarIndex) { mutableIntStateOf(activeProfile.customAvatarIndex) }
    var selectedColorHex by remember(activeProfile.avatarColorHex) { mutableStateOf(activeProfile.avatarColorHex) }
    var profilePicUri by remember(activeProfile.profilePicUri) { mutableStateOf(activeProfile.profilePicUri) }

    var isSaving by remember { mutableStateOf(false) }
    var showSavedNotification by remember { mutableStateOf(false) }
    var showAvatarPickerSheet by remember { mutableStateOf(false) }
    var showCameraCaptureDialog by remember { mutableStateOf(false) }

    // Camera and Gallery ActivityResult launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            try {
                val cachedUri = copyUriToLocalCache(context, pickedUri)
                profilePicUri = cachedUri.toString()
                selectedAvatarIndex = -1 // custom photo flag
                showAvatarPickerSheet = false
                Toast.makeText(context, "Profile photo loaded from Gallery", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                profilePicUri = pickedUri.toString()
                selectedAvatarIndex = -1
                showAvatarPickerSheet = false
            }
        }
    }

    val onCameraAction = {
        showAvatarPickerSheet = false
        showCameraCaptureDialog = true
    }

    // Calculate avatar initials dynamically
    val avatarInitial = remember(displayName) {
        if (displayName.isNotBlank()) {
            val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
            } else {
                parts[0].take(2).uppercase()
            }
        } else "VA"
    }

    // Background infinite subtle glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "profile_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (bColors.isDark) DarkBackground else LightBackground)
            .testTag("user_profile_screen")
    ) {
        // Ambient Tri-color background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BharatSaffron.copy(alpha = if (bColors.isDark) 0.12f else 0.07f),
                            Color.Transparent
                        ),
                        radius = 800f,
                        center = androidx.compose.ui.geometry.Offset(200f, 150f)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BharatGreenLight.copy(alpha = if (bColors.isDark) 0.10f else 0.05f),
                            Color.Transparent
                        ),
                        radius = 700f,
                        center = androidx.compose.ui.geometry.Offset(700f, 1200f)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = if (bColors.isDark) DarkSurface.copy(alpha = 0.88f) else LightSurface.copy(alpha = 0.92f),
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, bColors.glassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("profile_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = bColors.textPrimary
                            )
                        }

                        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                text = "User Profile",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = bColors.textPrimary
                            )
                            Text(
                                text = "DataStore Persisted Account",
                                fontSize = 11.5.sp,
                                color = BharatGreenLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Save Button in Top Bar
                        Button(
                            onClick = {
                                if (displayName.isBlank()) {
                                    Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSaving = true
                                coroutineScope.launch {
                                    if (viewModel != null) {
                                        viewModel.saveUserProfile(
                                            name = displayName.trim(),
                                            statusBio = statusBio.trim(),
                                            profilePicUri = profilePicUri,
                                            avatarInitial = avatarInitial,
                                            avatarColorHex = selectedColorHex,
                                            avatarIndex = selectedAvatarIndex
                                        )
                                    } else {
                                        dataStore.saveUserProfile(
                                            displayName = displayName.trim(),
                                            statusBio = statusBio.trim(),
                                            profilePicUri = profilePicUri,
                                            avatarInitial = avatarInitial,
                                            avatarColorHex = selectedColorHex,
                                            avatarIndex = selectedAvatarIndex
                                        )
                                    }
                                    delay(400)
                                    isSaving = false
                                    showSavedNotification = true
                                    delay(2500)
                                    showSavedNotification = false
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BharatSaffron
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("profile_save_top_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = BharatWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = BharatWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BharatWhite
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // --- Success Banner if DataStore Saved ---
                if (showSavedNotification) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BharatGreen.copy(alpha = 0.18f),
                            border = BorderStroke(1.2.dp, BharatGreenLight.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BharatGreenLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Changes Saved to DataStore!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = BharatGreenLight
                                    )
                                    Text(
                                        text = "Display name, avatar, and bio persisted locally.",
                                        fontSize = 11.sp,
                                        color = bColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 1. Interactive Avatar & Picture Selector ---
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar Badge with Glowing Halo
                            Box(
                                modifier = Modifier
                                    .size(112.dp)
                                    .scale(glowScale),
                                contentAlignment = Alignment.Center
                            ) {
                                val currentPreset = AVATAR_PRESETS.getOrElse(selectedAvatarIndex) { AVATAR_PRESETS[0] }
                                val customColor = try {
                                    Color(android.graphics.Color.parseColor(selectedColorHex))
                                } catch (e: Exception) {
                                    BharatSaffron
                                }

                                Box(
                                    modifier = Modifier
                                        .size(98.dp)
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
                                                listOf(BharatSaffron, BharatWhite, BharatGreenLight, BharatSaffron)
                                            ),
                                            CircleShape
                                        )
                                        .clickable { showAvatarPickerSheet = true }
                                        .testTag("profile_avatar_preview"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profilePicUri.isNotBlank()) {
                                        AsyncImage(
                                            model = profilePicUri,
                                            contentDescription = "Custom Profile Picture",
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
                                            fontSize = 42.sp
                                        )
                                    }
                                }

                                // Camera/Edit floating button
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(BharatSaffron)
                                        .border(2.dp, BharatWhite, CircleShape)
                                        .clickable { showAvatarPickerSheet = true }
                                        .testTag("profile_avatar_edit_fab"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Edit Profile Picture",
                                        tint = BharatWhite,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (displayName.isNotBlank()) displayName else "Your Name",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = bColors.textPrimary
                            )

                            Text(
                                text = "${activeProfile.bharatId} • ${activeProfile.phone}",
                                fontSize = 12.sp,
                                color = bColors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Action Buttons: Camera & Gallery
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCameraAction,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, BharatSaffron.copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = BharatSaffron.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("avatar_quick_camera_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = BharatSaffron,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Camera",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = BharatSaffron
                                    )
                                }

                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, BharatElectricCyan.copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = BharatElectricCyan.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("avatar_quick_gallery_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = BharatElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Gallery",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = BharatElectricCyan
                                    )
                                }
                            }

                            if (profilePicUri.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        profilePicUri = ""
                                        selectedAvatarIndex = 0
                                        Toast.makeText(context, "Custom photo removed", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("remove_photo_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Remove Custom Photo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Avatar Theme Colors Row
                            Text(
                                text = "AVATAR THEME ACCENT",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = bColors.textMuted,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                items(AVATAR_COLORS) { (hex, _) ->
                                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                                    val col = try {
                                        Color(android.graphics.Color.parseColor(hex))
                                    } catch (e: Exception) {
                                        BharatSaffron
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                if (isSelected) 2.5.dp else 1.dp,
                                                if (isSelected) BharatWhite else Color.Transparent,
                                                CircleShape
                                            )
                                            .clickable {
                                                selectedColorHex = hex
                                            }
                                            .testTag("color_picker_$hex"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = BharatWhite,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { showAvatarPickerSheet = true },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BharatGreenLight.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("change_avatar_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = BharatGreenLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Choose Avatar Style",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BharatGreenLight
                                )
                            }
                        }
                    }
                }

                // --- 2. Edit Profile Fields (Display Name & Bio) ---
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "PERSONAL INFORMATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatSaffron,
                                letterSpacing = 1.2.sp
                            )

                            // Display Name Input Field
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Display Name",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = bColors.textPrimary
                                    )
                                    Text(
                                        text = "${displayName.length}/40",
                                        fontSize = 11.sp,
                                        color = if (displayName.length > 35) RoseError else bColors.textMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { if (it.length <= 40) displayName = it },
                                    placeholder = { Text("Enter your full name", color = bColors.textMuted) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Name",
                                            tint = BharatSaffron
                                        )
                                    },
                                    trailingIcon = {
                                        if (displayName.isNotBlank()) {
                                            IconButton(onClick = { displayName = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Clear,
                                                    contentDescription = "Clear",
                                                    tint = bColors.textMuted
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BharatSaffron,
                                        unfocusedBorderColor = bColors.glassBorder,
                                        focusedContainerColor = if (bColors.isDark) Color(0x331E293B) else Color(0x22F1F5F9),
                                        unfocusedContainerColor = if (bColors.isDark) Color(0x221E293B) else Color(0x15F1F5F9),
                                        focusedTextColor = bColors.textPrimary,
                                        unfocusedTextColor = bColors.textPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_display_name_input")
                                )
                            }

                            // Status Bio Input Field
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Status Bio / About",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = bColors.textPrimary
                                    )
                                    Text(
                                        text = "${statusBio.length}/120",
                                        fontSize = 11.sp,
                                        color = if (statusBio.length > 110) RoseError else bColors.textMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = statusBio,
                                    onValueChange = { if (it.length <= 120) statusBio = it },
                                    placeholder = { Text("What's on your mind?", color = bColors.textMuted) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Bio",
                                            tint = BharatGreenLight
                                        )
                                    },
                                    minLines = 2,
                                    maxLines = 4,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BharatGreenLight,
                                        unfocusedBorderColor = bColors.glassBorder,
                                        focusedContainerColor = if (bColors.isDark) Color(0x331E293B) else Color(0x22F1F5F9),
                                        unfocusedContainerColor = if (bColors.isDark) Color(0x221E293B) else Color(0x15F1F5F9),
                                        focusedTextColor = bColors.textPrimary,
                                        unfocusedTextColor = bColors.textPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_status_bio_input")
                                )
                            }

                            // Quick Bio Suggestion Chips
                            Column {
                                Text(
                                    text = "QUICK STATUS PRESETS",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bColors.textMuted,
                                    letterSpacing = 0.8.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(STATUS_BIO_CHIPS) { chip ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (bColors.isDark) Color(0x331E293B) else Color(0x22E2E8F0),
                                            border = BorderStroke(0.8.dp, bColors.glassBorder),
                                            modifier = Modifier
                                                .clickable { statusBio = chip }
                                                .testTag("bio_chip_$chip")
                                        ) {
                                            Text(
                                                text = chip,
                                                fontSize = 11.5.sp,
                                                color = bColors.textPrimary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 3. Live Card Preview (How you look to others) ---
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = BharatElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "PUBLIC PREVIEW IN CHATS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BharatElectricCyan,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Contact Row Preview Mock
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (bColors.isDark) Color(0x440F172A) else Color(0x33F8FAFC),
                                border = BorderStroke(1.dp, bColors.glassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val currentPreset = AVATAR_PRESETS.getOrElse(selectedAvatarIndex) { AVATAR_PRESETS[0] }
                                    val customColor = try {
                                        Color(android.graphics.Color.parseColor(selectedColorHex))
                                    } catch (e: Exception) {
                                        BharatSaffron
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (profilePicUri.isNotBlank()) {
                                                    Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                                                } else if (selectedAvatarIndex == 0) {
                                                    Brush.linearGradient(listOf(customColor, customColor))
                                                } else {
                                                    Brush.linearGradient(currentPreset.gradientColors)
                                                }
                                            ),
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
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        } else {
                                            Text(
                                                text = currentPreset.emoji,
                                                fontSize = 20.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (displayName.isNotBlank()) displayName else "Vikram Aditya",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = bColors.textPrimary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedBadge()
                                        }
                                        Text(
                                            text = if (statusBio.isNotBlank()) statusBio else "Living in the moment ⚡",
                                            fontSize = 11.5.sp,
                                            color = bColors.textSecondary,
                                            maxLines = 1
                                        )
                                    }

                                    TricolorGlowPill(text = "Kyber-1024")
                                }
                            }
                        }
                    }
                }

                // --- 4. Security & Biometric App Lock Settings ---
                item {
                    var biometricEnabled by remember { mutableStateOf(true) }
                    var upiPinRequired by remember { mutableStateOf(true) }
                    var screenLockTimeout by remember { mutableStateOf("Immediately") }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = BharatGreenLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "SECURITY LOCK & PRIVACY",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BharatGreenLight,
                                    letterSpacing = 1.sp
                                )
                            }

                            // Biometric Fingerprint/Face Lock Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Biometric Lock (Fingerprint / Face)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = bColors.textPrimary
                                    )
                                    Text(
                                        text = "Require biometric sensor scan to open chats & financial vault",
                                        fontSize = 11.sp,
                                        color = bColors.textSecondary
                                    )
                                }
                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = {
                                        biometricEnabled = it
                                        if (it && viewModel != null) {
                                            viewModel.requestBiometricAuth("Enable Biometric App Lock") {}
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BharatWhite,
                                        checkedTrackColor = BharatGreenLight
                                    ),
                                    modifier = Modifier.testTag("biometric_lock_toggle")
                                )
                            }

                            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.5f))

                            // UPI Transaction PIN Confirmation Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "UPI Quantum Pin Shield",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = bColors.textPrimary
                                    )
                                    Text(
                                        text = "Mandatory 6-digit cryptographic PIN check for peer-to-peer transfers",
                                        fontSize = 11.sp,
                                        color = bColors.textSecondary
                                    )
                                }
                                Switch(
                                    checked = upiPinRequired,
                                    onCheckedChange = { upiPinRequired = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BharatWhite,
                                        checkedTrackColor = BharatSaffron
                                    ),
                                    modifier = Modifier.testTag("upi_pin_shield_toggle")
                                )
                            }

                            HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.5f))

                            // Auto Lock Timing Chips
                            Column {
                                Text(
                                    text = "AUTO-LOCK TIMEOUT",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bColors.textMuted,
                                    letterSpacing = 0.8.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Immediately", "1 Minute", "5 Minutes", "15 Minutes").forEach { timeout ->
                                        val isSelected = screenLockTimeout == timeout
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) BharatGreenLight.copy(alpha = 0.2f) else Color.Transparent,
                                            border = BorderStroke(1.dp, if (isSelected) BharatGreenLight else bColors.glassBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { screenLockTimeout = timeout }
                                        ) {
                                            Text(
                                                text = timeout,
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) BharatGreenLight else bColors.textSecondary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 5. Main Save Changes Action Button ---
                item {
                    Button(
                        onClick = {
                            if (displayName.isBlank()) {
                                Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSaving = true
                            coroutineScope.launch {
                                if (viewModel != null) {
                                    viewModel.saveUserProfile(
                                        name = displayName.trim(),
                                        statusBio = statusBio.trim(),
                                        profilePicUri = profilePicUri,
                                        avatarInitial = avatarInitial,
                                        avatarColorHex = selectedColorHex,
                                        avatarIndex = selectedAvatarIndex
                                    )
                                } else {
                                    dataStore.saveUserProfile(
                                        displayName = displayName.trim(),
                                        statusBio = statusBio.trim(),
                                        profilePicUri = profilePicUri,
                                        avatarInitial = avatarInitial,
                                        avatarColorHex = selectedColorHex,
                                        avatarIndex = selectedAvatarIndex
                                    )
                                }
                                delay(400)
                                isSaving = false
                                showSavedNotification = true
                                delay(2500)
                                showSavedNotification = false
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BharatSaffron
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = BharatSaffron)
                            .testTag("save_user_profile_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BharatWhite,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Writing to DataStore...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = BharatWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Profile Changes",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = BharatWhite
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // --- Profile DP & Avatar Selection Bottom Sheet ---
        if (showAvatarPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAvatarPickerSheet = false },
                containerColor = if (bColors.isDark) DarkSurfaceElevated else LightSurface,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Change Profile Picture (DP)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = bColors.textPrimary
                    )

                    Text(
                        text = "Capture a live photo, choose from device gallery, or select an avatar",
                        fontSize = 12.sp,
                        color = bColors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Camera & Gallery Main Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Camera Action Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = BharatSaffron.copy(alpha = if (bColors.isDark) 0.16f else 0.10f),
                            border = BorderStroke(1.2.dp, BharatSaffron.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showAvatarPickerSheet = false
                                    onCameraAction()
                                }
                                .testTag("sheet_action_camera")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(BharatSaffron),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Camera",
                                        tint = BharatWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Take Photo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = bColors.textPrimary
                                )
                                Text(
                                    text = "Use device camera",
                                    fontSize = 11.sp,
                                    color = bColors.textSecondary
                                )
                            }
                        }

                        // Gallery Action Card
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = BharatElectricCyan.copy(alpha = if (bColors.isDark) 0.16f else 0.10f),
                            border = BorderStroke(1.2.dp, BharatElectricCyan.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showAvatarPickerSheet = false
                                    galleryLauncher.launch("image/*")
                                }
                                .testTag("sheet_action_gallery")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(BharatElectricCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = "Gallery",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Choose Gallery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = bColors.textPrimary
                                )
                                Text(
                                    text = "Pick from device files",
                                    fontSize = 11.sp,
                                    color = bColors.textSecondary
                                )
                            }
                        }
                    }

                    if (profilePicUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                profilePicUri = ""
                                selectedAvatarIndex = 0
                                showAvatarPickerSheet = false
                                Toast.makeText(context, "Custom photo removed", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("sheet_remove_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Remove Custom Photo & Use Monogram",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    HorizontalDivider(color = bColors.glassBorder.copy(alpha = 0.6f))

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "OR CHOOSE AVATAR PRESET",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = bColors.textMuted,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4x2 Grid of avatar presets
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AVATAR_PRESETS.chunked(4).forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowPresets.forEach { preset ->
                                    val isSelected = selectedAvatarIndex == preset.id && profilePicUri.isBlank()
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable {
                                                profilePicUri = ""
                                                selectedAvatarIndex = preset.id
                                                showAvatarPickerSheet = false
                                            }
                                            .padding(4.dp)
                                            .testTag("avatar_preset_${preset.id}")
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(Brush.linearGradient(preset.gradientColors))
                                                .border(
                                                    if (isSelected) 3.dp else 1.dp,
                                                    if (isSelected) BharatGreenLight else Color.Transparent,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (preset.id == 0) {
                                                Text(
                                                    text = avatarInitial,
                                                    color = BharatWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            } else {
                                                Text(
                                                    text = preset.emoji,
                                                    fontSize = 26.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = preset.label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) BharatGreenLight else bColors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showCameraCaptureDialog) {
        ProfileCameraCaptureDialog(
            onDismiss = { showCameraCaptureDialog = false },
            onPhotoConfirmed = { uri, _ ->
                profilePicUri = uri.toString()
                selectedAvatarIndex = -1
                Toast.makeText(context, "Profile picture updated from Camera", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

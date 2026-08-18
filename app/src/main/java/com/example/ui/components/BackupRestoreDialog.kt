package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.core.content.FileProvider
import com.example.data.backup.BackupFileInfo
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreDialog(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bColors = LocalBharatColors.current
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val exportResult by viewModel.lastExportResult.collectAsState()
    val restoreResult by viewModel.lastRestoreResult.collectAsState()
    val availableBackups by viewModel.availableBackupsList.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Export / Backup, 1 = Restore Data
    var customBackupName by remember { mutableStateOf("") }
    var selectedFileToRestore by remember { mutableStateOf<BackupFileInfo?>(null) }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }

    // System File Picker for importing custom .json backup from downloads or external storage
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.restoreBackupFromUri(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAvailableBackups()
        viewModel.clearBackupRestoreMessages()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bColors.isDark) Color(0xF50B132B) else Color(0xFAF8FAFC),
            border = BorderStroke(1.dp, bColors.glassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Local Backup & Restore",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = bColors.textPrimary
                            )
                            Text(
                                text = "Sovereign On-Device E2EE Archive",
                                fontSize = 11.5.sp,
                                color = BharatElectricCyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_backup_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = bColors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Tab Switcher (Export vs Restore)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (bColors.isDark) Color(0x331E293B) else Color(0x1564748B))
                        .padding(4.dp)
                ) {
                    // Export Tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 0) BharatSaffron else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 0
                                viewModel.clearBackupRestoreMessages()
                            }
                            .testTag("tab_export_backup")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                tint = if (selectedTab == 0) BharatWhite else bColors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Export Data",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedTab == 0) BharatWhite else bColors.textSecondary
                            )
                        }
                    }

                    // Restore Tab
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == 1) BharatGreenLight else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedTab = 1
                                viewModel.clearBackupRestoreMessages()
                                viewModel.refreshAvailableBackups()
                            }
                            .testTag("tab_restore_backup")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = if (selectedTab == 1) DarkBackground else bColors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Restore Data",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedTab == 1) DarkBackground else bColors.textSecondary
                            )
                        }
                    }
                }

                // TAB 0: EXPORT DATA TO LOCAL FILE
                if (selectedTab == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Generate a complete sovereign backup file of all your chats, messages, media references, and encrypted contacts to local storage.",
                            fontSize = 12.5.sp,
                            color = bColors.textSecondary,
                            lineHeight = 17.sp
                        )

                        OutlinedTextField(
                            value = customBackupName,
                            onValueChange = { customBackupName = it },
                            label = { Text("Backup File Name (Optional)") },
                            placeholder = { Text("e.g. Venzora_Personal_Backup") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("custom_backup_filename_input")
                        )

                        // Status card or result
                        exportResult?.let { result ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (result.success) Color(0x2210B981) else Color(0x22EF4444),
                                border = BorderStroke(1.dp, if (result.success) BharatGreenLight else RoseError),
                                modifier = Modifier.fillMaxWidth().testTag("export_result_card")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (result.success) BharatGreenLight else RoseError,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (result.success) "Backup Created Successfully!" else "Backup Failed",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (result.success) BharatGreenLight else RoseError
                                        )
                                    }

                                    if (result.success) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "📁 File: ${result.fileName}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = bColors.textPrimary
                                        )
                                        Text(
                                            text = "📊 ${result.chatsCount} Chats • ${result.messagesCount} Messages • ${result.contactsCount} Contacts (${result.fileSizeFormatted})",
                                            fontSize = 11.5.sp,
                                            color = bColors.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Share file intent button
                                        Button(
                                            onClick = {
                                                try {
                                                    val file = File(result.filePath)
                                                    val uri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        file
                                                    )
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "application/json"
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share Venzora Backup"))
                                                } catch (e: Exception) {
                                                    // Fallback simple share
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight),
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("share_backup_file_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                tint = DarkBackground,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share / Save to Google Drive / Files", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = result.errorMessage ?: "Unknown error occurred during export",
                                            fontSize = 11.5.sp,
                                            color = RoseError
                                        )
                                    }
                                }
                            }
                        }

                        // Export Button
                        Button(
                            onClick = {
                                val name = if (customBackupName.isNotBlank()) {
                                    if (customBackupName.endsWith(".json")) customBackupName else "$customBackupName.json"
                                } else null
                                viewModel.exportChatAndContactsBackup(name)
                            },
                            enabled = !isBackingUp,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BharatSaffron),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_create_local_backup")
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    color = BharatWhite,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Encrypting & Exporting Archive...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export to Local Backup File", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // TAB 1: RESTORE DATA FROM LOCAL BACKUP
                if (selectedTab == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Select an existing local backup archive or pick a JSON backup file from your device to restore chats and contacts.",
                            fontSize = 12.5.sp,
                            color = bColors.textSecondary,
                            lineHeight = 17.sp
                        )

                        // Import from External Picker button
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("application/json") },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BharatElectricCyan),
                            modifier = Modifier.fillMaxWidth().testTag("pick_backup_file_external_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = BharatElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse & Import .json Backup File", color = BharatElectricCyan, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        // Restore Result Banner
                        restoreResult?.let { res ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (res.success) Color(0x2210B981) else Color(0x22EF4444),
                                border = BorderStroke(1.dp, if (res.success) BharatGreenLight else RoseError),
                                modifier = Modifier.fillMaxWidth().testTag("restore_result_card")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (res.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (res.success) BharatGreenLight else RoseError,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (res.success) "Restoration Complete!" else "Restore Failed",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (res.success) BharatGreenLight else RoseError
                                        )
                                    }

                                    if (res.success) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "✅ Restored ${res.chatsRestored} Chats, ${res.messagesRestored} Messages, and ${res.contactsRestored} Contacts.",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = bColors.textPrimary
                                        )
                                        Text(
                                            text = "Archive created: ${res.backupDateStr}",
                                            fontSize = 11.sp,
                                            color = bColors.textSecondary
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = res.errorMessage ?: "Unknown error while restoring backup",
                                            fontSize = 11.5.sp,
                                            color = RoseError
                                        )
                                    }
                                }
                            }
                        }

                        // Available On-Device Backups List
                        Text(
                            text = "Detected On-Device Backups (${availableBackups.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = bColors.textPrimary
                        )

                        if (availableBackups.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (bColors.isDark) Color(0x221E293B) else Color(0x1564748B),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = bColors.textMuted,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No local backups found in Documents folder yet.",
                                        fontSize = 11.5.sp,
                                        color = bColors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(availableBackups, key = { it.file.absolutePath }) { backup ->
                                    val isSelected = selectedFileToRestore?.file?.absolutePath == backup.file.absolutePath

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) BharatGreenLight.copy(alpha = 0.15f)
                                                else if (bColors.isDark) Color(0x221E293B) else Color(0x1564748B),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) BharatGreenLight else bColors.glassBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedFileToRestore = backup }
                                            .testTag("backup_item_${backup.name}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = if (isSelected) BharatGreenLight else BharatSaffron,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = backup.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = bColors.textPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${backup.lastModifiedFormatted} • ${backup.sizeFormatted}",
                                                        fontSize = 10.5.sp,
                                                        color = bColors.textSecondary
                                                    )
                                                }
                                            }

                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedFileToRestore = backup },
                                                colors = RadioButtonDefaults.colors(selectedColor = BharatGreenLight)
                                            )
                                        }
                                    }
                                }
                            }

                            // Restore Selected Action Button
                            Button(
                                onClick = {
                                    selectedFileToRestore?.let {
                                        viewModel.restoreBackupFromFile(it.file)
                                    }
                                },
                                enabled = selectedFileToRestore != null && !isRestoring,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_restore_selected_backup")
                            ) {
                                if (isRestoring) {
                                    CircularProgressIndicator(
                                        color = DarkBackground,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Restoring Chats & Contacts...", color = DarkBackground, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = null,
                                        tint = DarkBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedFileToRestore != null) "Restore Selected Backup" else "Select a Backup File Above",
                                        color = DarkBackground,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
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

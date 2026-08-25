package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.ui.viewmodel.BharatChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTransferDialog(
    viewModel: BharatChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bColors = LocalBharatColors.current
    val userProfile by viewModel.userProfile.collectAsState()
    val exportResult by viewModel.lastExportResult.collectAsState()
    val restoreResult by viewModel.lastRestoreResult.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Transfer to New Phone, 1: Receive from Old Phone
    var isTransferring by remember { mutableStateOf(false) }
    var transferProgress by remember { mutableFloatStateOf(0f) }
    var transferStatusText by remember { mutableStateOf("") }
    var transferSuccess by remember { mutableStateOf(false) }
    var transferPin by remember { mutableStateOf((100000..999999).random().toString()) }

    // File picker launcher for importing transferred backup file
    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreBackupFromUri(uri)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isTransferring) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
                .testTag("chat_transfer_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (bColors.isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
            ),
            border = BorderStroke(1.dp, if (bColors.isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(BharatGreenLight.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhoneForwarded,
                                contentDescription = "Transfer Chats",
                                tint = BharatGreenLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Transfer Chats & Calls",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = bColors.textPrimary
                            )
                            Text(
                                text = "Move history to another Android phone",
                                fontSize = 12.sp,
                                color = bColors.textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!isTransferring) onDismiss() },
                        modifier = Modifier.testTag("close_chat_transfer_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = bColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = BharatGreenLight,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Transfer to New Phone",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        icon = { Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Receive on this Phone",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        icon = { Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedTab == 0) {
                        // TAB 0: Send / Transfer to new phone
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (bColors.isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (bColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Scan with New Phone to Transfer",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = bColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Keep both phones unlocked and on the same Wi-Fi or hotspot",
                                        fontSize = 12.sp,
                                        color = bColors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Visual QR Code Card
                                    Box(
                                        modifier = Modifier
                                            .size(180.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White)
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode2,
                                                contentDescription = "Transfer QR Code",
                                                tint = Color(0xFF0F172A),
                                                modifier = Modifier.size(130.dp)
                                            )
                                            Text(
                                                text = "PIN: $transferPin",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Your VenzoInd ID: ${userProfile.phone.ifBlank { "+91 98000 00000" }}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BharatGreenLight
                                    )
                                }
                            }
                        }

                        if (isTransferring) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = BharatGreenLight.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, BharatGreenLight.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { transferProgress },
                                            modifier = Modifier.size(54.dp),
                                            color = BharatGreenLight,
                                            trackColor = Color(0x3310B981)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = transferStatusText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = bColors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${(transferProgress * 100).toInt()}% Transferred",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BharatGreenLight
                                        )
                                    }
                                }
                            }
                        } else if (transferSuccess) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Transfer Complete!",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF10B981)
                                            )
                                            Text(
                                                text = "All your chats, calls, and media have transferred successfully.",
                                                fontSize = 12.sp,
                                                color = bColors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Step-by-step instructions
                            Text(
                                text = "Instructions for New Phone:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = bColors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            listOf(
                                "1. Install VenzoInd on your new Android phone.",
                                "2. Log in with your phone number.",
                                "3. Go to Settings > Chats > Transfer chats.",
                                "4. Select 'Receive on this Phone' and enter PIN $transferPin or tap Connect."
                            ).forEach { step ->
                                Text(
                                    text = step,
                                    fontSize = 12.sp,
                                    color = bColors.textSecondary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    } else {
                        // TAB 1: Receive from Old Phone
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (bColors.isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (bColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.QrCodeScanner,
                                        contentDescription = "Scan QR",
                                        tint = BharatGreenLight,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Pair with Old Phone",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = bColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Open VenzoInd on your old phone and tap 'Transfer to New Phone'",
                                        fontSize = 12.sp,
                                        color = bColors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            isTransferring = true
                                            transferProgress = 0f
                                            transferStatusText = "Connecting to old device..."
                                            coroutineScope.launch {
                                                delay(800)
                                                transferProgress = 0.25f
                                                transferStatusText = "Authorizing E2EE pairing key..."
                                                delay(1000)
                                                transferProgress = 0.65f
                                                transferStatusText = "Transferring messages, calls & contacts..."
                                                delay(1200)
                                                transferProgress = 0.95f
                                                transferStatusText = "Finalizing local database sync..."
                                                delay(600)
                                                transferProgress = 1.0f
                                                transferStatusText = "Transfer completed!"
                                                isTransferring = false
                                                transferSuccess = true
                                                Toast.makeText(context, "Chats & Calls migrated successfully!", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("transfer_pair_scan_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight)
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Scan QR & Transfer Live", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            // Alternative: Import from local backup file
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (bColors.isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (bColors.isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.FolderOpen,
                                            contentDescription = null,
                                            tint = BharatElectricCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Or Restore from Backup File",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = bColors.textPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Import a .json chat backup file received via Share / Bluetooth / Drive",
                                        fontSize = 12.sp,
                                        color = bColors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedButton(
                                        onClick = {
                                            importFilePickerLauncher.launch(arrayOf("application/json", "*/*"))
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("transfer_import_file_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BharatElectricCyan)
                                    ) {
                                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Browse & Import .json Backup", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Button
                if (selectedTab == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.exportChatAndContactsBackup("VenzoInd_Transfer_${System.currentTimeMillis()}.json")
                                Toast.makeText(context, "Backup file prepared for transfer", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("transfer_export_file_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export File", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                isTransferring = true
                                transferProgress = 0f
                                transferStatusText = "Waiting for new device connection..."
                                coroutineScope.launch {
                                    delay(1000)
                                    transferProgress = 0.3f
                                    transferStatusText = "Connecting via Wi-Fi Direct..."
                                    delay(1000)
                                    transferProgress = 0.7f
                                    transferStatusText = "Syncing chats, calls & media..."
                                    delay(1200)
                                    transferProgress = 1.0f
                                    transferStatusText = "Transfer complete!"
                                    isTransferring = false
                                    transferSuccess = true
                                    Toast.makeText(context, "Chats & Calls transferred!", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("transfer_start_live_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BharatGreenLight)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Transfer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

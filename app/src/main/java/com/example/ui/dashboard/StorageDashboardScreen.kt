package com.example.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.DownloadedModelFile
import com.example.models.ModelItem
import com.example.models.ModelStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.ClaudeAmberWheat
import com.example.ui.theme.ClaudeCoral
import com.example.ui.theme.ClaudeCoralLight
import com.example.ui.theme.ClaudeOliveSage
import com.example.ui.theme.WarmErrorRed
import com.example.ui.theme.WarmSuccessGreen
import com.example.ui.theme.WarmWarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorageDashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageStats by viewModel.storageStats.collectAsState()
    val downloadedFiles by viewModel.downloadedFiles.collectAsState()
    val allModels by viewModel.modelManager.modelsState.collectAsState()
    val activeModelId by viewModel.modelManager.activeModelId.collectAsState()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<DownloadedModelFile?>(null) }

    val activeDownloads = allModels.filter { it.status is ModelStatus.Downloading }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Claude Top Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Storage & Download Dashboard",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Real-time storage size & download progress for offline models",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.refreshStorage()
                        Toast.makeText(context, "Storage metrics refreshed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("refresh_dashboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh stats",
                        tint = ClaudeCoral
                    )
                }
            }
        }

        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

        // 4 KPI Summary Cards Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClaudeKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Offline Models",
                    value = "${downloadedFiles.size}",
                    subtitle = "Stored locally on device",
                    icon = Icons.Default.Memory,
                    iconTint = ClaudeCoral,
                    testTag = "kpi_models_count"
                )

                ClaudeKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Models Storage",
                    value = "${"%.1f".format(storageStats.modelsUsageMb)} MB",
                    subtitle = if (storageStats.modelsUsageMb >= 1000f) "%.2f GB allocated".format(storageStats.modelsUsageMb / 1024f) else "Internal Sandbox",
                    icon = Icons.Default.Storage,
                    iconTint = ClaudeAmberWheat,
                    testTag = "kpi_models_storage"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClaudeKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Device Free Space",
                    value = "${"%.1f".format(storageStats.deviceFreeGb)} GB",
                    subtitle = "${"%.1f".format(storageStats.deviceTotalGb)} GB Total",
                    icon = Icons.Default.SdStorage,
                    iconTint = WarmSuccessGreen,
                    testTag = "kpi_free_space"
                )

                ClaudeKpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Downloads",
                    value = "${activeDownloads.size}",
                    subtitle = if (activeDownloads.isNotEmpty()) "Transferring weights" else "Queue Idle",
                    icon = Icons.Default.CloudDownload,
                    iconTint = if (activeDownloads.isNotEmpty()) WarmWarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    testTag = "kpi_active_downloads"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Download Progress Center
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("download_progress_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = if (activeDownloads.isNotEmpty()) ClaudeCoral else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download Progress Center",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    if (activeDownloads.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarmWarningAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "DOWNLOADING",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WarmWarningAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeDownloads.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        activeDownloads.forEach { item ->
                            val status = item.status as ModelStatus.Downloading
                            ClaudeDownloadProgressItem(
                                modelItem = item,
                                status = status,
                                onCancel = {
                                    viewModel.cancelDownload(item.info.id)
                                    Toast.makeText(context, "Cancelled ${item.info.name} download", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No downloads currently in progress",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "All selected offline model weights are stored locally on device.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val candidate = allModels.firstOrNull { it.status is ModelStatus.NotDownloaded }
                                    if (candidate != null) {
                                        viewModel.downloadModel(candidate.info.id)
                                        Toast.makeText(context, "Started downloading ${candidate.info.name}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.selectedTab.value = 2
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("start_test_download_button")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = ClaudeCoral)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Model Download Stream", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Storage Breakdown Bar Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Storage Allocation Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Relative disk space occupied by offline weights vs device storage",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                ClaudeStorageSegmentedBar(
                    downloadedFiles = downloadedFiles,
                    totalModelsBytes = storageStats.modelsUsageBytes
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    downloadedFiles.forEachIndexed { index, file ->
                        val color = getClaudeColor(index)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                if (file.isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(ACTIVE)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ClaudeCoral,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }

                            Text(
                                text = file.formattedSize,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Physical Files on Device
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = ClaudeCoral,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Model Files on Disk",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Text(
                        text = "${downloadedFiles.size} Stored",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ClaudeCoral,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = WarmSuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Path: ${storageStats.modelsDirectoryPath}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    downloadedFiles.forEach { file ->
                        ClaudeModelFileRow(
                            file = file,
                            onActivate = {
                                viewModel.activateModel(file.id)
                                Toast.makeText(context, "${file.name} activated for inference", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                fileToDelete = file
                            },
                            onCopyPath = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Model Path", file.filePath))
                                Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (downloadedFiles.any { !it.isEmbedded }) {
                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmErrorRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_models_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete All Downloaded Weights to Free Space")
                    }
                }
            }
        }
    }
}

    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Model File?") },
            text = { Text("Are you sure you want to delete \"${file.filename}\" (${file.formattedSize})? This will free storage on your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteModel(file.id)
                        fileToDelete = null
                        Toast.makeText(context, "Deleted ${file.name}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_delete_file_button")
                ) {
                    Text("Delete", color = WarmErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Downloaded Weights?") },
            text = { Text("This will delete all downloaded offline model weights from device storage. The pre-installed NanoLLM Core model will remain active.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllDownloadedModels()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "All downloaded weights cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_clear_all_button")
                ) {
                    Text("Clear All", color = WarmErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ClaudeKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    testTag: String
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    }
}

@Composable
fun ClaudeDownloadProgressItem(
    modelItem: ModelItem,
    status: ModelStatus.Downloading,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = modelItem.info.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${modelItem.info.parameterCount} • ${modelItem.info.quantization}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Download",
                        tint = WarmErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { status.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ClaudeCoral,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(status.progress * 100).toInt()}% • ${"%.1f".format(status.downloadedMb)} MB / ${"%.0f".format(status.totalMb)} MB",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = ClaudeCoral,
                        fontSize = 11.sp
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = ClaudeAmberWheat,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${"%.1f".format(status.speedMbps)} Mbps",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ClaudeStorageSegmentedBar(
    downloadedFiles: List<DownloadedModelFile>,
    totalModelsBytes: Long
) {
    if (downloadedFiles.isEmpty()) return

    val totalBytes = totalModelsBytes.coerceAtLeast(1L).toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        downloadedFiles.forEachIndexed { index, file ->
            val fraction = (file.sizeBytes.toFloat() / totalBytes).coerceIn(0.02f, 1f)
            val color = getClaudeColor(index)
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .height(14.dp)
                    .background(color)
            )
        }
    }
}

@Composable
fun ClaudeModelFileRow(
    file: DownloadedModelFile,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
    onCopyPath: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (file.isActive) androidx.compose.foundation.BorderStroke(1.dp, ClaudeCoral) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (file.isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ClaudeCoral.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ClaudeCoral,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${file.filename} • ${file.formattedSize}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCopyPath,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Path",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (!file.isEmbedded) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete File",
                            tint = WarmErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (!file.isActive) {
                    IconButton(
                        onClick = onActivate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Activate",
                            tint = ClaudeCoral,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getClaudeColor(index: Int): Color {
    val colors = listOf(ClaudeCoral, ClaudeAmberWheat, ClaudeOliveSage, ClaudeCoralLight, WarmSuccessGreen)
    return colors[index % colors.size]
}

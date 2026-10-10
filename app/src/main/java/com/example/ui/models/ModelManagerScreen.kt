package com.example.ui.models

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.ModelInfo
import com.example.models.ModelItem
import com.example.models.ModelStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val models by viewModel.modelManager.modelsState.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val activeModelId by viewModel.modelManager.activeModelId.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var modelToDelete by remember { mutableStateOf<ModelItem?>(null) }

    // SAF File Picker for custom GGUF / Bin models
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importModel(it)
            Toast.makeText(context, "Importing model into local engine...", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredModels = remember(models, selectedFilter) {
        when (selectedFilter) {
            "Downloaded" -> models.filter { it.status is ModelStatus.Downloaded || it.info.isEmbedded }
            "Compact (<1B)" -> models.filter {
                it.info.parameterCount.contains("M") ||
                it.info.parameterCount.contains("0.") ||
                it.info.isEmbedded
            }
            "High Accuracy (>2B)" -> models.filter {
                it.info.parameterCount.startsWith("2.") ||
                it.info.parameterCount.startsWith("3.")
            }
            else -> models
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Model Management",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Manage on-device quantized models for offline execution",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    // Import Custom Model button
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("import_model_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Import",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Storage Status Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Models Storage: ${"%.1f".format(storageStats.modelsUsageMb)} MB",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = "Device Free: ${"%.1f".format(storageStats.deviceFreeGb)} GB",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${models.count { it.status is ModelStatus.Downloaded || it.info.isEmbedded }} Available",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Downloaded", "Compact (<1B)", "High Accuracy (>2B)")
            filters.forEach { filter ->
                FilterChip(
                    selected = (selectedFilter == filter),
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_chip_$filter")
                )
            }
        }

        // Models List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredModels, key = { it.info.id }) { item ->
                ModelCard(
                    item = item,
                    isActive = item.isActive,
                    onActivate = {
                        viewModel.activateModel(item.info.id)
                        Toast.makeText(context, "${item.info.name} activated for inference", Toast.LENGTH_SHORT).show()
                    },
                    onDownload = {
                        viewModel.downloadModel(item.info.id)
                    },
                    onCancelDownload = {
                        viewModel.cancelDownload(item.info.id)
                    },
                    onRequestDelete = {
                        modelToDelete = item
                    }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    modelToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Delete Model Weights?") },
            text = {
                Text("This will remove the downloaded weights for \"${item.info.name}\" to free up storage. You can re-download it anytime.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteModel(item.info.id)
                        modelToDelete = null
                        Toast.makeText(context, "Model weights deleted", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelCard(
    item: ModelItem,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val info = item.info
    val status = item.status

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = ElectricCyan,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .testTag("model_card_${info.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Name & Active Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = info.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ElectricCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Specs badges row
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpecBadge(label = info.family)
                SpecBadge(label = info.parameterCount)
                SpecBadge(label = info.quantization)
                SpecBadge(label = "~${info.memoryFootprintMb} MB RAM")
                SpecBadge(label = "${info.contextLength / 1024}K Context")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description
            Text(
                text = info.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Actions & Status Bottom Row
            when (status) {
                is ModelStatus.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading: ${(status.progress * 100).toInt()}% • ${"%.1f".format(status.speedMbps)} Mbps",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            IconButton(
                                onClick = onCancelDownload,
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("cancel_download_${info.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Download",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { status.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ElectricCyan,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                is ModelStatus.Downloaded -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (info.isEmbedded) "Pre-installed" else "${"%.1f".format(status.fileSizeBytes / (1024f * 1024f))} MB on device",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = SuccessGreen,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!info.isEmbedded) {
                                IconButton(
                                    onClick = onRequestDelete,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("delete_model_${info.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete model",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            if (!isActive) {
                                Button(
                                    onClick = onActivate,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("activate_model_${info.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Activate", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                is ModelStatus.NotDownloaded -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Download size: ${info.downloadSizeMb} MB",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Button(
                            onClick = onDownload,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("download_model_${info.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download", fontSize = 12.sp)
                        }
                    }
                }

                is ModelStatus.Active -> {
                    // Handled above
                }
            }
        }
    }
}

@Composable
fun SpecBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

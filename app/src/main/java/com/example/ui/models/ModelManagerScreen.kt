package com.example.ui.models

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.material3.OutlinedTextField
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
import com.example.models.ModelSource
import com.example.models.ModelStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.ClaudeAmberWheat
import com.example.ui.theme.ClaudeCoral
import com.example.ui.theme.ClaudeCoralLight
import com.example.ui.theme.ClaudeOliveSage
import com.example.ui.theme.WarmErrorRed
import com.example.ui.theme.WarmSuccessGreen

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

    // Dialog state for pulling Ollama or downloading any HF/direct model URL
    var showOllamaDialog by remember { mutableStateOf(false) }
    var showUrlDownloadDialog by remember { mutableStateOf(false) }

    var ollamaTagInput by remember { mutableStateOf("") }
    var directUrlInput by remember { mutableStateOf("") }
    var directModelNameInput by remember { mutableStateOf("") }

    // SAF File Picker for local GGUF / Bin files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importModel(it)
            Toast.makeText(context, "Importing local model weights...", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredModels = remember(models, selectedFilter) {
        when (selectedFilter) {
            "Downloaded" -> models.filter { it.status is ModelStatus.Downloaded || it.info.isEmbedded }
            "Ollama" -> models.filter { it.info.source == ModelSource.OLLAMA }
            "Hugging Face" -> models.filter { it.info.source == ModelSource.HUGGING_FACE }
            "Reasoning (R1)" -> models.filter { it.info.family == "DeepSeek" || it.info.tags.contains("Reasoning") }
            "Compact (<1B)" -> models.filter {
                it.info.parameterCount.contains("M") ||
                it.info.parameterCount.contains("0.") ||
                it.info.isEmbedded
            }
            else -> models
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Claude-styled Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            text = "Browse, pull, and run any Ollama, Hugging Face, or GGUF model offline",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar: Pull Ollama, Download URL, Import File
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pull Ollama model button
                    Button(
                        onClick = { showOllamaDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ClaudeCoral),
                        modifier = Modifier.testTag("pull_ollama_button")
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pull Ollama Model", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Download from URL / Hugging Face button
                    OutlinedButton(
                        onClick = { showUrlDownloadDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("download_url_button")
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = ClaudeCoral)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Any URL / HF", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Import local file button
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("import_model_file_button")
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = ClaudeAmberWheat)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import .GGUF", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Storage summary banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = null,
                                tint = ClaudeCoral,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Models Storage: ${"%.1f".format(storageStats.modelsUsageMb)} MB",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Text(
                            text = "${"%.1f".format(storageStats.deviceFreeGb)} GB Free on Device",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Downloaded", "Ollama", "Hugging Face", "Reasoning (R1)", "Compact (<1B)")
            filters.forEach { filter ->
                FilterChip(
                    selected = (selectedFilter == filter),
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClaudeCoral.copy(alpha = 0.15f),
                        selectedLabelColor = ClaudeCoral,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = (selectedFilter == filter),
                        borderColor = if (selectedFilter == filter) ClaudeCoral else MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(16.dp),
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
                ClaudeModelCard(
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

    // Pull Ollama Dialog
    if (showOllamaDialog) {
        AlertDialog(
            onDismissRequest = { showOllamaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = ClaudeCoral)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pull from Ollama Library")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter any Ollama model tag or select from popular edge tags below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ollamaTagInput,
                        onValueChange = { ollamaTagInput = it },
                        label = { Text("e.g. deepseek-r1:1.5b, llama3.2:1b") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ollama_tag_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Popular Ollama Edge Models:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tags = listOf(
                            "deepseek-r1:1.5b",
                            "llama3.2:1b",
                            "llama3.2:3b",
                            "qwen2.5-coder:0.5b",
                            "phi3.5:mini",
                            "smollm2:135m",
                            "mistral:7b"
                        )
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.testTag("ollama_quick_tag_$tag")
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .clickable { ollamaTagInput = tag },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ClaudeCoral,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ollamaTagInput.isNotBlank()) {
                            viewModel.pullOllamaTag(ollamaTagInput)
                            Toast.makeText(context, "Pulling $ollamaTagInput into local storage...", Toast.LENGTH_SHORT).show()
                            showOllamaDialog = false
                            ollamaTagInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeCoral),
                    modifier = Modifier.testTag("confirm_pull_ollama_button")
                ) {
                    Text("Pull Model")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOllamaDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Download Any URL / Hugging Face Dialog
    if (showUrlDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDownloadDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = ClaudeCoral)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Any Model URL")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Paste any direct .gguf model URL or Hugging Face resolve link to download it natively for offline inference:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = directUrlInput,
                        onValueChange = { directUrlInput = it },
                        label = { Text("https://huggingface.co/.../model.gguf") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("direct_url_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = directModelNameInput,
                        onValueChange = { directModelNameInput = it },
                        label = { Text("Model Display Name (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("direct_model_name_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick Hugging Face Templates:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Qwen2.5-Coder-0.5B-Q4_K_M.gguf\n• Llama-3.2-1B-Instruct-Q4_K_M.gguf\n• DeepSeek-R1-Distill-1.5B-Q4_K_M.gguf",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (directUrlInput.isNotBlank()) {
                            viewModel.downloadFromDirectUrl(
                                url = directUrlInput,
                                customName = directModelNameInput.takeIf { it.isNotBlank() }
                            )
                            Toast.makeText(context, "Initiating model download from URL...", Toast.LENGTH_SHORT).show()
                            showUrlDownloadDialog = false
                            directUrlInput = ""
                            directModelNameInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClaudeCoral),
                    modifier = Modifier.testTag("confirm_download_url_button")
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDownloadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    modelToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text("Delete Model Weights?") },
            text = {
                Text("This will delete the local weights for \"${item.info.name}\" to free up storage space. You can re-download it anytime.")
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
                    Text("Delete", color = WarmErrorRed)
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
fun ClaudeModelCard(
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
                        color = ClaudeCoral,
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
        border = if (!isActive) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 2.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Name & Active Ribbon / Source badge
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
                        color = ClaudeCoral.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeCoral)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ClaudeCoral,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ClaudeCoral,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                } else {
                    // Source badge (Ollama / Hugging Face / Curated)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = when (info.source) {
                                ModelSource.OLLAMA -> "OLLAMA"
                                ModelSource.HUGGING_FACE -> "HUGGING FACE"
                                ModelSource.CUSTOM_URL -> "URL"
                                ModelSource.LOCAL_FILE -> "LOCAL"
                                else -> "EMBEDDED"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ClaudeAmberWheat,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
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
                ClaudeSpecBadge(label = info.family)
                ClaudeSpecBadge(label = info.parameterCount)
                ClaudeSpecBadge(label = info.quantization)
                ClaudeSpecBadge(label = "~${info.memoryFootprintMb} MB RAM")
                ClaudeSpecBadge(label = "${info.contextLength / 1024}K Context")
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
                                    color = ClaudeCoral,
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
                                    tint = WarmErrorRed,
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
                            color = ClaudeCoral,
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
                                color = WarmSuccessGreen,
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
                                        containerColor = ClaudeCoral
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
                            colors = ButtonDefaults.buttonColors(containerColor = ClaudeCoral),
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
fun ClaudeSpecBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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

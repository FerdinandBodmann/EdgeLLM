package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.MessageEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.ClaudeAmberWheat
import com.example.ui.theme.ClaudeCoral
import com.example.ui.theme.ClaudeCoralLight
import com.example.ui.theme.ClaudeOliveSage
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.WarmErrorRed
import com.example.ui.theme.WarmSuccessGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val promptInput by viewModel.promptInput.collectAsState()
    val selectedPersonaIndex by viewModel.selectedPersonaIndex.collectAsState()
    val allModels by viewModel.modelManager.modelsState.collectAsState()
    val activeModelId by viewModel.modelManager.activeModelId.collectAsState()
    val activeModel = viewModel.modelManager.getActiveModelInfo()

    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    var showModelPickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Redesigned Top Bar with Direct Model Selector
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Brand & Interactive Active Model Switcher
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ClaudeCoral.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ClaudeCoral,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Synapse",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.3.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Quick Model Switcher Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            ClaudeCoral.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .clickable { showModelPickerDialog = true }
                            .testTag("model_picker_trigger")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isStreaming) ClaudeCoral else WarmSuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = activeModel.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ClaudeCoral,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch model",
                                tint = ClaudeCoral,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Right Actions: New Chat & Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.createNewChat() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Conversation",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("chat_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Conversation") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = WarmErrorRed
                                    )
                                },
                                onClick = {
                                    viewModel.clearCurrentChat()
                                    showMenu = false
                                },
                                modifier = Modifier.testTag("clear_chat_menu_item")
                            )
                            DropdownMenuItem(
                                text = { Text("Switch AI Model") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = ClaudeCoral
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showModelPickerDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Persona Selection Capsule Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            viewModel.systemPersonas.forEachIndexed { index, (name, _) ->
                FilterChip(
                    selected = (selectedPersonaIndex == index),
                    onClick = { viewModel.selectPersona(index) },
                    label = {
                        Text(
                            name,
                            fontSize = 11.5.sp,
                            fontWeight = if (selectedPersonaIndex == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClaudeCoral.copy(alpha = 0.15f),
                        selectedLabelColor = ClaudeCoral,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = (selectedPersonaIndex == index),
                        borderColor = if (selectedPersonaIndex == index) ClaudeCoral else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("persona_chip_$index")
                )
            }
        }

        // Streaming progress bar
        AnimatedVisibility(visible = isStreaming) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = ClaudeCoral,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Messages List or Redesigned Hero Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                ClaudeEmptyPlaceholder(
                    activeModelName = activeModel.name,
                    onPromptSelected = { prompt ->
                        viewModel.promptInput.value = prompt
                        viewModel.sendMessage(prompt)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ClaudeMessageRow(
                            message = message,
                            isStreamingThis = isStreaming && (message == messages.lastOrNull()) && message.role == "assistant",
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Synapse Response", message.content))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteMessage(message.id)
                            }
                        )
                    }
                }
            }
        }

        // Redesigned Floating Bottom Composer Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // Unified Composer Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { viewModel.promptInput.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prompt_input_field"),
                            placeholder = {
                                Text(
                                    "Ask anything, check weather, solve math, write code...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        fontSize = 13.5.sp
                                    )
                                )
                            },
                            minLines = 1,
                            maxLines = 6,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp,
                                fontFamily = if (promptInput.contains("```") || promptInput.startsWith("fun ") || promptInput.startsWith("def ")) JetBrainsMono else FontFamily.Default
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent
                            )
                        )

                        // Action Tools inside the Composer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left quick helpers
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // One-Tap Insert Code Block Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            if (promptInput.isBlank()) {
                                                viewModel.promptInput.value = "```kotlin\n// Write or paste code here\n\n```"
                                            } else if (!promptInput.contains("```")) {
                                                viewModel.promptInput.value = "```kotlin\n$promptInput\n```"
                                            } else {
                                                viewModel.promptInput.value = promptInput + "\n\n```kotlin\n\n```"
                                            }
                                        }
                                        .testTag("insert_code_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = "Code snippet",
                                            tint = ClaudeCoral,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "{ } Code",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Quick Weather Helper
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.promptInput.value = "hows the weather in Haßloch Germany"
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = "Weather prompt",
                                            tint = ClaudeAmberWheat,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Weather",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                if (promptInput.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.promptInput.value = "" },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear input",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            // Right side: Send or Stop Button
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isStreaming) {
                                    IconButton(
                                        onClick = { viewModel.stopGeneration() },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(WarmErrorRed)
                                            .testTag("stop_generation_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Stop",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    val canSend = promptInput.isNotBlank()
                                    IconButton(
                                        onClick = {
                                            if (canSend) {
                                                viewModel.sendMessage(promptInput)
                                            }
                                        },
                                        enabled = canSend,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (canSend) ClaudeCoral
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .testTag("send_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Subtitle Telemetry & Privacy notice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp, start = 6.dp, end = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "100% On-Device Neural Compute • Zero Telemetry Leaves Phone",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                    Text(
                        text = "Airplane-Mode Safe",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = ClaudeAmberWheat,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }

    // Interactive Model Switcher Dialog
    if (showModelPickerDialog) {
        AlertDialog(
            onDismissRequest = { showModelPickerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = ClaudeCoral,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Select AI Model",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Choose an edge model to run locally on your device or Google Cloud AI:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    allModels.forEach { item ->
                        val isSelected = item.info.id == activeModelId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ClaudeCoral.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) ClaudeCoral else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.modelManager.setActiveModel(item.info.id)
                                    showModelPickerDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.modelManager.setActiveModel(item.info.id)
                                        showModelPickerDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = ClaudeCoral)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.info.name,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        if (item.info.isEmbedded) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ClaudeCoral.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = if (item.info.family == "Gemini") "Cloud AI" else "Embedded",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = ClaudeCoral,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${item.info.parameterCount} • ${item.info.quantization}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelPickerDialog = false }) {
                    Text("Close", color = ClaudeCoral)
                }
            }
        )
    }
}

@Composable
fun ClaudeMessageRow(
    message: MessageEntity,
    isStreamingThis: Boolean,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val isUser = message.role == "user"

    if (isUser) {
        // User message bubble in warm container
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ClaudeRenderFormattedContent(
                        text = message.content,
                        isStreamingThis = false
                    )
                }
            }
        }
    } else {
        // Assistant message: editorial, clean with asterisk logo like Claude
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                shape = CircleShape,
                color = ClaudeCoral.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ClaudeCoral,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.modelName.ifEmpty { "Synapse" },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    if (message.tokensPerSec > 0f) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ClaudeCoral.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${message.tokensPerSec} tok/s",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ClaudeCoral,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                SelectionContainer {
                    val contentToRender = if (message.content.isEmpty() && isStreamingThis) {
                        "Reasoning..."
                    } else {
                        message.content
                    }
                    ClaudeRenderFormattedContent(
                        text = contentToRender,
                        isStreamingThis = isStreamingThis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                ClaudeTelemetryRow(
                    tokensCount = message.tokensCount,
                    tokensPerSec = message.tokensPerSec,
                    ttftMs = message.timeToFirstTokenMs,
                    onCopy = onCopy,
                    onDelete = onDelete
                )
            }
        }
    }
}

/**
 * Editorial Markdown & Code formatter with collapsible DeepSeek <think> reasoning
 */
@Composable
fun ClaudeRenderFormattedContent(
    text: String,
    isStreamingThis: Boolean
) {
    var rawText = text
    var thoughtContent: String? = null

    // Extract DeepSeek reasoning tokens <think>...</think>
    if (rawText.contains("<think>")) {
        val thinkStart = rawText.indexOf("<think>")
        val thinkEnd = rawText.indexOf("</think>")
        if (thinkEnd != -1) {
            thoughtContent = rawText.substring(thinkStart + 7, thinkEnd).trim()
            rawText = rawText.substring(thinkEnd + 8).trimStart()
        } else {
            // Still streaming thinking phase
            thoughtContent = rawText.substring(thinkStart + 7).trim()
            rawText = ""
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // DeepSeek Chain-of-Thought collapsible card
        if (!thoughtContent.isNullOrBlank()) {
            var isThoughtExpanded by remember { mutableStateOf(isStreamingThis) }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    ClaudeAmberWheat.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isThoughtExpanded = !isThoughtExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = ClaudeAmberWheat,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isStreamingThis && rawText.isEmpty()) "Thinking..." else "Reasoning Process",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ClaudeAmberWheat,
                                    fontSize = 11.5.sp
                                )
                            )
                        }

                        Icon(
                            imageVector = if (isThoughtExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle thought",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isThoughtExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = thoughtContent,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        if (rawText.isNotEmpty()) {
            // Parse for Markdown code fences ```language\n...```
            val parts = remember(rawText) { splitCodeBlocks(rawText) }

            parts.forEach { part ->
                if (part.isCode) {
                    ClaudeCodeBlock(
                        code = part.content,
                        language = part.language,
                        isStreaming = isStreamingThis
                    )
                } else {
                    ClaudeRenderRichText(
                        content = part.content,
                        showCursor = isStreamingThis
                    )
                }
            }
        }
    }
}

data class ContentPart(val content: String, val isCode: Boolean, val language: String = "")

private fun splitCodeBlocks(raw: String): List<ContentPart> {
    val result = mutableListOf<ContentPart>()
    var cursor = 0

    while (cursor < raw.length) {
        val fenceStart = raw.indexOf("```", cursor)
        if (fenceStart == -1) {
            val remaining = raw.substring(cursor)
            if (remaining.isNotEmpty()) result.add(ContentPart(remaining, false))
            break
        }

        if (fenceStart > cursor) {
            result.add(ContentPart(raw.substring(cursor, fenceStart), false))
        }

        val langEnd = raw.indexOf('\n', fenceStart + 3)
        if (langEnd == -1) {
            // Code block not finished yet during streaming
            val code = raw.substring(fenceStart + 3)
            result.add(ContentPart(code, true, "code"))
            break
        }

        val language = raw.substring(fenceStart + 3, langEnd).trim().ifEmpty { "code" }
        val fenceEnd = raw.indexOf("```", langEnd + 1)

        if (fenceEnd == -1) {
            // Streaming in-progress code block
            val code = raw.substring(langEnd + 1)
            result.add(ContentPart(code, true, language))
            break
        } else {
            val code = raw.substring(langEnd + 1, fenceEnd)
            result.add(ContentPart(code, true, language))
            cursor = fenceEnd + 3
        }
    }
    return result
}

@Composable
fun ClaudeCodeBlock(
    code: String,
    language: String,
    isStreaming: Boolean
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181716), // Dark warm code surface
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38332D)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            // Header bar with language title & Copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF221F1C))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        color = ClaudeAmberWheat,
                        fontSize = 10.5.sp
                    )
                )

                Row(
                    modifier = Modifier
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code", code.trim()))
                            copied = true
                            Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) WarmSuccessGreen else Color(0xFFB5ADA2),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (copied) WarmSuccessGreen else Color(0xFFB5ADA2),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Syntax-highlighted code block with horizontal scroll
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = highlightSyntax(code, isStreaming),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

private fun highlightSyntax(code: String, isStreaming: Boolean): AnnotatedString {
    return buildAnnotatedString {
        val lines = code.lines()
        val keywordColor = Color(0xFFE08365) // Coral terracotta
        val stringColor = Color(0xFF98C379)  // Soft sage green
        val commentColor = Color(0xFF8A8275) // Muted warm gray
        val numberColor = Color(0xFFE5C07B)  // Soft warm gold
        val defaultColor = Color(0xFFF3EEE6) // Warm paper ivory

        val keywords = setOf(
            "val", "var", "fun", "class", "object", "interface", "def", "return",
            "if", "else", "when", "for", "while", "import", "package", "true",
            "false", "null", "None", "async", "await", "struct", "impl", "let",
            "mut", "const", "select", "from", "where", "group", "by", "order",
            "public", "private", "protected", "override", "typealias", "inline"
        )

        lines.forEachIndexed { lineIndex, line ->
            var i = 0
            val len = line.length

            while (i < len) {
                if ((line.startsWith("//", i) || line.startsWith("#", i)) && !line.startsWith("/*", i)) {
                    withStyle(SpanStyle(color = commentColor)) {
                        append(line.substring(i))
                    }
                    i = len
                    break
                }

                if (line[i] == '"') {
                    val endQuote = line.indexOf('"', i + 1)
                    if (endQuote != -1) {
                        withStyle(SpanStyle(color = stringColor)) {
                            append(line.substring(i, endQuote + 1))
                        }
                        i = endQuote + 1
                        continue
                    } else {
                        withStyle(SpanStyle(color = stringColor)) {
                            append(line.substring(i))
                        }
                        i = len
                        break
                    }
                }

                if (line[i] == '\'') {
                    val endQuote = line.indexOf('\'', i + 1)
                    if (endQuote != -1) {
                        withStyle(SpanStyle(color = stringColor)) {
                            append(line.substring(i, endQuote + 1))
                        }
                        i = endQuote + 1
                        continue
                    }
                }

                if (line[i].isLetter() || line[i] == '_') {
                    val startWord = i
                    while (i < len && (line[i].isLetterOrDigit() || line[i] == '_')) {
                        i++
                    }
                    val word = line.substring(startWord, i)
                    if (keywords.contains(word)) {
                        withStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold)) {
                            append(word)
                        }
                    } else {
                        withStyle(SpanStyle(color = defaultColor)) {
                            append(word)
                        }
                    }
                    continue
                }

                if (line[i].isDigit()) {
                    val startDigit = i
                    while (i < len && (line[i].isDigit() || line[i] == '.' || line[i] == 'f' || line[i] == 'L')) {
                        i++
                    }
                    withStyle(SpanStyle(color = numberColor)) {
                        append(line.substring(startDigit, i))
                    }
                    continue
                }

                withStyle(SpanStyle(color = defaultColor)) {
                    append(line[i].toString())
                }
                i++
            }

            if (lineIndex < lines.size - 1) {
                append("\n")
            }
        }

        if (isStreaming) {
            withStyle(SpanStyle(color = ClaudeCoral, fontWeight = FontWeight.Bold)) {
                append(" ▌")
            }
        }
    }
}

@Composable
fun ClaudeRenderRichText(
    content: String,
    showCursor: Boolean
) {
    val lines = remember(content) { content.lines() }

    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEachIndexed { index, rawLine ->
            val isLastLine = index == lines.size - 1
            val line = rawLine.trimEnd()

            when {
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### ").trim(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.5.sp
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# ").trim(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.5.sp
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                }
                line.startsWith("• ") || line.startsWith("* ") || line.startsWith("- ") -> {
                    val bulletText = line.substring(2).trim()
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ClaudeCoral,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        ClaudeInlineText(
                            text = bulletText,
                            appendCursor = showCursor && isLastLine
                        )
                    }
                }
                line.isNotBlank() -> {
                    ClaudeInlineText(
                        text = line,
                        appendCursor = showCursor && isLastLine,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun ClaudeInlineText(
    text: String,
    appendCursor: Boolean,
    modifier: Modifier = Modifier
) {
    val annotated = remember(text, appendCursor) {
        buildAnnotatedString {
            var i = 0
            val len = text.length

            while (i < len) {
                if (text[i] == '`') {
                    val endBacktick = text.indexOf('`', i + 1)
                    if (endBacktick != -1) {
                        val inlineCode = text.substring(i + 1, endBacktick)
                        withStyle(
                            SpanStyle(
                                fontFamily = JetBrainsMono,
                                background = Color(0x22C96846),
                                color = Color(0xFFE08365),
                                fontSize = 12.sp
                            )
                        ) {
                            append(" $inlineCode ")
                        }
                        i = endBacktick + 1
                        continue
                    }
                }

                if (text.startsWith("**", i)) {
                    val endBold = text.indexOf("**", i + 2)
                    if (endBold != -1) {
                        val boldText = text.substring(i + 2, endBold)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(boldText)
                        }
                        i = endBold + 2
                        continue
                    }
                }

                append(text[i].toString())
                i++
            }

            if (appendCursor) {
                withStyle(SpanStyle(color = ClaudeCoral, fontWeight = FontWeight.Bold)) {
                    append(" ▌")
                }
            }
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp
        ),
        modifier = modifier
    )
}

@Composable
fun ClaudeTelemetryRow(
    tokensCount: Int,
    tokensPerSec: Float,
    ttftMs: Long,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (tokensPerSec > 0f) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ClaudeCoral,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${tokensPerSec} tok/s",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            if (tokensCount > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${tokensCount} tokens",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            if (ttftMs > 0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${ttftMs}ms TTFT",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Gorgeous Hero Studio Empty State with banner art and instant prompt cards
 */
@Composable
fun ClaudeEmptyPlaceholder(
    activeModelName: String,
    onPromptSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Hero Banner Art with neural synapses
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeCoral.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_synapse_hero),
                    contentDescription = "Synapse Neural Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(18.dp))
                )

                // Overlay gradient with studio title
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xCC191816)
                                )
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(WarmSuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACTIVE: $activeModelName",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ClaudeCoralLight,
                                    fontSize = 10.5.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                        Text(
                            text = "Local Edge Intelligence",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }

        Text(
            text = "What would you like to explore?",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Instant weather, calculations, programming, or factual deep-dives:",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // High-value Prompt Suggestion Cards (Including weather in Haßloch Germany!)
        val suggestionCards = listOf(
            Triple(
                "🌤️ Weather in Haßloch, Germany",
                "Queries live Open-Meteo API & regional forecast",
                "hows the weather in haßloch germany"
            ),
            Triple(
                "⚡ Fast Arithmetic",
                "Solve: 25 * 14 or sqrt(144) or 15% of 250",
                "what is 25 * 14"
            ),
            Triple(
                "💻 Kotlin String Reversal",
                "Write idiomatic reverse string with two pointers",
                "Write a Kotlin function to reverse a string"
            ),
            Triple(
                "🪐 How Photosynthesis Works",
                "Chloroplast reactions, Calvin Cycle & chemical equations",
                "Explain how photosynthesis works in detail"
            ),
            Triple(
                "🇩🇪 Haßloch (Pfalz) Overview",
                "Holiday Park, market test hub, and municipality facts",
                "Tell me about Haßloch Germany and what it is known for"
            )
        )

        suggestionCards.forEach { (title, subtitle, prompt) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onPromptSelected(prompt) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ClaudeCoral,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

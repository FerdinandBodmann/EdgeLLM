package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MessageEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCodeBlockBackground
import com.example.ui.theme.GeminiDarkBackground
import com.example.ui.theme.GeminiDarkBorder
import com.example.ui.theme.GeminiDarkSurface
import com.example.ui.theme.GeminiDarkSurfaceElevated
import com.example.ui.theme.GeminiDarkSurfaceHighlight
import com.example.ui.theme.GeminiErrorRed
import com.example.ui.theme.GeminiLightBlue
import com.example.ui.theme.GeminiPurple
import com.example.ui.theme.GeminiSuccessGreen
import com.example.ui.theme.GeminiTextPrimary
import com.example.ui.theme.GeminiTextSecondary
import com.example.ui.theme.GeminiTextTertiary
import com.example.ui.theme.GeminiUserBubble
import com.example.ui.theme.GeminiWhite
import com.example.ui.theme.JetBrainsMono

@OptIn(ExperimentalMaterial3Api::class)
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
            .background(GeminiDarkBackground)
    ) {
        // Minimalist Claude Top App Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = GeminiDarkBackground,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Active Model Selector Dropdown Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GeminiDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        GeminiDarkBorder.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .clickable { showModelPickerDialog = true }
                        .testTag("model_picker_trigger")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = GeminiBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeModel.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = GeminiWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select model",
                            tint = GeminiTextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Right Actions: New Chat & Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.createNewChat() },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New chat",
                            tint = GeminiTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("chat_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = GeminiTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(GeminiDarkSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Chat history", color = GeminiTextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = GeminiBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.showChatHistory.value = true
                                },
                                modifier = Modifier.testTag("chat_history_menu_item")
                            )
                            DropdownMenuItem(
                                text = { Text("Export as Markdown", color = GeminiTextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = GeminiTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    shareAsText(
                                        context = context,
                                        text = viewModel.buildCurrentChatMarkdown(),
                                        subject = viewModel.currentChatTitle()
                                    )
                                },
                                modifier = Modifier.testTag("export_chat_menu_item")
                            )
                            DropdownMenuItem(
                                text = { Text("Clear chat", color = GeminiErrorRed) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = GeminiErrorRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    viewModel.clearCurrentChat()
                                    showMenu = false
                                },
                                modifier = Modifier.testTag("clear_chat_menu_item")
                            )
                            DropdownMenuItem(
                                text = { Text("Switch AI model", color = GeminiTextPrimary) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = GeminiBlue,
                                        modifier = Modifier.size(18.dp)
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

        // Persona Selection Chips in Gemini minimalist dark style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.systemPersonas.forEachIndexed { index, (name, _) ->
                val isSelected = selectedPersonaIndex == index
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) GeminiDarkSurfaceHighlight else GeminiDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) GeminiBlue.copy(alpha = 0.5f) else GeminiDarkBorder.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .clickable { viewModel.selectPersona(index) }
                        .testTag("persona_chip_$index")
                ) {
                    Text(
                        text = name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) GeminiWhite else GeminiTextSecondary
                        )
                    )
                }
            }
        }

        // Subtle streaming progress indicator line
        AnimatedVisibility(visible = isStreaming) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = GeminiBlue,
                trackColor = GeminiDarkSurface
            )
        }

        // Chat Conversation List or Gemini Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                ClaudeEmptyState(
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
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        GeminiMessageItem(
                            message = message,
                            isStreamingThis = isStreaming && (message == messages.lastOrNull()) && message.role == "assistant",
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Response", message.content))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteMessage(message.id)
                            },
                            onShare = {
                                shareAsText(context, message.content, "Synapse message")
                            },
                            onRegenerate = { viewModel.regenerateMessage(message.id) },
                            onEdit = { viewModel.startEditing(message.id) },
                            canRegenerate = !isStreaming && message.role == "assistant" && message == messages.lastOrNull()
                        )
                    }
                }
            }
        }
    }

    // Model Selector Dialog
    if (showModelPickerDialog) {
        AlertDialog(
            onDismissRequest = { showModelPickerDialog = false },
            containerColor = GeminiDarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GeminiBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Choose model",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = GeminiWhite
                        )
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
                    allModels.forEach { item ->
                        val isSelected = item.info.id == activeModelId
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) GeminiDarkSurfaceHighlight else GeminiDarkBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) GeminiBlue.copy(alpha = 0.5f) else GeminiDarkBorder.copy(alpha = 0.4f)
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
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.modelManager.setActiveModel(item.info.id)
                                        showModelPickerDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = GeminiBlue,
                                        unselectedColor = GeminiTextTertiary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.info.name,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = GeminiWhite
                                            )
                                        )
                                        if (item.info.isEmbedded) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = GeminiDarkSurfaceHighlight
                                            ) {
                                                Text(
                                                    text = if (item.info.family == "Gemini") "Cloud" else "On-Device",
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = GeminiBlue,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${item.info.parameterCount} • ${item.info.quantization}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.5.sp,
                                            color = GeminiTextTertiary
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
                    Text("Close", color = GeminiBlue)
                }
            }
        )
    }
}

/**
 * Clean Gemini Message Item:
 * - User: Clean dark rounded bubble on the right
 * - Gemini: Uncontained, spacious text directly on the dark canvas with sparkle logo
 */
@Composable
fun GeminiMessageItem(
    message: MessageEntity,
    isStreamingThis: Boolean,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    onEdit: () -> Unit = {},
    canRegenerate: Boolean = false
) {
    val isUser = message.role == "user"

    if (isUser) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GeminiUserBubble,
                modifier = Modifier.widthIn(max = 330.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = GeminiWhite,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit and resend",
                        tint = GeminiTextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = GeminiTextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    } else {
        // Gemini Response: Spacious, elegant open canvas
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp)
        ) {
            // Header with Gemini Sparkle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = GeminiBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.modelName.ifEmpty { "Synapse" },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = GeminiWhite,
                        fontSize = 13.sp
                    )
                )

                if (message.tokensPerSec > 0f) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${message.tokensPerSec} tok/s",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = GeminiTextTertiary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            SelectionContainer {
                val contentToRender = if (message.content.isEmpty() && isStreamingThis) {
                    "Thinking..."
                } else {
                    message.content
                }
                GeminiFormattedContent(
                    text = contentToRender,
                    isStreamingThis = isStreamingThis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: Copy, Delete, Telemetry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (message.timeToFirstTokenMs > 0L) {
                        Text(
                            text = "${message.timeToFirstTokenMs}ms TTFT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GeminiTextTertiary,
                                fontSize = 11.sp
                            )
                        )
                    }
                    if (message.tokensCount > 0) {
                        Text(
                            text = "• ${message.tokensCount} tokens",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = GeminiTextTertiary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canRegenerate) {
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("regenerate_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate",
                                tint = GeminiBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = GeminiTextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = GeminiTextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = GeminiTextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formats Gemini responses: Headings, Code fences, Lists, DeepSeek thinking cards
 */
@Composable
fun GeminiFormattedContent(
    text: String,
    isStreamingThis: Boolean
) {
    var rawText = text
    var thoughtContent: String? = null

    if (rawText.contains("<think>")) {
        val thinkStart = rawText.indexOf("<think>")
        val thinkEnd = rawText.indexOf("</think>")
        if (thinkEnd != -1) {
            thoughtContent = rawText.substring(thinkStart + 7, thinkEnd).trim()
            rawText = rawText.substring(thinkEnd + 8).trimStart()
        } else {
            thoughtContent = rawText.substring(thinkStart + 7).trim()
            rawText = ""
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!thoughtContent.isNullOrBlank()) {
            var isThoughtExpanded by remember { mutableStateOf(isStreamingThis) }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GeminiDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GeminiDarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
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
                                tint = GeminiBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chain of thought",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = GeminiBlue,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Icon(
                            imageVector = if (isThoughtExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = GeminiTextTertiary,
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
                                color = GeminiTextSecondary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        if (rawText.isNotEmpty()) {
            val parts = remember(rawText) { splitCodeBlocks(rawText) }

            parts.forEach { part ->
                if (part.isCode) {
                    GeminiCodeBlock(
                        code = part.content,
                        language = part.language,
                        isStreaming = isStreamingThis
                    )
                } else {
                    GeminiRichText(
                        content = part.content,
                        showCursor = isStreamingThis
                    )
                }
            }
        }
    }
}

data class CodePart(val content: String, val isCode: Boolean, val language: String = "")

private fun splitCodeBlocks(raw: String): List<CodePart> {
    val result = mutableListOf<CodePart>()
    var cursor = 0

    while (cursor < raw.length) {
        val fenceStart = raw.indexOf("```", cursor)
        if (fenceStart == -1) {
            val remaining = raw.substring(cursor)
            if (remaining.isNotEmpty()) result.add(CodePart(remaining, false))
            break
        }

        if (fenceStart > cursor) {
            result.add(CodePart(raw.substring(cursor, fenceStart), false))
        }

        val langEnd = raw.indexOf('\n', fenceStart + 3)
        if (langEnd == -1) {
            val code = raw.substring(fenceStart + 3)
            result.add(CodePart(code, true, "code"))
            break
        }

        val language = raw.substring(fenceStart + 3, langEnd).trim().ifEmpty { "code" }
        val fenceEnd = raw.indexOf("```", langEnd + 1)

        if (fenceEnd == -1) {
            val code = raw.substring(langEnd + 1)
            result.add(CodePart(code, true, language))
            break
        } else {
            val code = raw.substring(langEnd + 1, fenceEnd)
            result.add(CodePart(code, true, language))
            cursor = fenceEnd + 3
        }
    }
    return result
}

@Composable
fun GeminiCodeBlock(
    code: String,
    language: String,
    isStreaming: Boolean
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = GeminiCodeBlockBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, GeminiDarkBorder.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GeminiDarkSurface)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        color = GeminiTextSecondary,
                        fontSize = 11.sp
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
                        tint = if (copied) GeminiSuccessGreen else GeminiTextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (copied) GeminiSuccessGreen else GeminiTextTertiary,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = highlightCode(code, isStreaming),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

private fun highlightCode(code: String, isStreaming: Boolean): AnnotatedString {
    return buildAnnotatedString {
        val lines = code.lines()
        val keywordColor = Color(0xFFA8C7FA) // Gemini Blue
        val stringColor = Color(0xFFC4EED0)  // Soft Mint
        val commentColor = Color(0xFF757775) // Muted Grey
        val numberColor = Color(0xFFFFD563)  // Soft Warm Gold
        val defaultColor = Color(0xFFE3E3E3) // Crisp White

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
            withStyle(SpanStyle(color = GeminiBlue, fontWeight = FontWeight.Bold)) {
                append(" ▌")
            }
        }
    }
}

@Composable
fun GeminiRichText(
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
                            color = GeminiWhite,
                            fontSize = 15.5.sp
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GeminiWhite,
                            fontSize = 17.5.sp
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# ").trim(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = GeminiWhite,
                            fontSize = 19.5.sp
                        ),
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                }
                line.startsWith("• ") || line.startsWith("* ") || line.startsWith("- ") -> {
                    val bulletText = line.substring(2).trim()
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = GeminiBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        GeminiInlineText(
                            text = bulletText,
                            appendCursor = showCursor && isLastLine
                        )
                    }
                }
                line.isNotBlank() -> {
                    GeminiInlineText(
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
fun GeminiInlineText(
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
                                background = GeminiDarkSurfaceHighlight,
                                color = GeminiBlue,
                                fontSize = 13.sp
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
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = GeminiWhite)) {
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
                withStyle(SpanStyle(color = GeminiBlue, fontWeight = FontWeight.Bold)) {
                    append(" ▌")
                }
            }
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = GeminiTextPrimary,
            fontSize = 15.sp,
            lineHeight = 23.sp
        ),
        modifier = modifier
    )
}

/**
 * Shares arbitrary text via the system share sheet.
 */
private fun shareAsText(context: Context, text: String, subject: String = "Synapse") {
    if (text.isBlank()) {
        Toast.makeText(context, "Nothing to share yet", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

/**
 * Authentic Claude Warm Dark Empty State:
 * Editorial greeting with Claude star icon + warm prompt cards
 */
@Composable
fun ClaudeEmptyState(
    activeModelName: String,
    onPromptSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Claude Iconic Terracotta Star Icon
        Surface(
            shape = CircleShape,
            color = GeminiDarkSurfaceHighlight,
            border = androidx.compose.foundation.BorderStroke(1.dp, GeminiBlue.copy(alpha = 0.4f)),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = GeminiBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "How can I help you today?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = GeminiWhite,
                fontSize = 25.sp,
                letterSpacing = (-0.3).sp
            )
        )

        Text(
            text = "Offline Edge LLM • Private & fast on-device",
            style = MaterialTheme.typography.bodySmall.copy(
                color = GeminiTextTertiary,
                fontSize = 13.sp
            ),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(22.dp))

        // Clean Claude Prompt Cards
        val prompts = listOf(
            Pair("🌤️ Weather in Haßloch, Germany", "hows the weather in haßloch germany"),
            Pair("⚡ Fast Math: 25 * 14 or sqrt(144)", "what is 25 * 14"),
            Pair("💻 Reverse string in Kotlin", "Write a Kotlin function to reverse a string"),
            Pair("🌱 How photosynthesis works", "Explain how photosynthesis works in detail")
        )

        prompts.forEach { (label, query) ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = GeminiDarkSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    GeminiDarkBorder.copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onPromptSelected(query) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = GeminiWhite,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = GeminiBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MessageEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.ClaudeAmberWheat
import com.example.ui.theme.ClaudeCoral
import com.example.ui.theme.WarmErrorRed
import com.example.ui.theme.WarmSuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val activeModel = viewModel.modelManager.getActiveModelInfo()

    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }

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
        // Claude-styled Top Bar - Sleek & Compact for modern phone aspect ratios
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Claude-inspired Asterisk Icon
                    Surface(
                        shape = CircleShape,
                        color = ClaudeCoral.copy(alpha = 0.15f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ClaudeCoral,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Synapse",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ClaudeCoral.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeCoral.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(WarmSuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeModel.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ClaudeCoral,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

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
                        }
                    }
                }
            }
        }

        // Persona Selection Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            viewModel.systemPersonas.forEachIndexed { index, (name, _) ->
                FilterChip(
                    selected = (selectedPersonaIndex == index),
                    onClick = { viewModel.selectPersona(index) },
                    label = { Text(name, fontSize = 12.sp, fontWeight = if (selectedPersonaIndex == index) FontWeight.SemiBold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ClaudeCoral.copy(alpha = 0.15f),
                        selectedLabelColor = ClaudeCoral,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = (selectedPersonaIndex == index),
                        borderColor = if (selectedPersonaIndex == index) ClaudeCoral else MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("persona_chip_$index")
                )
            }
        }

        // Warm Terracotta Streaming progress bar indicator
        AnimatedVisibility(visible = isStreaming) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = ClaudeCoral,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Messages List or Empty State
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
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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

        // Bottom Input Bar with Dedicated Code Writing & Multi-line Support
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Unified Composer Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        // Multi-line Text Field that accommodates code comfortably
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { viewModel.promptInput.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prompt_input_field"),
                            placeholder = {
                                Text(
                                    "Message ${activeModel.name} or type code...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                )
                            },
                            minLines = 1,
                            maxLines = 8,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp,
                                fontFamily = if (promptInput.contains("```") || promptInput.startsWith("fun ") || promptInput.startsWith("def ")) FontFamily.Monospace else FontFamily.Default
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent
                            )
                        )

                        // Bottom Actions Row inside the Composer Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left helper buttons: Quick Code Block & Clear
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // One-Tap Insert Code Block Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = "Code snippet",
                                            tint = ClaudeCoral,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "{ } Code",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                if (promptInput.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.promptInput.value = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear input",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
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
                        .padding(top = 6.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Synapse runs 100% offline. Zero telemetry leaves device.",
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
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
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
                Text(
                    text = message.modelName.ifEmpty { "Synapse" },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

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

                if (message.content.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
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
}

/**
 * Data structures representing parsed segments of Markdown / Code content
 */
sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class Code(
        val language: String,
        val code: String,
        val isComplete: Boolean
    ) : MessageSegment()
}

/**
 * Robust parser that correctly handles in-progress streaming code blocks,
 * detects language identifiers without stealing non-language code lines,
 * and splits content into displayable segments.
 */
fun parseContentIntoSegments(rawText: String): List<MessageSegment> {
    if (!rawText.contains("```")) {
        return listOf(MessageSegment.Text(rawText))
    }

    val segments = mutableListOf<MessageSegment>()
    var cursor = 0
    val delimiter = "```"

    while (cursor < rawText.length) {
        val openIndex = rawText.indexOf(delimiter, cursor)
        if (openIndex == -1) {
            val trailingText = rawText.substring(cursor)
            if (trailingText.isNotEmpty()) {
                segments.add(MessageSegment.Text(trailingText))
            }
            break
        }

        // Text before the code block
        if (openIndex > cursor) {
            val precedingText = rawText.substring(cursor, openIndex)
            if (precedingText.isNotEmpty()) {
                segments.add(MessageSegment.Text(precedingText))
            }
        }

        val codeStart = openIndex + delimiter.length
        val closeIndex = rawText.indexOf(delimiter, codeStart)

        if (closeIndex == -1) {
            // Unclosed code block (currently streaming or typing)
            val codeBody = rawText.substring(codeStart)
            val (lang, code) = separateLangAndCode(codeBody)
            segments.add(MessageSegment.Code(language = lang, code = code, isComplete = false))
            break
        } else {
            // Fully closed code block
            val codeBody = rawText.substring(codeStart, closeIndex)
            val (lang, code) = separateLangAndCode(codeBody)
            segments.add(MessageSegment.Code(language = lang, code = code, isComplete = true))
            cursor = closeIndex + delimiter.length
        }
    }

    return segments
}

/**
 * Distinguishes an intentional language identifier from actual code.
 * Only treats the first line as a language tag if it is a single valid identifier
 * (e.g. "kotlin", "python", "json", "rust", "bash") without code punctuation or spaces.
 */
private fun separateLangAndCode(raw: String): Pair<String, String> {
    val trimmedLeading = raw.trimStart()
    if (!trimmedLeading.contains("\n")) {
        // Only one line exists so far (e.g. streaming just started)
        val singleLine = trimmedLeading.trim()
        val isTag = singleLine.matches(Regex("^[a-zA-Z0-9_+#.-]{1,20}$"))
        return if (isTag) {
            Pair(singleLine.lowercase(), "")
        } else {
            Pair("code", trimmedLeading)
        }
    }

    val firstLine = trimmedLeading.substringBefore("\n").trim()
    val isTag = firstLine.matches(Regex("^[a-zA-Z0-9_+#.-]{1,20}$"))
    return if (isTag) {
        val lang = firstLine.lowercase()
        val restOfCode = trimmedLeading.substringAfter("\n")
        Pair(lang, restOfCode)
    } else {
        // The first line was actual code! Do NOT drop it.
        Pair("code", raw)
    }
}

@Composable
fun ClaudeRenderFormattedContent(
    text: String,
    isStreamingThis: Boolean
) {
    val segments = remember(text) { parseContentIntoSegments(text) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        segments.forEachIndexed { index, segment ->
            val isLastSegment = index == segments.size - 1

            when (segment) {
                is MessageSegment.Code -> {
                    ClaudeCodeBlock(
                        language = segment.language,
                        code = segment.code,
                        isComplete = segment.isComplete,
                        isStreaming = isStreamingThis && isLastSegment,
                        onCopyCode = { codeToCopy ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Code snippet", codeToCopy))
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                is MessageSegment.Text -> {
                    ClaudeRenderRichText(
                        content = segment.content,
                        showCursor = isStreamingThis && isLastSegment
                    )
                }
            }
        }
    }
}

/**
 * Premium Dark Code Container with Horizontal Scroll, Syntax Highlighting,
 * Dedicated Copy Code button, and Streaming Indicator.
 */
@Composable
fun ClaudeCodeBlock(
    language: String,
    code: String,
    isComplete: Boolean,
    isStreaming: Boolean,
    onCopyCode: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    val formattedLanguage = remember(language) {
        when (language.lowercase()) {
            "kt", "kotlin" -> "KOTLIN"
            "py", "python" -> "PYTHON"
            "js", "javascript" -> "JAVASCRIPT"
            "ts", "typescript" -> "TYPESCRIPT"
            "rs", "rust" -> "RUST"
            "go", "golang" -> "GO"
            "sql" -> "SQL"
            "sh", "bash", "shell" -> "BASH"
            "json" -> "JSON"
            "xml", "html" -> "MARKUP"
            "cpp", "c++" -> "C++"
            "java" -> "JAVA"
            else -> language.uppercase().ifEmpty { "CODE" }
        }
    }

    val highlightedCode = remember(code, isStreaming) {
        highlightSyntax(code, isStreaming)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1D1A), // Deep warm charcoal
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38342E)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF262420))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = ClaudeAmberWheat,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedLanguage,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ClaudeAmberWheat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )

                    if (isStreaming && !isComplete) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(ClaudeCoral)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "streaming...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ClaudeCoral,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Copy Code Button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF33302A),
                    modifier = Modifier
                        .clickable {
                            onCopyCode(code)
                            isCopied = true
                            scope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        }
                        .testTag("copy_code_block_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (isCopied) WarmSuccessGreen else Color(0xFFD6D0C5),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCopied) "Copied!" else "Copy",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isCopied) WarmSuccessGreen else Color(0xFFD6D0C5)
                            )
                        )
                    }
                }
            }

            // Code Content with Horizontal Scroll to avoid breaking formatting
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = highlightedCode,
                    softWrap = false,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

/**
 * Lightweight and deterministic syntax highlighter using AnnotatedString
 */
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
                // Check for single line comments
                if ((line.startsWith("//", i) || line.startsWith("#", i)) && !line.startsWith("/*", i)) {
                    withStyle(SpanStyle(color = commentColor)) {
                        append(line.substring(i))
                    }
                    i = len
                    break
                }

                // Check for double quoted strings
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

                // Check for single quoted strings
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

                // Check for words / identifiers
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

                // Check for digits
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

                // Other characters
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

/**
 * Editorial Markdown renderer with support for Headings, Bullet Lists,
 * Bold typography, and Inline Code chips.
 */
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
                            fontSize = 14.5.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 3.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
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
                            fontSize = 18.sp
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
                    // Empty newline line
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

/**
 * Parses inline formatting: **bold** and `inline_code`
 */
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
                // Check for inline code `...`
                if (text[i] == '`') {
                    val endBacktick = text.indexOf('`', i + 1)
                    if (endBacktick != -1) {
                        val inlineCode = text.substring(i + 1, endBacktick)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
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

                // Check for bold **...**
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
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
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

@Composable
fun ClaudeEmptyPlaceholder(
    activeModelName: String,
    onPromptSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = CircleShape,
            color = ClaudeCoral.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ClaudeCoral.copy(alpha = 0.3f)),
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ClaudeCoral,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Synapse Neural Engine",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = "Active: $activeModelName • 100% On-Device Neural Compute",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        val promptSuggestions = listOf(
            "Write a Kotlin Coroutines Flow example with error handling",
            "Explain how on-device LLM quantization works",
            "Summarize the benefits of edge computing over cloud AI",
            "Write a Python script to compute moving averages"
        )

        promptSuggestions.forEach { prompt ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onPromptSelected(prompt) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ClaudeCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

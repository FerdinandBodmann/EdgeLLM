package com.example.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.ClaudeCoral
import com.example.ui.theme.GeminiDarkBackground
import com.example.ui.theme.GeminiDarkBorder
import com.example.ui.theme.GeminiDarkSurface
import com.example.ui.theme.GeminiDarkSurfaceHighlight
import com.example.ui.theme.GeminiErrorRed
import com.example.ui.theme.GeminiTextSecondary
import com.example.ui.theme.GeminiTextTertiary
import com.example.ui.theme.GeminiWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen conversation browser: search, pin, rename, delete and switch chats.
 */
@Composable
fun ChatHistoryScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chats by viewModel.allChats.collectAsState()
    val currentChatId by viewModel.currentChatId.collectAsState()

    var search by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ChatEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<ChatEntity?>(null) }

    val filtered = remember(chats, search) {
        if (search.isBlank()) chats
        else chats.filter { it.title.contains(search, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeminiDarkBackground)
    ) {
        // Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = GeminiDarkBackground
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Conversations",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GeminiWhite
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewModel.createNewChat()
                                onClose()
                            },
                            modifier = Modifier.size(36.dp).testTag("history_new_chat")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New chat", tint = ClaudeCoral)
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(36.dp).testTag("history_close")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = GeminiTextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().testTag("history_search_field"),
                    placeholder = { Text("Search conversations", color = GeminiTextTertiary, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = GeminiTextTertiary, modifier = Modifier.size(18.dp))
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (chats.isEmpty()) "No conversations yet" else "No matches for \"$search\"",
                    color = GeminiTextTertiary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.id }) { chat ->
                    ChatHistoryRow(
                        chat = chat,
                        isActive = chat.id == currentChatId,
                        onClick = {
                            viewModel.selectChat(chat.id)
                            onClose()
                        },
                        onTogglePin = { viewModel.toggleChatPinned(chat.id, !chat.isPinned) },
                        onRename = { renameTarget = chat },
                        onDelete = { deleteTarget = chat }
                    )
                }
            }
        }
    }

    renameTarget?.let { chat ->
        RenameChatDialog(
            initial = chat.title,
            onDismiss = { renameTarget = null },
            onConfirm = {
                viewModel.renameChat(chat.id, it)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { chat ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = GeminiDarkSurface,
            title = { Text("Delete conversation?", color = GeminiWhite) },
            text = {
                Text(
                    "\"${chat.title}\" and all of its messages will be permanently removed.",
                    color = GeminiTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteChatById(chat.id)
                    deleteTarget = null
                }) { Text("Delete", color = GeminiErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = GeminiTextSecondary) }
            }
        )
    }
}

@Composable
private fun ChatHistoryRow(
    chat: ChatEntity,
    isActive: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) GeminiDarkSurfaceHighlight else GeminiDarkSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) ClaudeCoral.copy(alpha = 0.5f) else GeminiDarkBorder.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_row_${chat.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (chat.isPinned) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = ClaudeCoral,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = chat.title.ifBlank { "Untitled" },
                        maxLines = 1,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                            color = GeminiWhite,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${chat.activeModelId} • ${timeFormat.format(Date(chat.lastMessageAt))}",
                    maxLines = 1,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                        color = GeminiTextTertiary,
                        fontSize = 10.5.sp
                    )
                )
            }

            IconButton(onClick = onTogglePin, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = if (chat.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (chat.isPinned) "Unpin" else "Pin",
                    tint = if (chat.isPinned) ClaudeCoral else GeminiTextTertiary,
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(onClick = onRename, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = GeminiTextTertiary, modifier = Modifier.size(15.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GeminiTextTertiary, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun RenameChatDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GeminiDarkSurface,
        title = { Text("Rename conversation", color = GeminiWhite) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("rename_chat_field"),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("Save", color = ClaudeCoral) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = GeminiTextSecondary) }
        }
    )
}
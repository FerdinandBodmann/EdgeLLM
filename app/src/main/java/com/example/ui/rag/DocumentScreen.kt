package com.example.ui.rag

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.ClaudeCoral
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiDarkBackground
import com.example.ui.theme.GeminiDarkBorder
import com.example.ui.theme.GeminiDarkSurface
import com.example.ui.theme.GeminiDarkSurfaceHighlight
import com.example.ui.theme.GeminiErrorRed
import com.example.ui.theme.GeminiTextSecondary
import com.example.ui.theme.GeminiTextTertiary
import com.example.ui.theme.GeminiWhite

/**
 * Local knowledge base ("chat with your documents") manager.
 * Imports files, shows what is indexed and toggles retrieval on/off.
 */
@Composable
fun DocumentScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.documents.collectAsState()
    val ragEnabled by viewModel.ragEnabled.collectAsState()
    val notice by viewModel.documentNotice.collectAsState()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importDocument(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GeminiDarkBackground)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            color = GeminiDarkBackground
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Local Knowledge",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GeminiWhite
                        )
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = GeminiTextSecondary)
                    }
                }
                Text(
                    text = "Import text, Markdown or PDF files. Everything is indexed and searched " +
                        "entirely on-device — your documents never leave the phone.",
                    style = MaterialTheme.typography.bodySmall.copy(color = GeminiTextTertiary, fontSize = 12.sp)
                )
            }
        }

        // Retrieval toggle
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = GeminiDarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ground answers in my documents",
                        style = MaterialTheme.typography.bodyMedium.copy(color = GeminiWhite)
                    )
                    Text(
                        text = "${documents.size} document(s) indexed",
                        style = MaterialTheme.typography.labelSmall.copy(color = GeminiTextTertiary, fontSize = 11.sp)
                    )
                }
                Switch(
                    checked = ragEnabled,
                    onCheckedChange = { viewModel.setRagEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = GeminiWhite, checkedTrackColor = GeminiBlue)
                )
            }
        }

        notice?.let { message ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GeminiDarkSurfaceHighlight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(color = GeminiTextSecondary, fontSize = 12.sp),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearDocumentNotice() }) {
                        Text("OK", color = ClaudeCoral)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { picker.launch(arrayOf("text/*", "application/pdf", "application/json", "*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = ClaudeCoral),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).testTag("import_document_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = GeminiWhite, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import document", color = GeminiWhite, fontWeight = FontWeight.SemiBold)
            }
            if (documents.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAllDocuments() }) {
                    Text("Clear all", color = GeminiErrorRed)
                }
            }
        }

        if (documents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = GeminiTextTertiary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No documents yet", color = GeminiTextSecondary)
                    Text(
                        "Import a file to start chatting with your own notes.",
                        color = GeminiTextTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(documents, key = { it.id }) { document ->
                    DocumentRow(document = document, onDelete = { viewModel.deleteDocument(document.id) })
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(document: DocumentEntity, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = GeminiDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, GeminiDarkBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().testTag("document_row_${document.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium.copy(color = GeminiWhite)
                )
                Text(
                    text = "${formatSize(document.sizeBytes)} • ${document.tokenEstimate} tokens • ${document.charCount} chars",
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall.copy(color = GeminiTextTertiary, fontSize = 10.5.sp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = GeminiTextTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "unknown size"
    val kb = bytes / 1024.0
    return when {
        kb < 1024 -> "%.0f KB".format(kb)
        else -> "%.1f MB".format(kb / 1024.0)
    }
}

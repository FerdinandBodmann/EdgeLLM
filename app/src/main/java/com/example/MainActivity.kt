package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.benchmark.BenchmarkScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.dashboard.StorageDashboardScreen
import com.example.ui.models.ModelManagerScreen
import com.example.ui.theme.ClaudeAmberWheat
import com.example.ui.theme.ClaudeCoral
import com.example.ui.theme.ClaudeCoralDark
import com.example.ui.theme.ClaudeDarkBackground
import com.example.ui.theme.ClaudeDarkBorder
import com.example.ui.theme.ClaudeDarkSurfaceElevated
import com.example.ui.theme.ClaudeDarkSurfaceHighlight
import com.example.ui.theme.ClaudeTextPrimary
import com.example.ui.theme.ClaudeTextSecondary
import com.example.ui.theme.ClaudeTextTertiary
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                SynapseApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SynapseApp(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler(enabled = selectedTab != 0) {
        viewModel.selectedTab.value = 0
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.modelManager.downloadMessages.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottom > 0
    val bottomInsets = WindowInsets.navigationBars.union(WindowInsets.ime)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClaudeDarkBackground)
    ) {
        // Main Screen Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(bottomInsets)
                .padding(
                    bottom = if (selectedTab == 0 && !isKeyboardOpen) 146.dp
                    else if (selectedTab == 0) 84.dp
                    else 76.dp
                )
        ) {
            when (selectedTab) {
                0 -> ChatScreen(viewModel = viewModel)
                1 -> StorageDashboardScreen(viewModel = viewModel)
                2 -> ModelManagerScreen(viewModel = viewModel)
                3 -> BenchmarkScreen(viewModel = viewModel)
            }
        }

        // ONE Unified Floating Bar: Integrates Text Input + Floating Navigation Dock
        // Always floats smoothly above keyboard or navigation bar without overlapping
        UnifiedFloatingBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { viewModel.selectedTab.value = it },
            viewModel = viewModel,
            isKeyboardOpen = isKeyboardOpen,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(bottomInsets)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

data class NavTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun UnifiedFloatingBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    viewModel: MainViewModel,
    isKeyboardOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavTabItem("Chat", Icons.Filled.AutoAwesome, Icons.Outlined.ChatBubbleOutline, "nav_tab_chat"),
        NavTabItem("Telemetry", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_tab_dashboard"),
        NavTabItem("Models", Icons.Filled.Memory, Icons.Outlined.Memory, "nav_tab_models"),
        NavTabItem("Benchmark", Icons.Filled.Speed, Icons.Outlined.Speed, "nav_tab_benchmark")
    )

    Surface(
        modifier = modifier
            .testTag("floating_nav_bar")
            .testTag("bottom_nav_bar"),
        shape = RoundedCornerShape(22.dp),
        color = ClaudeDarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            ClaudeDarkBorder.copy(alpha = 0.85f)
        ),
        shadowElevation = if (isKeyboardOpen) 4.dp else 12.dp,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isKeyboardOpen && selectedTab == 0) 2.dp else 4.dp)
        ) {
            // 1. Text Input Area (seamlessly unified in floating bar on Chat screen)
            if (selectedTab == 0) {
                ChatInputSection(
                    viewModel = viewModel,
                    isKeyboardOpen = isKeyboardOpen
                )

                if (!isKeyboardOpen) {
                    HorizontalDivider(
                        color = ClaudeDarkBorder.copy(alpha = 0.5f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                    )
                }
            }

            // 2. Navigation Tabs (smoothly collapses when keyboard is open)
            AnimatedVisibility(
                visible = !isKeyboardOpen || selectedTab != 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, item ->
                        val isSelected = selectedTab == index

                        Surface(
                            onClick = { onTabSelected(index) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) ClaudeDarkSurfaceHighlight else Color.Transparent,
                            interactionSource = remember { MutableInteractionSource() },
                            modifier = Modifier.testTag(item.testTag)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) ClaudeCoral else ClaudeTextTertiary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) ClaudeTextPrimary else ClaudeTextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputSection(
    viewModel: MainViewModel,
    isKeyboardOpen: Boolean
) {
    val promptInput by viewModel.promptInput.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = if (isKeyboardOpen) 4.dp else 6.dp
            )
    ) {
        // Claude-Style Text Input Field
        OutlinedTextField(
            value = promptInput,
            onValueChange = { viewModel.promptInput.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prompt_input_field"),
            placeholder = {
                Text(
                    "Ask anything...",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ClaudeTextTertiary,
                        fontSize = 14.5.sp
                    )
                )
            },
            minLines = 1,
            maxLines = if (isKeyboardOpen) 3 else 5,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = ClaudeTextPrimary,
                lineHeight = 21.sp,
                fontSize = 14.5.sp,
                fontFamily = if (promptInput.contains("```") || promptInput.startsWith("fun ") || promptInput.startsWith("def ")) JetBrainsMono else FontFamily.Default
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent
            )
        )

        // Shortcut Pills (Code, Weather) & Signature Claude Terracotta Send Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Helper buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ClaudeDarkSurfaceHighlight,
                    modifier = Modifier
                        .clickable {
                            if (promptInput.isBlank()) {
                                viewModel.promptInput.value = "```kotlin\n// Write code here\n\n```"
                            } else if (!promptInput.contains("```")) {
                                viewModel.promptInput.value = "```kotlin\n$promptInput\n```"
                            } else {
                                viewModel.promptInput.value = promptInput + "\n\n```kotlin\n\n```"
                            }
                        }
                        .testTag("insert_code_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Insert code",
                            tint = ClaudeCoral,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Code",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = ClaudeTextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ClaudeDarkSurfaceHighlight,
                    modifier = Modifier.clickable {
                        viewModel.promptInput.value = "hows the weather in haßloch germany"
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Weather",
                            tint = ClaudeAmberWheat,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Weather",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = ClaudeTextSecondary
                            )
                        )
                    }
                }

                if (promptInput.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.promptInput.value = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = ClaudeTextTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Claude Signature Terracotta Send / Stop button
            if (isStreaming) {
                IconButton(
                    onClick = { viewModel.stopGeneration() },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(ClaudeCoralDark)
                        .testTag("stop_generation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = ClaudeDarkBackground,
                        modifier = Modifier.size(16.dp)
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
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) ClaudeCoral
                            else ClaudeDarkSurfaceHighlight
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend) ClaudeTextPrimary else ClaudeTextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

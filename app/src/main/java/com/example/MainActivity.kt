package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.benchmark.BenchmarkScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.models.ModelManagerScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                EdgeLLMApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun EdgeLLMApp(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    // Handle back button to return to Chat tab if on sub-screens
    BackHandler(enabled = selectedTab != 0) {
        viewModel.selectedTab.value = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = (selectedTab == 0),
                    onClick = { viewModel.selectedTab.value = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Forum else Icons.Outlined.Forum,
                            contentDescription = "Chat"
                        )
                    },
                    label = { Text("Chat") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_chat")
                )

                NavigationBarItem(
                    selected = (selectedTab == 1),
                    onClick = { viewModel.selectedTab.value = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Memory else Icons.Outlined.Memory,
                            contentDescription = "Models"
                        )
                    },
                    label = { Text("Models") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_models")
                )

                NavigationBarItem(
                    selected = (selectedTab == 2),
                    onClick = { viewModel.selectedTab.value = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.Speed else Icons.Outlined.Speed,
                            contentDescription = "Benchmark & Settings"
                        )
                    },
                    label = { Text("Benchmark") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_benchmark")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChatScreen(viewModel = viewModel)
                1 -> ModelManagerScreen(viewModel = viewModel)
                2 -> BenchmarkScreen(viewModel = viewModel)
            }
        }
    }
}

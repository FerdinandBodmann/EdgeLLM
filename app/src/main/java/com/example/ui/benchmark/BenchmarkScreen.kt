package com.example.ui.benchmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inference.BenchmarkProgress
import com.example.inference.BenchmarkResult
import com.example.inference.ComputeBackend
import com.example.ui.MainViewModel
import com.example.ui.theme.ClaudeAmberWheat
import com.example.ui.theme.ClaudeCoral
import com.example.ui.theme.ClaudeCoralLight
import com.example.ui.theme.WarmErrorRed
import com.example.ui.theme.WarmSuccessGreen
import com.example.ui.theme.WarmWarningAmber

@Composable
fun BenchmarkScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeModel = viewModel.modelManager.getActiveModelInfo()
    val config by viewModel.inferenceConfig.collectAsState()
    val benchmarkProgress by viewModel.benchmarkProgress.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Benchmark & Engine Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = "Evaluate neural decode speed & tune execution parameters",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
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

        // Benchmark Section Card
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = ClaudeCoral,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "On-Device Inference Benchmark",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Target: ${activeModel.name} • 120 tokens synthetic stress test",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                when (val progress = benchmarkProgress) {
                    is BenchmarkProgress.Idle -> {
                        Button(
                            onClick = { viewModel.runBenchmark() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ClaudeCoral),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_benchmark_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Speed Stress Test", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    is BenchmarkProgress.Running -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Testing: ${progress.tokensCompleted}/${progress.totalTokens} tokens",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ClaudeCoral
                                    )
                                )
                                Text(
                                    text = "${progress.currentSpeedTokPerSec} tok/s",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ClaudeAmberWheat
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress.tokensCompleted.toFloat() / progress.totalTokens.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = ClaudeCoral,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.cancelBenchmark() },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmErrorRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel Benchmark")
                            }
                        }
                    }

                    is BenchmarkProgress.Completed -> {
                        ClaudeBenchmarkResultView(result = progress.result, onRerun = { viewModel.runBenchmark() })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hyperparameter Tuning Card
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = ClaudeCoral,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Inference Hyperparameters",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Compute Backend Selector
                Text(
                    text = "Hardware Accelerator Backend",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ComputeBackend.values().forEach { backend ->
                        FilterChip(
                            selected = (config.computeBackend == backend),
                            onClick = { viewModel.updateComputeBackend(backend) },
                            label = { Text(backend.tag.uppercase(), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ClaudeCoral.copy(alpha = 0.15f),
                                selectedLabelColor = ClaudeCoral,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = (config.computeBackend == backend),
                                borderColor = if (config.computeBackend == backend) ClaudeCoral else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.testTag("backend_chip_${backend.tag}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CPU Thread Count Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CPU Compute Threads",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "${config.threadCount} Cores",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ClaudeCoral
                        )
                    )
                }
                Slider(
                    value = config.threadCount.toFloat(),
                    onValueChange = { viewModel.updateThreadCount(it.toInt()) },
                    valueRange = 1f..8f,
                    steps = 6,
                    colors = SliderDefaults.colors(thumbColor = ClaudeCoral, activeTrackColor = ClaudeCoral),
                    modifier = Modifier.testTag("thread_count_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Temperature Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Temperature (Randomness)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "%.2f".format(config.temperature),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ClaudeCoral
                        )
                    )
                }
                Slider(
                    value = config.temperature,
                    onValueChange = { viewModel.updateTemperature(it) },
                    valueRange = 0.1f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = ClaudeCoral, activeTrackColor = ClaudeCoral),
                    modifier = Modifier.testTag("temperature_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Top-P Nucleus Sampling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Top-P (Nucleus Sampling)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "%.2f".format(config.topP),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ClaudeCoral
                        )
                    )
                }
                Slider(
                    value = config.topP,
                    onValueChange = { viewModel.updateTopP(it) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = ClaudeCoral, activeTrackColor = ClaudeCoral)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Max Tokens Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Max Tokens Per Generation",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "${config.maxTokens} tok",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ClaudeCoral
                        )
                    )
                }
                Slider(
                    value = config.maxTokens.toFloat(),
                    onValueChange = { viewModel.updateMaxTokens(it.toInt()) },
                    valueRange = 64f..2048f,
                    colors = SliderDefaults.colors(thumbColor = ClaudeCoral, activeTrackColor = ClaudeCoral)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // System Prompt text input
                Text(
                    text = "System Instruction Prompt",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = config.systemPrompt,
                    onValueChange = { viewModel.updateSystemPrompt(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("system_prompt_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Guarantee Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = WarmSuccessGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy & Offline Architecture",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• All inference runs strictly within local device RAM.\n" +
                           "• Zero network requests or telemetry emitted during generation.\n" +
                           "• Fully verified for complete Airplane Mode operation.\n" +
                           "• Models are stored in Android app private sandbox directory.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}
}

@Composable
fun ClaudeBenchmarkResultView(
    result: BenchmarkResult,
    onRerun: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stress Test Complete",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = WarmSuccessGreen
                )
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ClaudeCoral.copy(alpha = 0.2f)
            ) {
                Text(
                    text = result.performanceRating,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ClaudeCoral,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${result.tokensPerSecond}",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = ClaudeCoral
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "tokens / sec",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ClaudeBenchmarkStatCol(label = "Latency (TTFT)", value = "${result.timeToFirstTokenMs} ms")
            ClaudeBenchmarkStatCol(label = "Total Latency", value = "${"%.1f".format(result.totalDurationMs / 1000f)} s")
            ClaudeBenchmarkStatCol(label = "Peak Memory", value = "${result.peakRamMb} MB")
            ClaudeBenchmarkStatCol(label = "Hardware", value = result.computeBackend.take(7))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onRerun,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ClaudeCoral),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rerun_benchmark_button")
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Run Benchmark Again")
        }
    }
}

@Composable
fun ClaudeBenchmarkStatCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A persisted benchmark run, so performance can be compared across models,
 * devices and configurations over time.
 */
@Entity(tableName = "benchmark_runs")
data class BenchmarkRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modelName: String,
    val family: String = "",
    val parameterCount: String = "",
    val totalTokens: Int = 0,
    val timeToFirstTokenMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val tokensPerSecond: Float = 0f,
    val peakRamMb: Int = 0,
    val computeBackend: String = "",
    val performanceRating: String = "",
    val timestampMs: Long = System.currentTimeMillis()
)
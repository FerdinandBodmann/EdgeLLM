package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {
    @Insert
    suspend fun insertRun(run: BenchmarkRunEntity): Long

    @Query("SELECT * FROM benchmark_runs ORDER BY timestampMs DESC")
    fun getAllRuns(): Flow<List<BenchmarkRunEntity>>

    @Query("SELECT * FROM benchmark_runs ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecentRuns(limit: Int): Flow<List<BenchmarkRunEntity>>

    @Query("DELETE FROM benchmark_runs")
    suspend fun clearRuns()
}
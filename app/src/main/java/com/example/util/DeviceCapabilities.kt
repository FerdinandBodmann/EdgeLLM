package com.example.util

import android.app.ActivityManager
import android.content.Context

/**
 * Lightweight, dependency-free view of the device's memory budget.
 *
 * Used to keep model downloads and loads within what the phone can actually
 * handle, instead of letting the user queue a 40 GB checkpoint on a 4 GB phone.
 */
object DeviceCapabilities {

    data class MemoryInfo(
        val totalMb: Long,
        val availableMb: Long,
        val lowRamDevice: Boolean
    )

    fun memory(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(info)

        val totalMb = if (info.totalMem > 0) info.totalMem / (1024L * 1024L) else 0L
        val availableMb = info.availMem / (1024L * 1024L)
        return MemoryInfo(
            totalMb = totalMb,
            availableMb = availableMb,
            lowRamDevice = activityManager?.isLowRamDevice ?: false
        )
    }

    /** Free space in the app's private files directory, in megabytes. */
    fun freeStorageMb(context: Context): Long {
        val dir = context.filesDir
        return try {
            dir.usableSpace / (1024L * 1024L)
        } catch (t: Throwable) {
            0L
        }
    }

    /**
     * Heuristic: a model is comfortable when its footprint fits inside ~75% of
     * physical RAM, leaving room for the OS and the rest of the app.
     */
    fun fitsInMemory(context: Context, footprintMb: Int): Boolean {
        val total = memory(context).totalMb
        if (total <= 0L) return true
        return footprintMb.toLong() <= (total * 3) / 4
    }
}

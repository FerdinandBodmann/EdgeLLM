package com.example.inference.tools

import android.content.Context
import android.os.Build
import com.example.models.ModelInfo

/**
 * Reports device hardware/software capabilities fully offline.
 */
object DeviceInfoTool : ChatTool {

    override val name: String = "Device Info"

    override val description: String = "Device model, Android version, RAM and compute capabilities."

    private val triggers = listOf(
        "what device", "which phone", "what phone", "my phone", "phone model",
        "device model", "device info", "device information", "hardware",
        "how much ram", "device specs", "specifications", "specs of this device",
        "what android", "android version", "mein gerät", "welches handy"
    )

    override fun canHandle(prompt: String): Boolean {
        val lower = prompt.lowercase()
        return triggers.any { lower.contains(it) }
    }

    override suspend fun execute(
        prompt: String,
        model: ModelInfo,
        context: Context?
    ): ToolResult? {
        val manufacturer = Build.MANUFACTURER.capitalizeWord()
        val deviceModel = Build.MODEL
        val androidVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val abis = Build.SUPPORTED_ABIS.joinToString(", ")
        val totalRamMb = context?.let { totalRamMb(it) }

        val ramLine = if (totalRamMb != null) {
            "• **Total RAM:** ${totalRamMb} MB (${"%.2f".format(totalRamMb / 1024f)} GB)\n" +
                "• **Active model footprint:** ${model.memoryFootprintMb} MB" +
                if (model.memoryFootprintMb > totalRamMb) " ⚠️ exceeds device RAM\n" else " ✓ fits in device RAM\n"
        } else {
            "• **RAM:** unavailable\n"
        }

        val content = buildString {
            append("### Device Specifications\n\n")
            append("• **Device:** $manufacturer $deviceModel\n")
            append("• **Android:** $androidVersion\n")
            append("• **CPU ABIs:** $abis\n")
            append(ramLine)
            append("\n*(Reported offline from local system properties.)*")
        }
        return ToolResult(toolName = name, content = content)
    }

    private fun totalRamMb(context: Context): Long {
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        am?.getMemoryInfo(memoryInfo)
        return (memoryInfo.totalMem / (1024 * 1024))
    }

    private fun String.capitalizeWord(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecaseChar() else it }
}
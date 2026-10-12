package com.example.inference.tools

import android.content.Context
import com.example.models.ModelInfo

/**
 * Offline unit conversion tool: length, mass, temperature, speed and digital
 * storage. Supports prompts like "convert 15 km to miles" or "65 f in c".
 */
object UnitConverterTool : ChatTool {

    override val name: String = "Unit Converter"

    override val description: String =
        "Converts between units of length, mass, temperature, speed and data size — fully offline."

    private val lengthToMeters = mapOf(
        "km" to 1000.0, "kilometer" to 1000.0, "kilometers" to 1000.0,
        "m" to 1.0, "meter" to 1.0, "meters" to 1.0, "metre" to 1.0, "metres" to 1.0,
        "cm" to 0.01, "centimeter" to 0.01, "centimeters" to 0.01,
        "mm" to 0.001, "millimeter" to 0.001, "millimeters" to 0.001,
        "mi" to 1609.344, "mile" to 1609.344, "miles" to 1609.344,
        "ft" to 0.3048, "foot" to 0.3048, "feet" to 0.3048,
        "in" to 0.0254, "inch" to 0.0254, "inches" to 0.0254,
        "yard" to 0.9144, "yards" to 0.9144
    )

    private val massToKg = mapOf(
        "kg" to 1.0, "kilogram" to 1.0, "kilograms" to 1.0,
        "kilogramm" to 1.0,
        "g" to 0.001, "gram" to 0.001, "grams" to 0.001,
        "mg" to 1e-6, "milligram" to 1e-6, "milligrams" to 1e-6,
        "lb" to 0.45359237, "lbs" to 0.45359237, "pound" to 0.45359237, "pounds" to 0.45359237,
        "oz" to 0.028349523125, "ounce" to 0.028349523125, "ounces" to 0.028349523125
    )

    private val speedToKmh = mapOf(
        "kmh" to 1.0, "km/h" to 1.0, "kph" to 1.0,
        "mph" to 1.609344, "m/s" to 3.6, "ms" to 3.6,
        "knot" to 1.852, "knots" to 1.852
    )

    private val dataToMb = mapOf(
        "b" to 1e-6, "byte" to 1e-6, "bytes" to 1e-6,
        "kb" to 0.001, "kilobyte" to 0.001, "kilobytes" to 0.001,
        "mb" to 1.0, "megabyte" to 1.0, "megabytes" to 1.0,
        "gb" to 1024.0, "gigabyte" to 1024.0, "gigabytes" to 1024.0,
        "tb" to 1048576.0, "terabyte" to 1048576.0, "terabytes" to 1048576.0
    )

    private val temperatureUnits = listOf("c", "celsius", "f", "fahrenheit", "k", "kelvin")

    override fun canHandle(prompt: String): Boolean {
        val lower = prompt.lowercase()
        if (lower.contains("convert") || lower.contains("umrechnen")) return true
        // "15 km in miles", "65 f in c", "2 hours to minutes" (only singular keyword-based)
        return hasConversionPattern(lower)
    }

    private fun hasConversionPattern(lower: String): Boolean {
        val conversions = lengthToMeters.keys + massToKg.keys + speedToKmh.keys +
            dataToMb.keys + temperatureUnits
        val hasFrom = conversions.any { lower.contains(it) }
        val hasTo = lower.contains(" in ") || lower.contains(" to ") || lower.contains(" als ") ||
            lower.contains(" in ") || conversions.any { lower.endsWith(it) }
        return hasFrom && hasTo
    }

    override suspend fun execute(
        prompt: String,
        model: ModelInfo,
        context: Context?
    ): ToolResult? {
        val lower = prompt.lowercase()

        // Extract a number and the "from/to" unit phrases.
        val valueMatch = Regex("([0-9]+(?:[.,][0-9]+)?)").find(lower)
        val value = valueMatch?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: return null

        val afterValue = lower.substring(valueMatch!!.range.last + 1)

        // We may have text before the number ("convert") — find the two unit words after the value.
        val unitWords = Regex("[a-z][a-z0-9/]*").findAll(afterValue).map { it.value }.toList()

        if (unitWords.size < 2) return null

        // Temperature special case: "65 f in c" -> unitWords = [f, in, c]
        val candidatePairs = buildList {
            for (i in 0 until unitWords.size - 1) {
                add(unitWords[i] to unitWords[i + 1])
            }
        }

        for (unit1 in unitWords) {
            for (unit2 in unitWords) {
                if (unit1 == unit2) continue
                val converted = convert(value, unit1, unit2)
                if (converted != null) {
                    return ToolResult(
                        toolName = name,
                        content = "### Unit Conversion\n\n" +
                            "• **Input:** $value ${displayUnit(unit1)}\n" +
                            "• **Result:** **${formatResult(converted)} ${displayUnit(unit2)}**\n\n" +
                            "*(Computed fully offline.)*"
                    )
                }
            }
        }

        // Sometimes the units are written as "km to miles": try the whole afterValue.
        for ((u1, u2) in candidatePairs) {
            val converted = convert(value, u1, u2)
            if (converted != null) {
                return ToolResult(
                    toolName = name,
                    content = "### Unit Conversion\n\n" +
                        "• **Input:** $value ${displayUnit(u1)}\n" +
                        "• **Result:** **${formatResult(converted)} ${displayUnit(u2)}**\n\n" +
                        "*(Computed fully offline.)*"
                )
            }
        }

        return null
    }

    private fun convert(value: Double, fromRaw: String, toRaw: String): Double? {
        val from = normalizeUnit(fromRaw)
        val to = normalizeUnit(toRaw)

        // Temperature: direct formulas.
        if (from in temperatureUnits && to in temperatureUnits) {
            return convertTemperature(value, from, to)
        }

        val lengthFrom = lengthToMeters[from]
        val lengthTo = lengthToMeters[to]
        if (lengthFrom != null && lengthTo != null) {
            return value * lengthFrom / lengthTo
        }

        val massFrom = massToKg[from]
        val massTo = massToKg[to]
        if (massFrom != null && massTo != null) {
            return value * massFrom / massTo
        }

        val speedFrom = speedToKmh[from]
        val speedTo = speedToKmh[to]
        if (speedFrom != null && speedTo != null) {
            return value * speedFrom / speedTo
        }

        val dataFrom = dataToMb[from]
        val dataTo = dataToMb[to]
        if (dataFrom != null && dataTo != null) {
            return value * dataFrom / dataTo
        }

        return null
    }

    private fun normalizeUnit(raw: String): String {
        val u = raw.lowercase().trim().replace(" ", "").replace("\u00a0", "")
        // NOTE: "in" must NOT be stripped here — it is a valid unit (inches).
        // The parser handles the English preposition "in" by skipping tokens
        // that resolve to no known unit.
        return u
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        // Normalize Celsius/Fahrenheit/Kelvin.
        fun toCelsius(v: Double, unit: String): Double = when (unit) {
            "c", "celsius" -> v
            "f", "fahrenheit" -> (v - 32.0) * 5.0 / 9.0
            "k", "kelvin" -> v - 273.15
            else -> v
        }

        val celsius = toCelsius(value, from)
        return when (to) {
            "c", "celsius" -> celsius
            "f", "fahrenheit" -> celsius * 9.0 / 5.0 + 32.0
            "k", "kelvin" -> celsius + 273.15
            else -> celsius
        }
    }

    private fun displayUnit(unit: String): String = when (normalizeUnit(unit)) {
        "km" -> "km"
        "kilometer", "kilometers" -> "kilometers"
        "m", "meter", "meters", "metre", "metres" -> "meters"
        "cm", "centimeter", "centimeters" -> "cm"
        "mm", "millimeter", "millimeters" -> "mm"
        "mi", "mile", "miles" -> "miles"
        "ft", "foot", "feet" -> "feet"
        "in", "inch", "inches" -> "inches"
        "yard", "yards" -> "yards"
        "kg", "kilogram", "kilograms", "kilogramm" -> "kg"
        "g", "gram", "grams" -> "grams"
        "lb", "lbs", "pound", "pounds" -> "pounds"
        "oz", "ounce", "ounces" -> "ounces"
        "kmh", "km/h", "kph" -> "km/h"
        "mph" -> "mph"
        "m/s", "ms" -> "m/s"
        "knot", "knots" -> "knots"
        "gb", "gigabyte", "gigabytes" -> "GB"
        "mb", "megabyte", "megabytes" -> "MB"
        "kb", "kilobyte", "kilobytes" -> "KB"
        "tb", "terabyte", "terabytes" -> "TB"
        "c", "celsius" -> "°C"
        "f", "fahrenheit" -> "°F"
        "k", "kelvin" -> "K"
        else -> unit
    }

    private fun formatResult(v: Double): String = MathTool.formatNumber(v)
}
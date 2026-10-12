package com.example.inference.tools

import android.content.Context
import com.example.models.ModelInfo

/**
 * Deterministic math solver: percentages, square roots, order-of-operations
 * arithmetic and exponentiation.
 */
object MathTool : ChatTool {

    override val name: String = "Math Solver"

    override val description: String =
        "Solves arithmetic, percentages, square roots and exponentiation with formatted results."

    override fun canHandle(prompt: String): Boolean = solve(prompt) != null

    override suspend fun execute(
        prompt: String,
        model: ModelInfo,
        context: Context?
    ): ToolResult? {
        val solved = solve(prompt) ?: return null
        return ToolResult(toolName = name, content = solved)
    }

    private fun solve(prompt: String): String? {
        val lower = prompt.lowercase()

        // Percentage: "what is 15% of 250" or "15% of 250"
        val pctRegex = Regex("(?:what is\\s+)?([0-9.]+)\\s*%\\s*of\\s*([0-9.]+)", RegexOption.IGNORE_CASE)
        pctRegex.find(lower)?.let { match ->
            val p = match.groupValues[1].toDoubleOrNull()
            val base = match.groupValues[2].toDoubleOrNull()
            if (p != null && base != null) {
                val res = (p / 100.0) * base
                return buildString {
                    append("### Calculation: $p% of $base\n\n")
                    append("• **Formula:** `($p / 100) * $base`\n")
                    append("• **Result:** **${formatNumber(res)}**\n")
                }
            }
        }

        // Square root: "sqrt(144)" or "square root of 144"
        val sqrtRegex = Regex("(?:sqrt|square root of)\\s*\\(?([0-9.]+)\\)?", RegexOption.IGNORE_CASE)
        sqrtRegex.find(lower)?.let { match ->
            val num = match.groupValues[1].toDoubleOrNull()
            if (num != null && num >= 0) {
                val res = Math.sqrt(num)
                return buildString {
                    append("### Calculation: √$num\n\n")
                    append("• **Result:** **${formatNumber(res)}**\n")
                }
            }
        }

        // Multi-expression order of operations: "2 + 3 * 4" -> 14
        evaluableExpression(lower)?.let { expr ->
            val result = evaluate(expr)
            if (result != null) {
                return buildString {
                    append("### Calculation: $expr\n\n")
                    append("• **Result:** **${formatNumber(result)}**\n")
                }
            }
        }

        // Simple arithmetic: "25 * 14", "100 + 45", "1250 / 5", "50 - 18", "2^10"
        val arithRegex = Regex("([0-9.]+)\\s*([\\+\\-\\*/^])\\s*([0-9.]+)")
        arithRegex.find(lower)?.let { match ->
            val a = match.groupValues[1].toDoubleOrNull()
            val op = match.groupValues[2]
            val b = match.groupValues[3].toDoubleOrNull()
            if (a != null && b != null) {
                val result = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b != 0.0) a / b else null
                    "^" -> Math.pow(a, b)
                    else -> null
                }
                if (result != null) {
                    return buildString {
                        append("### Calculation: $a $op $b\n\n")
                        append("• **Result:** **${formatNumber(result)}**\n")
                    }
                }
            }
        }

        return null
    }

    /** Picks the longest arithmetic expression string that satisfies [evaluate]. */
    private fun evaluableExpression(lower: String): String? {
        val tokens = Regex("[0-9.]+|[\\+\\-\\*/^()]").findAll(lower).map { it.value }.toList()
        if (tokens.size <= 1) return null

        // Try progressively shorter token windows (longest first).
        for (window in tokens.size downTo 3) {
            for (start in 0..tokens.size - window) {
                val windowTokens = tokens.subList(start, start + window)
                val candidate = windowTokens.joinToString(" ")
                if (evaluate(candidate) != null) {
                    return candidate
                }
            }
        }
        return null
    }

    /**
     * Evaluates a simple expression using proper precedence.
     * Returns null when the expression is not fully evaluable.
     */
    private fun evaluate(expr: String): Double? {
        val tokens = expr.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        // Parentheses (innermost first), then ^, then */, then +-
        val chars = expr.replace(" ", "")
        val withoutParens = evaluateWithoutParens(chars) ?: return null
        return withoutParens
    }

    /** Recursive-descent parser is overkill; handle precedence with a shunting-yard-lite pass. */
    private fun evaluateWithoutParens(expr: String): Double? {
        val tokens = tokenizeExpression(expr) ?: return null
        return evaluateTokens(tokens)
    }

    private fun tokenizeExpression(expr: String): List<String>? {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    tokens.add(expr.substring(start, i))
                }
                c in "+-*/^()" -> {
                    tokens.add(c.toString())
                    i++
                }
                else -> {
                    // Unsupported character -> not a clean math expression.
                    return null
                }
            }
        }
        if (tokens.isEmpty()) return null
        // Replace the "what is" / "calculate" text prefix issues: the expression must start with a number or '('.
        val first = tokens.first()
        if (first != "(" && first.toDoubleOrNull() == null) return null
        return tokens
    }

    private fun evaluateTokens(tokens: List<String>): Double? {
        // Parentheses resolution.
        var list = tokens.toMutableList()
        while ("(" in list) {
            val open = list.indexOfLast { it == "(" }
            var close = -1
            for (idx in open + 1 until list.size) {
                if (list[idx] == ")") {
                    close = idx
                    break
                }
            }
            if (close == -1) return null
            val inner = list.subList(open + 1, close).toList()
            if (inner.isEmpty()) return null
            val innerValue = evaluateLinear(inner) ?: return null
            list = (list.subList(0, open) + listOf(innerValue.toString()) + list.subList(close + 1, list.size)).toMutableList()
        }
        return evaluateLinear(list)
    }

    private fun evaluateLinear(tokens: List<String>): Double? {
        var values = tokens.toMutableList()
        // 1. exponentiation (right associative)
        values = applyBinary(values, listOf("^")) ?: return null
        // 2. multiplication / division
        values = applyBinary(values, listOf("*", "/")) ?: return null
        // 3. addition / subtraction
        values = applyBinary(values, listOf("+", "-")) ?: return null

        if (values.size != 1) return null
        return values[0].toDoubleOrNull()
    }

    private fun applyBinary(tokens: List<String>, operators: List<String>): MutableList<String>? {
        val result = tokens.toMutableList()
        var i = 1
        while (i < result.size - 1) {
            val op = result[i]
            if (op in operators) {
                val a = result[i - 1].toDoubleOrNull() ?: return null
                val b = result[i + 1].toDoubleOrNull() ?: return null
                val value = when (op) {
                    "^" -> Math.pow(a, b)
                    "*" -> a * b
                    "/" -> if (b != 0.0) a / b else return null
                    "+" -> a + b
                    "-" -> a - b
                    else -> return null
                }
                result[i - 1] = value.toString()
                result.removeAt(i)
                result.removeAt(i)
                i = 1
            } else {
                i++
            }
        }
        return result
    }

    fun formatNumber(num: Double): String {
        return if (num == num.toLong().toDouble()) {
            num.toLong().toString()
        } else {
            "%.4f".format(num).trimEnd('0').trimEnd('.')
        }
    }
}
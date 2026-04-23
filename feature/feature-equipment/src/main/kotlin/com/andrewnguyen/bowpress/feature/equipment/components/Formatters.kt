package com.andrewnguyen.bowpress.feature.equipment.components

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Display helpers — 1:1 with the private formatters in iOS `BowConfigEditView.swift`
 * and `BowConfigDetailView.swift`. Keep them as plain top-level functions so both
 * Composables and unit tests can use them without pulling in Compose runtime.
 */

/** `"+3/16""` / `"-5/16""` / `"0/16""`. Matches iOS `sixteenthLabel(_:)`. */
fun sixteenthLabel(n: Int): String {
    if (n == 0) return "0/16\""
    val sign = if (n > 0) "+" else "-"
    return "$sign${abs(n)}/16\""
}

/** `"+1.5 twists"` / `"0 twists"`. `n` is encoded in half-twist units. */
fun halfTwistLabel(n: Int): String {
    if (n == 0) return "0 twists"
    val twists = n / 2.0
    val formatted = if (twists == twists.roundToInt().toDouble()) {
        "%.0f".format(twists)
    } else {
        stripTrailingZeros(twists)
    }
    val plural = if (abs(twists) == 1.0) "" else "s"
    val prefix = if (n > 0) "+" else ""
    return "$prefix$formatted twist$plural"
}

/** `"2 turns in"` / `"0.5 turn out"` / `"0 turns"`. Positive = in, negative = out. */
fun limbTurnsLabel(turns: Double): String {
    if (turns == 0.0) return "0 turns"
    val absVal = abs(turns)
    val direction = if (turns < 0) "out" else "in"
    val formatted = if (absVal == absVal.roundToInt().toDouble()) {
        "%.0f".format(absVal)
    } else {
        "%.1f".format(absVal)
    }
    val plural = if (absVal == 1.0) "" else "s"
    return "$formatted turn$plural $direction"
}

/** `"+3"` / `"-2"` / `"0 (baseline)"`. Mirrors iOS edit-view sight position. */
fun sightPositionLabel(n: Int): String {
    if (n == 0) return "0 (baseline)"
    val prefix = if (n > 0) "+" else ""
    return "$prefix$n"
}

/** `"+2.5 mm"` / `"-3.0 mm"`. Always signed, one decimal. */
fun tillerLabel(mm: Double): String = "%+.1f mm".format(mm)

/** `"+3 mm"` / `"-5 mm"`. Always signed, integer mm — matches iOS clicker display. */
fun clickerLabel(mm: Double): String = "%+.0f mm".format(mm)

/**
 * Strip trailing zeros — approximates Swift's `%g`. Java's built-in `%g` expands
 * to scientific notation / six significant figures, neither of which matches
 * Swift's compact display, so we roll our own.
 */
fun formatG(v: Double): String {
    val s = v.toString()
    return if ('.' in s) s.trimEnd('0').trimEnd('.') else s
}

private fun stripTrailingZeros(v: Double): String = formatG(v)

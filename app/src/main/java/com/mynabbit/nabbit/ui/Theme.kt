package com.mynabbit.nabbit.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** An sRGB color that knows which text color reads best on top of it. */
data class ThemeColor(val hex: Long) {
    private val red get() = ((hex shr 16) and 0xFF) / 255.0
    private val green get() = ((hex shr 8) and 0xFF) / 255.0
    private val blue get() = (hex and 0xFF) / 255.0

    val color: Color get() = Color(0xFF000000 or hex)

    /** WCAG relative luminance. */
    val luminance: Double
        get() {
            fun linear(c: Double) = if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
            return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)
        }

    fun contrast(other: ThemeColor): Double {
        val a = luminance
        val b = other.luminance
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    /** Warm near-black or warm off-white, whichever contrasts better. */
    val text: ThemeColor get() = if (contrast(INK) >= contrast(PAPER)) INK else PAPER

    companion object {
        val INK = ThemeColor(0x1E1A16)
        val PAPER = ThemeColor(0xFFF6EE)
    }
}

/** Five themes, no more. Every color clears 4.5:1 against its automatic text color. */
data class NabbitTheme(
    val id: String,
    val name: String,
    /** Sticky-note colors for the verb, thing, and when steps. */
    val tap1: ThemeColor,
    val tap2: ThemeColor,
    val tap3: ThemeColor,
    /** Full-screen alert background. */
    val alert: ThemeColor,
    /** Completion celebration. */
    val done: ThemeColor,
) {
    companion object {
        val primary = NabbitTheme("primary", "Primary", ThemeColor(0xFFC93C), ThemeColor(0x3D7BFF),
            ThemeColor(0xFF5A5F), ThemeColor(0x14213D), ThemeColor(0x2EC4B6))
        val midnight = NabbitTheme("midnight", "Midnight", ThemeColor(0x26334D), ThemeColor(0x5B2A86),
            ThemeColor(0x0F5257), ThemeColor(0xFF5E3A), ThemeColor(0xFFD23F))
        val slate = NabbitTheme("slate", "Slate", ThemeColor(0xB5D4F4), ThemeColor(0xA9B8C6),
            ThemeColor(0xD3D1C7), ThemeColor(0x0C447C), ThemeColor(0xEF9F27))
        // Berry's alert is lifted a shade from the brief's #D4537E, which fell just short of 4.5:1.
        val berry = NabbitTheme("berry", "Berry", ThemeColor(0xF4C0D1), ThemeColor(0xED93B1),
            ThemeColor(0xF0997B), ThemeColor(0xD55681), ThemeColor(0xFAC775))
        val retro = NabbitTheme("retro", "Retro", ThemeColor(0xF4D35E), ThemeColor(0x0D3B66),
            ThemeColor(0xEE964B), ThemeColor(0xB8341B), ThemeColor(0xF4D35E))

        val all = listOf(primary, midnight, slate, berry, retro)

        /** Preselected on first launch, and the fallback for a theme that's no longer offered. */
        val standard = primary

        fun named(id: String): NabbitTheme = all.firstOrNull { it.id == id } ?: standard
    }
}

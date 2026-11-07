package com.mynabbit.nabbit.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.mynabbit.nabbit.copy.Tone
import com.mynabbit.nabbit.model.NudgeTiming
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How a reminder's first alert arrives. */
enum class AlertStyle(val title: String) {
    /** Rings on the alarm volume, even on silent, with a full-screen alert. */
    ALARM("Alarm"),

    /** A notification that follows the phone's sound, vibrate, and Do Not Disturb settings. */
    NOTIFICATION("Notification"),
}

/** The verb grid: added verbs, the order, and the hidden ones. */
@Serializable
data class VerbLayout(
    val added: List<String> = emptyList(),
    val order: List<String> = emptyList(),
    val hidden: List<String> = emptyList(),
)

/**
 * Every setting, each a StateFlow so screens redraw when it changes. Changes apply the
 * moment they're made. No Save button.
 */
object Prefs {
    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    private val _themeId = MutableStateFlow("primary")
    val themeId: StateFlow<String> = _themeId
    private val _tone = MutableStateFlow(Tone.PLAIN)
    val tone: StateFlow<Tone> = _tone
    private val _morningHour = MutableStateFlow(9)
    val morningHour: StateFlow<Int> = _morningHour
    private val _tonightHour = MutableStateFlow(19)
    val tonightHour: StateFlow<Int> = _tonightHour
    private val _alertStyle = MutableStateFlow(AlertStyle.ALARM)
    val alertStyle: StateFlow<AlertStyle> = _alertStyle
    private val _nudgeMinutes = MutableStateFlow(NudgeTiming.DEFAULT_MINUTES)
    val nudgeMinutes: StateFlow<Int> = _nudgeMinutes
    private val _themeIntroShown = MutableStateFlow(false)
    val themeIntroShown: StateFlow<Boolean> = _themeIntroShown
    private val _alertsIntroShown = MutableStateFlow(false)
    val alertsIntroShown: StateFlow<Boolean> = _alertsIntroShown
    private val _verbLayout = MutableStateFlow(VerbLayout())
    val verbLayout: StateFlow<VerbLayout> = _verbLayout

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("nabbit", Context.MODE_PRIVATE)
        _themeId.value = prefs.getString("theme", "primary") ?: "primary"
        _tone.value = Tone.entries.firstOrNull { it.name == prefs.getString("tone", null) } ?: Tone.PLAIN
        _morningHour.value = prefs.getInt("morningHour", 9)
        _tonightHour.value = prefs.getInt("tonightHour", 19)
        _alertStyle.value = AlertStyle.entries.firstOrNull { it.name == prefs.getString("alertStyle", null) }
            ?: AlertStyle.ALARM
        _nudgeMinutes.value = prefs.getInt("nudgeMinutes", NudgeTiming.DEFAULT_MINUTES)
        _themeIntroShown.value = prefs.getBoolean("themeIntroShown", false)
        _alertsIntroShown.value = prefs.getBoolean("alertsIntroShown", false)
        _verbLayout.value = prefs.getString("verbLayout", null)
            ?.let { runCatching { json.decodeFromString<VerbLayout>(it) }.getOrNull() } ?: VerbLayout()
    }

    fun setTheme(id: String) = set(_themeId, id) { putString("theme", it) }
    fun setTone(tone: Tone) = set(_tone, tone) { putString("tone", it.name) }
    fun setMorningHour(hour: Int) = set(_morningHour, hour) { putInt("morningHour", it) }
    fun setTonightHour(hour: Int) = set(_tonightHour, hour) { putInt("tonightHour", it) }
    fun setAlertStyle(style: AlertStyle) = set(_alertStyle, style) { putString("alertStyle", it.name) }
    fun setNudgeMinutes(minutes: Int) = set(_nudgeMinutes, minutes) { putInt("nudgeMinutes", it) }
    fun setThemeIntroShown() = set(_themeIntroShown, true) { putBoolean("themeIntroShown", it) }
    fun setAlertsIntroShown() = set(_alertsIntroShown, true) { putBoolean("alertsIntroShown", it) }
    fun setVerbLayout(layout: VerbLayout) =
        set(_verbLayout, layout) { putString("verbLayout", json.encodeToString(VerbLayout.serializer(), it)) }

    private fun <T> set(flow: MutableStateFlow<T>, value: T, write: SharedPreferences.Editor.(T) -> Unit) {
        flow.value = value
        prefs.edit { write(value) }
    }
}

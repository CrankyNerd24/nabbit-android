package com.mynabbit.nabbit.ui

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mynabbit.nabbit.BuildConfig
import com.mynabbit.nabbit.copy.Tone
import com.mynabbit.nabbit.data.AlertStyle
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.data.VerbStore
import com.mynabbit.nabbit.model.NudgeTiming
import com.mynabbit.nabbit.model.Times

/** Every change applies the moment it's made. No Save button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(theme: NabbitTheme, onRequestNotifications: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val themeId by Prefs.themeId.collectAsState()
    val tone by Prefs.tone.collectAsState()
    val morning by Prefs.morningHour.collectAsState()
    val tonight by Prefs.tonightHour.collectAsState()
    val style by Prefs.alertStyle.collectAsState()
    val nudge by Prefs.nudgeMinutes.collectAsState()
    val layout by Prefs.verbLayout.collectAsState()

    Scaffold(
        containerColor = ThemeColor.PAPER.color,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeColor.PAPER.color),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Group("Theme") {
                ThemeSwatches(themeId) { Prefs.setTheme(it) }
            }

            // From the collected state, so the chips redraw the moment the tone changes.
            val shownTone = tone.takeIf { it.offered } ?: Tone.PLAIN
            Group("Tone", footer = shownTone.sample +
                "\nSalty and Sweary need an age check, coming in a later version.") {
                Choices(Tone.entries.filter { it.offered }, shownTone, { it.title }) { Prefs.setTone(it) }
            }

            Group(
                "Default times",
                footer = "Morning and Tonight mean whatever they mean to you. Up at 6? Make Morning 6 AM. " +
                    "Tonight is when you're home and can actually do it. Tomorrow AM, Next Monday, and " +
                    "Pick day & time start at your Morning time; Tonight uses your Tonight time.",
            ) {
                HourRow("Morning", morning, 5..11) { Prefs.setMorningHour(it) }
                HourRow("Tonight", tonight, 16..23) { Prefs.setTonightHour(it) }
            }

            Group(
                "Alerts",
                footer = when (style) {
                    AlertStyle.ALARM -> "The first alert rings like an alarm clock, even on silent and in Do Not Disturb."
                    AlertStyle.NOTIFICATION -> "The first alert is a notification, like a text. It follows your phone's " +
                        "settings: on silent it stays quiet or just vibrates, and Do Not Disturb holds it."
                } + " Nudges are always notifications, so they follow your sound, vibrate, and Do Not Disturb settings.",
            ) {
                Choices(AlertStyle.entries, style, { it.title }) {
                    Prefs.setAlertStyle(it)
                    com.mynabbit.nabbit.alerts.Scheduler.reschedule(context)
                }
            }

            Group("Nudges", footer = "If it's not done, Nabbit nudges you ${NudgeTiming.COUNT} more times, then goes quiet.") {
                Text("Nudge every", fontWeight = FontWeight.SemiBold)
                Choices(NudgeTiming.choices, NudgeTiming.minutes.let { if (nudge in NudgeTiming.choices) nudge else it },
                    { "$it min" }) {
                    Prefs.setNudgeMinutes(it)
                    com.mynabbit.nabbit.alerts.Scheduler.reschedule(context)
                }
            }

            Group("Ring on time", footer = "Android needs these on for Nabbit to ring at the minute it's due.") {
                RingOnTimeChecklist(onRequestNotifications)
            }

            val hidden = VerbStore.hidden(layout)
            if (hidden.isNotEmpty()) {
                Group("Hidden verbs", footer = "Tap one to put it back on the grid.") {
                    hidden.forEach { verb ->
                        OutlinedButton(onClick = { VerbStore.setHidden(verb, false) }) { Text("Show ${verb.title}") }
                    }
                }
            }

            Group("Help") {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://mynabbit.com/support.html".toUri()))
                }) { Text("Help and FAQ") }
            }

            Text(
                "Nabbit ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = ThemeColor.INK.color.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun Group(title: String, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.semantics { heading() })
        content()
        if (footer != null) Text(footer, fontSize = 14.sp, color = ThemeColor.INK.color.copy(alpha = 0.7f))
    }
}

/** A row of choices; the picked one is filled. Wraps onto more lines at large text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Choices(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onPick(option) },
                label = { Text(label(option), fontSize = 16.sp) },
                leadingIcon = if (option == selected) {
                    { Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun HourRow(label: String, hour: Int, range: IntRange, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 17.sp, modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { open = true }) { Text(Times.hourLabel(hour)) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                range.forEach { h ->
                    DropdownMenuItem(text = { Text(Times.hourLabel(h)) }, onClick = { open = false; onPick(h) })
                }
            }
        }
    }
}

/** One row of swatches. The pick applies instantly. */
@Composable
fun ThemeSwatches(selectedId: String, size: Int = 48, onPick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NabbitTheme.all.forEach { theme ->
            val picked = theme.id == selectedId
            Box(
                Modifier
                    .size(size.dp)
                    .border(if (picked) 3.dp else 1.dp, ThemeColor.INK.color, CircleShape)
                    .padding(4.dp)
                    .background(theme.tap1.color, CircleShape)
                    .clickable(role = Role.RadioButton) { onPick(theme.id) }
                    .semantics { contentDescription = theme.name; selected = picked },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size((size / 3).dp).background(theme.alert.color, CircleShape))
            }
        }
    }
}

/** Notifications, exact alarms, and full-screen alerts: what's on, and a button for what isn't. */
@Composable
fun RingOnTimeChecklist(onRequestNotifications: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    key(refresh) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PermissionRow("Notifications", "So alerts and nudges can show up.", Permissions.notificationsOn(context)) {
                onRequestNotifications()
            }
            PermissionRow("Alarms & reminders", "So Nabbit rings at the minute it's due, not a few minutes late.",
                Permissions.exactAlarmsOn(context)) { Permissions.openExactAlarmSettings(context) }
            PermissionRow("Full-screen alerts", "So the alarm can fill the screen, even when it's locked.",
                Permissions.fullScreenOn(context)) { Permissions.openFullScreenSettings(context) }
        }
    }
}

@Composable
private fun PermissionRow(title: String, detail: String, on: Boolean, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(ThemeColor(0xFFFFFF).color, RoundedCornerShape(10.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (on) Icons.Filled.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = if (on) "On" else "Off",
            tint = if (on) ThemeColor(0x1E7F5C).color else ThemeColor(0xB8341B).color,
        )
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 14.sp)
        }
        if (!on) Button(onClick = onFix) { Text("Turn on") }
    }
}

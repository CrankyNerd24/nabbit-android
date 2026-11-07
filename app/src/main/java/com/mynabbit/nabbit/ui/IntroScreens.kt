package com.mynabbit.nabbit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynabbit.nabbit.alerts.Scheduler
import com.mynabbit.nabbit.data.AlertStyle
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.model.NudgeTiming
import com.mynabbit.nabbit.model.Times
import androidx.compose.ui.platform.LocalContext

/** Shown once at first launch: pick colors, see them on real notes, start. */
@Composable
fun ThemeIntro(onStart: () -> Unit) {
    val themeId by Prefs.themeId.collectAsState()
    val theme = NabbitTheme.named(themeId)
    Column(
        Modifier.fillMaxSize().background(ThemeColor.PAPER.color).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Pick your colors", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Text("Not sure? Pick one. You can change it any time in Settings.", fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StickyNote("Call", theme.tap1, Modifier.weight(1f), compact = true) {}
            StickyNote("Dentist", theme.tap2, Modifier.weight(1f), compact = true) {}
            StickyNote("Tonight", theme.tap3, Modifier.weight(1f), compact = true) {}
        }
        ThemeSwatches(themeId, size = 54) { Prefs.setTheme(it) }
        Text(theme.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        BigButton("Start", theme, onStart)
    }
}

/**
 * Shown once, after the theme pick: how a reminder gets your attention. Tapping a card
 * sets the first alert's style, like Settings > Alerts. Also covers the switches
 * Android needs on to ring on time.
 */
@Composable
fun AlertsIntro(onRequestNotifications: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val theme = NabbitTheme.named(Prefs.themeId.collectAsState().value)
    val style by Prefs.alertStyle.collectAsState()
    val morning by Prefs.morningHour.collectAsState()
    val tonight by Prefs.tonightHour.collectAsState()
    Column(
        Modifier.fillMaxSize().background(ThemeColor.PAPER.color).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("How Nabbit gets you", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Text("Pick how the first alert arrives. Change it any time in Settings > Alerts.", fontSize = 16.sp)

        StyleCard(AlertStyle.ALARM, style, theme, "Rings like an alarm clock", listOf(
            "Plays sound even on silent",
            "Gets through Do Not Disturb",
            "Fills the screen, even when it's locked",
            "Rings until you tap Done, or the first nudge takes over",
        )) { Prefs.setAlertStyle(it); Scheduler.reschedule(context) }
        StyleCard(AlertStyle.NOTIFICATION, style, theme, "Arrives like a text", listOf(
            "Follows your phone's settings: silent stays quiet, vibrate just buzzes",
            "Do Not Disturb holds it until it ends",
            "Easy to miss if your phone's on silent",
        )) { Prefs.setAlertStyle(it); Scheduler.reschedule(context) }

        Block("Then come the nudges",
            "Not done yet? Nabbit nudges you every ${NudgeTiming.minutes} minutes, ${NudgeTiming.COUNT} more times, " +
                "then goes quiet. Nudges are notifications either way, so they follow your sound and Do Not Disturb " +
                "settings. Change how often in Settings > Nudges.")
        Block("Your Morning and Tonight",
            "Tomorrow AM means ${Times.hourLabel(morning)} and Tonight means ${Times.hourLabel(tonight)}. They mean " +
                "whatever they mean to you, so set them in Settings > Default times.")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Let Nabbit ring on time", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
            RingOnTimeChecklist(onRequestNotifications)
        }

        Row(
            Modifier.fillMaxWidth().background(theme.tap2.color, RoundedCornerShape(12.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Settings, null, tint = theme.tap2.text.color)
            Text(
                "Colors, Alarm or Notification, Morning and Tonight, and nudges can all be changed any time in " +
                    "Settings, the gear at the top of Home.",
                color = theme.tap2.text.color, fontWeight = FontWeight.SemiBold,
            )
        }
        BigButton("Got it", theme, onDone)
    }
}

@Composable
private fun StyleCard(
    option: AlertStyle,
    selectedStyle: AlertStyle,
    theme: NabbitTheme,
    headline: String,
    points: List<String>,
    onPick: (AlertStyle) -> Unit,
) {
    val picked = option == selectedStyle
    val fill = if (picked) theme.tap1 else ThemeColor(0xEFE7DF)
    Column(
        Modifier.fillMaxWidth().background(fill.color, RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton) { onPick(option) }
            .semantics(mergeDescendants = true) { selected = picked }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(option.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = fill.text.color, modifier = Modifier.weight(1f))
            Icon(if (picked) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null, tint = fill.text.color)
        }
        Text(headline, fontWeight = FontWeight.SemiBold, color = fill.text.color)
        points.forEach { Text("•  $it", color = fill.text.color) }
    }
}

@Composable
private fun Block(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Text(body)
    }
}

@Composable
private fun BigButton(title: String, theme: NabbitTheme, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = theme.done.color, contentColor = theme.done.text.color),
    ) { Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
}

package com.mynabbit.nabbit.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mynabbit.nabbit.copy.Copy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mynabbit.nabbit.data.AlertStyle
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.data.Store
import com.mynabbit.nabbit.data.VerbStore
import com.mynabbit.nabbit.model.Reminder
import com.mynabbit.nabbit.model.ReminderAction
import com.mynabbit.nabbit.model.Thing
import com.mynabbit.nabbit.model.Times
import com.mynabbit.nabbit.model.Verb
import com.mynabbit.nabbit.voice.SpeechListener

/** Home, top to bottom: the mic, the verb grid, Today and Coming up, the hot key offer, Again?, then Wins today. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    theme: NabbitTheme,
    now: Long,
    onVerb: (Verb) -> Unit,
    onHotKey: (Thing) -> Unit,
    onHeard: (String) -> Unit,
    onAct: (ReminderAction, Reminder) -> Unit,
    onRemove: (Reminder) -> Unit,
    onPutBack: (Reminder) -> Unit,
    onSettings: () -> Unit,
    onRequestNotifications: () -> Unit,
    banner: Banner?,
    onBannerTap: () -> Unit,
) {
    val context = LocalContext.current
    val data by Store.data.collectAsState()
    val layout by Prefs.verbLayout.collectAsState()
    Prefs.tone.collectAsState().value // Redraw copy when the tone changes.
    val verbs = VerbStore.visible(layout)
    var addingVerb by remember { mutableStateOf(false) }

    val tomorrow = Times.startOfTomorrow()
    val open = data.reminders.filter { !it.isDone }.sortedBy { it.dueAt }
    val today = open.filter { it.dueAt < tomorrow }
    val comingUp = open.filter { it.dueAt >= tomorrow }
    val startOfToday = Times.startOfToday()
    val wins = data.reminders.filter { (it.completedAt ?: 0) >= startOfToday }.sortedByDescending { it.completedAt }
    val hotKeys = data.things.filter { it.hotKey }.sortedByDescending { it.useCount }
    val offer = data.things.firstOrNull { it.useCount >= Thing.HOT_KEY_THRESHOLD && !it.hotKey && !it.hotKeyOffered }

    Scaffold(
        containerColor = ThemeColor.PAPER.color,
        topBar = {
            TopAppBar(
                title = { Text("Nabbit", fontWeight = FontWeight.Black, fontSize = 30.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeColor.PAPER.color),
                actions = {
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://mynabbit.com/support.html".toUri()))
                    }) { Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Help") }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
                },
            )
        },
        bottomBar = {
            if (banner != null) BannerView(banner.text, theme.done, banner.undo, onBannerTap)
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RingWarning(onRequestNotifications)
            MicButton(theme, onHeard)

            NoteGrid(verbs.size + 1) { index, modifier ->
                if (index < verbs.size) {
                    val verb = verbs[index]
                    var menu by remember { mutableStateOf(false) }
                    Box(modifier) {
                        StickyNote(
                            verb.title, theme.tap1,
                            Modifier.fillMaxWidth().semantics {
                                customActions = listOfNotNull(
                                    CustomAccessibilityAction("Hide") { VerbStore.setHidden(verb, true); true },
                                    if (!verb.isBuiltIn) CustomAccessibilityAction("Remove") { VerbStore.remove(verb); true } else null,
                                )
                            },
                            onLongClick = { menu = true },
                        ) { onVerb(verb) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Hide") }, onClick = { menu = false; VerbStore.setHidden(verb, true) })
                            if (!verb.isBuiltIn) {
                                DropdownMenuItem(text = { Text("Remove") }, onClick = { menu = false; VerbStore.remove(verb) })
                            }
                        }
                    }
                } else {
                    StickyNote("+ New", theme.tap1, modifier) { addingVerb = true }
                }
            }

            if (today.isNotEmpty()) {
                Section("Today") {
                    today.forEach { ReminderRow(it, theme, now, onAct, onRemove) }
                }
            }
            if (comingUp.isNotEmpty()) {
                Section("Coming up") {
                    comingUp.forEach { ReminderRow(it, theme, now, onAct, onRemove) }
                }
            }

            if (offer != null) HotKeyOffer(offer, theme)

            if (hotKeys.isNotEmpty()) {
                Section("Again?") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        hotKeys.forEach { thing ->
                            var menu by remember { mutableStateOf(false) }
                            val removeHotKey = { Store.updateThing(thing) { it.copy(hotKey = false) } }
                            Box {
                                StickyNote(
                                    "${thing.verb.title} ${thing.name}", theme.tap2,
                                    Modifier.semantics {
                                        customActions = listOf(CustomAccessibilityAction("Remove hot key") { removeHotKey(); true })
                                    },
                                    compact = true,
                                    onLongClick = { menu = true },
                                ) { onHotKey(thing) }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text("Remove hot key") }, onClick = { menu = false; removeHotKey() })
                                }
                            }
                        }
                    }
                }
            }

            if (wins.isNotEmpty()) {
                Section("Wins today") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        wins.forEach { win ->
                            var asking by remember { mutableStateOf(false) }
                            Box {
                                StickyNote(win.text, theme.done, Modifier.semantics {
                                    contentDescription = "Won: ${win.text}. Double tap to put it back."
                                }, compact = true) { asking = true }
                                DropdownMenu(expanded = asking, onDismissRequest = { asking = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Put it back on the list") },
                                        onClick = { asking = false; onPutBack(win) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (addingVerb) {
        OneWordDialog(title = "New verb", hint = "One word, like Water or Text", onDone = { word ->
            addingVerb = false
            VerbStore.add(word)?.let(onVerb)
        }, onCancel = { addingVerb = false })
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.semantics { heading() })
        content()
    }
}

/** A row on Today or Coming up. Tap for Done; press and hold for 15 min, Tonight, or Remove. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderRow(
    reminder: Reminder,
    theme: NabbitTheme,
    now: Long,
    onAct: (ReminderAction, Reminder) -> Unit,
    onRemove: (Reminder) -> Unit,
) {
    val slipped = reminder.isSlipped(now)
    val fill = if (slipped) theme.alert else theme.tap3
    val time = if (slipped) "Slipped" else if (reminder.dueAt <= now) "Now" else Times.rowLabel(reminder.dueAt)
    var menu by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(6.dp))
                .background(fill.color, RoundedCornerShape(6.dp))
                .combinedClickable(onLongClick = { menu = true }) { onAct(ReminderAction.DONE, reminder) }
                .semantics {
                    contentDescription = "${if (slipped) "Slipped: " else ""}${reminder.text}, $time. Double tap for Done."
                    customActions = listOf(
                        CustomAccessibilityAction("15 min") { onAct(ReminderAction.SNOOZE_15, reminder); true },
                        CustomAccessibilityAction("Tonight") { onAct(ReminderAction.TONIGHT, reminder); true },
                        CustomAccessibilityAction("Remove") { onRemove(reminder); true },
                    )
                }
                .heightIn(min = 56.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            TaskAndTime(if (slipped) "Slipped: ${reminder.text}" else reminder.text, time, fill.text.color)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Done") }, onClick = { menu = false; onAct(ReminderAction.DONE, reminder) })
            DropdownMenuItem(text = { Text("15 min") }, onClick = { menu = false; onAct(ReminderAction.SNOOZE_15, reminder) })
            DropdownMenuItem(text = { Text("Tonight") }, onClick = { menu = false; onAct(ReminderAction.TONIGHT, reminder) })
            DropdownMenuItem(text = { Text("Remove") }, onClick = { menu = false; onRemove(reminder) })
        }
    }
}

/**
 * Shows when something Android needs for ringing on time is off: notifications, exact
 * alarms, or (for Alarm style) full-screen alerts. One at a time, rechecked whenever
 * Nabbit comes forward, and gone once it's on.
 */
@Composable
private fun RingWarning(onRequestNotifications: () -> Unit) {
    val context = LocalContext.current
    val style by Prefs.alertStyle.collectAsState()
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val problem = remember(refresh, style) {
        when {
            !Permissions.notificationsOn(context) ->
                Triple("Notifications are off, so alerts and nudges can't show up.", "Turn on", onRequestNotifications)
            !Permissions.exactAlarmsOn(context) ->
                Triple("Reminders can ring up to 10 minutes late until Alarms & reminders is on.", "Turn on") {
                    Permissions.openExactAlarmSettings(context)
                }
            style == AlertStyle.ALARM && !Permissions.fullScreenOn(context) ->
                Triple("The alarm can't fill the screen until Full-screen alerts is on.", "Turn on") {
                    Permissions.openFullScreenSettings(context)
                }
            else -> null
        }
    } ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .background(ThemeColor(0xFFFFFF).color, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = ThemeColor(0xB8341B).color)
        Text(problem.first, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Button(onClick = problem.third) { Text(problem.second) }
    }
}

/** Offered once per combination after it's been made three times. Yes pins it to Again?; no never asks again. */
@Composable
private fun HotKeyOffer(thing: Thing, theme: NabbitTheme) {
    val fill = theme.tap2
    Column(
        Modifier.fillMaxWidth().background(fill.color, RoundedCornerShape(10.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(Copy.hotKeyOffer("${thing.verb.title} ${thing.name}", thing.useCount),
            color = fill.text.color, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { Store.updateThing(thing) { it.copy(hotKey = true, hotKeyOffered = true) } },
                colors = ButtonDefaults.buttonColors(containerColor = theme.done.color, contentColor = theme.done.text.color),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Add hot key", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = { Store.updateThing(thing) { it.copy(hotKeyOffered = true) } },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("No thanks", color = fill.text.color, fontWeight = FontWeight.Bold) }
        }
    }
}

/** The big mic above the verb grid: say the whole reminder, or as much of it as you know. */
@Composable
private fun MicButton(theme: NabbitTheme, onHeard: (String) -> Unit) {
    val context = LocalContext.current
    val listener = remember { SpeechListener(context) }
    DisposableEffect(Unit) { onDispose { listener.release() } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listener.start(onHeard)
    }
    val listening = listener.state == SpeechListener.State.LISTENING
    val title = when (listener.state) {
        SpeechListener.State.LISTENING -> listener.transcript.ifEmpty { "Listening…" }
        SpeechListener.State.UNAVAILABLE -> "Speech isn't available on this phone."
        SpeechListener.State.IDLE -> "Say it: “call the dentist tomorrow”"
    }
    val fill = theme.alert
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(6.dp))
            .background(fill.color, RoundedCornerShape(6.dp))
            .combinedClickableSimple {
                when {
                    listening -> listener.stop()
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED -> listener.start(onHeard)
                    else -> permission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            .semantics { contentDescription = if (listening) "Stop listening" else "Say a reminder" }
            .heightIn(min = 72.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(if (listening) Icons.Filled.GraphicEq else Icons.Filled.Mic, null, tint = fill.text.color, modifier = Modifier.size(36.dp))
        // The prompt shrinks a little to stay on one line; what's heard wraps instead.
        val idle = listener.state == SpeechListener.State.IDLE
        BasicText(
            title,
            style = TextStyle(color = fill.text.color, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp),
            maxLines = if (idle) 1 else 3,
            overflow = TextOverflow.Ellipsis,
            autoSize = if (idle) TextAutoSize.StepBased(minFontSize = 15.sp, maxFontSize = 20.sp) else null,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedClickableSimple(onClick: () -> Unit): Modifier = this.combinedClickable(onClick = onClick)

/** One text field, for a new verb or thing. Done adds it; there's no Save button. */
@Composable
fun OneWordDialog(title: String, hint: String, onDone: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, singleLine = true,
                placeholder = { Text(hint) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                    if (text.isNotBlank()) onDone(text.trim())
                }),
            )
        },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onDone(text.trim()) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

/** A short line at the bottom of Home, with Undo where it helps. */
data class Banner(val text: String, val undo: (() -> Unit)? = null, val id: Long = System.nanoTime())

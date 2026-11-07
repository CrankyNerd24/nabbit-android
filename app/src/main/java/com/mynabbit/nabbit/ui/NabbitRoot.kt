package com.mynabbit.nabbit.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mynabbit.nabbit.alerts.AlertCenter
import com.mynabbit.nabbit.alerts.Notifier
import com.mynabbit.nabbit.celebration.Celebration
import com.mynabbit.nabbit.celebration.CelebrationLines
import com.mynabbit.nabbit.celebration.CelebrationOverlay
import com.mynabbit.nabbit.celebration.Haptics
import com.mynabbit.nabbit.copy.Copy
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.data.Reminders
import com.mynabbit.nabbit.data.Store
import com.mynabbit.nabbit.model.Reminder
import com.mynabbit.nabbit.model.ReminderAction
import com.mynabbit.nabbit.model.Times
import com.mynabbit.nabbit.model.Verb
import com.mynabbit.nabbit.voice.VoiceParser
import kotlinx.coroutines.delay

/** Where the person is: Home, a capture step, or Settings. */
sealed interface Screen {
    data object Home : Screen
    data class Thing(val verb: Verb) : Screen
    data class When(val verb: Verb, val thing: String) : Screen
    data object Settings : Screen
    data object Fridge : Screen
}

@Composable
fun NabbitRoot() {
    val context = LocalContext.current
    val theme = NabbitTheme.named(Prefs.themeId.collectAsState().value)
    val themeIntroShown by Prefs.themeIntroShown.collectAsState()
    val alertsIntroShown by Prefs.alertsIntroShown.collectAsState()
    val alertId by AlertCenter.current.collectAsState()
    val data by Store.data.collectAsState()

    var stack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
    var banner by remember { mutableStateOf<Banner?>(null) }
    var celebration by remember { mutableStateOf<Celebration?>(null) }
    // Ticks each minute so "Now" and Slipped stay current.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(data) { now = System.currentTimeMillis() }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    // Asks once through Android's prompt; after that (or with notifications switched off
    // for the app), opens Nabbit's notification settings instead.
    val requestNotifications = {
        val askable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (askable) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Permissions.openNotificationSettings(context)
        }
    }

    fun push(screen: Screen) { stack = stack + screen }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun home() { stack = listOf(Screen.Home) }

    fun saved(reminder: Reminder) {
        banner = Banner(Copy.saved(reminder.text, reminder.dueAt), undo = { Store.remove(reminder.id) })
        home()
        // Asked on the first save, when it's clear what the permission is for.
        if (!Notifier.canPost(context)) requestNotifications()
    }

    fun act(action: ReminderAction, reminder: Reminder) {
        if (action == ReminderAction.DONE) {
            // Sized before it's marked done, while slipped and nudge state still apply.
            val size = Celebration.size(reminder)
            val clearsBoard = Store.openReminders.none { it.id != reminder.id && it.dueAt < Times.startOfTomorrow() } &&
                reminder.dueAt < Times.startOfTomorrow()
            val done = Celebration.make(reminder, if (clearsBoard) Celebration.Size.CLEAR_BOARD else size)
            Reminders.perform(context, action, reminder.id)
            celebration = done
            Haptics.play(context, done.size)
            banner = Banner(done.line, undo = { Store.reopen(reminder.id) })
        } else {
            Reminders.perform(context, action, reminder.id)
            Store.reminder(reminder.id)?.let { banner = Banner(Copy.moved(it.thing, it.dueAt)) }
        }
    }

    /** On the to-do list, Done is quieter: a small tap and a line with Undo, no confetti. */
    fun fridgeAct(action: ReminderAction, reminder: Reminder) {
        if (action == ReminderAction.DONE) {
            Haptics.tap(context)
            val line = CelebrationLines.next(reminder)
            Reminders.perform(context, action, reminder.id)
            banner = Banner(line, undo = { Store.reopen(reminder.id) })
        } else {
            act(action, reminder)
        }
    }

    fun heard(text: String) {
        val spoken = VoiceParser.parse(text, morningHour = Prefs.morningHour.value, tonightHour = Prefs.tonightHour.value)
        val verb = spoken.verb ?: run { banner = Banner(Copy.missed); return }
        val said = spoken.thing ?: run { stack = listOf(Screen.Home, Screen.Thing(verb)); return }
        val thing = Store.thingMatching(said, verb)
        val due = spoken.due ?: run { stack = listOf(Screen.Home, Screen.Thing(verb), Screen.When(verb, thing)); return }
        saved(Store.save(verb, thing, Times.toMillis(due)))
    }

    LaunchedEffect(banner?.id) {
        val current = banner ?: return@LaunchedEffect
        delay(if (current.undo == null) 2_500 else 5_000)
        if (banner?.id == current.id) banner = null
    }
    LaunchedEffect(celebration?.id) {
        val current = celebration ?: return@LaunchedEffect
        delay(current.size.millis)
        if (celebration?.id == current.id) celebration = null
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = theme.alert.color, onPrimary = theme.alert.text.color)) {
        Box(Modifier.fillMaxSize()) {
            when {
                !themeIntroShown -> ThemeIntro { Prefs.setThemeIntroShown() }
                !alertsIntroShown -> AlertsIntro(requestNotifications) { Prefs.setAlertsIntroShown() }
                else -> {
                    BackHandler(enabled = stack.size > 1) { pop() }
                    when (val screen = stack.last()) {
                        Screen.Home -> HomeScreen(
                            theme = theme,
                            now = now,
                            onVerb = { push(Screen.Thing(it)) },
                            onHotKey = { push(Screen.When(it.verb, it.name)) },
                            onHeard = ::heard,
                            onAct = ::act,
                            onRemove = { reminder ->
                                Store.remove(reminder.id)
                                Notifier.cancel(context, reminder.id)
                                banner = Banner(Copy.removed(reminder.thing), undo = { Store.restore(reminder) })
                            },
                            onPutBack = { win ->
                                Store.putBack(win.id)
                                Store.reminder(win.id)?.let {
                                    banner = Banner("${it.text} is back, ${Times.dueDescription(it.dueAt)}.")
                                }
                            },
                            onSettings = { push(Screen.Settings) },
                            onFridge = { push(Screen.Fridge) },
                            onRequestNotifications = requestNotifications,
                            banner = banner,
                            onBannerTap = { banner = null },
                        )
                        is Screen.Thing -> ThingStep(screen.verb, theme, onPick = { push(Screen.When(screen.verb, it)) }, onBack = ::pop)
                        is Screen.When -> WhenStep(screen.verb, screen.thing, theme, onSave = { due ->
                            saved(Store.save(screen.verb, screen.thing, due))
                        }, onBack = ::pop)
                        Screen.Settings -> SettingsScreen(theme, requestNotifications, onBack = ::pop)
                        Screen.Fridge -> FridgeScreen(theme, now, ::fridgeAct, banner, onBannerTap = { banner = null }, onBack = ::pop)
                    }
                }
            }

            celebration?.let { CelebrationOverlay(it, theme) }

            val alerting = alertId?.let { id -> data.reminders.firstOrNull { it.id == id && !it.isDone } }
            if (alertId != null && alerting == null) {
                // Already handled somewhere else.
                LaunchedEffect(alertId) { AlertCenter.dismiss() }
            }
            if (alerting != null) {
                BackHandler { Notifier.cancel(context, alerting.id); AlertCenter.dismiss() }
                AlertScreen(
                    alerting, theme,
                    onAction = { action ->
                        AlertCenter.dismiss()
                        home()
                        act(action, alerting)
                    },
                    onQuiet = {
                        // Quiets the ringing; the nudges still come.
                        Notifier.cancel(context, alerting.id)
                        AlertCenter.dismiss()
                    },
                )
            }
        }
    }
}


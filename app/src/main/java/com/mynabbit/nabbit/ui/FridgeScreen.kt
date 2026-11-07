package com.mynabbit.nabbit.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynabbit.nabbit.celebration.reduceMotion
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.data.Store
import com.mynabbit.nabbit.model.NudgeTiming
import com.mynabbit.nabbit.model.Reminder
import com.mynabbit.nabbit.model.ReminderAction
import com.mynabbit.nabbit.model.Times
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A shelf on the to-do list: Slipped, Today, Tomorrow, then one per later day. */
private data class Shelf(val title: String, val kind: Kind, val reminders: List<Reminder>) {
    enum class Kind { SLIPPED, TODAY, TOMORROW, LATER }
}

private fun shelves(open: List<Reminder>, now: Long): List<Shelf> {
    val slipped = open.filter { now >= NudgeTiming.lastNudgeAt(it.dueAt) }
    val rest = open.filter { now < NudgeTiming.lastNudgeAt(it.dueAt) }
    val today = LocalDate.now()
    val result = mutableListOf<Shelf>()
    if (slipped.isNotEmpty()) result += Shelf("Slipped", Shelf.Kind.SLIPPED, slipped)
    // Anything due just before midnight and still being nudged counts as today.
    rest.groupBy { maxOf(Times.toLocal(it.dueAt).toLocalDate(), today) }.toSortedMap().forEach { (day, items) ->
        result += when (day) {
            today -> Shelf("Today", Shelf.Kind.TODAY, items)
            today.plusDays(1) -> Shelf("Tomorrow", Shelf.Kind.TOMORROW, items)
            else -> Shelf(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()).format(day), Shelf.Kind.LATER, items)
        }
    }
    return result
}

/**
 * Everything coming up as notes on a fridge door: slipped first, then each day. Tap a
 * note when it's done (Undo shows for a few seconds); press and hold for 15 min or
 * Tonight. The screen stays awake while it's open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeScreen(
    theme: NabbitTheme,
    now: Long,
    onAct: (ReminderAction, Reminder) -> Unit,
    banner: Banner?,
    onBannerTap: () -> Unit,
    onBack: () -> Unit,
) {
    val data by Store.data.collectAsState()
    val showsList by Prefs.fridgeShowsList.collectAsState()
    val open = data.reminders.filter { !it.isDone }.sortedBy { it.dueAt }
    val groups = shelves(open, now)

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        containerColor = ThemeColor(0xEFE7DF).color,
        topBar = {
            TopAppBar(
                title = { Text("On the to-do list", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { Prefs.setFridgeShowsList(!showsList) }) {
                        Icon(
                            if (showsList) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = if (showsList) "Show as cards" else "Show as list",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeColor(0xEFE7DF).color),
            )
        },
        bottomBar = { if (banner != null) BannerView(banner.text, theme.done, banner.undo, onBannerTap) },
    ) { padding ->
        if (groups.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing on the to-do list.", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            groups.forEach { shelf ->
                val fill = when (shelf.kind) {
                    Shelf.Kind.SLIPPED -> theme.alert
                    Shelf.Kind.TODAY -> theme.tap3
                    Shelf.Kind.TOMORROW -> theme.tap1
                    Shelf.Kind.LATER -> theme.tap2
                }
                // Later days show the day too; Slipped, Today, and Tomorrow just the time.
                val time = { r: Reminder ->
                    if (shelf.kind == Shelf.Kind.LATER) Times.dueDescription(r.dueAt) else Times.clock(r.dueAt)
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(shelf.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                    if (showsList) {
                        shelf.reminders.forEach { reminder ->
                            FridgeNote(reminder, fill, time(reminder), list = true, tilt = 0f, onAct)
                        }
                    } else {
                        NoteGrid(shelf.reminders.size) { index, modifier ->
                            val reminder = shelf.reminders[index]
                            Box(modifier) {
                                FridgeNote(reminder, fill, time(reminder), list = false,
                                    tilt = if (index % 2 == 0) -1.5f else 1.2f, onAct)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A card or a row. A tap marks it done; press and hold, or TalkBack actions, give all three. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FridgeNote(
    reminder: Reminder,
    fill: ThemeColor,
    time: String,
    list: Boolean,
    tilt: Float,
    onAct: (ReminderAction, Reminder) -> Unit,
) {
    val context = LocalContext.current
    val still = remember { reduceMotion(context) }
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Box {
        Box(
            Modifier
                .fillMaxWidth()
                .rotate(if (still) 0f else tilt)
                .shadow(if (list) 2.dp else 4.dp, shape)
                .background(fill.color, shape)
                .combinedClickable(onLongClick = { menu = true }) { onAct(ReminderAction.DONE, reminder) }
                .semantics {
                    contentDescription = "${reminder.text}, $time. Double tap for Done."
                    customActions = listOf(
                        CustomAccessibilityAction("15 min") { onAct(ReminderAction.SNOOZE_15, reminder); true },
                        CustomAccessibilityAction("Tonight") { onAct(ReminderAction.TONIGHT, reminder); true },
                    )
                }
                .heightIn(min = if (list) 56.dp else 120.dp)
                .padding(14.dp),
        ) {
            if (list) {
                TaskAndTime(reminder.text, time, fill.text.color, Modifier.align(Alignment.CenterStart))
            } else {
                Column(Modifier.heightIn(min = 92.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(reminder.text, color = fill.text.color, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp)
                    Text(time, color = fill.text.color.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            ReminderAction.entries.forEach { action ->
                DropdownMenuItem(text = { Text(action.title) }, onClick = { menu = false; onAct(action, reminder) })
            }
        }
    }
}


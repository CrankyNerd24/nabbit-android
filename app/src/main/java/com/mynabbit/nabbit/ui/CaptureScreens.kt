package com.mynabbit.nabbit.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynabbit.nabbit.copy.Copy
import com.mynabbit.nabbit.data.Prefs
import com.mynabbit.nabbit.data.Store
import com.mynabbit.nabbit.model.Times
import com.mynabbit.nabbit.model.Verb
import com.mynabbit.nabbit.model.WhenPreset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = ThemeColor.PAPER.color,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ThemeColor.PAPER.color),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** Second tap: who or what, most used first, or + New with one text field. */
@Composable
fun ThingStep(verb: Verb, theme: NabbitTheme, onPick: (String) -> Unit, onBack: () -> Unit) {
    val data by Store.data.collectAsState()
    val things = remember(data, verb) { Store.things(verb) }
    var adding by remember { mutableStateOf(false) }
    StepScaffold(Copy.thingPrompt(verb), onBack) {
        NoteGrid(things.size + 1) { index, modifier ->
            if (index < things.size) {
                StickyNote(things[index].name, theme.tap2, modifier) { onPick(things[index].name) }
            } else {
                StickyNote("+ New", theme.tap2, modifier) { adding = true }
            }
        }
    }
    if (adding) {
        OneWordDialog(title = Copy.thingPrompt(verb), hint = "Like Plumber or Car wash", onDone = { name ->
            adding = false
            onPick(Store.addThing(verb, name))
        }, onCancel = { adding = false })
    }
}

/**
 * Third tap: when. Tapping a chip saves. Pick day & time opens a row of days (the next
 * Morning's day picked) and every half hour from 6 AM to 10 PM, scrolled to the Morning
 * time; tapping a time saves. Another time… takes an exact minute.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WhenStep(verb: Verb, thing: String, theme: NabbitTheme, onSave: (Long) -> Unit, onBack: () -> Unit) {
    var picking by remember { mutableStateOf(false) }
    var exact by remember { mutableStateOf(false) }
    val morning by Prefs.morningHour.collectAsState()
    val days = remember { pickableDays() }
    var day by remember { mutableStateOf(Times.nextMorning().toLocalDate().takeIf { it in days } ?: days.first()) }
    val times = halfHours(day)
    val morningTime = remember { BringIntoViewRequester() }

    StepScaffold("When?", onBack) {
        Text(
            "${verb.title} $thing", fontSize = 30.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        NoteGrid(WhenPreset.entries.size) { index, modifier ->
            val preset = WhenPreset.entries[index]
            StickyNote(preset.title, theme.tap3, modifier) {
                val due = preset.dueAt()
                if (due != null) onSave(Times.toMillis(due)) else picking = true
            }
        }

        if (picking) {
            Text("Day", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.semantics { heading() })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { option ->
                    FilterChip(
                        selected = option == day,
                        onClick = { day = option },
                        label = { Text(dayLabel(option), fontSize = 16.sp) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
            Text("Time", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.semantics { heading() })
            val columns = if (LocalDensity.current.fontScale >= 1.3f) 2 else 3
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (times.map { it as LocalDateTime? } + listOf(null)).chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { time ->
                            if (time == null) {
                                StickyNote("Another time\u2026", theme.tap3, Modifier.weight(1f), compact = true) { exact = true }
                            } else {
                                val millis = Times.toMillis(time)
                                val first = time == times.firstOrNull { it.hour >= morning }
                                StickyNote(
                                    Times.clock(millis), theme.tap3,
                                    Modifier.weight(1f)
                                        .then(if (first) Modifier.bringIntoViewRequester(morningTime) else Modifier)
                                        .semantics { contentDescription = "Set for ${Times.dueDescription(millis)}" },
                                    compact = true,
                                ) { onSave(millis) }
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            // Lands on the usual pick: the Morning time.
            LaunchedEffect(day) { morningTime.bringIntoView() }
        }
    }

    if (exact) {
        val start = if (day == Times.nextMorning().toLocalDate()) Times.nextMorning() else day.atTime(morning, 0)
        val timeState = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute)
        AlertDialog(
            onDismissRequest = { exact = false },
            title = { Text("${dayLabel(day)} at") },
            text = { TimeInput(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    exact = false
                    // The picked minute, exactly. One that's already here or past gets a few
                    // seconds of grace, so it still rings.
                    val millis = Times.toMillis(day.atTime(timeState.hour, timeState.minute))
                    val now = System.currentTimeMillis()
                    onSave(if (millis > now) millis else now + 5_000)
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { exact = false }) { Text("Cancel") } },
        )
    }
}

/** Today (while a half hour is left) and the next two weeks. */
private fun pickableDays(): List<LocalDate> {
    val today = LocalDate.now()
    val first = if (halfHours(today).isEmpty()) 1L else 0L
    return (first..14L).map { today.plusDays(it) }
}

/** Every half hour from 6 AM to 10 PM on a day, the ones still ahead. */
private fun halfHours(day: LocalDate): List<LocalDateTime> {
    val now = LocalDateTime.now()
    return (12..44).map { day.atStartOfDay().plusMinutes(it * 30L) }.filter { it.isAfter(now) }
}

/** "Today", "Tomorrow", or "Mon 10". */
private fun dayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> DateTimeFormatter.ofPattern("EEE MMM d", Locale.getDefault()).format(day)
    }
}

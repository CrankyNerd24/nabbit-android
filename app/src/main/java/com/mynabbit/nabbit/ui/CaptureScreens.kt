package com.mynabbit.nabbit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mynabbit.nabbit.copy.Copy
import com.mynabbit.nabbit.data.Store
import com.mynabbit.nabbit.model.Times
import com.mynabbit.nabbit.model.Verb
import com.mynabbit.nabbit.model.WhenPreset
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

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
 * Third tap: when. Tapping a chip saves. Pick day & time opens a calendar, then a clock,
 * starting on the next Morning time; tapping the note under them saves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhenStep(verb: Verb, thing: String, theme: NabbitTheme, onSave: (Long) -> Unit, onBack: () -> Unit) {
    var picking by remember { mutableStateOf(false) }
    var choosingTime by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf<LocalDateTime?>(null) }
    val start = remember { Times.nextMorning() }

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
        picked?.let { time ->
            val millis = Times.toMillis(time)
            // The last tap saves, like every other chip.
            StickyNote(
                Times.dueDescription(millis).replaceFirstChar { it.uppercase() }, theme.tap3,
                Modifier.fillMaxWidth().semantics { contentDescription = "Set for ${Times.dueDescription(millis)}" },
            ) {
                // The picked minute, exactly. Only one that's already here or past gets a
                // few seconds of grace, so it still rings.
                val now = System.currentTimeMillis()
                onSave(if (millis > now) millis else now + 5_000)
            }
        }
    }

    if (picking) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = (picked ?: start).toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atOffset(ZoneOffset.UTC).toLocalDate().isBefore(LocalDate.now())
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    val day = dateState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate()
                    } ?: start.toLocalDate()
                    picked = day.atTime((picked ?: start).toLocalTime())
                    picking = false
                    choosingTime = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { DatePicker(state = dateState) }
    }

    if (choosingTime) {
        val base = picked ?: start
        val timeState = rememberTimePickerState(initialHour = base.hour, initialMinute = base.minute)
        AlertDialog(
            onDismissRequest = { choosingTime = false },
            title = { Text("What time?") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    picked = base.toLocalDate().atTime(timeState.hour, timeState.minute)
                    choosingTime = false
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { choosingTime = false }) { Text("Cancel") } },
        )
    }
}

package com.mynabbit.nabbit.data

import android.content.Context
import com.mynabbit.nabbit.model.Reminder
import com.mynabbit.nabbit.model.Thing
import com.mynabbit.nabbit.model.Verb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class StoreData(
    val reminders: List<Reminder> = emptyList(),
    val things: List<Thing> = emptyList(),
)

/**
 * Reminders and things, kept as one small JSON file on the phone. Local-only: no
 * account, no server. Every change saves right away and tells `onChange`, which
 * reschedules alarms.
 */
object Store {
    private lateinit var file: File
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val lock = Any()

    private val _data = MutableStateFlow(StoreData())
    val data: StateFlow<StoreData> = _data

    /** Set by the app to reschedule alarms after any change. */
    var onChange: () -> Unit = {}

    fun init(context: Context) {
        synchronized(lock) {
            if (::file.isInitialized) return
            file = File(context.filesDir, "nabbit.json")
            val loaded = if (file.exists()) {
                runCatching { json.decodeFromString<StoreData>(file.readText()) }.getOrNull()
            } else {
                null
            }
            _data.value = loaded ?: StoreData()
            // Each verb's starter things on first launch.
            if (_data.value.things.isEmpty()) {
                val seeded = Verb.builtIns.flatMap { verb -> verb.starterThings.map { Thing(it, verb.raw) } }
                write(_data.value.copy(things = seeded), notify = false)
            }
        }
    }

    fun reminder(id: String): Reminder? = _data.value.reminders.firstOrNull { it.id == id }

    val openReminders: List<Reminder>
        get() = _data.value.reminders.filter { !it.isDone }.sortedBy { it.dueAt }

    /** Most used first, then most recent, then oldest added. */
    fun things(verb: Verb): List<Thing> =
        _data.value.things.filter { it.verbRaw == verb.raw }
            .sortedWith(compareByDescending<Thing> { it.useCount }.thenByDescending { it.lastUsed ?: 0 }
                .thenBy { it.createdAt })

    // Reminders

    /** The one save path for new reminders: three taps and the mic. */
    fun save(verb: Verb, thing: String, dueAt: Long): Reminder {
        val now = System.currentTimeMillis()
        val reminder = Reminder(UUID.randomUUID().toString(), verb.raw, thing, dueAt, now)
        update { data ->
            data.copy(
                reminders = data.reminders + reminder,
                things = data.things.map {
                    if (it.verbRaw == verb.raw && it.name == thing) it.copy(useCount = it.useCount + 1, lastUsed = now) else it
                },
            )
        }
        return reminder
    }

    fun complete(id: String) = updateReminder(id) { it.copy(completedAt = System.currentTimeMillis()) }

    fun reopen(id: String) = updateReminder(id) { it.copy(completedAt = null) }

    fun snooze(id: String, until: Long) = updateReminder(id) { it.copy(dueAt = until, snoozeCount = it.snoozeCount + 1) }

    /** Puts a reminder back exactly as it was, for Undo. */
    fun restore(reminder: Reminder) = update { data ->
        data.copy(reminders = data.reminders.filter { it.id != reminder.id } + reminder)
    }

    fun remove(id: String) = update { data -> data.copy(reminders = data.reminders.filter { it.id != id }) }

    // Things

    /**
     * The saved thing that matches what was said ("the kids" → "Kids", "med" → "Meds"),
     * or a new one added under this verb.
     */
    fun thingMatching(spoken: String, verb: Verb): String {
        val clean = spoken.trim()
        val match = things(verb).firstOrNull {
            it.name.equals(clean, true) || it.name.equals(clean + "s", true) || (it.name + "s").equals(clean, true)
        }
        if (match != null) return match.name
        return addThing(verb, clean)
    }

    /** Adds a thing under a verb, or returns the one already there. */
    fun addThing(verb: Verb, name: String): String {
        val clean = name.trim().replaceFirstChar { it.uppercase() }
        things(verb).firstOrNull { it.name.equals(clean, true) }?.let { return it.name }
        update(notify = false) { it.copy(things = it.things + Thing(clean, verb.raw)) }
        return clean
    }

    fun updateThing(thing: Thing, change: (Thing) -> Thing) = update(notify = false) { data ->
        data.copy(things = data.things.map { if (it.verbRaw == thing.verbRaw && it.name == thing.name) change(it) else it })
    }

    // Saving

    private fun updateReminder(id: String, change: (Reminder) -> Reminder) = update { data ->
        data.copy(reminders = data.reminders.map { if (it.id == id) change(it) else it })
    }

    private fun update(notify: Boolean = true, change: (StoreData) -> StoreData) {
        synchronized(lock) { write(change(_data.value), notify = false) }
        if (notify) onChange()
    }

    private fun write(data: StoreData, notify: Boolean) {
        _data.value = data
        val temp = File(file.parentFile, "nabbit.json.tmp")
        temp.writeText(json.encodeToString(StoreData.serializer(), data))
        temp.renameTo(file)
        if (notify) onChange()
    }
}

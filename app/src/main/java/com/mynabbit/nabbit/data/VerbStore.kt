package com.mynabbit.nabbit.data

import com.mynabbit.nabbit.model.Verb

/**
 * The verbs on the grid: the built-ins plus added ones, in the person's order, minus
 * hidden ones. Hidden or removed verbs keep their reminders.
 */
object VerbStore {
    fun all(layout: VerbLayout = Prefs.verbLayout.value): List<Verb> {
        val verbs = Verb.builtIns + layout.added.mapNotNull(Verb::fromRaw)
        val position = layout.order.withIndex().associate { it.value to it.index }
        return verbs.withIndex()
            .sortedBy { position[it.value.raw] ?: (layout.order.size + it.index) }
            .map { it.value }
    }

    fun visible(layout: VerbLayout = Prefs.verbLayout.value): List<Verb> =
        all(layout).filter { it.raw !in layout.hidden }

    fun hidden(layout: VerbLayout = Prefs.verbLayout.value): List<Verb> =
        all(layout).filter { it.raw in layout.hidden }

    /** Adds a one-word verb, or shows a hidden one again. Null when the word is empty. */
    fun add(word: String): Verb? {
        val clean = word.trim().split(" ").firstOrNull().orEmpty()
        if (clean.isEmpty()) return null
        val existing = all().firstOrNull { it.title.equals(clean, ignoreCase = true) }
        val layout = Prefs.verbLayout.value
        if (existing != null) {
            Prefs.setVerbLayout(layout.copy(hidden = layout.hidden - existing.raw))
            return existing
        }
        val verb = Verb.custom(clean)
        Prefs.setVerbLayout(layout.copy(added = layout.added + verb.raw))
        return verb
    }

    fun setHidden(verb: Verb, hidden: Boolean) {
        val layout = Prefs.verbLayout.value
        Prefs.setVerbLayout(layout.copy(hidden = if (hidden) layout.hidden + verb.raw else layout.hidden - verb.raw))
    }

    /** Only added verbs can be removed; built-ins can be hidden. */
    fun remove(verb: Verb) {
        val layout = Prefs.verbLayout.value
        Prefs.setVerbLayout(
            layout.copy(
                added = layout.added - verb.raw,
                order = layout.order - verb.raw,
                hidden = layout.hidden - verb.raw,
            ),
        )
    }
}

package com.mynabbit.nabbit.model

/**
 * The first tap: eight built in, plus any the person adds.
 *
 * Reminders and things store a verb's raw value: a built-in's key ("call"), or
 * "custom:" and the word ("custom:Text"). The word lives in the raw value, so a
 * reminder keeps its verb even after that verb is removed from the grid.
 */
data class Verb(val raw: String, val title: String) {
    val isBuiltIn: Boolean get() = !raw.startsWith(CUSTOM_PREFIX)

    /** The question on the second tap. */
    val thingPrompt: String
        get() = when (raw) {
            "call", "email" -> "$title who?"
            "goTo" -> "Go where?"
            else -> "$title what?"
        }

    /** The short push at the end of an alert. */
    val push: String
        get() = when (raw) {
            "call" -> "It's a two-minute call."
            "pickUp" -> "Keys, shoes, go."
            "take" -> "Glass of water. Done."
            "pay" -> "Two minutes and it's paid."
            "buy" -> "Add it to the cart now."
            "email" -> "Three lines is plenty."
            "book" -> "One call or one click."
            "goTo" -> "Shoes on. Out the door."
            else -> "Two minutes, tops."
        }

    /** Things offered on the second tap before the person has added any. */
    val starterThings: List<String>
        get() = when (raw) {
            "call" -> listOf("Dentist", "Mom", "Doctor", "Vet", "Pharmacy")
            "pickUp" -> listOf("Kids", "Prescription", "Dry cleaning")
            "take" -> listOf("Meds", "Vitamins")
            "pay" -> listOf("Rent", "Phone bill", "Credit card")
            "buy" -> listOf("Groceries", "Milk", "Gift")
            "email" -> listOf("Boss", "School", "Landlord")
            "book" -> listOf("Haircut", "Doctor", "Dentist")
            "goTo" -> listOf("Pharmacy", "Post office", "Gym")
            else -> emptyList()
        }

    companion object {
        private const val CUSTOM_PREFIX = "custom:"

        val call = Verb("call", "Call")
        val pickUp = Verb("pickUp", "Pick up")
        val take = Verb("take", "Take")
        val pay = Verb("pay", "Pay")
        val buy = Verb("buy", "Buy")
        val email = Verb("email", "Email")
        val book = Verb("book", "Book")
        val goTo = Verb("goTo", "Go to")

        val builtIns = listOf(call, pickUp, take, pay, buy, email, book, goTo)

        fun fromRaw(raw: String): Verb? =
            builtIns.firstOrNull { it.raw == raw }
                ?: if (raw.startsWith(CUSTOM_PREFIX) && raw.length > CUSTOM_PREFIX.length) {
                    Verb(raw, raw.removePrefix(CUSTOM_PREFIX))
                } else {
                    null
                }

        /** A verb the person added, from the word they typed or said. */
        fun custom(word: String): Verb {
            val title = word.trim().replaceFirstChar { it.uppercase() }
            return Verb(CUSTOM_PREFIX + title, title)
        }
    }
}

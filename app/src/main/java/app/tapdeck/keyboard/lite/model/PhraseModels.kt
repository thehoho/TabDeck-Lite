package app.tapdeck.keyboard.lite.model

enum class PhraseAction {
    INSERT,
    INSERT_AND_SEND,
}

data class DeckLayout(
    val keysPerRow: Int = DEFAULT_KEYS_PER_ROW,
    val configuredOnly: Boolean = false,
) {
    fun normalized(): DeckLayout = copy(
        keysPerRow = keysPerRow.coerceIn(MIN_KEYS_PER_ROW, MAX_KEYS_PER_ROW),
    )

    companion object {
        const val MIN_KEYS_PER_ROW = 1
        const val MAX_KEYS_PER_ROW = 5
        const val DEFAULT_KEYS_PER_ROW = 5
    }
}

data class PhraseKey(
    val position: Int,
    val label: String,
    val message: String,
    val action: PhraseAction,
) {
    val isConfigured: Boolean get() = message.isNotBlank()
    val sendsImmediately: Boolean get() = action == PhraseAction.INSERT_AND_SEND

    val pageIndex: Int get() = position / PhraseConfig.KEYS_PER_PAGE
    val slotOnPage: Int get() = (position % PhraseConfig.KEYS_PER_PAGE) + 1
    val displayLabel: String
        get() = label.trim().ifBlank { "Key $slotOnPage" }
}

data class PhraseConfig(
    val keys: List<PhraseKey>,
) {
    fun normalized(): PhraseConfig {
        val byPosition = keys
            .filter { it.position in 0 until KEY_COUNT }
            .associateBy(PhraseKey::position)
        return PhraseConfig(
            keys = (0 until KEY_COUNT).map { position ->
                val source = byPosition[position] ?: emptyKey(position)
                source.copy(
                    position = position,
                    label = source.label.trim().take(MAX_LABEL_LENGTH),
                    message = source.message.take(MAX_MESSAGE_LENGTH),
                )
            },
        )
    }

    fun update(updated: PhraseKey): PhraseConfig {
        if (updated.position !in 0 until KEY_COUNT) return normalized()
        return copy(
            keys = normalized().keys.map { current ->
                if (current.position == updated.position) updated else current
            },
        ).normalized()
    }

    fun swap(firstPosition: Int, secondPosition: Int): PhraseConfig {
        if (firstPosition !in 0 until KEY_COUNT || secondPosition !in 0 until KEY_COUNT) {
            return normalized()
        }
        val swapped = normalized().keys.toMutableList()
        if (firstPosition != secondPosition) {
            val first = swapped[firstPosition]
            val second = swapped[secondPosition]
            swapped[firstPosition] = second.copy(position = firstPosition)
            swapped[secondPosition] = first.copy(position = secondPosition)
        }
        return PhraseConfig(swapped).normalized()
    }

    fun keysForPage(pageIndex: Int): List<PhraseKey> {
        require(pageIndex in 0 until PAGE_COUNT)
        val start = pageIndex * KEYS_PER_PAGE
        return normalized().keys.subList(start, start + KEYS_PER_PAGE)
    }

    fun isPageConfigured(pageIndex: Int): Boolean = keysForPage(pageIndex).any(PhraseKey::isConfigured)

    companion object {
        const val PAGE_COUNT = 2
        const val KEYS_PER_PAGE = 20
        const val KEY_COUNT = PAGE_COUNT * KEYS_PER_PAGE
        const val MAX_LABEL_LENGTH = 30
        const val MAX_MESSAGE_LENGTH = 4_000

        fun emptyKey(position: Int) = PhraseKey(
            position = position,
            label = "",
            message = "",
            action = PhraseAction.INSERT_AND_SEND,
        )

        fun empty() = PhraseConfig((0 until KEY_COUNT).map(::emptyKey))
    }
}

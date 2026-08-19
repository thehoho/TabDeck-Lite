package app.tapdeck.keyboard.lite.model

enum class PhraseAction {
    INSERT,
    INSERT_AND_SEND,
}

data class PhraseKey(
    val position: Int,
    val label: String,
    val message: String,
    val action: PhraseAction,
) {
    val isConfigured: Boolean get() = message.isNotBlank()
    val sendsImmediately: Boolean get() = action == PhraseAction.INSERT_AND_SEND

    val displayLabel: String
        get() = label.trim().ifBlank { "Key ${position + 1}" }
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

    fun move(fromPosition: Int, toPosition: Int): PhraseConfig {
        if (fromPosition !in 0 until KEY_COUNT || toPosition !in 0 until KEY_COUNT) {
            return normalized()
        }
        val reordered = normalized().keys.toMutableList()
        if (fromPosition != toPosition) {
            val moving = reordered.removeAt(fromPosition)
            reordered.add(toPosition, moving)
        }
        return PhraseConfig(
            reordered.mapIndexed { position, key -> key.copy(position = position) },
        ).normalized()
    }

    companion object {
        const val KEY_COUNT = 20
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

package app.tapdeck.keyboard.lite.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseModelsTest {
    @Test
    fun newDeckHasExactlyTwentyEmptyKeys() {
        val config = PhraseConfig.empty().normalized()

        assertEquals(20, config.keys.size)
        assertEquals((0 until 20).toList(), config.keys.map(PhraseKey::position))
        assertTrue(config.keys.all { !it.isConfigured })
        assertTrue(config.keys.all(PhraseKey::sendsImmediately))
    }

    @Test
    fun normalizeFillsMissingPositionsAndPreservesConfiguredKey() {
        val source = PhraseConfig(
            listOf(PhraseKey(7, "Hello", "Hello there", PhraseAction.INSERT)),
        ).normalized()

        assertEquals(20, source.keys.size)
        assertEquals("Hello there", source.keys[7].message)
        assertFalse(source.keys[7].sendsImmediately)
        assertTrue(source.keys.filterIndexed { index, _ -> index != 7 }.all { !it.isConfigured })
    }

    @Test
    fun normalizeClampsUserControlledText() {
        val config = PhraseConfig(
            listOf(
                PhraseKey(
                    position = 0,
                    label = " L".repeat(100),
                    message = "x".repeat(5_000),
                    action = PhraseAction.INSERT_AND_SEND,
                ),
            ),
        ).normalized()

        assertEquals(PhraseConfig.MAX_LABEL_LENGTH, config.keys[0].label.length)
        assertEquals(PhraseConfig.MAX_MESSAGE_LENGTH, config.keys[0].message.length)
    }

    @Test
    fun updateChangesOnlyTheSelectedPosition() {
        val original = PhraseConfig.empty()
        val updated = original.update(
            PhraseKey(3, "Status", "All good", PhraseAction.INSERT_AND_SEND),
        )

        assertEquals("All good", updated.keys[3].message)
        assertTrue(updated.keys.filterIndexed { index, _ -> index != 3 }.all { !it.isConfigured })
    }
}

package app.tapdeck.keyboard.lite.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseModelsTest {
    @Test
    fun deckLayoutClampsKeysPerRowAndKeepsVisibilityChoice() {
        assertEquals(1, DeckLayout(keysPerRow = -4).normalized().keysPerRow)
        assertEquals(5, DeckLayout(keysPerRow = 12).normalized().keysPerRow)
        assertTrue(DeckLayout(configuredOnly = true).normalized().configuredOnly)
    }

    @Test
    fun newDeckHasTwoPagesOfTwentyEmptyKeys() {
        val config = PhraseConfig.empty().normalized()

        assertEquals(40, config.keys.size)
        assertEquals((0 until 40).toList(), config.keys.map(PhraseKey::position))
        assertTrue(config.keys.all { !it.isConfigured })
        assertTrue(config.keys.all(PhraseKey::sendsImmediately))
    }

    @Test
    fun normalizeFillsMissingPositionsAndPreservesConfiguredKey() {
        val source = PhraseConfig(
            listOf(PhraseKey(7, "Hello", "Hello there", PhraseAction.INSERT)),
        ).normalized()

        assertEquals(40, source.keys.size)
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

    @Test
    fun pageSlicesKeepTwentyIndependentPositionsAndLocalSlotNumbers() {
        val config = PhraseConfig.empty()
            .update(PhraseKey(0, "First", "p1", PhraseAction.INSERT))
            .update(PhraseKey(20, "Second", "p2", PhraseAction.INSERT))

        assertEquals(20, config.keysForPage(0).size)
        assertEquals(20, config.keysForPage(1).size)
        assertEquals("p1", config.keysForPage(0).first().message)
        assertEquals("p2", config.keysForPage(1).first().message)
        assertEquals(1, config.keys[0].slotOnPage)
        assertEquals(1, config.keys[20].slotOnPage)
        assertTrue(config.isPageConfigured(1))
    }

    @Test
    fun swappingLastKeyWithFirstOnlyExchangesThoseTwoSlots() {
        val source = configuredDeck()

        val swapped = source.swap(firstPosition = 19, secondPosition = 0)

        assertEquals("Message 19", swapped.keys[0].message)
        assertEquals("Message 1", swapped.keys[1].message)
        assertEquals("Message 0", swapped.keys[19].message)
        assertEquals((0 until 40).toList(), swapped.keys.map(PhraseKey::position))
    }

    @Test
    fun swappingFifthKeyWithSeventhLeavesTheSixthKeyUntouched() {
        val source = configuredDeck()

        val swapped = source.swap(firstPosition = 4, secondPosition = 6)

        assertEquals(
            listOf("Message 6", "Message 5", "Message 4"),
            swapped.keys.slice(4..6).map(PhraseKey::message),
        )
        assertEquals(PhraseAction.INSERT_AND_SEND, swapped.keys[4].action)
    }

    @Test
    fun sequentialEditsUseTheLatestDeckAndPreserveAllFortyKeys() {
        val configured = (0 until PhraseConfig.KEY_COUNT).fold(PhraseConfig.empty()) { deck, position ->
            deck.update(
                PhraseKey(
                    position = position,
                    label = "Shortcut ${position + 1}",
                    message = "Command ${position + 1}",
                    action = PhraseAction.INSERT_AND_SEND,
                ),
            )
        }

        assertEquals(
            (1..PhraseConfig.KEY_COUNT).map { "Command $it" },
            configured.keys.map(PhraseKey::message),
        )
        assertTrue(configured.keys.all(PhraseKey::isConfigured))
    }

    private fun configuredDeck() = PhraseConfig(
        (0 until PhraseConfig.KEY_COUNT).map { position ->
            PhraseKey(
                position = position,
                label = "Key $position",
                message = "Message $position",
                action = if (position == 0) PhraseAction.INSERT else PhraseAction.INSERT_AND_SEND,
            )
        },
    )
}

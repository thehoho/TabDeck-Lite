package app.tapdeck.keyboard.lite.data

import app.tapdeck.keyboard.lite.model.PhraseAction
import app.tapdeck.keyboard.lite.model.PhraseConfig
import app.tapdeck.keyboard.lite.model.PhraseKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhraseConfigCodecTest {
    @Test
    fun roundTripPreservesMessagesAndActions() {
        val source = PhraseConfig.empty()
            .update(PhraseKey(0, "Send", "hello", PhraseAction.INSERT_AND_SEND))
            .update(PhraseKey(1, "Edit", "draft", PhraseAction.INSERT))

        val decoded = PhraseConfigCodec.decode(PhraseConfigCodec.encode(source))

        assertEquals("hello", decoded.keys[0].message)
        assertTrue(decoded.keys[0].sendsImmediately)
        assertEquals("draft", decoded.keys[1].message)
        assertFalse(decoded.keys[1].sendsImmediately)
    }

    @Test
    fun malformedJsonReturnsSafeEmptyDeck() {
        val decoded = PhraseConfigCodec.decode("not-json")

        assertEquals(40, decoded.keys.size)
        assertTrue(decoded.keys.all { !it.isConfigured })
    }

    @Test
    fun oldTwentyKeyStorageExpandsToAnEmptySecondPage() {
        val raw = """{"keys":[{"position":0,"label":"Old","message":"kept","action":"INSERT"}]}"""

        val decoded = PhraseConfigCodec.decode(raw)

        assertEquals(40, decoded.keys.size)
        assertEquals("kept", decoded.keys[0].message)
        assertTrue(decoded.keysForPage(1).all { !it.isConfigured })
    }

    @Test
    fun unknownActionUsesSafeImmediateSendDefault() {
        val raw = """{"keys":[{"position":0,"label":"A","message":"B","action":"UNKNOWN"}]}"""

        val decoded = PhraseConfigCodec.decode(raw)

        assertTrue(decoded.keys[0].sendsImmediately)
    }
}

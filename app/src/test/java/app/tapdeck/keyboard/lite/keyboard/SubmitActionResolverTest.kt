package app.tapdeck.keyboard.lite.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitActionResolverTest {
    @Test
    fun discordPlanWaitsForComposerState() {
        val plan = SubmitActionResolver.immediateMessage()

        assertEquals(250L, plan.delayAfterCommitMs)
    }

    @Test
    fun discordPlanUsesVerifiedRawEnter() {
        assertTrue(SubmitActionResolver.immediateMessage().useRawEnter)
    }
}

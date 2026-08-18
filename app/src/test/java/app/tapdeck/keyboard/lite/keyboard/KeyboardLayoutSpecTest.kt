package app.tapdeck.keyboard.lite.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutSpecTest {
    @Test
    fun twentyPhraseKeysFillFiveColumnsAndFourRows() {
        val cells = (0 until KeyboardLayoutSpec.PHRASE_KEY_COUNT).map { position ->
            KeyboardLayoutSpec.rowFor(position) to KeyboardLayoutSpec.columnFor(position)
        }

        assertEquals(20, cells.distinct().size)
        assertEquals(0..3, cells.map { it.first }.toSet().min()..cells.map { it.first }.toSet().max())
        assertEquals(0..4, cells.map { it.second }.toSet().min()..cells.map { it.second }.toSet().max())
    }

    @Test
    fun keyboardIncludesACompactNavigationSafeArea() {
        assertEquals(310, KeyboardLayoutSpec.TOTAL_HEIGHT_DP)
        assertTrue(KeyboardLayoutSpec.TOTAL_HEIGHT_DP <= 320)
        assertTrue(KeyboardLayoutSpec.BOTTOM_PADDING_DP >= 36)
    }

    @Test
    fun utilityRowHasOneControlPerPhraseColumn() {
        assertEquals(KeyboardLayoutSpec.PHRASE_COLUMNS, KeyboardLayoutSpec.UTILITY_KEY_COUNT)
    }

    @Test
    fun bottomPaddingUsesFallbackWhenNavigationInsetIsSmall() {
        assertEquals(
            36,
            KeyboardLayoutSpec.safeBottomPaddingPx(
                navigationInsetPx = 0,
                fallbackPaddingPx = 36,
                navigationGapPx = 10,
            ),
        )
    }

    @Test
    fun bottomPaddingClearsTallNavigationControls() {
        assertEquals(
            58,
            KeyboardLayoutSpec.safeBottomPaddingPx(
                navigationInsetPx = 48,
                fallbackPaddingPx = 36,
                navigationGapPx = 10,
            ),
        )
    }
}

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
    fun fourConfiguredKeysCanFillOneLargeRow() {
        assertEquals(1, KeyboardLayoutSpec.rowCount(keyCount = 4, columns = 4))
        assertEquals(212, KeyboardLayoutSpec.keyHeightDp(keyCount = 4, columns = 4))
        assertEquals(216, KeyboardLayoutSpec.contentHeightDp(keyCount = 4, columns = 4))
    }

    @Test
    fun twentyKeysSupportBothFourByFiveAndFiveByFourGrids() {
        assertEquals(5, KeyboardLayoutSpec.rowCount(keyCount = 20, columns = 4))
        assertEquals(4, KeyboardLayoutSpec.rowCount(keyCount = 20, columns = 5))
    }

    @Test
    fun threeKeysSupportVerticalAndHorizontalLayouts() {
        assertEquals(3, KeyboardLayoutSpec.rowCount(keyCount = 3, columns = 1))
        assertEquals(1, KeyboardLayoutSpec.rowCount(keyCount = 3, columns = 3))
    }

    @Test
    fun oneKeyPerRowCreatesScrollableContent() {
        assertEquals(20, KeyboardLayoutSpec.rowCount(keyCount = 20, columns = 1))
        assertEquals(50, KeyboardLayoutSpec.keyHeightDp(keyCount = 20, columns = 1))
        assertEquals(1080, KeyboardLayoutSpec.contentHeightDp(keyCount = 20, columns = 1))
    }

    @Test
    fun pageTwoReplacesEnterOnlyWhenSwipeIsOff() {
        assertTrue(KeyboardLayoutSpec.usesPageToggle(pageTwoConfigured = true, swipeEnabled = false))
        assertTrue(!KeyboardLayoutSpec.usesPageToggle(pageTwoConfigured = false, swipeEnabled = false))
        assertTrue(!KeyboardLayoutSpec.usesPageToggle(pageTwoConfigured = true, swipeEnabled = true))
    }

    @Test
    fun swipesMoveImmediatelyBetweenPagesWithoutLeavingBounds() {
        assertEquals(1, KeyboardLayoutSpec.pageAfterSwipe(activePage = 0, direction = 1, pageCount = 2))
        assertEquals(1, KeyboardLayoutSpec.pageAfterSwipe(activePage = 1, direction = 1, pageCount = 2))
        assertEquals(0, KeyboardLayoutSpec.pageAfterSwipe(activePage = 1, direction = -1, pageCount = 2))
        assertEquals(0, KeyboardLayoutSpec.pageAfterSwipe(activePage = 0, direction = -1, pageCount = 2))
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
    fun utilityRowKeepsSettingsAwayFromTheCenterTapTarget() {
        assertEquals(
            listOf(
                KeyboardLayoutSpec.UtilityKey.ABC,
                KeyboardLayoutSpec.UtilityKey.SETTINGS,
                KeyboardLayoutSpec.UtilityKey.BACKSPACE,
                KeyboardLayoutSpec.UtilityKey.KEYBOARD_PICKER,
                KeyboardLayoutSpec.UtilityKey.ENTER,
            ),
            KeyboardLayoutSpec.UTILITY_KEY_ORDER,
        )
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

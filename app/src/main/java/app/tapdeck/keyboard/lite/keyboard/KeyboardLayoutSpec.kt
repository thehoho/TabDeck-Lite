package app.tapdeck.keyboard.lite.keyboard

object KeyboardLayoutSpec {
    const val PHRASE_COLUMNS = 5
    const val PHRASE_ROWS = 4
    const val PHRASE_KEY_COUNT = PHRASE_COLUMNS * PHRASE_ROWS
    const val PHRASE_KEY_HEIGHT_DP = 54
    const val PHRASE_VIEWPORT_HEIGHT_DP = PHRASE_ROWS * PHRASE_KEY_HEIGHT_DP
    const val PHRASE_KEY_MARGIN_DP = 2

    enum class UtilityKey {
        ABC,
        SETTINGS,
        BACKSPACE,
        KEYBOARD_PICKER,
        ENTER,
    }

    val UTILITY_KEY_ORDER = listOf(
        UtilityKey.ABC,
        UtilityKey.SETTINGS,
        UtilityKey.BACKSPACE,
        UtilityKey.KEYBOARD_PICKER,
        UtilityKey.ENTER,
    )
    const val UTILITY_KEY_COUNT = 5
    const val UTILITY_ROW_HEIGHT_DP = 48
    const val UTILITY_ROW_GAP_DP = 4

    const val TOP_PADDING_DP = 6
    const val BOTTOM_PADDING_DP = 36
    const val NAVIGATION_GAP_DP = 10

    const val TOTAL_HEIGHT_DP =
        TOP_PADDING_DP +
            PHRASE_VIEWPORT_HEIGHT_DP +
            UTILITY_ROW_GAP_DP +
            UTILITY_ROW_HEIGHT_DP +
            BOTTOM_PADDING_DP

    fun rowFor(position: Int, columns: Int = PHRASE_COLUMNS): Int {
        require(position >= 0)
        require(columns in 1..PHRASE_COLUMNS)
        return position / columns
    }

    fun columnFor(position: Int, columns: Int = PHRASE_COLUMNS): Int {
        require(position >= 0)
        require(columns in 1..PHRASE_COLUMNS)
        return position % columns
    }

    fun rowCount(keyCount: Int, columns: Int): Int {
        require(keyCount >= 0)
        require(columns in 1..PHRASE_COLUMNS)
        return maxOf(1, (keyCount + columns - 1) / columns)
    }

    fun keyHeightDp(keyCount: Int, columns: Int): Int {
        val rows = rowCount(keyCount, columns)
        val outerHeight = if (rows <= PHRASE_ROWS) {
            PHRASE_VIEWPORT_HEIGHT_DP / rows
        } else {
            PHRASE_KEY_HEIGHT_DP
        }
        return maxOf(1, outerHeight - (PHRASE_KEY_MARGIN_DP * 2))
    }

    fun contentHeightDp(keyCount: Int, columns: Int): Int {
        val rows = rowCount(keyCount, columns)
        return maxOf(PHRASE_VIEWPORT_HEIGHT_DP, rows * PHRASE_KEY_HEIGHT_DP)
    }

    fun usesPageToggle(pageTwoConfigured: Boolean, swipeEnabled: Boolean): Boolean =
        pageTwoConfigured && !swipeEnabled

    fun pageAfterSwipe(activePage: Int, direction: Int, pageCount: Int): Int {
        require(pageCount > 0)
        require(activePage in 0 until pageCount)
        require(direction == -1 || direction == 1)
        return (activePage + direction).coerceIn(0, pageCount - 1)
    }

    fun safeBottomPaddingPx(
        navigationInsetPx: Int,
        fallbackPaddingPx: Int,
        navigationGapPx: Int,
    ): Int {
        require(navigationInsetPx >= 0)
        require(fallbackPaddingPx >= 0)
        require(navigationGapPx >= 0)
        return maxOf(fallbackPaddingPx, navigationInsetPx + navigationGapPx)
    }
}

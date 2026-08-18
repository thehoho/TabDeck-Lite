package app.tapdeck.keyboard.lite.keyboard

object KeyboardLayoutSpec {
    const val PHRASE_COLUMNS = 5
    const val PHRASE_ROWS = 4
    const val PHRASE_KEY_COUNT = PHRASE_COLUMNS * PHRASE_ROWS
    const val PHRASE_KEY_HEIGHT_DP = 54

    const val UTILITY_KEY_COUNT = 5
    const val UTILITY_ROW_HEIGHT_DP = 48
    const val UTILITY_ROW_GAP_DP = 4

    const val TOP_PADDING_DP = 6
    const val BOTTOM_PADDING_DP = 36
    const val NAVIGATION_GAP_DP = 10

    const val TOTAL_HEIGHT_DP =
        TOP_PADDING_DP +
            (PHRASE_ROWS * PHRASE_KEY_HEIGHT_DP) +
            UTILITY_ROW_GAP_DP +
            UTILITY_ROW_HEIGHT_DP +
            BOTTOM_PADDING_DP

    fun rowFor(position: Int): Int {
        require(position in 0 until PHRASE_KEY_COUNT)
        return position / PHRASE_COLUMNS
    }

    fun columnFor(position: Int): Int {
        require(position in 0 until PHRASE_KEY_COUNT)
        return position % PHRASE_COLUMNS
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

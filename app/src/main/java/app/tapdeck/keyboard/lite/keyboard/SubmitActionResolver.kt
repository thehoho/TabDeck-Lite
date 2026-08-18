package app.tapdeck.keyboard.lite.keyboard

data class ImmediateSubmitPlan(
    val delayAfterCommitMs: Long,
    val useRawEnter: Boolean,
)

object SubmitActionResolver {
    /**
     * Discord updates its composer after commitText(). A short delay followed by
     * raw Enter reproduces the sequence verified on physical devices without
     * dismissing and reopening the keyboard.
     */
    fun immediateMessage() = ImmediateSubmitPlan(
        delayAfterCommitMs = 250L,
        useRawEnter = true,
    )
}

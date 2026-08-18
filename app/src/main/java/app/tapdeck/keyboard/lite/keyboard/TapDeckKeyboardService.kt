package app.tapdeck.keyboard.lite.keyboard

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import app.tapdeck.keyboard.lite.MainActivity
import app.tapdeck.keyboard.lite.data.PhraseRepository
import app.tapdeck.keyboard.lite.model.PhraseConfig
import app.tapdeck.keyboard.lite.model.PhraseKey

@SuppressLint("SetTextI18n")
class TapDeckKeyboardService : InputMethodService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var repository: PhraseRepository
    private var subscription: PhraseRepository.Subscription? = null
    private var latestConfig = PhraseConfig.empty()
    private var keyGrid: GridLayout? = null
    private var inputRoot: LinearLayout? = null
    private var inputIsActive = false
    private var pendingSend: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        repository = PhraseRepository.get(this)
        subscription = repository.observe { config ->
            mainHandler.post {
                latestConfig = config
                renderKeys()
            }
        }
    }

    override fun onCreateInputView(): View {
        latestConfig = repository.getConfig()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(BACKGROUND))
        }
        applySafeAreaPadding(root)
        inputRoot = root

        keyGrid = GridLayout(this).apply {
            columnCount = KeyboardLayoutSpec.PHRASE_COLUMNS
            rowCount = KeyboardLayoutSpec.PHRASE_ROWS
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }
        root.addView(
            keyGrid,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        root.addView(
            createUtilityRow(),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(KeyboardLayoutSpec.UTILITY_ROW_HEIGHT_DP)).apply {
                topMargin = dp(KeyboardLayoutSpec.UTILITY_ROW_GAP_DP)
            },
        )
        renderKeys()
        return root
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        cancelPendingSend()
        inputIsActive = true
    }

    override fun onFinishInput() {
        inputIsActive = false
        cancelPendingSend()
        super.onFinishInput()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    @Suppress("DEPRECATION")
    private fun applySafeAreaPadding(root: View) {
        val horizontalPadding = dp(6)
        val topPadding = dp(KeyboardLayoutSpec.TOP_PADDING_DP)
        val fallbackBottomPadding = dp(KeyboardLayoutSpec.BOTTOM_PADDING_DP)
        val navigationGap = dp(KeyboardLayoutSpec.NAVIGATION_GAP_DP)

        root.setPadding(horizontalPadding, topPadding, horizontalPadding, fallbackBottomPadding)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val navigationInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                insets.systemWindowInsetBottom
            }
            val bottomPadding = KeyboardLayoutSpec.safeBottomPaddingPx(
                navigationInsetPx = navigationInset,
                fallbackPaddingPx = fallbackBottomPadding,
                navigationGapPx = navigationGap,
            )
            view.setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding)
            insets
        }
        root.requestApplyInsets()
    }

    private fun renderKeys() {
        val grid = keyGrid ?: return
        grid.removeAllViews()
        latestConfig.normalized().keys.forEach { phrase ->
            val row = KeyboardLayoutSpec.rowFor(phrase.position)
            val column = KeyboardLayoutSpec.columnFor(phrase.position)
            grid.addView(
                createPhraseButton(phrase),
                GridLayout.LayoutParams(
                    GridLayout.spec(row, 1f),
                    GridLayout.spec(column, 1f),
                ).apply {
                    width = 0
                    height = dp(KeyboardLayoutSpec.PHRASE_KEY_HEIGHT_DP)
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                },
            )
        }
    }

    private fun createPhraseButton(phrase: PhraseKey): Button = Button(this).apply {
        val configured = phrase.isConfigured
        text = if (configured) {
            "${phrase.position + 1}\n${phrase.displayLabel}"
        } else {
            "${phrase.position + 1}\nEmpty"
        }
        contentDescription = when {
            !configured -> "Key ${phrase.position + 1}, empty"
            phrase.sendsImmediately -> "Key ${phrase.position + 1}, ${phrase.displayLabel}, insert and send"
            else -> "Key ${phrase.position + 1}, ${phrase.displayLabel}, insert only"
        }
        isAllCaps = false
        textSize = 10.5f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (configured) Color.WHITE else color(MUTED))
        gravity = Gravity.CENTER
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(2), 0, dp(2), 0)
        stateListAnimator = null
        background = keyBackground(phrase)
        isEnabled = configured
        alpha = if (configured) 1f else 0.78f
        setOnClickListener { runPhrase(phrase) }
    }

    private fun createUtilityRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        addUtilityButton(
            label = "ABC",
            description = "Switch back to the previous typing keyboard",
            style = UtilityStyle.ACCENT,
            textSize = 11f,
        ) { switchBackToTypingKeyboard() }
        addUtilityButton(
            label = "⌫",
            description = "Backspace",
            style = UtilityStyle.NORMAL,
            textSize = 21f,
        ) { deleteOneCharacter() }
        addUtilityButton(
            label = "SET",
            description = "Open TapDeck Lite settings",
            style = UtilityStyle.NORMAL,
            textSize = 10f,
        ) { openSettings() }
        addUtilityButton(
            label = "⌨",
            description = "Show all enabled keyboards",
            style = UtilityStyle.NORMAL,
            textSize = 19f,
        ) { showKeyboardPicker() }
        addUtilityButton(
            label = "↵",
            description = "Enter",
            style = UtilityStyle.SEND,
            textSize = 21f,
        ) { sendRawEnter() }
    }

    private fun LinearLayout.addUtilityButton(
        label: String,
        description: String,
        style: UtilityStyle,
        textSize: Float,
        action: () -> Unit,
    ) {
        addView(Button(context).apply {
            text = label
            contentDescription = description
            isAllCaps = false
            this.textSize = textSize
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (style == UtilityStyle.ACCENT) color(BACKGROUND) else Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(2), 0, dp(2), 0)
            stateListAnimator = null
            background = when (style) {
                UtilityStyle.ACCENT -> rounded(ACCENT, ACCENT, 11)
                UtilityStyle.SEND -> rounded(SEND_KEY, ACCENT_DARK, 11)
                UtilityStyle.NORMAL -> rounded(PANEL, BORDER, 11)
            }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                action()
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        })
    }

    private fun runPhrase(phrase: PhraseKey) {
        if (!phrase.isConfigured || pendingSend != null) return
        val connection = currentInputConnection ?: return
        if (!inputIsActive) return

        connection.beginBatchEdit()
        connection.finishComposingText()
        connection.commitText(phrase.message, 1)
        connection.endBatchEdit()
        inputRoot?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

        if (phrase.sendsImmediately) scheduleEnter()
    }

    private fun scheduleEnter() {
        val plan = SubmitActionResolver.immediateMessage()
        val runnable = Runnable {
            pendingSend = null
            if (!inputIsActive) return@Runnable
            currentInputConnection?.finishComposingText()
            if (plan.useRawEnter) sendRawEnter()
        }
        pendingSend = runnable
        mainHandler.postDelayed(runnable, plan.delayAfterCommitMs)
    }

    private fun deleteOneCharacter() {
        if (!inputIsActive) return
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun sendRawEnter() {
        if (!inputIsActive) return
        currentInputConnection?.let { connection ->
            connection.finishComposingText()
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun openSettings() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun showKeyboardPicker() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    private fun cancelPendingSend() {
        pendingSend?.let(mainHandler::removeCallbacks)
        pendingSend = null
    }

    @Suppress("DEPRECATION")
    private fun switchBackToTypingKeyboard() {
        val switched = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchToPreviousInputMethod() || switchToNextInputMethod(false)
        } else {
            val token = window?.window?.attributes?.token
            val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            token != null && (
                manager.switchToLastInputMethod(token) ||
                    manager.switchToNextInputMethod(token, false)
                )
        }
        if (!switched) showKeyboardPicker()
    }

    private fun keyBackground(phrase: PhraseKey): GradientDrawable {
        val (fill, stroke) = when {
            !phrase.isConfigured -> EMPTY_KEY to BORDER
            phrase.sendsImmediately -> SEND_KEY to ACCENT_DARK
            else -> INSERT_KEY to WARM
        }
        return rounded(fill = fill, stroke = stroke, radius = 10)
    }

    private fun rounded(fill: String, stroke: String, radius: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radius).toFloat()
        setColor(color(fill))
        setStroke(dp(1), color(stroke))
    }

    private fun color(hex: String): Int = Color.parseColor(hex)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        inputIsActive = false
        cancelPendingSend()
        subscription?.cancel()
        keyGrid = null
        inputRoot = null
        super.onDestroy()
    }

    private enum class UtilityStyle {
        ACCENT,
        NORMAL,
        SEND,
    }

    companion object {
        private const val BACKGROUND = "#0B1118"
        private const val PANEL = "#111C27"
        private const val BORDER = "#2A3948"
        private const val EMPTY_KEY = "#101821"
        private const val SEND_KEY = "#183A35"
        private const val INSERT_KEY = "#352B1C"
        private const val ACCENT = "#70E1B5"
        private const val ACCENT_DARK = "#4ABF98"
        private const val WARM = "#F4B860"
        private const val MUTED = "#9DAEBC"
    }
}

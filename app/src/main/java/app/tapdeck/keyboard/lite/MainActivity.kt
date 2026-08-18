package app.tapdeck.keyboard.lite

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import app.tapdeck.keyboard.lite.data.PhraseRepository
import app.tapdeck.keyboard.lite.keyboard.TapDeckKeyboardService
import app.tapdeck.keyboard.lite.model.PhraseAction
import app.tapdeck.keyboard.lite.model.PhraseConfig
import app.tapdeck.keyboard.lite.model.PhraseKey

@SuppressLint("SetTextI18n")
class MainActivity : Activity() {
    private lateinit var repository: PhraseRepository
    private lateinit var screenContent: LinearLayout
    private lateinit var tabRow: LinearLayout
    private var subscription: PhraseRepository.Subscription? = null
    private var latestConfig = PhraseConfig.empty()
    private var currentTab = Tab.SETUP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = color(BACKGROUND)
        window.navigationBarColor = color(BACKGROUND)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        repository = PhraseRepository.get(this)
        currentTab = if (readKeyboardStatus().enabled) Tab.KEYS else Tab.SETUP

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(BACKGROUND))
            addView(createHeader())
        }
        tabRow = createTabs()
        root.addView(tabRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(52),
        ).apply {
            marginStart = dp(16)
            marginEnd = dp(16)
            bottomMargin = dp(6)
        })

        screenContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(44))
        }
        root.addView(ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(
                screenContent,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        subscription = repository.observe { config ->
            runOnUiThread {
                latestConfig = config
                renderCurrentTab()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            latestConfig = repository.getConfig()
            renderCurrentTab()
        }
    }

    override fun onDestroy() {
        subscription?.cancel()
        super.onDestroy()
    }

    private fun createHeader(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(25), dp(20), dp(20))
        addView(TextView(context).apply {
            text = "TapDeck Lite"
            setTextColor(Color.WHITE)
            textSize = 31f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        })
        addView(TextView(context).apply {
            text = "One tap. One command."
            setTextColor(color(MUTED))
            textSize = 14f
            setPadding(0, dp(6), 0, 0)
        })
    }

    private fun createTabs(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        background = rounded(PANEL, BORDER, 15)
        setPadding(dp(4), dp(4), dp(4), dp(4))
        Tab.entries.forEach { tab ->
            addView(Button(context).apply {
                tag = tab
                text = tab.label
                isAllCaps = false
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                minWidth = 0
                minimumWidth = 0
                minHeight = 0
                minimumHeight = 0
                setPadding(dp(4), 0, dp(4), 0)
                stateListAnimator = null
                setOnClickListener {
                    currentTab = tab
                    renderCurrentTab()
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            })
        }
    }

    private fun renderCurrentTab() {
        if (!::screenContent.isInitialized || !::tabRow.isInitialized) return
        tabRow.forEachChild { view ->
            val button = view as Button
            val active = button.tag == currentTab
            button.setTextColor(if (active) color(BACKGROUND) else color(MUTED))
            button.background = rounded(
                if (active) ACCENT else PANEL,
                if (active) ACCENT else PANEL,
                11,
            )
        }

        screenContent.removeAllViews()
        when (currentTab) {
            Tab.SETUP -> addSetupTab()
            Tab.KEYS -> addKeysTab()
            Tab.PRIVACY -> addPrivacyTab()
        }
        addFooter()
    }

    private fun addSetupTab() {
        val status = readKeyboardStatus()
        val statusColor = if (status.selected) ACCENT else WARM
        screenContent.addView(card(statusColor).apply {
            addView(TextView(context).apply {
                text = when {
                    status.selected -> "●  Ready to use"
                    status.enabled -> "●  Enabled — select it next"
                    else -> "●  Two-minute setup"
                }
                setTextColor(color(statusColor))
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
            })
            addView(bodyText(when {
                status.selected -> "TapDeck Lite is your current keyboard. Open Discord or any text field to see your 20 keys."
                status.enabled -> "Android has enabled TapDeck Lite. Choose it once from the keyboard list."
                else -> "Android asks you to approve every downloaded keyboard. TapDeck Lite cannot enable itself."
            }), blockParams(top = 8, bottom = 2))
        }, blockParams(bottom = 20))

        addSectionTitle("Set up the keyboard", "Complete the steps in order. You can return here whenever you need them.")
        addSetupStep(
            number = "1",
            title = if (status.enabled) "Keyboard enabled" else "Enable TapDeck Lite",
            detail = if (status.enabled) {
                "Done. TapDeck Lite is allowed in Android keyboard settings."
            } else {
                "Open Android's keyboard settings and turn on TapDeck Lite."
            },
            buttonLabel = if (status.enabled) "Open settings" else "Enable keyboard",
            primary = !status.enabled,
        ) { safeStartActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }

        addSetupStep(
            number = "2",
            title = if (status.selected) "Keyboard selected" else "Choose TapDeck Lite",
            detail = if (status.selected) {
                "Done. TapDeck Lite is the currently selected keyboard."
            } else {
                "Choose TapDeck Lite from Android's keyboard picker."
            },
            buttonLabel = if (status.selected) "Switch keyboard" else "Choose keyboard",
            primary = status.enabled && !status.selected,
        ) { showKeyboardPicker() }

        screenContent.addView(card(BORDER).apply {
            addView(TextView(context).apply {
                text = "3   Try it here"
                setTextColor(Color.WHITE)
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
            })
            addView(bodyText("Tap the field below. The ABC button on the keyboard returns directly to your previous typing keyboard."), blockParams(top = 7, bottom = 12))
            addView(EditText(context).apply {
                hint = "Tap here, then press a phrase key"
                setHintTextColor(color(MUTED))
                setTextColor(Color.WHITE)
                textSize = 15f
                minLines = 3
                maxLines = 6
                gravity = Gravity.TOP or Gravity.START
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                setPadding(dp(13), dp(12), dp(13), dp(12))
                background = rounded(EMPTY, BORDER, 12)
            }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }, blockParams(bottom = 22))

        addInfoCard(
            "Discord-ready behavior",
            "Insert + send waits briefly for Discord's composer, then sends raw Enter. Insert only leaves the phrase in the text box for editing.",
            ACCENT,
        )

    }

    private fun addSetupStep(
        number: String,
        title: String,
        detail: String,
        buttonLabel: String,
        primary: Boolean,
        action: () -> Unit,
    ) {
        screenContent.addView(card(BORDER).apply {
            val titleRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = number
                    gravity = Gravity.CENTER
                    setTextColor(color(BACKGROUND))
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    background = rounded(if (primary) ACCENT else MUTED, if (primary) ACCENT else MUTED, 18)
                }, LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(11) })
                addView(TextView(context).apply {
                    text = title
                    setTextColor(Color.WHITE)
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            addView(titleRow)
            addView(bodyText(detail), blockParams(top = 9, bottom = 12))
            addView(actionButton(buttonLabel, primary, action))
        }, blockParams(bottom = 13))
    }

    private fun addKeysTab() {
        val config = latestConfig.normalized()
        val configuredCount = config.keys.count(PhraseKey::isConfigured)
        addSectionTitle(
            "Your 20 keys",
            "$configuredCount configured. Tap a card to set its label, message, and action.",
        )
        screenContent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(legendDot(ACCENT))
            addView(bodyText("Insert + send"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(7) })
            addView(legendDot(WARM))
            addView(bodyText("Insert only"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(7) })
        }, blockParams(bottom = 16))

        val grid = GridLayout(this).apply {
            columnCount = 2
            rowCount = 10
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        config.keys.forEach { phrase ->
            val row = phrase.position / 2
            val column = phrase.position % 2
            grid.addView(editorKey(phrase), GridLayout.LayoutParams(
                GridLayout.spec(row, 1f),
                GridLayout.spec(column, 1f),
            ).apply {
                width = 0
                height = dp(94)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
        screenContent.addView(grid, blockParams(bottom = 20))
        addInfoCard(
            "Kept intentionally simple",
            "One deck of 20 command keys, with no accounts, advertisements, tracking, or distracting extras.",
            BORDER,
        )
    }

    private fun editorKey(phrase: PhraseKey): Button = Button(this).apply {
        text = when {
            !phrase.isConfigured -> "${phrase.position + 1}\nSet up\nEmpty"
            phrase.sendsImmediately -> "${phrase.position + 1}\n${phrase.displayLabel}\nInsert + send"
            else -> "${phrase.position + 1}\n${phrase.displayLabel}\nInsert only"
        }
        isAllCaps = false
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (phrase.isConfigured) Color.WHITE else color(MUTED))
        gravity = Gravity.CENTER
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(dp(7), dp(6), dp(7), dp(6))
        stateListAnimator = null
        background = when {
            !phrase.isConfigured -> rounded(EMPTY, BORDER, 14)
            phrase.sendsImmediately -> rounded(SEND, ACCENT_DARK, 14)
            else -> rounded(INSERT, WARM, 14)
        }
        setOnClickListener { showKeyEditor(phrase) }
    }

    private fun showKeyEditor(phrase: PhraseKey) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(5), dp(20), 0)
        }
        val labelInput = EditText(this).apply {
            hint = "Label shown on the key"
            setText(phrase.label)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(PhraseConfig.MAX_LABEL_LENGTH))
            setSelection(text.length)
        }
        val messageInput = EditText(this).apply {
            hint = "Message or phrase"
            setText(phrase.message)
            gravity = Gravity.TOP or Gravity.START
            minLines = 5
            maxLines = 10
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(PhraseConfig.MAX_MESSAGE_LENGTH))
        }
        val sendSwitch = Switch(this).apply {
            text = "Send immediately after inserting"
            isChecked = phrase.sendsImmediately
            setPadding(0, dp(12), 0, dp(5))
        }
        layout.addView(labelInput)
        layout.addView(messageInput, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8) })
        layout.addView(sendSwitch)
        layout.addView(TextView(this).apply {
            text = "On Discord, immediate send uses the tested 250 ms composer delay. Turn this off when you want to edit before sending."
            setTextColor(color("#52616E"))
            textSize = 12f
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle("Key ${phrase.position + 1}")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear", null)
            .setPositiveButton("Save", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val message = messageInput.text.toString()
                val updated = phrase.copy(
                    label = if (message.isBlank()) "" else labelInput.text.toString(),
                    message = message,
                    action = if (sendSwitch.isChecked) {
                        PhraseAction.INSERT_AND_SEND
                    } else {
                        PhraseAction.INSERT
                    },
                )
                repository.updateKey(updated)
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                repository.updateKey(PhraseConfig.emptyKey(phrase.position))
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun addPrivacyTab() {
        addSectionTitle("Private by design", "A small keyboard should have a small permission footprint.")
        addInfoCard(
            "Offline",
            "TapDeck Lite declares no Internet or network permission. It cannot upload your phrases.",
            ACCENT,
        )
        addInfoCard(
            "Local phrases",
            "Your 20 keys are stored in this app's private local storage. Android cloud backup is disabled for the app.",
            ACCENT,
        )
        addInfoCard(
            "No tracking",
            "No account, analytics, advertisements, clipboard access, Accessibility Service, camera, microphone, contacts, or location access.",
            ACCENT,
        )

        addInfoCard(
            "What Android will say",
            "Android shows a strong standard warning for every third-party keyboard because keyboards can receive text-field context. TapDeck Lite only inserts the phrases you configure and does not record what you type elsewhere.",
            WARM,
        )
        addInfoCard(
            "Compatibility",
            "Insert + send is optimized for Discord. It also works in apps where raw Enter sends a message; in apps that treat Enter as a new line, use Insert only and tap that app's Send button.",
            BORDER,
        )
    }

    private fun addSectionTitle(title: String, subtitle: String) {
        screenContent.addView(TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        })
        screenContent.addView(bodyText(subtitle), blockParams(top = 5, bottom = 17))
    }

    private fun addInfoCard(title: String, detail: String, accent: String) {
        screenContent.addView(card(accent).apply {
            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
            })
            addView(bodyText(detail), blockParams(top = 7))
        }, blockParams(bottom = 13))
    }

    private fun addFooter() {
        screenContent.addView(TextView(this).apply {
            text = "TapDeck Lite 1.0.3  •  20 keys  •  Offline"
            setTextColor(color("#657582"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(8))
        }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun card(stroke: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(17), dp(17), dp(17), dp(17))
        background = rounded(PANEL, stroke, 16)
    }

    private fun bodyText(value: String) = TextView(this).apply {
        text = value
        setTextColor(color(TEXT))
        textSize = 14f
        setLineSpacing(0f, 1.12f)
    }

    private fun actionButton(label: String, primary: Boolean, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (primary) color(BACKGROUND) else Color.WHITE)
        minHeight = 0
        minimumHeight = 0
        stateListAnimator = null
        background = rounded(
            if (primary) ACCENT else EMPTY,
            if (primary) ACCENT else BORDER,
            12,
        )
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46))
    }

    private fun legendDot(value: String) = View(this).apply {
        background = rounded(value, value, 8)
        layoutParams = LinearLayout.LayoutParams(dp(12), dp(12))
    }

    private fun readKeyboardStatus(): KeyboardStatus {
        val manager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = manager.enabledInputMethodList.any { it.packageName == packageName }
        val selectedId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
        ).orEmpty()
        val component = ComponentName(this, TapDeckKeyboardService::class.java)
        val selected = selectedId == component.flattenToString() ||
            selectedId == component.flattenToShortString()
        return KeyboardStatus(enabled = enabled, selected = selected)
    }

    private fun showKeyboardPicker() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    private fun safeStartActivity(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(this, "Android could not open that settings screen", Toast.LENGTH_LONG).show()
        }
    }

    private fun rounded(fill: String, stroke: String, radius: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radius).toFloat()
        setColor(color(fill))
        setStroke(dp(1), color(stroke))
    }

    private fun blockParams(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply {
        topMargin = dp(top)
        bottomMargin = dp(bottom)
    }

    private inline fun LinearLayout.forEachChild(action: (View) -> Unit) {
        for (index in 0 until childCount) action(getChildAt(index))
    }

    private fun color(hex: String): Int = Color.parseColor(hex)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private enum class Tab(val label: String) {
        SETUP("Setup"),
        KEYS("Keys"),
        PRIVACY("Privacy"),
    }

    private data class KeyboardStatus(
        val enabled: Boolean,
        val selected: Boolean,
    )

    companion object {
        private const val BACKGROUND = "#0B1118"
        private const val PANEL = "#111C27"
        private const val EMPTY = "#101821"
        private const val SEND = "#183A35"
        private const val INSERT = "#352B1C"
        private const val BORDER = "#2A3948"
        private const val ACCENT = "#70E1B5"
        private const val ACCENT_DARK = "#4ABF98"
        private const val WARM = "#F4B860"
        private const val MUTED = "#9DAEBC"
        private const val TEXT = "#C2CDD6"
    }
}

package app.tapdeck.keyboard.lite

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import app.tapdeck.keyboard.lite.data.PhraseRepository
import app.tapdeck.keyboard.lite.keyboard.KeyboardLayoutSpec
import app.tapdeck.keyboard.lite.keyboard.TapDeckKeyboardService
import app.tapdeck.keyboard.lite.model.DeckLayout
import app.tapdeck.keyboard.lite.model.PhraseAction
import app.tapdeck.keyboard.lite.model.PhraseConfig
import app.tapdeck.keyboard.lite.model.PhraseKey

@SuppressLint("SetTextI18n")
class MainActivity : Activity() {
    private lateinit var repository: PhraseRepository
    private lateinit var screenContent: LinearLayout
    private lateinit var tabRow: LinearLayout
    private lateinit var contentScroll: ScrollView
    private var subscription: PhraseRepository.Subscription? = null
    private var latestConfig = PhraseConfig.empty()
    private var currentTab = Tab.SETUP
    private var editorPage = 0

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
        applyTopSafeArea(root)
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
        contentScroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(
                screenContent,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        root.addView(contentScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()
        latestConfig = repository.getConfig()
        renderCurrentTab()

        subscription = repository.observe { config ->
            runOnUiThread {
                if (config != latestConfig) {
                    latestConfig = config
                    renderCurrentTab()
                }
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

    @Suppress("DEPRECATION")
    private fun applyTopSafeArea(root: View) {
        val designedGap = dp(TOP_SYSTEM_BAR_GAP_DP)
        root.setPadding(0, designedGap, 0, 0)
        root.setOnApplyWindowInsetsListener { view, insets ->
            val statusBarInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.statusBars()).top
            } else {
                insets.systemWindowInsetTop
            }
            view.setPadding(
                0,
                statusBarInset + designedGap,
                0,
                0,
            )
            insets
        }
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
                status.selected -> "TapDeck Lite is your current keyboard. Open Discord or any text field to use your command pages."
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

        addKeyVibrationSetting()
        addPageSwipeSetting()

        addInfoCard(
            "Discord-ready behavior",
            "Insert + send waits briefly for Discord's composer, then sends raw Enter. Insert only leaves the phrase in the text box for editing.",
            ACCENT,
        )

    }

    private fun addKeyVibrationSetting() {
        screenContent.addView(card(BORDER).apply {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(context).apply {
                    text = "Key vibration"
                    setTextColor(Color.WHITE)
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(
                    bodyText("Optional vibration when a keyboard key is tapped. Off by default."),
                    blockParams(top = 6),
                )
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(12)
            })
            row.addView(Switch(context).apply {
                contentDescription = "Vibrate when a TapDeck key is pressed"
                isChecked = repository.isKeyVibrationEnabled()
                setOnCheckedChangeListener { _, enabled ->
                    repository.setKeyVibrationEnabled(enabled)
                }
            })
            addView(row)
        }, blockParams(bottom = 20))
    }

    private fun addPageSwipeSetting() {
        screenContent.addView(card(BORDER).apply {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            row.addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(context).apply {
                    text = "Swipe between pages"
                    setTextColor(Color.WHITE)
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(
                    bodyText(
                        "Off by default. When enabled, horizontal swipes switch command pages and Enter stays available. When off, Page 2 uses a P1/P2 switch key.",
                    ),
                    blockParams(top = 6),
                )
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(12)
            })
            row.addView(Switch(context).apply {
                contentDescription = "Swipe horizontally between TapDeck command pages"
                isChecked = repository.isPageSwipeEnabled()
                setOnCheckedChangeListener { _, enabled ->
                    repository.setPageSwipeEnabled(enabled)
                    Toast.makeText(
                        this@MainActivity,
                        if (enabled) "Page swiping enabled" else "Page switch key enabled",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            })
            addView(row)
        }, blockParams(bottom = 20))
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
        val pageKeys = config.keysForPage(editorPage)
        val configuredCount = pageKeys.count(PhraseKey::isConfigured)
        val totalConfigured = config.keys.count(PhraseKey::isConfigured)
        addSectionTitle(
            "Page ${editorPage + 1} · 20 keys",
            "$configuredCount configured on this page, $totalConfigured across both pages. Tap to edit, or long press and drag onto another slot to swap.",
        )
        addKeyPageSelector(config)
        screenContent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(legendDot(ACCENT))
            addView(bodyText("Insert + send"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(7) })
            addView(legendDot(WARM))
            addView(bodyText("Insert only"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(7) })
        }, blockParams(bottom = 16))

        addKeyboardLayoutSetting()

        val grid = GridLayout(this).apply {
            columnCount = 2
            rowCount = 10
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        pageKeys.forEachIndexed { pagePosition, phrase ->
            val row = pagePosition / 2
            val column = pagePosition % 2
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
            "Two pages, one simple deck",
            "Save up to 40 command keys across two pages, with no accounts, advertisements, tracking, or upgrade prompts.",
            BORDER,
        )
    }

    private fun addKeyPageSelector(config: PhraseConfig) {
        screenContent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
            clipChildren = false
            clipToPadding = false
            repeat(PhraseConfig.PAGE_COUNT) { pageIndex ->
                val active = pageIndex == editorPage
                val count = config.keysForPage(pageIndex).count(PhraseKey::isConfigured)
                addView(Button(context).apply {
                    text = "Page ${pageIndex + 1}  ·  $count saved"
                    isAllCaps = false
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(if (active) color(BACKGROUND) else Color.WHITE)
                    stateListAnimator = null
                    background = rounded(
                        if (active) ACCENT else EMPTY,
                        if (active) ACCENT else BORDER,
                        11,
                    )
                    setOnClickListener {
                        if (editorPage != pageIndex) {
                            editorPage = pageIndex
                            renderCurrentTab()
                            contentScroll.post { contentScroll.scrollTo(0, 0) }
                        }
                    }
                }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                    setMargins(dp(3), dp(2), dp(3), dp(14))
                })
            }
        }, blockParams(top = 8, bottom = 2))
    }

    private fun addKeyboardLayoutSetting() {
        val layout = repository.getDeckLayout()
        val visibility = if (layout.configuredOnly) "Configured keys only" else "All 20 keys per page"
        val columns = if (layout.keysPerRow == 1) {
            "1 key per row"
        } else {
            "${layout.keysPerRow} keys per row"
        }
        screenContent.addView(card(BORDER).apply {
            addView(TextView(context).apply {
                text = "Keyboard layout"
                setTextColor(Color.WHITE)
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
            })
            addView(
                bodyText("$visibility  •  $columns"),
                blockParams(top = 6, bottom = 12),
            )
            addView(actionButton("Customize layout", false, ::showKeyboardLayoutDialog))
        }, blockParams(bottom = 16))
    }

    private fun showKeyboardLayoutDialog() {
        val current = repository.getDeckLayout()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(6), dp(20), 0)
        }
        val configuredOnlySwitch = Switch(this).apply {
            text = "Show configured keys only"
            isChecked = current.configuredOnly
            setPadding(0, dp(6), 0, dp(12))
        }
        val picker = NumberPicker(this).apply {
            minValue = DeckLayout.MIN_KEYS_PER_ROW
            maxValue = DeckLayout.MAX_KEYS_PER_ROW
            value = current.keysPerRow
            wrapSelectorWheel = false
            displayedValues = arrayOf(
                "1 — vertical",
                "2 per row",
                "3 per row",
                "4 per row",
                "5 — widest",
            )
            contentDescription = "Buttons per row"
        }
        val preview = TextView(this).apply {
            setTextColor(color(ACCENT_DARK))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, dp(10))
        }
        fun updatePreview() {
            val keyCount = if (configuredOnlySwitch.isChecked) {
                latestConfig.keysForPage(editorPage).count(PhraseKey::isConfigured)
            } else {
                PhraseConfig.KEYS_PER_PAGE
            }
            if (keyCount == 0) {
                preview.text = "No configured keys — the keyboard will show an empty state."
                return
            }
            val columns = picker.value
            val rows = KeyboardLayoutSpec.rowCount(keyCount, columns)
            preview.text = when {
                columns == 1 -> "Vertical layout  •  1 column × $rows rows"
                rows == 1 -> "Horizontal layout  •  $columns columns × 1 row"
                else -> "Grid layout  •  $columns columns × $rows rows"
            }
        }
        picker.setOnValueChangedListener { _, _, _ -> updatePreview() }
        configuredOnlySwitch.setOnCheckedChangeListener { _, _ -> updatePreview() }
        updatePreview()
        layout.addView(configuredOnlySwitch)
        layout.addView(TextView(this).apply {
            text = "Buttons per row (layout width)"
            setTextColor(color("#52616E"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        })
        layout.addView(
            picker,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(130)),
        )
        layout.addView(preview)
        layout.addView(TextView(this).apply {
            text = "Every shape is supported: 1 per row creates a vertical list; matching the configured-key count creates one horizontal row; 4 per row gives a 4 × 5 grid for all 20 keys; and 5 per row gives 5 × 4. Longer grids scroll without increasing keyboard height."
            setTextColor(color("#52616E"))
            textSize = 12f
        })

        AlertDialog.Builder(this)
            .setTitle("Keyboard layout")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply") { _, _ ->
                repository.setDeckLayout(
                    DeckLayout(
                        keysPerRow = picker.value,
                        configuredOnly = configuredOnlySwitch.isChecked,
                    ),
                )
                renderCurrentTab()
                Toast.makeText(this, "Keyboard layout updated", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun editorKey(phrase: PhraseKey): Button = Button(this).apply {
        text = when {
            !phrase.isConfigured -> "${phrase.slotOnPage}\nSet up\nEmpty"
            phrase.sendsImmediately -> "${phrase.slotOnPage}\n${phrase.displayLabel}\nInsert + send"
            else -> "${phrase.slotOnPage}\n${phrase.displayLabel}\nInsert only"
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
        contentDescription = if (phrase.isConfigured) {
            "Page ${phrase.pageIndex + 1}, key ${phrase.slotOnPage}, ${phrase.displayLabel}. Tap to edit; long press and drag to swap."
        } else {
            "Page ${phrase.pageIndex + 1}, key ${phrase.slotOnPage}, empty. Tap to configure or drop another key here."
        }
        setOnClickListener { showKeyEditor(phrase) }
        if (phrase.isConfigured) {
            setOnLongClickListener { view -> startEditorDrag(view, phrase.position) }
        }
        setOnDragListener { view, event -> handleEditorDrag(view, phrase, event) }
    }

    private fun startEditorDrag(view: View, fromPosition: Int): Boolean {
        val dragState = EditorDragState(fromPosition = fromPosition, sourceView = view)
        val started = view.startDragAndDrop(
            ClipData.newPlainText("TapDeck key", ""),
            View.DragShadowBuilder(view),
            dragState,
            0,
        )
        if (started) {
            view.alpha = DRAG_SOURCE_ALPHA
            view.scaleX = DRAG_SOURCE_SCALE
            view.scaleY = DRAG_SOURCE_SCALE
        }
        return started
    }

    private fun handleEditorDrag(target: View, targetKey: PhraseKey, event: DragEvent): Boolean {
        val state = event.localState as? EditorDragState ?: return false
        val isDifferentTarget = state.fromPosition != targetKey.position
        return when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> state.fromPosition in 0 until PhraseConfig.KEY_COUNT
            DragEvent.ACTION_DRAG_ENTERED -> {
                if (isDifferentTarget) showEditorDropTarget(target)
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                autoScrollDuringEditorDrag(target, event)
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                restoreEditorKeyVisual(target)
                true
            }
            DragEvent.ACTION_DROP -> {
                restoreEditorKeyVisual(target)
                if (isDifferentTarget) {
                    state.toPosition = targetKey.position
                    target.announceForAccessibility(
                        "Swap key ${(state.fromPosition % PhraseConfig.KEYS_PER_PAGE) + 1} with key ${targetKey.slotOnPage}",
                    )
                }
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                restoreEditorKeyVisual(target)
                restoreEditorKeyVisual(state.sourceView)
                if (!state.completed) {
                    state.completed = true
                    val toPosition = state.toPosition
                    if (toPosition != null && toPosition != state.fromPosition) {
                        screenContent.post {
                            swapEditorKeysImmediately(state.fromPosition, toPosition)
                        }
                    }
                }
                true
            }
            else -> true
        }
    }

    private fun showEditorDropTarget(view: View) {
        view.animate()
            .alpha(DROP_TARGET_ALPHA)
            .scaleX(DROP_TARGET_SCALE)
            .scaleY(DROP_TARGET_SCALE)
            .setDuration(DRAG_ANIMATION_DURATION_MS)
            .start()
    }

    private fun restoreEditorKeyVisual(view: View) {
        view.animate().cancel()
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
    }

    private fun autoScrollDuringEditorDrag(target: View, event: DragEvent) {
        if (!::contentScroll.isInitialized) return
        val targetLocation = IntArray(2)
        val scrollLocation = IntArray(2)
        target.getLocationOnScreen(targetLocation)
        contentScroll.getLocationOnScreen(scrollLocation)
        val dragY = targetLocation[1] + event.y.toInt()
        val topEdge = scrollLocation[1] + dp(DRAG_SCROLL_EDGE_DP)
        val bottomEdge = scrollLocation[1] + contentScroll.height - dp(DRAG_SCROLL_EDGE_DP)
        when {
            dragY < topEdge && contentScroll.canScrollVertically(-1) -> {
                contentScroll.scrollBy(0, -dp(DRAG_SCROLL_STEP_DP))
            }
            dragY > bottomEdge && contentScroll.canScrollVertically(1) -> {
                contentScroll.scrollBy(0, dp(DRAG_SCROLL_STEP_DP))
            }
        }
    }

    private fun swapEditorKeysImmediately(firstPosition: Int, secondPosition: Int) {
        val swapped = latestConfig.swap(firstPosition, secondPosition)
        persistConfigImmediately(swapped)
        screenContent.announceForAccessibility(
            "Page ${editorPage + 1} keys ${(firstPosition % PhraseConfig.KEYS_PER_PAGE) + 1} and ${(secondPosition % PhraseConfig.KEYS_PER_PAGE) + 1} swapped",
        )
    }

    private fun persistConfigImmediately(config: PhraseConfig) {
        val normalized = config.normalized()
        if (normalized == latestConfig) return
        latestConfig = normalized
        renderCurrentTab()
        repository.saveConfig(normalized)
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
            .setTitle("Page ${phrase.pageIndex + 1} · Key ${phrase.slotOnPage}")
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
                dialog.dismiss()
                persistConfigImmediately(latestConfig.update(updated))
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                dialog.dismiss()
                persistConfigImmediately(
                    latestConfig.update(PhraseConfig.emptyKey(phrase.position)),
                )
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
            "Your 40 keys are stored in this app's private local storage. Android cloud backup is disabled for the app.",
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
            text = "TapDeck Lite ${BuildConfig.VERSION_NAME}  •  40 keys  •  Offline"
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

    private data class EditorDragState(
        val fromPosition: Int,
        val sourceView: View,
        var toPosition: Int? = null,
        var completed: Boolean = false,
    )

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
        private const val TOP_SYSTEM_BAR_GAP_DP = 12
        private const val DRAG_SOURCE_ALPHA = 0.46f
        private const val DRAG_SOURCE_SCALE = 0.96f
        private const val DROP_TARGET_ALPHA = 0.62f
        private const val DROP_TARGET_SCALE = 0.94f
        private const val DRAG_ANIMATION_DURATION_MS = 90L
        private const val DRAG_SCROLL_EDGE_DP = 72
        private const val DRAG_SCROLL_STEP_DP = 18
    }
}

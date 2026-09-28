package app.tapdeck.keyboard.lite.data

import android.content.Context
import android.content.SharedPreferences
import app.tapdeck.keyboard.lite.model.DeckLayout
import app.tapdeck.keyboard.lite.model.PhraseConfig
import java.util.concurrent.CopyOnWriteArraySet

class PhraseRepository private constructor(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val listeners = CopyOnWriteArraySet<(PhraseConfig) -> Unit>()
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == CONFIG_KEY || key == DECK_LAYOUT_KEY || key == PAGE_SWIPE_ENABLED_KEY) {
            notifyListeners()
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun getConfig(): PhraseConfig = PhraseConfigCodec.decode(
        preferences.getString(CONFIG_KEY, null),
    )

    fun saveConfig(config: PhraseConfig) {
        val normalized = config.normalized()
        preferences.edit().putString(CONFIG_KEY, PhraseConfigCodec.encode(normalized)).apply()
    }

    fun getDeckLayout(): DeckLayout {
        val stored = preferences.getString(DECK_LAYOUT_KEY, null) ?: return DeckLayout()
        val parts = stored.split(':', limit = 2)
        return DeckLayout(
            keysPerRow = parts.getOrNull(0)?.toIntOrNull() ?: DeckLayout.DEFAULT_KEYS_PER_ROW,
            configuredOnly = parts.getOrNull(1)?.toBooleanStrictOrNull() ?: false,
        ).normalized()
    }

    fun setDeckLayout(layout: DeckLayout) {
        val normalized = layout.normalized()
        preferences.edit()
            .putString(DECK_LAYOUT_KEY, "${normalized.keysPerRow}:${normalized.configuredOnly}")
            .apply()
    }

    fun isKeyVibrationEnabled(): Boolean = preferences.getBoolean(
        KEY_VIBRATION_ENABLED_KEY,
        false,
    )

    fun setKeyVibrationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_VIBRATION_ENABLED_KEY, enabled).apply()
    }

    fun isPageSwipeEnabled(): Boolean = preferences.getBoolean(
        PAGE_SWIPE_ENABLED_KEY,
        false,
    )

    fun setPageSwipeEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(PAGE_SWIPE_ENABLED_KEY, enabled)
            .apply()
    }

    fun observe(listener: (PhraseConfig) -> Unit): Subscription {
        listeners += listener
        listener(getConfig())
        return Subscription { listeners -= listener }
    }

    private fun notifyListeners() {
        val config = getConfig()
        listeners.forEach { it(config) }
    }

    fun interface Subscription {
        fun cancel()
    }

    companion object {
        private const val PREFERENCES_NAME = "tapdeck_lite_phrases"
        private const val CONFIG_KEY = "phrase_config_json"
        private const val DECK_LAYOUT_KEY = "deck_layout"
        private const val KEY_VIBRATION_ENABLED_KEY = "key_vibration_enabled"
        private const val PAGE_SWIPE_ENABLED_KEY = "page_swipe_enabled"

        @Volatile
        private var instance: PhraseRepository? = null

        fun get(context: Context): PhraseRepository = instance ?: synchronized(this) {
            instance ?: PhraseRepository(context).also { instance = it }
        }
    }
}

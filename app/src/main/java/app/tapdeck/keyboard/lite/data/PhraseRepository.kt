package app.tapdeck.keyboard.lite.data

import android.content.Context
import android.content.SharedPreferences
import app.tapdeck.keyboard.lite.model.PhraseConfig
import app.tapdeck.keyboard.lite.model.PhraseKey
import java.util.concurrent.CopyOnWriteArraySet

class PhraseRepository private constructor(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val listeners = CopyOnWriteArraySet<(PhraseConfig) -> Unit>()
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == CONFIG_KEY) notifyListeners()
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun getConfig(): PhraseConfig = PhraseConfigCodec.decode(
        preferences.getString(CONFIG_KEY, null),
    )

    fun updateKey(updated: PhraseKey) {
        val next = getConfig().update(updated)
        preferences.edit().putString(CONFIG_KEY, PhraseConfigCodec.encode(next)).apply()
    }

    fun moveKey(fromPosition: Int, toPosition: Int) {
        val next = getConfig().move(fromPosition, toPosition)
        preferences.edit().putString(CONFIG_KEY, PhraseConfigCodec.encode(next)).apply()
    }

    fun isKeyVibrationEnabled(): Boolean = preferences.getBoolean(
        KEY_VIBRATION_ENABLED_KEY,
        false,
    )

    fun setKeyVibrationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_VIBRATION_ENABLED_KEY, enabled).apply()
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
        private const val KEY_VIBRATION_ENABLED_KEY = "key_vibration_enabled"

        @Volatile
        private var instance: PhraseRepository? = null

        fun get(context: Context): PhraseRepository = instance ?: synchronized(this) {
            instance ?: PhraseRepository(context).also { instance = it }
        }
    }
}

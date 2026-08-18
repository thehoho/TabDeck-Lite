package app.tapdeck.keyboard.lite.data

import app.tapdeck.keyboard.lite.model.PhraseAction
import app.tapdeck.keyboard.lite.model.PhraseConfig
import app.tapdeck.keyboard.lite.model.PhraseKey
import org.json.JSONArray
import org.json.JSONObject

object PhraseConfigCodec {
    private const val SCHEMA_VERSION = 1

    fun encode(config: PhraseConfig): String {
        val keys = JSONArray()
        config.normalized().keys.forEach { key ->
            keys.put(
                JSONObject()
                    .put("position", key.position)
                    .put("label", key.label)
                    .put("message", key.message)
                    .put("action", key.action.name),
            )
        }
        return JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("keys", keys)
            .toString()
    }

    fun decode(raw: String?): PhraseConfig {
        if (raw.isNullOrBlank()) return PhraseConfig.empty()
        return runCatching {
            val keysJson = JSONObject(raw).optJSONArray("keys") ?: JSONArray()
            val keys = buildList {
                for (index in 0 until keysJson.length()) {
                    val item = keysJson.optJSONObject(index) ?: continue
                    val action = runCatching {
                        PhraseAction.valueOf(item.optString("action"))
                    }.getOrDefault(PhraseAction.INSERT_AND_SEND)
                    add(
                        PhraseKey(
                            position = item.optInt("position", index),
                            label = item.optString("label", ""),
                            message = item.optString("message", ""),
                            action = action,
                        ),
                    )
                }
            }
            PhraseConfig(keys).normalized()
        }.getOrElse { PhraseConfig.empty() }
    }
}

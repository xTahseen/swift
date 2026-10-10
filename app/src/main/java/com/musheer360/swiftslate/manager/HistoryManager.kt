package com.musheer360.swiftslate.manager

import android.content.Context
import android.content.SharedPreferences
import com.musheer360.swiftslate.model.ChatTurn
import org.json.JSONArray
import org.json.JSONObject

/**
 * Per-command conversation memory for commands created with "Remember conversation" on.
 *
 * Stored in app-private preferences (not backed up, not exported) as alternating user/assistant
 * messages. The oldest exchanges are dropped once [MAX_MESSAGES] or [MAX_CHARS] is exceeded, so
 * a long chat never grows the request without bound. Always trimmed in whole exchanges: the
 * list starts with a user message and alternates, which Gemini requires.
 *
 * Keyed by the trigger WITHOUT its one-character prefix, so changing the trigger prefix in
 * Settings keeps every command's history.
 */
class HistoryManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)

    companion object {
        const val MAX_MESSAGES = 20
        const val MAX_CHARS = 12_000
        private const val KEY_PREFIX = "h:"

        private fun key(trigger: String) = KEY_PREFIX + trigger.drop(1)
    }

    @Synchronized fun get(trigger: String): List<ChatTurn> {
        val raw = try { prefs.getString(key(trigger), null) } catch (_: Exception) { null }
            ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val t = o.optString("t", "")
                if (t.isEmpty()) null else ChatTurn(o.optBoolean("u", true), t)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Number of remembered messages (user and assistant) for [trigger]. */
    fun count(trigger: String): Int = get(trigger).size

    /** Records one finished exchange. */
    @Synchronized fun append(trigger: String, userText: String, reply: String) {
        if (userText.isBlank() || reply.isBlank()) return
        val turns = ArrayList(get(trigger))
        turns.add(ChatTurn(true, userText))
        turns.add(ChatTurn(false, reply))
        // Drop whole exchanges from the front.
        while (turns.size > 2 &&
            (turns.size > MAX_MESSAGES || turns.sumOf { it.text.length } > MAX_CHARS)) {
            turns.removeAt(0)
            turns.removeAt(0)
        }
        val arr = JSONArray()
        for (t in turns) arr.put(JSONObject().put("u", t.fromUser).put("t", t.text))
        prefs.edit().putString(key(trigger), arr.toString()).apply()
    }

    @Synchronized fun clear(trigger: String) {
        prefs.edit().remove(key(trigger)).apply()
    }

    @Synchronized fun clearAll() {
        prefs.edit().clear().apply()
    }

    /** Moves a command's memory when its trigger is renamed. */
    @Synchronized fun rename(oldTrigger: String, newTrigger: String) {
        if (key(oldTrigger) == key(newTrigger)) return
        val raw = prefs.getString(key(oldTrigger), null) ?: return
        prefs.edit().remove(key(oldTrigger)).putString(key(newTrigger), raw).apply()
    }
}

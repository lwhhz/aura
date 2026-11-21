package com.aura.chat.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 会话与设置的本地持久化（SharedPreferences + org.json，避免额外依赖）。 */
class ChatStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("aura_store", Context.MODE_PRIVATE)

    fun loadConversations(): List<Conversation> {
        val raw = prefs.getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        return runCatching { parseConversations(JSONArray(raw)) }.getOrElse { emptyList() }
    }

    fun saveConversations(list: List<Conversation>) {
        val arr = JSONArray()
        list.forEach { arr.put(conversationToJson(it)) }
        prefs.edit().putString(KEY_CONVERSATIONS, arr.toString()).apply()
    }

    fun loadSettings(): AppSettings = AppSettings(
        apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
        searchKey = prefs.getString(KEY_SEARCH_KEY, "") ?: "",
        baseUrl = prefs.getString(KEY_BASE_URL, "https://api.deepseek.com") ?: "https://api.deepseek.com",
        model = prefs.getString(KEY_MODEL, Models.DEEPSEEK_FLASH.id) ?: Models.DEEPSEEK_FLASH.id,
        themeMode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrElse { ThemeMode.SYSTEM },
        demoMode = prefs.getBoolean(KEY_DEMO, true),
        webSearchOn = prefs.getBoolean(KEY_WEB, true),
    )

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_API_KEY, settings.apiKey)
            .putString(KEY_SEARCH_KEY, settings.searchKey)
            .putString(KEY_BASE_URL, settings.baseUrl)
            .putString(KEY_MODEL, settings.model)
            .putString(KEY_THEME, settings.themeMode.name)
            .putBoolean(KEY_DEMO, settings.demoMode)
            .putBoolean(KEY_WEB, settings.webSearchOn)
            .apply()
    }

    fun clearConversations() = prefs.edit().remove(KEY_CONVERSATIONS).apply()

    private fun conversationToJson(c: Conversation): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("title", c.title)
        put("updatedAt", c.updatedAt)
        put("model", c.model)
        put("accent", c.accent)
        put("pinned", c.pinned)
        c.pinnedAt?.let { put("pinnedAt", it) } ?: remove("pinnedAt")
        put("messages", JSONArray().apply { c.messages.forEach { put(messageToJson(it)) } })
    }

    private fun messageToJson(m: ChatMessage): JSONObject = JSONObject().apply {
        put("id", m.id)
        put("role", m.role.name)
        put("text", m.text)
        put("model", m.model)
        put("reasoning", m.reasoning ?: "")
        put("reasoningSeconds", m.reasoningSeconds ?: 0)
        put("elapsedSeconds", m.elapsedSeconds ?: 0)
        put("status", m.status.name)
        put("citations", JSONArray().apply {
            m.citations.forEach {
                put(JSONObject().put("title", it.title).put("source", it.source).put("url", it.url))
            }
        })
    }

    private fun parseConversations(arr: JSONArray): List<Conversation> = buildList {
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            add(
                Conversation(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    updatedAt = o.optLong("updatedAt"),
                    model = o.optString("model", Models.DEEPSEEK_FLASH.id),
                    accent = o.optInt("accent"),
                    pinned = o.optBoolean("pinned"),
                    // 旧数据只有 pinned 布尔值，补一个时间戳，避免升级后置顶丢失
                    pinnedAt = when {
                        o.has("pinnedAt") -> o.optLong("pinnedAt")
                        o.optBoolean("pinned") -> o.optLong("updatedAt", 0L)
                        else -> null
                    },
                    messages = parseMessages(o.optJSONArray("messages")),
                )
            )
        }
    }

    private fun parseMessages(arr: JSONArray?): List<ChatMessage> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    ChatMessage(
                        id = o.optString("id"),
                        role = runCatching { Role.valueOf(o.optString("role")) }.getOrElse { Role.USER },
                        text = o.optString("text"),
                        model = o.optString("model"),
                        reasoning = o.optString("reasoning").ifBlank { null },
                        reasoningSeconds = o.optInt("reasoningSeconds").takeIf { it > 0 },
                        elapsedSeconds = o.optInt("elapsedSeconds").takeIf { it > 0 },
                        status = runCatching { MessageStatus.valueOf(o.optString("status")) }
                            .getOrElse { MessageStatus.DONE },
                        citations = parseCitations(o.optJSONArray("citations")),
                    )
                )
            }
        }
    }

    private fun parseCitations(arr: JSONArray?): List<Citation> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(Citation(o.optString("title"), o.optString("source"), o.optString("url")))
            }
        }
    }

    private companion object {
        const val KEY_CONVERSATIONS = "conversations"
        const val KEY_API_KEY = "api_key"
        const val KEY_SEARCH_KEY = "search_key"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_THEME = "theme"
        const val KEY_DEMO = "demo_mode"
        const val KEY_WEB = "web_search"
    }
}
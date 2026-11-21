package com.aura.chat.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SuggestionItem(val label: String, val conversationId: String)

data class SuggestionState(
    val items: List<SuggestionItem> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val generatedAt: Long = 0L,
    val fromModel: Boolean = false,
)

/**
 * 首页「灵感」标签：只读取现有会话标题与摘要，压缩成 3–5 个标签，并保留到对应会话的映射，
 * 点击可跳转到该会话。不生成新话题；失败降级为真实会话标题。
 */
class SuggestionEngine(
    private val client: DeepSeekClient,
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences("aura_suggestions", Context.MODE_PRIVATE)

    fun initial(conversations: List<Conversation>): SuggestionState = SuggestionState(
        items = loadItems().ifEmpty { fallback(conversations) },
        generatedAt = prefs.getLong(KEY_GENERATED_AT, 0L),
        fromModel = prefs.getBoolean(KEY_FROM_MODEL, false),
    )

    fun fallback(conversations: List<Conversation>): List<SuggestionItem> =
        conversations
            .filter { it.messages.isNotEmpty() }
            .sortedByDescending { it.updatedAt }
            .distinctBy { it.title }
            .take(4)
            .map { conversation ->
                val label = conversation.title.trim().let { if (it.length > 8) it.take(8) else it }
                SuggestionItem(label.ifBlank { "对话" }, conversation.id)
            }

    suspend fun refresh(
        baseUrl: String,
        apiKey: String,
        model: String,
        conversations: List<Conversation>,
        force: Boolean,
    ): SuggestionState {
        val recent = conversations.filter { it.messages.isNotEmpty() }.sortedByDescending { it.updatedAt }.take(5)
        if (recent.isEmpty()) return SuggestionState(items = fallback(conversations))
        val hash = recent.joinToString("|") { it.id + ":" + it.updatedAt }
        if (!force && hash == prefs.getString(KEY_HASH, null) && loadItems().isNotEmpty()) return initial(conversations)
        if (!canUseToday()) return initial(conversations)
        if (apiKey.isBlank()) {
            val items = fallback(conversations)
            save(items, hash, fromModel = false)
            return SuggestionState(items = items)
        }
        val lines = recent.mapIndexed { index, conversation ->
            val summary = conversation.lastMessage?.text.orEmpty().replace("\n", " ").take(60)
            (index + 1).toString() + ". 标题：" + conversation.title + " ｜ 摘要：" + summary
        }.joinToString("\n")
        val system = "你是编辑。下面会给若干会话的编号、标题与摘要。为每个会话给出一个 2 到 6 字的中文主题标签；" +
            "不得创造新话题，不要解释。只输出 JSON 数组，元素形如 {\"label\":\"主题\",\"index\":1}。"
        return runCatching {
            val raw = client.complete(baseUrl, apiKey, model, system, lines)
            val parsed = parse(raw, recent)
            val items = parsed.ifEmpty { fallback(conversations) }
            consumeToday()
            save(items, hash, fromModel = parsed.isNotEmpty())
            SuggestionState(items = items, fromModel = parsed.isNotEmpty(), generatedAt = System.currentTimeMillis())
        }.getOrElse {
            SuggestionState(items = fallback(conversations), failed = true)
        }
    }

    private fun parse(raw: String, recent: List<Conversation>): List<SuggestionItem> {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return runCatching {
            val array = JSONArray(raw.substring(start, end + 1))
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val label = obj.optString("label").trim()
                    val index = obj.optInt("index", 0)
                    val conversation = recent.getOrNull(index - 1) ?: continue
                    if (label.length in 2..8) add(SuggestionItem(label, conversation.id))
                }
            }.distinctBy { it.label }.take(5)
        }.getOrElse { emptyList() }
    }

    private fun loadItems(): List<SuggestionItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val label = obj.optString("label")
                    val id = obj.optString("id")
                    if (label.isNotBlank() && id.isNotBlank()) add(SuggestionItem(label, id))
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun save(items: List<SuggestionItem>, hash: String, fromModel: Boolean) {
        val array = JSONArray()
        items.forEach { array.put(JSONObject().put("label", it.label).put("id", it.conversationId)) }
        prefs.edit()
            .putString(KEY_ITEMS, array.toString())
            .putString(KEY_HASH, hash)
            .putLong(KEY_GENERATED_AT, System.currentTimeMillis())
            .putBoolean(KEY_FROM_MODEL, fromModel)
            .apply()
    }

    private fun canUseToday(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return prefs.getString(KEY_DAY, null) != today || prefs.getInt(KEY_COUNT, 0) < 30
    }

    private fun consumeToday() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val same = prefs.getString(KEY_DAY, null) == today
        val count = if (same) prefs.getInt(KEY_COUNT, 0) + 1 else 1
        prefs.edit().putString(KEY_DAY, today).putInt(KEY_COUNT, count).apply()
    }

    private companion object {
        const val KEY_ITEMS = "items"
        const val KEY_HASH = "hash"
        const val KEY_GENERATED_AT = "generated_at"
        const val KEY_FROM_MODEL = "from_model"
        const val KEY_DAY = "day"
        const val KEY_COUNT = "count"
    }
}

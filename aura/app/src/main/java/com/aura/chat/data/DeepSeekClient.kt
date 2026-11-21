package com.aura.chat.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/** DeepSeek /chat/completions 流式客户端（SSE，无需第三方 HTTP 库）。 */
class DeepSeekClient {

    sealed interface Delta {
        data class Reasoning(val text: String) : Delta
        data class Content(val text: String) : Delta
        data class ToolEvent(val label: String) : Delta
        data class Final(
            val citations: List<Citation>,
            val toolSteps: List<String>,
            val rawServerToolCalls: String?,
            val refused: Boolean,
            val searchCount: Int = 0,
            val fetchCount: Int = 0,
            val imageCount: Int = 0,
        ) : Delta
        data object Done : Delta
    }

    fun stream(
        baseUrl: String,
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        tools: Boolean = false,
        thinking: Boolean = false,
        thinkingBudget: Int? = null,
        fileId: String? = null,
    ): Flow<Delta> = flow {
        val endpoint = baseUrl.trimEnd('/') + "/chat/completions"
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("Authorization", "Bearer " + apiKey)
        }
        try {
            val payload = JSONObject().apply {
                put("model", model)
                put("stream", true)
                put("messages", JSONArray().apply {
                    history.forEachIndexed { index, m ->
                        put(JSONObject().apply {
                            put("role", if (m.role == Role.USER) "user" else "assistant")
                            val useFile = fileId != null && index == history.lastIndex && m.role == Role.USER
                            if (useFile) {
                                put("content", JSONArray().apply {
                                    put(JSONObject().put("type", "file").put("file_id", fileId))
                                    put(JSONObject().put("type", "text").put("text", m.text))
                                })
                            } else {
                                put("content", m.text)
                            }
                            // 多轮：server_tool_calls 必须原样回传，否则读过的网页会丢
                            m.rawServerToolCalls?.let { raw ->
                                runCatching { JSONArray(raw) }.getOrNull()?.let { put("server_tool_calls", it) }
                            }
                        })
                    }
                })
                if (thinking) {
                    put("thinking", JSONObject().apply {
                        put("type", "enabled")
                        if (thinkingBudget != null) put("budget_tokens", thinkingBudget)
                    })
                }
                if (tools) {
                    put("tools", JSONArray().apply {
                        put(JSONObject().put("type", "deepseek_web_search"))
                        put(JSONObject().put("type", "deepseek_read_url"))
                    })
                }
            }
            conn.outputStream.use { out: OutputStream ->
                out.write(payload.toString().toByteArray(Charsets.UTF_8))
                out.flush()
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val body = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull().orEmpty()
                throw IOException(errorMessage(code, body))
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
            val citations = mutableListOf<Citation>()
            val steps = mutableListOf<String>()
            var rawCalls: String? = null
            var refused = false
            var searchCount = 0
            var fetchCount = 0
            var imageCount = 0
            while (currentCoroutineContext().isActive) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                val json = runCatching { JSONObject(data) }.getOrNull() ?: continue
                val choice = json.optJSONArray("choices")?.optJSONObject(0) ?: continue
                choice.optJSONObject("server_tool_event")?.let { event ->
                    when (event.optString("type")) {
                        "call_started" -> {
                            val call = event.optJSONObject("call")
                            val kind = call?.optString("kind").orEmpty()
                            val queries = call?.optJSONObject("action")?.optJSONArray("queries")
                                ?.let { arr -> (0 until arr.length()).joinToString("、") { arr.optString(it) } }
                            val label = when {
                                kind.contains("search") && !queries.isNullOrBlank() -> "搜索 " + queries
                                kind.contains("read") -> "阅读链接"
                                kind.contains("search") -> "搜索中"
                                else -> "调用工具"
                            }
                            emit(Delta.ToolEvent(label))
                        }
                        "call_completed" -> {
                            val call = event.optJSONObject("call")
                            if (call?.optString("status") == "failed") emit(Delta.ToolEvent("工具调用失败"))
                        }
                    }
                }
                val delta = choice.optJSONObject("delta")
                if (delta != null) {
                    if (!delta.isNull("reasoning_content")) {
                        delta.optString("reasoning_content").takeIf { it.isNotEmpty() }?.let { emit(Delta.Reasoning(it)) }
                    }
                    if (!delta.isNull("content")) {
                        val piece = delta.optString("content")
                        if (piece.isNotEmpty() && piece != "null") emit(Delta.Content(piece))
                    }
                    delta.optJSONArray("server_tool_calls")?.let { calls ->
                        rawCalls = calls.toString()
                        for (i in 0 until calls.length()) {
                            val step = calls.optJSONObject(i) ?: continue
                            val stepCalls = step.optJSONArray("calls") ?: continue
                            for (j in 0 until stepCalls.length()) {
                                val call = stepCalls.optJSONObject(j) ?: continue
                                val sources = call.optJSONArray("sources")
                                if (sources != null) {
                                    for (k in 0 until sources.length()) {
                                        val s = sources.optJSONObject(k) ?: continue
                                        val url = s.optString("url")
                                        if (url.isNotBlank()) {
                                            steps += "搜索到 " + s.optString("title", url)
                                            citations += Citation(
                                                title = s.optString("title", url),
                                                source = s.optString("site_name", url),
                                                url = url,
                                            )
                                        }
                                    }
                                }
                                call.optJSONObject("page")?.let { page ->
                                    val url = page.optString("url")
                                    if (url.isNotBlank()) {
                                        steps += "阅读 " + page.optString("title", url)
                                        citations += Citation(page.optString("title", url), url, url)
                                    }
                                }
                            }
                        }
                    }
                    delta.optJSONArray("annotations")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue
                            val c2 = item.optJSONObject("url_citation") ?: continue
                            val url = c2.optString("url")
                            if (url.isNotBlank() && citations.none { it.url == url }) {
                                citations += Citation(c2.optString("title", url), url, url)
                            }
                        }
                    }
                }
                val finish = if (choice.isNull("finish_reason")) "" else choice.optString("finish_reason")
                if (finish.isNotEmpty()) refused = finish == "content_filter"
                json.optJSONObject("usage")?.optJSONObject("server_tool_use")?.let { use ->
                    searchCount = use.optInt("tool_search", searchCount)
                    fetchCount = use.optInt("tool_fetch", fetchCount)
                    imageCount = use.optInt("tool_image_search", imageCount)
                }
            }
            emit(Delta.Final(citations.distinctBy { it.url }, steps.distinct(), rawCalls, refused, searchCount, fetchCount, imageCount))
            emit(Delta.Done)
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    /** 非流式单次补全，用于首页灵感标签这类短请求。 */
    suspend fun complete(
        baseUrl: String,
        apiKey: String,
        model: String,
        system: String,
        user: String,
        maxTokens: Int = 200,
        temperature: Double = 0.4,
    ): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val endpoint = baseUrl.trimEnd('/') + "/chat/completions"
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer " + apiKey)
        }
        try {
            val payload = JSONObject().apply {
                put("model", model)
                put("stream", false)
                put("max_tokens", maxTokens)
                put("temperature", temperature)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", system))
                    put(JSONObject().put("role", "user").put("content", user))
                })
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) error("complete HTTP " + conn.responseCode)
            val body = JSONObject(conn.inputStream.bufferedReader().readText())
            body.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
                .trim()
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(code: Int, body: String): String {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrNull().orEmpty()
        return when {
            code == 401 -> "API Key 无效或已过期，请在设置里重新填写。"
            code == 402 -> "账户余额不足，请到 DeepSeek 平台充值后重试。"
            code == 429 -> "请求太频繁了，请稍后再试。"
            code in 500..599 -> "服务器繁忙，请稍后再试。"
            detail.isNotBlank() -> "请求失败（$code）：$detail"
            else -> "请求失败（$code），请稍后再试。"
        }
    }
}
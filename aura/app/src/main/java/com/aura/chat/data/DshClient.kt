package com.aura.chat.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 与 Aura 配对桥（sidecar/插件）通信。 */
data class DshDetailMessage(val seq: Long, val role: String, val text: String)

data class DshSessionDetail(val id: String, val title: String, val cwd: String, val messages: List<DshDetailMessage>)

data class DshRemoteSession(val id: String, val title: String, val cwd: String, val createdAt: Long)

object DshClient {
    /** 拉取单个会话的消息。 */
    suspend fun detail(host: String, port: Int, token: String, id: String): DshSessionDetail =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = URL("http://" + host + ":" + port + "/aura/detail?id=" + java.net.URLEncoder.encode(id, "UTF-8"))
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 8000
                setRequestProperty("x-aura-token", token)
            }
            try {
                val text = conn.inputStream.bufferedReader().readText()
                val root = JSONObject(text)
                val array = root.optJSONArray("messages") ?: org.json.JSONArray()
                val all = buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val role = item.optString("role")
                        if (role == "system") continue
                        add(DshDetailMessage(item.optLong("seq"), role, item.optString("text")))
                    }
                }
                val messages = if (all.size > 200) all.takeLast(200) else all
                DshSessionDetail(
                    id = root.optString("id"),
                    title = root.optString("title"),
                    cwd = root.optString("cwd"),
                    messages = messages,
                )
            } finally {
                conn.disconnect()
            }
        }

    /** 拉取电脑端 live 会话列表。 */
    suspend fun sessions(host: String, port: Int, token: String): List<DshRemoteSession> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = URL("http://" + host + ":" + port + "/aura/sessions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 8000
                setRequestProperty("x-aura-token", token)
            }
            try {
                val text = conn.inputStream.bufferedReader().readText()
                val array = JSONObject(text).optJSONArray("sessions") ?: org.json.JSONArray()
                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        add(
                            DshRemoteSession(
                                id = item.optString("id"),
                                title = item.optString("title"),
                                cwd = item.optString("cwd"),
                                createdAt = item.optLong("createdAt"),
                            )
                        )
                    }
                }
            } finally {
                conn.disconnect()
            }
        }

    /** 用 6 位配对码换长期 token。 */
    suspend fun verify(host: String, port: Int, code: String): String =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = URL("http://" + host + ":" + port + "/aura/pair/verify")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-aura-code", code)
            }
            try {
                conn.outputStream.use { it.write("{}".toByteArray()) }
                val body = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
                val text = body?.bufferedReader()?.readText().orEmpty()
                if (conn.responseCode !in 200..299) throw IOException("配对失败（" + conn.responseCode + "）")
                val token = JSONObject(text).optString("token")
                if (token.isBlank()) throw IOException("配对失败：没有返回 token")
                token
            } finally {
                conn.disconnect()
            }
        }
}

package com.aura.chat.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 与 Aura 配对桥（sidecar/插件）通信。 */
object DshClient {
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

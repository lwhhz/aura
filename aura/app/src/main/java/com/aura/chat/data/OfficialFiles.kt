package com.aura.chat.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 官方 Files API：上传并解析文件，返回 file_id。 */
object OfficialFiles {

    const val PURPOSE_TEXT = "text_recognition"
    const val PURPOSE_RICH = "rich_layout_recognition"
    const val PURPOSE_USER = "user_data"

    data class Uploaded(val fileId: String, val purpose: String)

    fun purposeFor(mime: String?, name: String): String {
        val lower = name.lowercase()
        val type = mime.orEmpty().lowercase()
        return when {
            type.startsWith("image/") -> PURPOSE_RICH
            lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") ||
                lower.endsWith(".ppt") || lower.endsWith(".pptx") -> PURPOSE_TEXT
            lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv") ||
                lower.endsWith(".json") || lower.endsWith(".xlsx") || lower.endsWith(".xls") ||
                lower.endsWith(".epub") -> PURPOSE_USER
            type.startsWith("text/") -> PURPOSE_USER
            else -> PURPOSE_TEXT
        }
    }

    suspend fun upload(
        baseUrl: String,
        apiKey: String,
        bytes: ByteArray,
        fileName: String,
        mime: String?,
        purpose: String,
    ): Uploaded = withContext(Dispatchers.IO) {
        if (bytes.size > 32 * 1024 * 1024) throw IOException("文件超过 32 MiB，无法上传")
        val boundary = "----AuraBoundary" + System.nanoTime()
        val body = ByteArrayOutputStream()
        fun text(value: String) = value.toByteArray(Charsets.UTF_8)
        // purpose 必须排在 file 字段之前
        body.write(text("--" + boundary + "\r\n"))
        body.write(text("Content-Disposition: form-data; name=\"purpose\"\r\n\r\n"))
        body.write(text(purpose + "\r\n"))
        body.write(text("--" + boundary + "\r\n"))
        body.write(text("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"))
        body.write(text("Content-Type: " + (mime ?: "application/octet-stream") + "\r\n\r\n"))
        body.write(bytes)
        body.write(text("\r\n--" + boundary + "--\r\n"))

        val endpoint = baseUrl.trimEnd('/') + "/files"
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            // 上传 + 解析都在这个请求里完成，超时要放宽
            readTimeout = 180_000
            setRequestProperty("Authorization", "Bearer " + apiKey)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader()?.readText().orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull()
                throw IOException("上传失败（" + code + "）" + (message?.let { "：" + it } ?: ""))
            }
            val json = JSONObject(raw)
            val id = json.optString("id")
            if (id.isBlank()) throw IOException("上传成功但未返回 file_id")
            Uploaded(id, purpose)
        } finally {
            conn.disconnect()
        }
    }
}

package com.aura.chat.data

import java.util.UUID

enum class Role { USER, ASSISTANT }

enum class MessageStatus { DONE, STREAMING, ERROR }

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** 引用卡片：网页搜索结果的摘要（对应 chat-demo 的「搜索到 N 个网页」）。 */
data class Citation(
    val title: String,
    val source: String,
    val url: String = "",
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val reasoning: String? = null,
    val reasoningSeconds: Int? = null,
    val citations: List<Citation> = emptyList(),
    val status: MessageStatus = MessageStatus.DONE,
    val model: String = "",
    val elapsedSeconds: Int? = null,
    /** 官方工具：原样回传用的 server_tool_calls JSON。 */
    val rawServerToolCalls: String? = null,
    /** 官方工具：供 UI 折叠展示的步骤摘要。 */
    val toolSteps: List<String> = emptyList(),
    val refused: Boolean = false,
    val toolSearchCount: Int = 0,
    val toolFetchCount: Int = 0,
    val toolImageCount: Int = 0,
)

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val model: String = "deepseek-flash",
    val accent: Int = 0,
    val messages: List<ChatMessage> = emptyList(),
    val pinned: Boolean = false,
    val pinnedAt: Long? = null,
) {
    val lastMessage: ChatMessage? get() = messages.lastOrNull()
}

data class ModelInfo(
    val id: String,
    val label: String,
    val shortLabel: String,
    val tagline: String,
    val reasoning: Boolean,
)

object Models {

    val DEEPSEEK_FLASH = ModelInfo(
        id = "deepseek-flash",
        label = "DeepSeek Flash",
        shortLabel = "Flash",
        tagline = "常驻官方工具，联网与文件解析用",
        reasoning = false,
    )

    // Chat / Reasoner 已下线（无法联网），只保留 Flash
    val all = listOf(DEEPSEEK_FLASH)
    fun of(id: String): ModelInfo = all.firstOrNull { it.id == id } ?: DEEPSEEK_FLASH
}

data class AppSettings(
    val apiKey: String = "",
    val searchKey: String = "",
    val baseUrl: String = "https://api.deepseek.com",
    val model: String = Models.DEEPSEEK_FLASH.id,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val demoMode: Boolean = true,
    val webSearchOn: Boolean = false,
    val toolsConsent: Boolean = false,
    val deepThinkingOn: Boolean = true,
    val showNotices: Boolean = true,
    val reasoningLevel: Int = 2,
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()
    val hasSearchKey: Boolean get() = searchKey.isNotBlank()
}
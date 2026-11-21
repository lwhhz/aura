package com.aura.chat

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.chat.data.AppSettings
import com.aura.chat.data.Citation
import com.aura.chat.data.DocumentReader
import com.aura.chat.data.ChatMessage
import com.aura.chat.data.ChatStore
import com.aura.chat.data.Conversation
import com.aura.chat.data.DeepSeekClient
import com.aura.chat.data.DemoBrain
import com.aura.chat.data.DemoChats
import com.aura.chat.data.MessageStatus
import com.aura.chat.data.Models
import com.aura.chat.data.OfficialFiles
import com.aura.chat.data.Role
import com.aura.chat.data.SuggestionEngine
import com.aura.chat.data.SuggestionState
import com.aura.chat.data.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface Route {
    data object Home : Route
    data class Chat(val id: String) : Route
    data object Settings : Route
}

class AuraViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ChatStore(app)
    private val client = DeepSeekClient()
    private val suggestionEngine = SuggestionEngine(client, app)
    private var suggestionJob: Job? = null
    private var streamJob: Job? = null

    var settings by mutableStateOf(store.loadSettings())
        private set

    var conversations by mutableStateOf(
        store.loadConversations().ifEmpty { DemoChats.seed() }.sortedByPreferred()
    )
        private set

    var route by mutableStateOf<Route>(Route.Home)
        private set

    var streaming by mutableStateOf(false)
        private set

    var modelSheetOpen by mutableStateOf(false)
        private set

    var notice by mutableStateOf<String?>(null)
        private set

    /** 首页「灵感」标签。 */
    var suggestions by mutableStateOf(suggestionEngine.initial(emptyList()))
        private set

    /** 平板双栏时右侧详情选中的会话。 */
    var twoPaneSelection by mutableStateOf<String?>(null)
        private set

    /** 已上传到官方 Files API 的待发送文件。 */
    data class PendingFile(val fileId: String, val name: String, val purpose: String)

    var pendingFile by mutableStateOf<PendingFile?>(null)
        private set

    var uploadingFile by mutableStateOf(false)
        private set

    fun attachUri(uri: android.net.Uri) {
        if (settings.apiKey.isBlank()) {
            notice = "先在设置里填 API Key，才能上传文件"
            return
        }
        val context = getApplication<android.app.Application>()
        viewModelScope.launch {
            uploadingFile = true
            notice = "正在上传并解析文件…"
            runCatching {
                val resolver = context.contentResolver
                val name = queryDisplayName(resolver, uri) ?: "attachment"
                val mime = resolver.getType(uri)
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("读不到文件内容")
                val purpose = OfficialFiles.purposeFor(mime, name)
                val uploaded = OfficialFiles.upload(settings.baseUrl, settings.apiKey, bytes, name, mime, purpose)
                Triple(uploaded, name, purpose)
            }.onSuccess { (uploaded, name) ->
                pendingFile = PendingFile(uploaded.fileId, name, uploaded.purpose)
                notice = "已上传：" + name
            }.onFailure {
                notice = it.message ?: "上传失败"
            }
            uploadingFile = false
        }
    }

    private fun queryDisplayName(resolver: android.content.ContentResolver, uri: android.net.Uri): String? =
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()

    fun clearAttachment() {
        pendingFile = null
    }

    fun notifyUser(message: String) {
        notice = message
    }

    val activeConversation: Conversation?
        get() = (route as? Route.Chat)?.let { r -> conversations.firstOrNull { it.id == r.id } }

    val featuredConversation: Conversation?
        get() = conversations.filter { it.pinnedAt != null }
            .maxByOrNull { it.pinnedAt ?: 0L }
            ?: conversations.filter { it.messages.isNotEmpty() }.maxByOrNull { it.updatedAt }
            ?: conversations.maxByOrNull { it.updatedAt }

    init {
        // 一次性迁移：旧会话的 deepseek-chat / deepseek-reasoner 统一写成 deepseek-flash
        if (conversations.any { it.model != Models.DEEPSEEK_FLASH.id }) {
            conversations = conversations.map { it.copy(model = Models.DEEPSEEK_FLASH.id) }
            persist()
        }
        suggestions = suggestionEngine.initial(conversations)
        scheduleSuggestions(force = false)
    }

    // ---------------------------------------------------------------- 置顶与灵感

    fun togglePin(id: String) {
        val conversation = conversations.firstOrNull { it.id == id } ?: return
        if (conversation.pinnedAt != null) {
            updateConversation(id) { it.copy(pinned = false, pinnedAt = null) }
            notice = "已取消置顶"
        } else {
            if (conversations.count { it.pinnedAt != null } >= 3) {
                notice = "最多置顶 3 个对话"
                return
            }
            updateConversation(id) { it.copy(pinned = true, pinnedAt = System.currentTimeMillis()) }
            notice = "已置顶"
        }
        persist()
        scheduleSuggestions(force = false)
    }

    fun refreshSuggestions(force: Boolean = true) {
        scheduleSuggestions(force)
    }

    private fun scheduleSuggestions(force: Boolean) {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            if (!force) delay(1200)
            suggestions = suggestions.copy(loading = true, failed = false)
            val next = suggestionEngine.refresh(
                baseUrl = settings.baseUrl,
                apiKey = settings.apiKey,
                model = Models.DEEPSEEK_FLASH.id,
                conversations = conversations,
                force = force,
            )
            suggestions = next.copy(loading = false)
        }
    }

    val recentConversations: List<Conversation>
        get() {
            val featuredId = featuredConversation?.id
            return conversations.filter { it.id != featuredId }.sortedByDescending { it.updatedAt }
        }

    // ---------------------------------------------------------------- 导航

    fun deleteConversation(id: String) {
        conversations = conversations.filterNot { it.id == id }
        if (route is Route.Chat && (route as Route.Chat).id == id) route = Route.Home
        if (twoPaneSelection == id) twoPaneSelection = null
        persist()
        scheduleSuggestions(force = false)
        notice = "已删除对话"
    }

    fun openConversation(id: String) {
        twoPaneSelection = id
        route = Route.Chat(id)
    }

    fun newConversation() {
        val conv = Conversation(
            title = "新的对话",
            model = settings.model,
            accent = Random.nextInt(5),
            updatedAt = System.currentTimeMillis(),
        )
        conversations = (listOf(conv) + conversations).sortedByPreferred()
        route = Route.Chat(conv.id)
        persist()
    }

    fun startWithPrompt(prompt: String) {
        newConversation()
        send(prompt)
    }

    fun back() {
        route = when (route) {
            is Route.Chat -> Route.Home
            Route.Settings -> Route.Home
            Route.Home -> Route.Home
        }
    }

    fun openSettings() { route = Route.Settings }
    fun openModelSheet() { modelSheetOpen = true }
    fun closeModelSheet() { modelSheetOpen = false }
    fun dismissNotice() { notice = null }

    // ---------------------------------------------------------------- 设置

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(settings)
        settings = next
        store.saveSettings(next)
    }

    fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }

    fun selectModel(modelId: String) {
        updateSettings { it.copy(model = modelId) }
        val conv = activeConversation
        if (conv != null) {
            updateConversation(conv.id) { it.copy(model = modelId) }
            persist()
        }
        // 关闭动画由 ModelSheet 负责，这里不直接收起
    }

    // ---------------------------------------------------------------- 对话

    fun send(raw: String) {
        val text = raw.trim()
        val conv = activeConversation ?: return
        if (text.isEmpty() || streaming) return

        val pending = pendingFile
        pendingFile = null
        val displayText = if (pending == null) text else "【附件】" + pending.name + "\n\n" + text
        val wantsReasoning = Models.of(conv.model).reasoning
        // 官方工具只在有 Key 且非演示模式时启用；启用时模型自动切到 deepseek-flash
        // 未完成首次计费/上传确认时，不允许发送工具请求
        val useTools = settings.hasKey && !settings.demoMode && settings.webSearchOn && settings.toolsConsent
        // 用户显式选了 Flash 就常驻 Flash；文件或联网工具需要 Flash
        val requestModel = when {
            pending != null -> Models.DEEPSEEK_FLASH.id
            conv.model == Models.DEEPSEEK_FLASH.id -> Models.DEEPSEEK_FLASH.id
            useTools -> Models.DEEPSEEK_FLASH.id
            else -> conv.model
        }
        val userMessage = ChatMessage(role = Role.USER, text = displayText, model = requestModel)
        val assistantMessage = ChatMessage(
            role = Role.ASSISTANT,
            text = "",
            model = requestModel,
            reasoning = if (wantsReasoning) "" else null,
            status = MessageStatus.STREAMING,
        )
        val apiText = text
        val apiHistory = conv.messages + ChatMessage(role = Role.USER, text = apiText, model = conv.model)

        updateConversation(conv.id) { c ->
            c.copy(
                title = if (c.messages.isEmpty()) titleFor(text) else c.title,
                messages = c.messages + userMessage + assistantMessage,
                updatedAt = System.currentTimeMillis(),
            )
        }
        streaming = true
        streamJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            try {
                if (settings.hasKey && !settings.demoMode) {
                    var rawCalls: String? = null
                    var searchCount = 0
                    var fetchCount = 0
                    var imageCount = 0
                    var refused = false
                    var steps: List<String> = emptyList()
                    client.stream(
                        settings.baseUrl,
                        settings.apiKey,
                        requestModel,
                        apiHistory,
                        tools = useTools || pending != null,
                        thinking = settings.deepThinkingOn,
                        fileId = pending?.fileId,
                    ).collect { delta ->
                        when (delta) {
                            is DeepSeekClient.Delta.Reasoning -> appendReasoning(conv.id, delta.text)
                            is DeepSeekClient.Delta.Content -> appendContent(conv.id, delta.text)
                            is DeepSeekClient.Delta.ToolEvent -> notice = delta.label
                            is DeepSeekClient.Delta.Final -> {
                                rawCalls = delta.rawServerToolCalls
                                refused = delta.refused
                                steps = delta.toolSteps
                                if (delta.citations.isNotEmpty()) setCitations(conv.id, delta.citations)
                                searchCount = delta.searchCount
                                fetchCount = delta.fetchCount
                                imageCount = delta.imageCount
                            }
                            DeepSeekClient.Delta.Done -> Unit
                        }
                    }
                    if (rawCalls != null || steps.isNotEmpty() || refused) {
                        updateConversation(conv.id) { c ->
                            c.copy(messages = c.messages.map { msg ->
                                if (msg.id == assistantMessage.id) {
                                    msg.copy(
                                        rawServerToolCalls = rawCalls,
                                        toolSteps = steps,
                                        refused = refused,
                                        toolSearchCount = searchCount,
                                        toolFetchCount = fetchCount,
                                        toolImageCount = imageCount,
                                        text = if (refused) "该请求被内容安全审核拒绝，未展示回答。" else msg.text,
                                    )
                                } else msg
                            })
                        }
                    }
                } else {
                    runDemo(conv.id, text, conv.model)
                }
            } catch (cancel: CancellationException) {
                // 用户主动停止或页面销毁：按「已停止」收尾，不报错
            } catch (t: Throwable) {
                val message = t.message ?: "请求失败，请稍后再试。"
                failAssistant(conv.id, message)
                notice = message
            } finally {
                val elapsed = ((System.currentTimeMillis() - startedAt) / 1000).toInt().coerceIn(1, 999)
                finishAssistant(conv.id, elapsed)
                streaming = false
                persist()
                scheduleSuggestions(force = false)
            }
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
    }

    fun regenerate() {
        val conv = activeConversation ?: return
        if (streaming) return
        val lastUser = conv.messages.lastOrNull { it.role == Role.USER } ?: return
        val trimmed = conv.messages.dropLastWhile { it.role == Role.ASSISTANT }
        updateConversation(conv.id) { it.copy(messages = trimmed) }
        send(lastUser.text)
    }

    fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Aura", text))
        notice = "已复制到剪贴板"
    }

    fun clearAllConversations() {
        stop()
        conversations = emptyList()
        store.clearConversations()
        refresh()
        route = Route.Home
        notice = "已清空全部对话"
    }

    fun restoreDemoConversations() {
        conversations = DemoChats.seed().sortedByPreferred()
        persist()
        notice = "已恢复演示对话"
    }

    // ---------------------------------------------------------------- 内部

    private suspend fun runDemo(conversationId: String, prompt: String, model: String) {
        val reply = DemoBrain.reply(prompt, model)
        reply.reasoning?.let { reasoning ->
            reasoning.chunked(5).forEach { part ->
                appendReasoning(conversationId, part)
                delay(14)
            }
            delay(140)
        }
        reply.text.chunked(3).forEach { part ->
            appendContent(conversationId, part)
            delay(16)
        }
        if (reply.citations.isNotEmpty()) {
            setCitations(conversationId, reply.citations)
        }
    }

    private fun updateConversation(id: String, transform: (Conversation) -> Conversation) {
        conversations = conversations.map { if (it.id == id) transform(it) else it }
    }

    private fun updateLastAssistant(id: String, transform: (ChatMessage) -> ChatMessage) {
        updateConversation(id) { conv ->
            if (conv.messages.isEmpty()) return@updateConversation conv
            val updated = conv.messages.mapIndexed { index, message ->
                if (index == conv.messages.lastIndex && message.role == Role.ASSISTANT) transform(message) else message
            }
            conv.copy(messages = updated)
        }
    }

    private fun appendContent(id: String, delta: String) =
        updateLastAssistant(id) { it.copy(text = it.text + delta) }

    private fun appendReasoning(id: String, delta: String) =
        updateLastAssistant(id) { it.copy(reasoning = (it.reasoning ?: "") + delta) }

    private fun setCitations(id: String, citations: List<com.aura.chat.data.Citation>) =
        updateLastAssistant(id) { it.copy(citations = citations) }

    private fun failAssistant(id: String, message: String) =
        updateLastAssistant(id) { it.copy(text = it.text.ifBlank { message }, status = MessageStatus.ERROR) }

    private fun finishAssistant(id: String, elapsed: Int) {
        updateLastAssistant(id) { message ->
            message.copy(
                status = message.status,
                text = message.text.ifBlank { "（已停止）" },
                elapsedSeconds = elapsed,
                reasoningSeconds = if (!message.reasoning.isNullOrBlank()) elapsed.coerceIn(1, 120) else null,
            )
        }
    }

    private fun titleFor(text: String): String {
        val single = text.replace("\n", " ").trim()
        return if (single.length <= 18) single else single.take(18) + "…"
    }

    private fun refresh() {
        conversations = conversations.sortedByPreferred()
    }

    private fun persist() {
        conversations = conversations.sortedByPreferred()
        store.saveConversations(conversations)
    }

    private fun List<Conversation>.sortedByPreferred(): List<Conversation> =
        sortedWith(compareByDescending<Conversation> { it.pinned }.thenByDescending { it.updatedAt })
}
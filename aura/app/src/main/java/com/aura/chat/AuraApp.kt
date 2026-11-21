package com.aura.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.chat.data.DocumentReader
import com.aura.chat.data.Models
import com.aura.chat.data.ThemeMode
import com.aura.chat.ui.screens.DshDetailScreen
import com.aura.chat.ui.screens.DshPairScreen
import com.aura.chat.ui.screens.DshSessionsScreen
import com.aura.chat.ui.screens.ChatScreen
import com.aura.chat.ui.screens.HomeScreen
import com.aura.chat.ui.screens.ModelSheet
import com.aura.chat.ui.screens.SettingsScreen
import com.aura.chat.ui.theme.AuraTheme
import com.aura.chat.ui.theme.LocalAuraAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuraApp(vm: AuraViewModel = viewModel()) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (vm.settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attachLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.attachUri(uri)
    }

    AuraTheme(darkTheme = darkTheme) {
        val route = vm.route
        BackHandler(enabled = route != Route.Home) { vm.back() }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
            val density = androidx.compose.ui.platform.LocalDensity.current
            val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp().value }
            val twoPane = LocalConfiguration.current.screenWidthDp >= 840 || windowWidthDp >= 840

            if (twoPane) {
                AdaptiveTwoPane(
                    vm = vm,
                    darkTheme = darkTheme,
                    onAttach = {
                                    attachLauncher.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                            "image/*", "text/*", "application/json",
                                        )
                                    )
                                },
                )
            }

            if (!twoPane) {
            AnimatedContent(
                targetState = route,
                transitionSpec = {
                    (fadeIn(tween(280)) + slideInHorizontally(tween(320)) { it / 14 }) togetherWith fadeOut(tween(160))
                },
                label = "route",
            ) { target ->
                when (target) {
                    Route.Home -> HomeScreen(
                        featured = vm.featuredConversation,
                        recent = vm.recentConversations,
                        demoMode = vm.settings.demoMode || !vm.settings.hasKey,
                        onOpen = vm::openConversation,
                        onNewChat = vm::newConversation,
                        onQuickStart = vm::startWithPrompt,
                        onSettings = vm::openSettings,
                        onTogglePin = vm::togglePin,
                        onDelete = vm::deleteConversation,
                        onOpenDsh = vm::openDshSessions,
                    )

                    Route.DshSessions -> DshSessionsScreen(
                        onBack = vm::back,
                        peer = vm.dshPeer,
                        sessions = vm.dshSessions,
                        loading = vm.dshSessionsLoading,
                        error = vm.dshSessionsError,
                        onRefresh = vm::loadDshSessions,
                        onOpenSession = { vm.openDshDetail(it) },
                        onPair = vm::openDshPair,
                    )

                    is Route.DshDetail -> {
                        val detailSessionId = target.id
                        DshDetailScreen(
                            peer = vm.dshPeer,
                            sessionId = detailSessionId,
                            detail = vm.dshDetail,
                            loading = vm.dshDetailLoading,
                            error = vm.dshDetailError,
                            onBack = vm::back,
                            onRefresh = { vm.loadDshDetail(detailSessionId) },
                            maxTurns = vm.settings.detailMaxTurns,
                            maxItems = vm.settings.detailMaxItems,
                            sending = vm.dshSending,
                            uploadProgress = vm.dshUploadProgress,
                            sendError = vm.dshSendError,
                            onSend = { text -> vm.dshSend(detailSessionId, text) },
                            onUploadMany = { files, text -> vm.dshUploadManyAndSend(detailSessionId, files, text) },
                        )
                    }

                    Route.DshPair -> DshPairScreen(
                        onBack = vm::back,
                        onConnect = { host, port, code -> vm.pairDsh(host, port, code) },
                    )

                    Route.Settings -> SettingsScreen(
                        settings = vm.settings,
                        dark = darkTheme,
                        onBack = vm::back,
                        onChange = vm::updateSettings,
                        onClear = vm::clearAllConversations,
                        onRestoreDemo = vm::restoreDemoConversations,
                        )

                    is Route.Chat -> {
                        val conversation = vm.conversations.firstOrNull { it.id == target.id }
                        if (conversation == null) {
                            LaunchedEffect(target.id) { vm.back() }
                            Box(Modifier.fillMaxSize())
                        } else {
                            ChatScreen(
                                conversation = conversation,
                                dark = darkTheme,
                                settings = vm.settings,
                                streaming = vm.streaming,
                                onSend = vm::send,
                                onStop = vm::stop,
                                onBack = vm::back,
                                onPickModel = vm::openModelSheet,
                                onCopy = vm::copyToClipboard,
                                onRegenerate = vm::regenerate,
                                onToggleWebSearch = {
                                    vm.updateSettings { it.copy(webSearchOn = !it.webSearchOn) }
                                },
                                onChangeReasoning = { level -> vm.updateSettings { it.copy(reasoningLevel = level) } },
                                onConsentTools = { vm.updateSettings { it.copy(toolsConsent = true) } },
                                attachmentName = vm.pendingFile?.name,
                                uploading = vm.uploadingFile,
                                onAttach = {
                                    attachLauncher.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                            "image/*", "text/*", "application/json",
                                        )
                                    )
                                },
                                onClearAttachment = vm::clearAttachment,
                            )
                        }
                    }
                }
            }

            }

            NoticeBanner(
                text = if (vm.settings.showNotices) vm.notice else null,
                onDismiss = vm::dismissNotice,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            if (vm.modelSheetOpen) {
                ModelSheet(
                    models = Models.all,
                    current = vm.activeConversation?.model ?: vm.settings.model,
                    onSelect = vm::selectModel,
                    onDismiss = vm::closeModelSheet,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AdaptiveTwoPane(
    vm: AuraViewModel,
    darkTheme: Boolean,
    onAttach: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()
    val selectedId = navigator.currentDestination?.contentKey
    BackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
    }
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
                HomeScreen(
                    featured = vm.featuredConversation,
                    recent = vm.recentConversations,
                    demoMode = vm.settings.demoMode || !vm.settings.hasKey,
                    onOpen = { id -> scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) } },
                    onNewChat = {
                        vm.newConversation()
                        vm.conversations.firstOrNull()?.let { latest ->
                            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, latest.id) }
                        }
                    },
                    onQuickStart = vm::startWithPrompt,
                    onSettings = vm::openSettings,
                    onTogglePin = vm::togglePin,
                        onDelete = vm::deleteConversation,
                        onOpenDsh = vm::openDshSessions,
                    selectedConversationId = selectedId,
                    contentMaxWidth = 340.dp,
                    onOpenSuggestion = { id -> scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, id) } },
                )
        },
        detailPane = {
                val conversation = selectedId?.let { id -> vm.conversations.firstOrNull { it.id == id } }
                    ?: vm.featuredConversation
                if (conversation != null) {
                    ChatScreen(
                        conversation = conversation,
                        dark = darkTheme,
                        twoPane = true,
                        settings = vm.settings,
                        streaming = vm.streaming,
                        onSend = vm::send,
                        onStop = vm::stop,
                        onBack = {},
                        onPickModel = vm::openModelSheet,
                        onCopy = vm::copyToClipboard,
                        onRegenerate = vm::regenerate,
                        onToggleWebSearch = {
                            vm.updateSettings { it.copy(webSearchOn = !it.webSearchOn) }
                        },
                        onChangeReasoning = { level -> vm.updateSettings { it.copy(reasoningLevel = level) } },
                        onConsentTools = { vm.updateSettings { it.copy(toolsConsent = true) } },
                        attachmentName = vm.pendingFile?.name,
                        uploading = vm.uploadingFile,
                        onAttach = onAttach,
                        onClearAttachment = vm::clearAttachment,
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "选一段对话，开始",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
        },
    )
}

@Composable
private fun NoticeBanner(text: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAuraAccent.current
    if (text == null) return
    LaunchedEffect(text) {
        delay(5000)
        onDismiss()
    }
    Surface(
        modifier = modifier.navigationBarsPadding().padding(bottom = 172.dp),
        shape = RoundedCornerShape(18.dp),
        color = accent.ink,
        contentColor = accent.onInk,
        shadowElevation = 12.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
private fun displayName(context: Context, uri: Uri): String {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                return cursor.getString(index) ?: "附件"
            }
        }
    }
    return uri.lastPathSegment ?: "附件"
}
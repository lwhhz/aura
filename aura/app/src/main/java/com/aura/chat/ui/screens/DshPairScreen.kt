package com.aura.chat.ui.screens

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aura.chat.ui.components.AuraPill
import com.aura.chat.ui.components.Eyebrow
import com.aura.chat.ui.components.LavenderField
import com.aura.chat.ui.components.LightTopBar
import com.aura.chat.ui.components.PillStyle
import com.aura.chat.ui.theme.LocalAuraAccent
import com.aura.chat.ui.theme.editorialFamily

private const val SERVICE_TYPE = "_dsh._tcp."

data class DiscoveredHost(val name: String, val host: String, val port: Int)

/** 局域网配对：mDNS 自动发现 + 手动 IP/端口 + 配对码。 */
@Composable
fun DshPairScreen(
    onBack: () -> Unit,
    onConnect: (host: String, port: Int, code: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAuraAccent.current
    val context = LocalContext.current
    var hosts by remember { mutableStateOf<List<DiscoveredHost>>(emptyList()) }
    var scanning by remember { mutableStateOf(true) }
    var manualHost by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("8765") }
    var code by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { scanning = false }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) { scanning = false }
            override fun onServiceFound(info: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                nsd(context).resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host?.hostAddress ?: return
                        hosts = (hosts + DiscoveredHost(serviceInfo.serviceName, host, serviceInfo.port))
                            .distinctBy { it.host + ":" + it.port }
                    }
                })
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                hosts = hosts.filterNot { it.name == info.serviceName }
            }
        }
        val manager = nsd(context)
        runCatching { manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { scanning = false }
        onDispose { runCatching { manager.stopServiceDiscovery(listener) } }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LavenderField()
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            LightTopBar(title = "配对新电脑", onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "intro") {
                    Column {
                        Eyebrow("局域网 · mDNS", color = accent.onChip)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "让手机找到这台电脑",
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = editorialFamily("让手机找到这台电脑"),
                            color = accent.text,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "电脑端需要运行 DSH 的配对桥（后续提供）。手机与电脑需在同一 Wi-Fi。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent.muted,
                        )
                    }
                }
                item(key = "found-title") {
                    Text(
                        text = if (scanning) "正在搜索局域网…" else "发现 " + hosts.size + " 台电脑",
                        style = MaterialTheme.typography.titleMedium,
                        color = accent.text,
                    )
                }
                if (hosts.isEmpty()) {
                    item(key = "empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = accent.card,
                        ) {
                            Text(
                                text = "没有发现设备。请确认电脑端桥已启动、手机在同一 Wi-Fi，或手动填写地址。",
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = accent.muted,
                            )
                        }
                    }
                } else {
                    items(hosts) { host ->
                        Surface(
                            onClick = { onConnect(host.host, host.port, code) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = accent.card,
                        ) {
                            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(host.name, style = MaterialTheme.typography.titleMedium, color = accent.text)
                                    Text(
                                        host.host + ":" + host.port,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accent.muted,
                                    )
                                }
                                AuraPill(text = "连接", onClick = { onConnect(host.host, host.port, code) }, style = PillStyle.Deep)
                            }
                        }
                    }
                }
                item(key = "manual") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = accent.card,
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("手动连接", style = MaterialTheme.typography.titleMedium, color = accent.text)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = manualHost,
                                    onValueChange = { manualHost = it },
                                    label = { Text("IP 地址") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = manualPort,
                                    onValueChange = { manualPort = it.filter { c -> c.isDigit() } },
                                    label = { Text("端口") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.size(width = 96.dp, height = 56.dp),
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("6 位配对码") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(12.dp))
                            AuraPill(
                                text = "连接",
                                onClick = {
                                    onConnect(manualHost.trim(), manualPort.toIntOrNull() ?: 8765, code)
                                },
                                style = PillStyle.Deep,
                            )
                        }
                    }
                }
                item(key = "qr") {
                    Text(
                        text = "扫码配对（后续支持）：电脑端展示二维码，手机相机直接识别。",
                        style = MaterialTheme.typography.bodySmall,
                        color = accent.muted,
                    )
                }
            }
        }
    }
}

private fun nsd(context: Context): NsdManager =
    context.getSystemService(Context.NSD_SERVICE) as NsdManager

package com.aura.chat.ui.screens

import com.aura.chat.ui.theme.AuraShapeTokens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.net.nsd.NsdManager
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

/** 局域网配对：mDNS 自动发现 + 手动 IP/端口/配对码。 */
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
    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES else Manifest.permission.ACCESS_FINE_LOCATION
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(permission)
    }

    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        // UDP 广播兜底：监听 8766，补 mDNS 被防火墙拦截的场景
        var running = true
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    DatagramSocket(null).apply {
                        reuseAddress = true
                        bind(InetSocketAddress(8766))
                    }
                }.onSuccess { socket ->
                    val buffer = ByteArray(2048)
                    while (running) {
                        runCatching {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            val text = String(packet.data, 0, packet.length)
                            val json = JSONObject(text)
                            val name = json.optString("name", "DSH Desktop")
                            val port = json.optInt("port", 8765)
                            val host = packet.address.hostAddress ?: return@runCatching
                            withContext(Dispatchers.Main) {
                                hosts = (hosts + DiscoveredHost(name, host, port)).distinctBy { it.host + ":" + it.port }
                            }
                        }
                    }
                    socket.close()
                }
            }
        }
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
        onDispose {
            running = false
            runCatching { manager.stopServiceDiscovery(listener) }
        }
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
                if (!permissionGranted) {
                    item(key = "perm") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AuraShapeTokens.extraLarge,
                            color = accent.lemon,
                            contentColor = accent.onLemon,
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Text(
                                    "需要「附近设备」权限才能扫描局域网",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = accent.onLemon,
                                )
                                Spacer(Modifier.height(10.dp))
                                AuraPill(
                                    text = "授予权限",
                                    onClick = { permissionLauncher.launch(permission) },
                                    style = PillStyle.Deep,
                                )
                            }
                        }
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
                            shape = AuraShapeTokens.extraLarge,
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
                            modifier = Modifier.fillMaxWidth(),
                            shape = AuraShapeTokens.extraLarge,
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
                        shape = AuraShapeTokens.extraLarge,
                        color = accent.card,
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("手动连接", style = MaterialTheme.typography.titleMedium, color = accent.text)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextField(
                                    value = manualHost,
                                    onValueChange = { manualHost = it },
                                    label = { Text("IP 地址") },
                                    singleLine = true,
                                    shape = AuraShapeTokens.largeIncreased,
                                    colors = pairFieldColors(),
                                    modifier = Modifier.weight(1f),
                                )
                                TextField(
                                    value = manualPort,
                                    onValueChange = { manualPort = it.filter { c -> c.isDigit() } },
                                    label = { Text("端口") },
                                    singleLine = true,
                                    shape = AuraShapeTokens.largeIncreased,
                                    colors = pairFieldColors(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.size(width = 104.dp, height = 64.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            TextField(
                                value = code,
                                onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                                label = { Text("6 位配对码") },
                                singleLine = true,
                                shape = AuraShapeTokens.largeIncreased,
                                colors = pairFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(14.dp))
                            AuraPill(
                                text = "连接",
                                onClick = { onConnect(manualHost.trim(), manualPort.toIntOrNull() ?: 8765, code) },
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

@Composable
private fun pairFieldColors() = run {
    val accent = LocalAuraAccent.current
    TextFieldDefaults.colors(
        focusedContainerColor = accent.chip,
        unfocusedContainerColor = accent.chip,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = accent.deep,
        focusedLabelColor = accent.onChip.copy(alpha = 0.85f),
        unfocusedLabelColor = accent.onChip.copy(alpha = 0.7f),
        focusedTextColor = accent.onChip,
        unfocusedTextColor = accent.onChip,
    )
}

private fun nsd(context: Context): NsdManager =
    context.getSystemService(Context.NSD_SERVICE) as NsdManager
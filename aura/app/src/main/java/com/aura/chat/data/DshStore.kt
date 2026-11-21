package com.aura.chat.data

import android.content.Context

/** 已配对 DSH 电脑（token 仅存本机）。 */
data class DshPeer(val host: String, val port: Int, val token: String)

class DshStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("aura_dsh", Context.MODE_PRIVATE)

    fun load(): DshPeer? {
        val host = prefs.getString("host", null) ?: return null
        val token = prefs.getString("token", null) ?: return null
        return DshPeer(host, prefs.getInt("port", 8765), token)
    }

    fun save(peer: DshPeer) {
        prefs.edit()
            .putString("host", peer.host)
            .putInt("port", peer.port)
            .putString("token", peer.token)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

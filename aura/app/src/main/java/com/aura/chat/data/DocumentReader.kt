package com.aura.chat.data

/**
 * 本地文档解析已停用：官方 Files API 接入（P2）之前，附件入口暂时关闭。
 * 保留无依赖的占位实现，避免 UI 层出现空引用。
 */
object DocumentReader {
    data class Result(val text: String = "", val note: String = "附件功能暂停：等待官方文件解析（P2）")
}

package com.aura.chat

import com.aura.chat.data.DemoBrain
import com.aura.chat.data.Models
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoBrainTest {

    @Test
    fun alarmPromptRoutesToClockReply() {
        val reply = DemoBrain.reply("早上七点半的闹钟，我不想被催", Models.DEEPSEEK_CHAT.id)
        assertTrue(reply.text.contains("01 逐段亮起"))
    }

    @Test
    fun podcastPromptRoutesToPodcastReply() {
        val reply = DemoBrain.reply("把这周的播客剪成三句话", Models.DEEPSEEK_CHAT.id)
        assertTrue(reply.text.contains("注意力预算"))
    }

    @Test
    fun reasonerAddsReasoningChatDoesNot() {
        val withReasoning = DemoBrain.reply("帮我整理一个决定", Models.DEEPSEEK_REASONER.id)
        assertNotNull(withReasoning.reasoning)
        val withoutReasoning = DemoBrain.reply("帮我整理一个决定", Models.DEEPSEEK_CHAT.id)
        assertNull(withoutReasoning.reasoning)
    }

    @Test
    fun musicPromptCarriesCitations() {
        val reply = DemoBrain.reply("帮我写音乐之夜邀请文案", Models.DEEPSEEK_REASONER.id)
        assertEquals(2, reply.citations.size)
    }
}

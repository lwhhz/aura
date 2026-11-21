package com.aura.chat.data

data class DemoReply(
    val text: String,
    val reasoning: String? = null,
    val citations: List<Citation> = emptyList(),
)

/**
 * 演示模式回复：无 API Key 时也能看到「思考 / 搜索引用 / 编辑式回答」的完整形态。
 * 内容与截图里的场景对应（音乐之夜、穿搭、闹钟、播客剪辑）。
 */
object DemoBrain {

    fun reply(prompt: String, model: String): DemoReply {
        val p = prompt.lowercase()
        val base = when {
            listOf("闹钟", "alarm", "morning", "早").any { p.contains(it) } -> clock()
            listOf("播客", "podcast", "剪").any { p.contains(it) } -> podcast()
            listOf("穿", "fit", "athleisure", "搭").any { p.contains(it) } -> stylist()
            listOf("音乐", "邀请", "music", "night").any { p.contains(it) } -> music()
            else -> generic()
        }
        return if (Models.of(model).reasoning) {
            base.copy(reasoning = base.reasoning ?: defaultReasoning(prompt))
        } else {
            base.copy(reasoning = null)
        }
    }

    private fun defaultReasoning(prompt: String): String =
        "先把问题定性：" + prompt.take(14) + " 属于需要取舍的问题，不是信息缺口。" +
            " 于是按「先给结论，再给两条例由，最后留一个问题」组织回答，" +
            "避免把建议写成一长串清单。"

    private fun music() = DemoReply(
        reasoning = "场景是小规模 listening session，人数 20 以内。" +
            " 语气要克制：不喊口号、不用感叹号；信息层次按地点、时间、主人、报名排。" +
            " 结尾只留一句低声量的行动句。",
        citations = listOf(
            Citation("小型 listening session 的场地与声学建议", "Echo Bridge 场地手册"),
            Citation("独立杂志式邀请函的排版参考", "Aura Journal · Issue 04"),
        ),
        text = "**Serafina — a listening night**\n\n" +
            "Where  ·  Echo Bridge, room 2\n" +
            "When  ·  Friday, 10:30 pm\n" +
            "Host  ·  Odette\n\n" +
            "二十把椅子，一盏灯，一张完整听完的唱片。\n" +
            "不做开场白，也不放歌单之外的歌。\n\n" +
            "Bring one record you keep returning to.\n" +
            "**18 guests going** —— 报名已满，可以排候补。",
    )

    private fun stylist() = DemoReply(
        citations = listOf(
            Citation("Soft tailoring 的三条版型原则", "Atelier Notes"),
            Citation("雾紫与石蓝的低饱和配色", "Palette Weekly"),
        ),
        text = "**雾紫阔腿裤 + 石蓝针织 + 一双薄底跑鞋。**\n\n" +
            "上身留一点余量，裤脚堆一点褶；配饰只留一件——耳骨夹或细链。\n\n" +
            "· 颜色：雾紫 / 石蓝 / 米白\n" +
            "· 面料：垂坠感要大于弹力感\n" +
            "· 鞋：薄底，别选厚底\n\n" +
            "整体读起来像「刚练完，但要去见人」。",
    )

    private fun clock() = DemoReply(
        text = "**01 逐段亮起**\n音量从 20% 起，每 8 秒 +10%，配一段单簧管。\n\n" +
            "**02 只提醒一次**\n响一次就停，屏上留一句「窗边的光已经进来了」。\n\n" +
            "**03 反向倒计时**\n从 06:58 开始倒数，把「还剩多久」换成「已经多久」。\n\n" +
            "三个方案都不再响第二遍——这才是「不像催促」的关键。",
    )

    private fun podcast() = DemoReply(
        reasoning = "本周两期节目都在讲「注意力预算」：一期从神经科学讲，一期从产品设计讲。" +
            " 先合并共同结论，再各留一个反例，最后用问题收尾，而不是给行动建议。",
        text = "**本周关键词：注意力预算。**\n\n" +
            "1. 一天里能做的「深」决定不超过 3 个，其余的都应该被提前定好。\n" +
            "2. 通知不是打断，是替你做了一次优先级投票。\n" +
            "3. 让界面安静，比让界面好看更难，也更有用。\n\n" +
            "**留给你的问题：** 如果今天只允许一次真正的打断，你会把它留给谁？",
    )

    private fun generic() = DemoReply(
        text = "**先给一个能立刻动手的角度。**\n\n" +
            "把问题拆成三栏：已经确定 / 还需要验证 / 可以晚点决定。" +
            "今天就只推进第一栏里的两件事。\n\n" +
            "· 已经确定：目标、截止时间、不可让步的约束\n" +
            "· 还需要验证：唯一那个假设\n" +
            "· 可以晚点决定：其余全部细节\n\n" +
            "当前是演示模式。在「设置」里填入 DeepSeek API Key 后，这里会换成真实模型的流式回答。",
    )
}

package com.aura.chat.data

/** 内置演示会话：截图里的编辑式英文内容 + 中文界面标签。没有 API Key 也能完整体验。 */
object DemoChats {

    fun seed(now: Long = System.currentTimeMillis()): List<Conversation> = listOf(
        eventThread(now),
        stylistThread(now),
        clockThread(now),
        podcastThread(now),
    )

    private fun eventThread(now: Long) = Conversation(
        title = "Serafina · music night",
        updatedAt = now - 12 * 60_000,
        model = Models.DEEPSEEK_FLASH.id,
        accent = 0,
        pinned = true,
        pinnedAt = now,
        messages = listOf(
            ChatMessage(
                role = Role.USER,
                text = "帮我写一条音乐之夜的邀请文案，风格像一本独立杂志，别太吵。",
                model = Models.DEEPSEEK_FLASH.id,
            ),
            ChatMessage(
                role = Role.ASSISTANT,
                model = Models.DEEPSEEK_FLASH.id,
                reasoningSeconds = 6,
                reasoning = "先定位场景：小型 listening session，人数 20 以内。" +
                    " 语气要克制，不喊口号；信息层次按「地点 / 时间 / 阵容 / 报名」排。" +
                    " 结尾留一个低声量的行动句。",
                citations = listOf(
                    Citation("Listening session 的场地声学建议", "Echo Bridge 场地手册"),
                    Citation("独立杂志式邀请函的排版参考", "Aura Journal · Issue 04"),
                ),
                text = "**Serafina — a listening night**\n\n" +
                    "Where  ·  Echo Bridge, room 2\n" +
                    "When  ·  Friday, 10:30 pm\n" +
                    "Host  ·  Odette\n\n" +
                    "二十把椅子，一盏灯，一张完整听完的唱片。\n" +
                    "不做开场白，也不放歌单之外的歌。\n\n" +
                    "Bring one record you keep returning to.\n" +
                    "**18 guests going · 报名已满**",
            ),
        ),
    )

    private fun stylistThread(now: Long) = Conversation(
        title = "Athleisure, but elevated",
        updatedAt = now - 3 * 60 * 60_000,
        model = Models.DEEPSEEK_FLASH.id,
        accent = 1,
        messages = listOf(
            ChatMessage(
                role = Role.USER,
                text = "Make me a fit that's athleisure but elevated.",
                model = Models.DEEPSEEK_FLASH.id,
            ),
            ChatMessage(
                role = Role.ASSISTANT,
                model = Models.DEEPSEEK_FLASH.id,
                elapsedSeconds = 3,
                citations = listOf(
                    Citation("Soft tailoring 的三条版型原则", "Atelier Notes"),
                    Citation("雾紫与石蓝的低饱和配色", "Palette Weekly"),
                ),
                text = "**雾紫阔腿裤 + 石蓝针织 + 一双薄底跑鞋。**\n\n" +
                    "上身留一点余量，裤脚堆一点褶；配饰只留一件，耳骨夹或细链。\n\n" +
                    "· 颜色：雾紫 / 石蓝 / 米白\n" +
                    "· 面料：垂坠感要大于弹力感\n" +
                    "· 鞋：薄底，别选厚底\n\n" +
                    "整体读起来像「刚练完，但要去见人」。",
            ),
        ),
    )

    private fun clockThread(now: Long) = Conversation(
        title = "07:30, by the window",
        updatedAt = now - 26 * 60 * 60_000,
        model = Models.DEEPSEEK_FLASH.id,
        accent = 2,
        messages = listOf(
            ChatMessage(
                role = Role.USER,
                text = "早上七点半的闹钟，我想让它听起来不像催促。给我三个方案。",
                model = Models.DEEPSEEK_FLASH.id,
            ),
            ChatMessage(
                role = Role.ASSISTANT,
                model = Models.DEEPSEEK_FLASH.id,
                elapsedSeconds = 4,
                text = "**01 逐段亮起**\n音量从 20% 起，每 8 秒 +10%，配一段单簧管。\n\n" +
                    "**02 只有一次提醒**\n响一次就停，屏上留一句「窗边的光已经进来了」。\n\n" +
                    "**03 反向倒计时**\n从 06:58 开始倒数，把「还剩多久」换成「已经多久」。\n\n" +
                    "三个方案都不再响第二遍——这才是「不像催促」的关键。",
            ),
        ),
    )

    private fun podcastThread(now: Long) = Conversation(
        title = "Aura Wave · weekly edit",
        updatedAt = now - 2 * 24 * 60 * 60_000,
        model = Models.DEEPSEEK_FLASH.id,
        accent = 3,
        messages = listOf(
            ChatMessage(
                role = Role.USER,
                text = "把这周的播客剪成三句话，最后给我一个问题。",
                model = Models.DEEPSEEK_FLASH.id,
            ),
            ChatMessage(
                role = Role.ASSISTANT,
                model = Models.DEEPSEEK_FLASH.id,
                reasoningSeconds = 9,
                reasoning = "本周两期节目都在讲「注意力预算」：一期从神经科学讲，" +
                    "一期从产品设计讲。先合并共同结论，再各自留一个反例，" +
                    "最后用问题收尾，而不是给行动建议。",
                text = "**本周关键词：注意力预算。**\n\n" +
                    "1. 一天里能做的「深」决定不超过 3 个，其余的都应该被提前定好。\n" +
                    "2. 通知不是打断，是替你做了一次优先级投票。\n" +
                    "3. 让界面安静，比让界面好看更难，也更有用。\n\n" +
                    "**留给你的问题：** 如果今天只允许一次真正的打断，你会把它留给谁？",
            ),
        ),
    )
}
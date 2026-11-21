# 官方开源 chat-demo 调研：联网搜索与文件识别

调研日期：2026-11-21 ｜ 性质：只调研，不改代码 ｜ 结论先行：**这两项能力不是前端实现，而是官方 API 的服务端工具**

## 一、事实（带来源）

1. 仓库 `deepseek-ai/chat-demo`，MIT 许可，5,149 stars，2026-11-20 仍在提交，未归档。
   来源：api.github.com/repos/deepseek-ai/chat-demo
2. 官方公告《DeepSeek 发布全家桶应用 API》（2026-11-13）：
   联网搜索、深度思考搜索、文件输入、阅读链接、模型安全等能力在**原有 API 中开启参数**即可获得，
   "相关工具调用与上下文拼接均由服务端自动完成"；底层能力也可单独调用（Search / Fetch / OCR / ASR / TTS）。
   来源：api-docs.deepseek.com/zh-cn/news/news261113.md
3. 《构建 Chatbot》指南明确写了：
   "**搜索和阅读链接都由服务端完成。客户端只需要展示返回的内容块，并在下一轮把它们原样回传。**"
   并要求使用 **deepseek-flash** 模型；协议支持 Messages / Responses / Chat Completions。
   文件输入：支持 PDF、Word、PowerPoint、图片，服务端完成纯文本提取或图文混排解析后拼入上下文。
   来源：api-docs.deepseek.com/zh-cn/guides/build_a_chatbot.md
4. 公告同时给出定价说明：除 Token 费外，工具调用另行收费（具体价格表为图片，未取到数值）。
5. 本地 chat-demo-v0.1.3 是打包后的静态前端（只有 index.html、压缩后的 JS/CSS），
   没有可复用的搜索/文件解析源码；字符串检索只命中打包产物，无实现逻辑。

## 二、能力对照（官方 vs Aura 现状）

| 能力 | 官方 chat-demo | Aura 现状 | 差异 |
|---|---|---|---|
| 联网搜索 | 服务端 `deepseek_web_search`，自动搜网页/图片并附引用 | 客户端 DuckDuckGo HTML 抓取，可选 Tavily | 引用质量、时效性、稳定性均弱于官方；且客户端抓取易被封 |
| 深度思考搜索 | 思考模式多步搜索 + 阅读链接 | 无 | 需要模型侧支持 |
| 阅读链接 | `deepseek_read_url` 自动抓链接 | 无 | 缺失 |
| 文件输入 | 服务端解析 PDF / Office / 图片（含图文混排） | 本地 ML Kit OCR + PdfRenderer + docx 解析 | 本地不上传原件、免费，但版式与表格能力弱 |
| 安全风控 | 参数开启基础风控 | 无 | 若对外发布需要 |
| 会话标题 | 回答结束自动生成 | 本地截断 | 观感差异 |
| 语音 | ASR / TTS 独立接口 | 仅占位按钮 | 缺失 |
| 模型要求 | deepseek-flash | deepseek-chat / reasoner | 互斥选择 |

## 三、可选方案

| 方案 | 做法 | 优点 | 成本/风险 |
|---|---|---|---|
| A 直接接官方工具 | 加 `deepseek_web_search` / `deepseek_read_url` / 文件输入参数，模型切到 deepseek-flash，客户端只渲染 content blocks 并回传 | 与官方体验一致，引用真实，解析更强 | 每次工具调用额外计费；必须 deepseek-flash；文件原样上传官方；需要按新协议改造客户端 |
| B 保持现状 | 继续 DuckDuckGo/Tavily + 本地 OCR | 免费、离线、隐私好 | 引用质量与文件解析弱；第三方搜索不稳定 |
| C 混合可切换 | 设置里加"检索方式：本地 / 官方工具"，文件默认本地，超出版式能力时提示切官方 | 兼顾成本与体验 | 两套链路都要维护；协议分支多 |

## 四、建议与待决策

建议先做 C 的最小版本（官方工具开关 + 协议适配层），但需要你先确认：

1. 是否接受为联网/文件解析的**工具调用额外付费**？
2. 是否接受把用户上传的 PDF/图片**原样上传**到官方服务端解析？
3. 是否愿意把默认模型切到 deepseek-flash（与现有 chat/reasoner 二选一，或按场景切换）？
4. 这一轮只做调研，还是要我据此写实施计划书（不动代码）？

## 五、限制说明

- 官方定价表是图片，未取到具体单价，只用"额外计费"这一事实。
- 未能抓取 GitHub README 原文（raw.githubusercontent 连接被重置），仓库信息来自 GitHub API，能力细节来自官方文档。
- chat-demo 前端为压缩产物，无法核对它渲染 content block 的具体实现。

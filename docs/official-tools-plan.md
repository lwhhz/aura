# 官方联网工具与文件解析接入计划书（Aura 0.8）

编制日期：2026-11-21 ｜ 状态：待评审（本文件不含实现）｜ 决策依据：用户选定方案 A（直接接官方工具）、模型按场景自动切换、接受工具计费与原件上传

## 一、目标与范围

目标：把 Aura 的联网检索与文件理解，从"客户端自建"改为"官方服务端工具"，与 DeepSeek App/网页端体验对齐。

范围内：
1. Chat Completions 协议下声明 deepseek_web_search / deepseek_read_url 工具。
2. 通过 Files API 上传并解析 PDF / Word / PowerPoint / 图片，对话中用 file_id 引用。
3. 渲染真实引用（annotations + sources），保留工具步骤与思考。
4. 按场景自动切换模型：需要工具时用 deepseek-flash，否则沿用用户选择的 chat/reasoner。
5. 计费可见与开关控制。

范围外（本轮不做）：语音 ASR/TTS、会话标题自动生成、内容安全对外合规改造、历史对话导入导出。

## 二、协议与字段（已按官方文档核对）

### 2.1 请求

- endpoint：POST /chat/completions（沿用现有 SSE 客户端）。
- model：deepseek-flash（仅这个模型支持工具）。
- tools：[{"type":"deepseek_web_search"},{"type":"deepseek_read_url"}]。
- 思考：thinking:{"type":"enabled"} 开启深度思考搜索；关闭时是普通联网搜索。
- 禁止：tool_choice 不能指向两个工具；声明工具后不能再传 web_search_options；response_format 必须为 text；不能传 logprobs。
- 文件：先把文件 POST /files（multipart，purpose 放在 file 字段之前）拿到 file_id，再在 user message 的 content 数组里放 {"type":"file","file_id":...} + {"type":"text","text":...}。
  purpose 三选一：text_recognition（只取文字）、rich_layout_recognition（保留图表位置）、user_data（纯文本/表格/电子书，免费）。
- 限制：单请求最多 20 个文档，文字合计 8 MiB；原文件最大 32 MiB；原件与解析结果都不可下载。
- 安全（可选）：safety_check:{"level":"high"}，拒答时 finish_reason=content_filter。

### 2.2 响应与流式

- 工具步骤在 message.server_tool_calls：每步含 reasoning_content 与 calls；call 含 type/kind/action/status/sources/page/signature。
- 引用在 message.annotations（url_citation：url/title/start_index/end_index），正文里没有角标，需要客户端按 index 插入。
- 流式：delta.reasoning_content / delta.content 照常；工具过程通过 choice 上的 server_tool_event（step_reasoning_done / call_started / call_completed）。
- 完整 server_tool_calls 与 annotations 只在最后一个 chunk 的 delta 中下发一次。
- 用量：usage.server_tool_use.{tool_search,tool_fetch,tool_image_search}。
- 多轮：assistant 消息必须连同 server_tool_calls 原样回传，否则之前读过的网页不进入上下文（阅读链接靠 signature 还原）。

## 三、模型自动切换

| 场景 | 模型 | 工具 |
|---|---|---|
| 打开"联网检索"（官方模式） | deepseek-flash | web_search（+read_url 当消息含链接） |
| 附带文件 | deepseek-flash | 文件块；不声明搜索除非同时开启 |
| 两者都无 | 用户选择（deepseek-chat / reasoner） | 无 |
| 无 Key / 演示模式 | 本地演示 | 无 |

UI：输入区上方显示"工具模式 · deepseek-flash"小标签；设置页增加开关"官方联网工具"与"文件走官方解析"，默认开启（需确认计费）。

## 四、数据模型改动

1. ChatMessage 增加：
   - parts：List<Part>（Text / Citation / ToolStep），用于渲染；
   - rawServerToolCalls：String?（原样回传用）；
   - annotations：List<Citation>；
   - finishReason / refused：Boolean；
   - toolUsage：search/fetch/image 次数。
2. Attachment 改为：fileName、mime、fileId、purpose、uploadState（idle/uploading/parsing/ready/failed）、error。
3. 本地解析（ML Kit OCR / PdfRenderer / docx）保留为 fallback：仅在无 Key、官方解析失败、或用户显式选"本地解析"时使用。

## 五、交互与 UI

- 引用：回答下方显示来源卡片（站点图标、标题、域名、发布时间），点击用系统浏览器打开；正文里的角标（start_index/end_index）用上标数字。
- 工具过程：默认折叠为"已完成搜索 2 次 · 阅读 1 个链接"，展开看每步 queries 与来源。
- 文件：附件 chip 显示上传/解析进度；失败可重试；超过 32 MiB 或 8 MiB 文本上限时提前拦下并提示。
- 拒答：finish_reason=content_filter 时删除已流式内容，展示"该请求被内容安全审核拒绝"。
- 计费：每条回答底部显示本次工具调用次数；设置页显示今日累计次数与"日上限"（默认 30 次，可改）。

## 六、分阶段任务与验收

| 阶段 | 任务 | 验收 |
|---|---|---|
| P0 协议层 | SSE 解析扩展：server_tool_event、server_tool_calls、annotations、usage.server_tool_use；模型切换 | 单元测试覆盖 4 类 chunk；真机一次联网提问能拿到 sources 与 citations |
| P1 联网 | 开关、请求声明 tools、引用渲染、多轮回传 server_tool_calls | 追问第二轮能引用上一轮读过的网页；关闭开关后不发起搜索 |
| P2 文件 | /files 上传（3 种 purpose）、进度、错误、file_id 引用、上限校验 | 上传 PDF/Word/图片各 1 份，问答正确；超限提示 |
| P3 稳健性 | 拒答处理、超时（上传+解析）、400 分支、计费显示与日上限、本地 fallback | 断网/超时/401/400 各有可读提示；拒答清空内容 |
| P4 回归 | 双栏/手机两套布局、深浅主题、README 与截图 | 与 0.7.x 功能不回归 |

预估：P0 1 天，P1 1 天，P2 1.5 天，P3 1 天，P4 0.5 天。

## 七、风险

1. 计费不可预估：工具与文件解析按次收费，价格表为图片，无单价；只能做事后展示 + 日上限。
2. 原件上传：用户已接受，但需在首次开启时明确提示"文件会上传到官方服务端解析"。
3. 上下文膨胀：文档文字最多 8 MiB，多轮累计可能触发 400 超长；需要"仅本轮引用"策略或摘要降级。
4. 解析耗时：上传请求要等解析完成才返回，客户端与代理超时需设为上传+解析时间。
5. 协议耦合：server_tool_calls 必须原样回传，一旦清洗/裁剪就会丢网页上下文。
6. 合规：若对外发布，safety_check 与 AI 标识等义务需自行承担。

## 七点五、实施进度（2026-11-21）

- P0/P1 代码已提交并编译通过（0.8.0）：协议字段、SSE 扩展解析、多轮回传、引用与工具步骤 UI、模型自动切换、首次计费/上传确认。
- 未验证：没有可用 API Key，官方便宜路径一次都没有真跑；工具用量计数（usage.server_tool_use）未接入。
- 决策 3（不设上限）：未实现任何日上限。
- 决策 4（不保留本地检索/OCR）：本地检索已移除；本地 OCR 暂留到 P2，否则附件功能会空窗。

## 八、待决策

1. 首次开启官方工具时，是否强制弹一次"计费 + 上传"确认框？
2. 文件默认 purpose：文本类默认 text_recognition、图片默认 rich_layout_recognition，是否可接受？
3. 日上限默认 30 次是否合适？
4. 本地检索/OCR 是否保留为 fallback（建议保留），还是彻底移除？
5. P0–P4 是否按顺序交付，还是先做 P0+P1（联网）验收后再做文件？
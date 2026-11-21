# 主页展示功能落实计划书（Aura 0.7）

编制日期：2026-11-21 ｜ 状态：待评审（未开始实现）｜ 依据：本轮对话 + 开源调研

## 一、背景与现状

主页当前是三块固定内容：

1. 「继续上次」大卡：取 conversations 里第一条 pinned，没有 pinned 时取 updatedAt 最大的一条；
   但示例数据把 Serafina 设为 pinned，所以它长期不变。
2. 右上角 PINNED：只是卡片上的一个装饰标签，不能点击，也没有对应的置顶时间字段（只有 Boolean）。
3. 「灵感 · 一点就开」：4 条硬编码文案，不随对话变化。

## 二、需求拆解（以用户可见行为为准）

| 编号 | 行为 | 验收标准 |
|---|---|---|
| R1 | 继续上次卡跟随最近一次有内容的会话 | 新建/发送消息后，卡片标题、摘要、模型标签在 1 秒内更新；点击进入该会话 |
| R2 | PINNED 可切换 | 点击置顶/取消置顶有动画；重启 App 后状态保持；置顶会话固定展示，可多个 |
| R3 | 灵感标签由模型总结并实时更新 | 对话变更后节流刷新；有 Key 时生成 3–5 个短标签；无 Key 走本地规则；失败不阻塞 |

## 三、数据与状态设计

- Conversation 增加 pinnedAt: Long?（保留 pinned 字段做兼容，读时以 pinnedAt 优先）。
- 新增 SuggestionRepository：
  - state: suggestions(List<String>), sourceHash, generatedAt, dailyCount
  - 持久化：SharedPreferences（suggestions / hash / generatedAt / dailyCount / dayKey）
- 刷新触发：
  - 应用启动后 800ms 防抖
  - 每次对话发送完成后 1.5s 防抖
  - 手动点击「换个灵感」立即触发
- 去重：sourceHash = 最近 5 条会话的 id + updatedAt 摘要；hash 未变不重复调用。
- 失败：8 秒超时 → 指数退避重试 2 次 → 降级本地规则。

## 四、接口与提示词

- 新增 DeepSeekClient.complete()：非流式，max_tokens 160，temperature 0.5，8 秒超时。
- system 提示：你是编辑助手；根据最近对话，输出 3–5 个 2–6 字的可点击标签；只输出 JSON 数组。
- user 内容：最近 5 条会话的标题 + 各自最近一条消息的前 80 字。
- 解析：JSON 解析失败 → 正则提取引号内容 → 仍失败则降级。
- 成本：每次刷新 1 次短调用；本地日上限 30 次，超出直接用缓存。

## 五、交互与视觉

- 置顶卡：PINNED 从装饰标签改为可点 chip（选中实心柠檬、未选中白 14%），点击切换并持久化；
  卡片内容切换用 AnimatedContent + spring。
- 灵感行：LazyRow；生成中显示 3 个骨架（自绘 alpha 呼吸动画，不引第三方库）；
  失败显示「换个灵感」重试 chip；点击标签 = 以该文案开新会话。
- 空态：无会话时「继续上次」卡替换为引导块（标题 + 新建按钮）。

## 六、开源调研（2026-11-21）

| 项目/资料 | 链接 | 类型 | 许可 | 结论 |
|---|---|---|---|---|
| NextChat | github.com/ChatGPTNextWeb/NextChat | 开源聊天客户端 | 未核验 | 参考「置顶会话」交互，不引入 |
| LobeChat | github.com/lobehub/lobe-chat | 开源 AI 工作台 | 未核验 | 参考话题置顶与分组思路，不引入 |
| Prompt suggestions（NN/g） | nngroup.com/articles/prompt-suggestions | 设计文章 | — | 作为标签文案与位置的设计依据 |
| PromptHelper（论文） | arxiv.org/html/2601.15575v1 | 学术原型 | — | 参考提示词推荐做法，不引入代码 |
| compose-shimmer-skeleton 等 | github.com/timoseyfarth/compose-shimmer-skeleton 等 | Compose 库 | 未核验 | 骨架屏自绘，不引入 |
| 自研 | — | — | — | 推荐：数据层、生成管线、骨架动效全部自研 |

说明：以上许可证与维护活跃度均未逐一核验，按「只参考交互、不引入依赖」处理；若要引入需再做一轮核验。

## 七、分阶段任务与验收

| 阶段 | 内容 | 预估 | 验收 |
|---|---|---|---|
| P0 | pinnedAt 数据 + 置顶切换 + 继续上次动态更新 | 1 天 | 切换/重启/新建会话三条路径手动验证 |
| P1 | 灵感生成管线（提示词、解析、缓存、降级、日上限） | 1 天 | 有 Key / 无 Key / 断网 / 超时 四种情况 |
| P2 | 骨架屏、失败重试、空态与动效 | 0.5 天 | 三态截图 |
| P3 | 平板双栏适配 | 0.5 天 | 需平板设备可连 |

## 八、风险与待决策

1. 置顶数量是否需要上限（例如 3 个）？
2. 灵感标签会读取最近会话摘要，是否接受把这段内容发给模型（隐私）？
3. 无 Key / 断网时降级文案是否沿用当前 4 条演示文案？
4. 生成标签会产生额外 API 调用（每次约 100–200 tokens），是否接受？
5. 平板双栏是否与 P0–P2 同批交付？

## 九、实施状态（2026-11-21 更新）

- P0–P3 代码已全部落地，编译通过并打进 0.7.0 release 包。
- 决策已按用户口径执行：置顶上限 3；灵感只读取现有会话摘要、允许发给模型；降级改用真实会话标题；平板双栏与 P0–P2 同批。
- 平板已验证（2026-11-21，无线 adb）：双栏生效、置顶可切换、灵感标签走真实会话标题降级且暂不接行为。
- 手机仍无法连接，紧凑单栏布局本轮未回归。
- 0.7.2 已按决策改为 material3-adaptive 1.4.0-alpha03 的 ListDetailPaneScaffold（directive/value 签名）；
  实测 API 与知识库片段有差异：AnimatedPane 在 1.4.0-alpha03 已变成 Modifier（AnimatedPaneScope），导航用 navigateTo(role, key) 而非 navigateToDetail，已记录。
- 灵感标签已带 conversationId 映射，点击跳转到对应会话（平板上进入右栏）。

## 十、当前阻塞

本轮设备 adb 显示 no serial number offline，手机与平板都无法连接；0.6.7（设置页模型图标 + 浅色光标改紫）已编译并产出 APK，但尚未在真机截图验证，平板测试也无法开始。
# Aura UI 全流程优化方案（布局 + 配色）

> 依据：`D:\AI项目\Android-MD3-Knowledge`（设计令牌、色彩系统、间距、形状、动效、无障碍、布局断点、组件规格、Review Checklist）
> 适用范围：Aura Android App（Home / Chat / Settings / DSH 三个页面 / 通用组件）
> 日期：2026-11-22 ｜ 状态：**方案待确认，未改动任何代码**

---

## 0. 目标与边界

### 目标
1. 让界面从「手工拼装的编辑风」升级为**有系统、可维护、可验证**的 Material 3 Expressive 设计。
2. 解决当前硬编码、角色混乱、对比度不足、断点/平板不适配等结构性问题。
3. 保留 Aura 现有品牌识别：薰衣草纸面、长紫实色块、柠檬强调、衬线编辑感、无描边。

### 不在本方案内
- 新功能（会话搜索、设置项扩展、插件配置页等）。
- 图标重绘、字体替换（只讨论字体使用策略）。
- 动画重写为全新交互（只统一到运动令牌）。

### 验收总标准
- 通过知识库 `07-Development-Rules/ui-review-checklist.md` 全部阻断项。
- 浅色/深色、200% 字体、平板（≥840dp）、IME 四种场景截图对照通过。
- 代码中除 `ui/theme/Color.kt` 外**零裸色值**；除 `ui/theme/Shape.kt` 外**零裸圆角**；间距使用令牌。

---

## 1. 现状审计（代码事实）

### 1.1 主题层

| 文件 | 现状 | 问题 |
|---|---|---|
| `ui/theme/Color.kt` | 28 个品牌色（Lavender/Violet/Brand/Lemon/Ink/Paper…） | 是 primitive，但又混入语义名（Paper、Muted） |
| `ui/theme/Theme.kt` | `AuraAccent` 组件色 + 覆盖 `ColorScheme`（约 60 个 hex） | 语义层与组件层混在一起；部分角色值未成体系 |
| `ui/theme/Shape.kt` | 5 档（8/12/20/28/36） | 知识库要求 **10 档**，缺 4/16/20/32/48 与 Full 的明确映射 |
| `ui/theme/Type.kt` | 自定义 15 档，标题用 Playfair/Noto Serif CJK | 展示字与正文混用同一套 family 策略；行高/字重未按 CJK 校准 |

### 1.2 硬编码统计（`ui/` 目录）

| 项 | 数量 | 说明 |
|---|---|---|
| `Color(0x…)` | 89 处 | 多数在 Color.kt，但 UI 层仍有裸色 |
| `RoundedCornerShape(...)` | 55 处 | 出现 2/8/10/12/14/16/18/20/22/24/28/30/32/36/44/50 等十余种值 |
| `fontSize = …` | 18 处 | 绕过 Typography 档位 |
| 间距 | 无令牌 | 直接写 `padding(18.dp)`、`spacedBy(14.dp)` 等非 8 倍数 |

### 1.3 已暴露的视觉/体验问题（本项目实测）

1. **对比度不达标**（按 WCAG 计算）：
   - `Muted #827B91` on `LavenderPage #EDE5FF` = **3.32:1**（正文需 4.5）；
   - `Muted` on `Paper #FFFCF8` = **3.96:1**；
   - 状态绿 `#2FBF71` on Paper = **2.33:1**（连图形 3:1 都不到）。
2. **状态色散落**：成功绿、离线灰、错误红直接写在页面里，无 token。
3. **圆角失控**：同类卡片出现 18/20/22/24/28/30 多种圆角；AI 气泡 28/8 非对称未进形状系统。
4. **两个 FAB**：Home 同时有「DSH」「新的对话」两个同款 FAB，违反「一屏一个首要动作」。
5. **平板适配**：靠 `screenWidthDp >= 840 || containerSize >= 840` 硬判断，未使用断点令牌；两栏使用占位式实现。
6. **触控目标**：部分 26–32dp 图标/按钮（删除按钮 54×74 内图标 26dp）未验证 48dp 最小热区。
7. **文本缩放**：未做 200% 字体回归；多处固定高度容器（输入框、Dock、卡片高度）有截断风险。
8. **动态色**：无策略（当前固定品牌色，属于合理选择，但需写死规则与 fallback）。

---

## 2. 设计原则（本项目专用）

1. **品牌进角色，不进组件**：品牌紫/柠檬先映射到 MD3 26 个颜色角色；组件只取角色或 component token，不直接取 primitive。
2. **纯色块、零描边**：用 surface 层级 + 实色 container 表达层次，不用 1dp outline；`outline` 仅保留给输入框/焦点环（如必须）。
3. **衬线只做展示**：`display/headline` 用衬线（Playfair + Noto Serif CJK），`title/body/label` 用无衬线；拉丁与 CJK 分别指定 family。
4. **状态必须显式**：颜色 + 文本/图标双通道，禁止只靠颜色表达运行/离线/错误。
5. **一屏一主动作**：FAB 唯一；次要动作进顶栏或菜单。
6. **8dp 基线**：间距用 token（space0…space1200），4dp 只做紧凑微调。
7. **动效物理化**：组件内用 spring（spatial/effects × fast/default/slow），页面转场用 easing+duration 过渡方案。

---

## 3. 颜色系统方案

### 3.1 三层令牌

```
primitive（固定色值）
  → semantic（MD3 26 角色，Light/Dark 两套）
    → component（AuraAccent：card/chip/dock/bubble/reasoning/tool/status…）
```

**规则**：Composable 只允许访问 `MaterialTheme.colorScheme` 与 `LocalAuraAccent`；二者都在 theme 包内定义；其它文件出现 `Color(0x` 直接评审阻断。

### 3.2 角色映射（Light）

| MD3 角色 | 取值 | 用途 |
|---|---|---|
| primary | `#5B5BF2` Brand | 主按钮、选中态、链接 |
| onPrimary | `#FFFCF8` Paper | primary 上的文字（4.86:1 ✅） |
| primaryContainer | `#E9DEFF` Lavender100 | 次级按钮、选中 chip |
| onPrimaryContainer | `#2C2836` Ink800 | 容器文字 |
| secondary | `#7256D9` Violet500 | 次强调图标 |
| secondaryContainer | `#D4BFFF` Lavender200 | 低强调填充 |
| tertiary | `#5B5BF2`（复用） | 无独立品牌第三色，避免新色 |
| tertiaryContainer | `#E9FF7A` Lemon | 高亮块（柠檬只做强调，不做大面积底） |
| onTertiaryContainer | `#15131A` Ink950 | 柠檬上的文字（16.71:1 ✅） |
| surface | `#FFFCF8` Paper | 页面底 |
| onSurface | `#15131A` Ink950 | 主文字（15.13:1 on LavenderPage ✅） |
| surfaceVariant | `#EDE5FF` LavenderPage | 分区底 |
| onSurfaceVariant | `#4D4658` **新增更深 muted** | 次要文字（需 ≥4.5:1） |
| outline | `#C9BCE8` | 仅输入框/焦点 |
| error | `#B3261E` | 错误 |
| onError | `#FFFFFF` | |
| errorContainer | `#FFDAD6` | |
| onErrorContainer | `#410002` | |
| inverseSurface / inverseOnSurface | 深底/浅字 | Snackbar |
| surfaceContainerLowest→Highest | 5 档纸面 | 卡片/弹层/输入框分层 |

**关键修正**：现有 `Muted #827B91` 在薰衣草底只有 3.32:1，正文不可用。方案新增 `onSurfaceVariant = #4D4658`（在 LavenderPage 上 ≥4.5），`Muted` 降级为**装饰/占位**专用（仍需 ≥3:1 图形线）。

### 3.3 角色映射（Dark）

| 角色 | 取值 |
|---|---|
| primary | `#B9B7FF`（提亮保证 onPrimary 对比） |
| onPrimary | `#1B1650` |
| primaryContainer | `#3A3470` |
| onPrimaryContainer | `#E9E5FF` |
| surface | `#0D0B14` Ink |
| onSurface | `#F2EFFF`（15.54:1 on card ✅） |
| surfaceVariant | `#1A1726` |
| onSurfaceVariant | `#C9C4E4`（≥7:1 ✅） |
| tertiaryContainer | `#E9FF7A` Lemon（深色下仍用亮柠檬 + 墨字） |
| onTertiaryContainer | `#15131A` |

深色不是浅色反相：卡片用 `surfaceContainer` 逐级提亮（#1A1726 → #241E36 → #2C2836），文字保持 15:1 级。

### 3.4 状态色 token（新增）

| token | Light | Dark | 用途 |
|---|---|---|---|
| statusRunning | `#1E7F4F` | `#5FDD9B` | live/运行中（文本+点） |
| statusIdle | `#8A6D00` | `#E9FF7A` | 空闲/等待 |
| statusOffline | `#6B6475` | `#B4ACD0` | 离线 |
| statusError | `#B3261E` | `#FFB4AB` | 错误 |
| statusRunningContainer | `#D7F5E4` | `#123B27` | 状态 pill 底 |

所有状态点必须配文字（现有 DSH 卡片已有 label，保留并统一）。

### 3.5 动态色策略

- **默认关闭**：品牌识别优先，使用固定 Aura scheme。
- 设置页保留未来开关位；开启时按知识库 fallback 三条规则执行（支持+开启→dynamic；支持+关闭→自定义；不支持→自定义）。
- 不在本次实现开关，只写死策略与注释。

---

## 4. 排版方案

### 4.1 字族分工

| 层级 | 字族 | 说明 |
|---|---|---|
| display / headline | Playfair Display（拉丁）+ Noto Serif CJK SC（中文） | 保留现有 `editorialFamily()` 选择器 |
| title / body / label | 系统无衬线（Roboto / 思源黑体） | 统一 UI 可读性 |
| code | Monospace | Markdown 代码块 |

### 4.2 档位调整

- 只覆盖需要改的槽位（当前 15 档全改，维护成本高）：
  - 覆盖 displayLarge/Medium/Small、headlineLarge/Medium/Small、titleLarge、bodyLarge/Medium、labelMedium/Small；
  - 其余用 MD3 默认。
- CJK 标题：`lineHeight` 比拉丁高 6–8%，字重只使用 400/600/700（避免 500 合成加粗）。
- body 最小 14sp；正文行高 ≥1.45×；label 不低于 11sp。
- 200% 字体下：`heightIn(min=…)` 替代固定高度；Dock/输入框/卡片全部改自适应。

### 4.3 混合语言

- 现有按字符判断 CJK 的 `editorialFamily()` 保留；
- 新增规则：**同一 Text 内中英混排时以 CJK 为准**（避免拉丁衬线在中文句子里跳脱）；
- 纯英文标题（Aura 字标）继续 Playfair。

---

## 5. 形状系统方案

### 5.1 十档映射（对齐知识库）

| MD3 档位 | dp | Aura 用途 |
|---|---|---|
| None | 0 | 全宽分区/列表行 |
| Extra small | 4 | 标签微调、代码行内 |
| Small | 8 | 小 chip、状态 pill |
| Medium | 12 | 输入框内块、代码块 |
| Large | 16 | 次级卡片、工具卡 |
| Large increased | 20 | 输入框（填充式）、小组件 |
| Extra large | 28 | 主卡片、对话气泡、Settings 卡 |
| Extra large increased | 32 | 底部弹层、Dialog 顶部 |
| Extra extra large | 48 | 大强调块（首页 Featured 可选） |
| Full | 50% | FAB、Dock、Chip、头像 |

### 5.2 组件映射表

| 组件 | 形状 | 变更 |
|---|---|---|
| 主卡片/会话卡 | Extra large 28 | 统一现有 24/26/28 |
| Settings 卡 | 28 | 统一 |
| AI 气泡 | 28/28/28/8（非对称） | 保留，但改为 token `bubbleShape` |
| 用户气泡 | 28/28/8/28 | 新增对称规则 |
| 输入框 | 20（填充式） | 现状 20/22 混用，统一 20 |
| 工具/推理卡 | Large 16 | 现 14/16 混用 |
| FAB/Dock | Full（胶囊） | 保留 |
| Dialog | 32（顶角） | 现 28/30 混用 |
| 代码块 | 12 | 现 12 为主，统一 |
| Markdown 图片 | 12 | 统一 |

**Grep 门禁**：`RoundedCornerShape(` 仅允许出现在 `Shape.kt` 与 `Theme.kt` 的 token 定义；页面里用 `MaterialTheme.shapes.*` 或 `AuraShapes.*`。

---

## 6. 间距方案

### 6.1 Token 表（取常用 12 档）

`space25=2, space50=4, space75=6, space100=8, space150=12, space200=16, space300=24, space400=32, space500=40, space600=48, space800=64, space1200=96`

### 6.2 规则

| 场景 | 值 |
|---|---|
| 页面边距 compact | 16（现状 20 改为 16） |
| 页面边距 ≥600dp | 24 |
| 卡片内边距 | 16（紧凑）/ 20（主卡） |
| 卡片间距 | 12 |
| 分组间距 | 24 |
| 列表行最小高 | 56（单行）/ 72（双行）/ 88（三行，DSH 卡） |
| 相邻触控目标间隔 | ≥8 |
| FAB 距边 | 16 / 底部 16 + insets |
| 图标与文字 | 8 / 12 |

**改动点**：现有 `padding(18.dp)`、`spacedBy(14.dp)`、`padding(horizontal = 20.dp)` 全部替换为令牌。

---

## 7. 布局与自适应

### 7.1 断点

| 宽度 | 布局 |
|---|---|
| <600 | 单栏；页面边距 16；FAB 右下 |
| 600–839 | 单栏 + 可选双栏；边距 24 |
| 840–1199 | 双栏：列表 360dp 固定 + 详情自适应；gutter 24 |
| ≥1200 | 双栏 + 最大内容宽 1200 居中 |

用 `BoxWithConstraints`/`LocalWindowInfo` 统一封装 `AuraWindowSize`，删除散落的 `screenWidthDp >= 840` 判断。

### 7.2 各页面布局

**Home**
- 现状：Hero + 连接状态 + Featured + 最近列表 + 两个同款 FAB（违规）。
- 方案：保留 Hero/Featured；**FAB 只保留「新的对话」**；「DSH」移入顶栏图标按钮（或做成 FAB 菜单，长按展开）。
- 最近列表：改 MD3 list 行高 72/88，删除按钮独立 48dp 热区。
- 页面边距 16，卡片间距 12。

**Chat**
- 顶栏：`TopAppBar` 中号（Medium），标题 + 返回 + 溢出菜单。
- 消息区：列表间距 8/12；气泡最大宽度 78%；引用条简化。
- Composer：输入框高度自适应（`heightIn(min=56)`），底部 insets + IME 双重处理；附件/联网/推理等级按钮 48dp 热区。
- 推理面板：Popup 保留，内部改用 surfaceContainer + 20 圆角 + spring 展开。

**Settings**
- 采用 `ListItem` 结构（headline + supporting），卡片 28 圆角、16 内边距。
- 主题选择用 SegmentedButton 组，间距 8。
- 危险操作（清空）用 error 角色文字 + 二次确认 Dialog。

**DSH 会话列表**
- 卡片 28；行内部：状态点 + 状态文字 + 主机（label）+ 标题（titleMedium）+ 元信息（labelSmall）。
- 卡片必须有 `key` 与 `contentType`；加载/空/错误/内容四态齐全。
- 配对按钮：单一主按钮 + 次要说明，禁用态样式明确。

**DSH 详情**
- 顶部信息区收窄（标题 + sessionId），状态行 + 波浪条固定在列表上方。
- 消息只保留用户与最终输出（现状），过程用状态行；Markdown 统一间距。
- Composer 与 Chat 共用组件（抽 `AuraComposer`）。

**配对页**
- 表单区：IP/端口/配对码用同一 filled 输入框（20 圆角、56 高）；
- 发现列表：卡片 16 + 72 行高；
- 错误/空态：说明 + 重试按钮。

### 7.3 Edge-to-edge 与 IME

- 统一 `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`，删除各页面重复 `statusBarsPadding()/navigationBarsPadding()` 混用。
- 键盘：`imePadding` + `BringIntoViewRequester`（输入框聚焦时滚入可视区）。
- 三键导航/手势导航各验证一次。

---

## 8. 动效方案

| 场景 | 令牌 |
|---|---|
| 按钮按压/形状变化 | spring fast spatial |
| 卡片进入/列表增删 | spring default spatial |
| 页面转场 | fade + slide（easing 280/160，过渡期保留） |
| 底部弹层 | spring slow spatial |
| 推理面板 Popup | spring default spatial + 背景淡化 effects |
| 波浪进度 | 线性循环（装饰，不参与 reduce motion） |
| Snackbar | spring fast effects |

- 统一 `MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())`（现状已用）。
- 尊重系统「移除动画」：`LocalMotionDurationScale` 为 0 时跳过非必要动效。

---

## 9. 无障碍

| 项 | 要求 | 现状风险 |
|---|---|---|
| 触控目标 | ≥48×48 | 图标按钮、删除按钮、卡片内 × 需核查 |
| 正文对比度 | ≥4.5:1 | Muted 3.32/3.96 不达标 → 换 onSurfaceVariant |
| 图形对比度 | ≥3:1 | 成功绿 2.33 → 换 statusRunning |
| 字体 200% | 不截断/不重叠 | 固定高度容器需改 heightIn |
| 语义标签 | 图标按钮必须有 contentDescription | 部分 Icon null |
| 状态表达 | 颜色+文字 | DSH 已做，其他状态需补 |
| TalkBack 顺序 | 视觉顺序 | 需逐屏验证 |

---

## 10. 全流程实施阶段

### P0 基座（1–2 天）
1. 重写 `Color.kt / Theme.kt`：primitive → semantic → component 三层；补 26 角色 + 状态色 token。
2. 重写 `Shape.kt`：10 档；新增组件形状 token（bubble/dialog/dock/code）。
3. 新增 `Spacing.kt`：18 档 token + 页面边距常量。
4. `Type.kt` 收敛为 10 个覆盖槽位，CJK 行高校准。
5. 增加静态门禁（脚本/grep）：UI 层裸 `Color(0x`、裸 `RoundedCornerShape(`、裸 `fontSize` 计数为 0。

**验收**：编译通过；全 App 颜色/形状视觉不变（纯重构）；截图对比无差异。

### P1 核心页面（2–3 天）
1. Home：单 FAB、列表行高、页面边距、Featured 卡层级。
2. Chat：顶栏、气泡宽度、Composer 自适应、按钮热区。
3. Settings：ListItem 化、主题 SegmentedButton、危险操作角色色。
4. 共用组件抽取：`AuraComposer`、`AuraCard`、`AuraListItem`、`StatusPill`。

**验收**：四态齐全；200% 字体无截断；浅/深色截图通过。

### P2 DSH 页面（2–3 天）
1. 会话列表：卡片规格、四态、key/contentType、删除/配对按钮热区。
2. 详情：信息区、状态行、Markdown 间距、图片预览。
3. 配对：表单统一、错误态。
4. 与 Chat 共用 Composer。

**验收**：返回键/IME/图片/发送回归通过（重点防 1.6.7 的返回崩溃类问题）。

### P3 打磨与自适应（1–2 天）
1. 断点封装 + 平板双栏（列表 360 + 详情）。
2. 动效令牌替换。
3. 无障碍全量核查（48dp/TalkBack/200%）。
4. 深浅色全屏对照 + 对比度脚本复算。

**验收**：Review Checklist 阻断项全过；平板/手机截图各一套。

---

## 11. 验证矩阵

| 维度 | 方法 | 通过标准 |
|---|---|---|
| 颜色 | 脚本复算关键配对对比度 | 正文 ≥4.5、图形 ≥3 |
| 主题 | 浅/深各截 6 页 | 无裸色、层级清晰 |
| 字体 | 系统字体 200% | 无截断/重叠/横向滚动 |
| 布局 | 手机竖屏 + 平板横屏 | 断点正确、双栏可用 |
| IME | 每个输入框 | 不遮挡、可滚动 |
| 无障碍 | TalkBack 遍历 + 布局边界 | 顺序正确、48dp |
| 回归 | 配对/连接/发送/上传/图片/返回 | 全通过 |
| 静态 | grep 门禁 | 裸值计数 0 |

---

## 12. 风险与待决策

### 风险
1. 品牌紫/柠檬与 MD3 角色不是一一对应，映射后要接受部分角色「复用」。
2. 衬线排版在 CJK 200% 下容易溢出，需要逐页调行高。
3. 两 FAB 改单 FAB 是交互变更，需要确认 DSH 入口是否放顶栏。
4. 大范围替换圆角/间距会产生大量视觉 diff，需要逐屏截图验收。
5. 平板双栏改动涉及导航状态，回归成本高。

### 待你决策
1. 品牌色处理：严格映射到 MD3 角色（推荐），还是保留独立 `AuraAccent` 双轨（现状）？
2. 动态色：确认默认关闭？
3. 衬线字体：保留（推荐）还是正文也换无衬线？
4. 两个 FAB：合并为一个（DSH 进顶栏）还是保留两个但改成 FAB 菜单？
5. 平板支持范围：本次做到 840dp 双栏，还是先只做手机？
6. 是否接受 P0 纯重构期间「视觉不变」的中间态？

---

## 13. 交付物

1. `ui/theme/Color.kt / Shape.kt / Spacing.kt / Type.kt / Theme.kt` 重写。
2. `ui/components/` 基础组件：AuraCard、AuraListItem、AuraComposer、StatusPill、AuraDialog。
3. 6 个页面布局重构。
4. `docs/ui-audit.md`（逐屏问题清单与截图对照）。
5. 对比度脚本 `tools/contrast-check.mjs`。
6. 静态门禁脚本 `tools/ui-lint.sh`。
7. 验收截图集：浅/深 × 手机/平板 × 200% 字体。

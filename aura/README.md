# Aura 工程说明（0.6.3）

Kotlin + Jetpack Compose 的 DeepSeek 对话客户端。视觉语言：薰衣草编辑风 + 纯色块高对比；
主题入口改为 MaterialExpressiveTheme（material3 1.5.0-rc01）。规范数值取自
Android-MD3-Knowledge（2026-11-16 实证线）。

## 一行任务（屏幕地图）

| 屏幕 | 一句话任务 | 主视觉焦点 |
|---|---|---|
| 首页 Home | 让用户 4 秒内决定「继续上次」还是「开新对话」 | 深紫实心编辑卡 + 巨大衬线字标 |
| 对话 Chat | 让用户读完一个回答并继续追问 | 助手回答卡（思考折叠 + 波浪进度 + 引用） |
| 设置 Settings | 让用户把 App 接到自己的 DeepSeek Key 上 | 「连接」卡 |
| 模型弹层 | 在两分钟内选对模型 | 深色实心弹层 + 柠檬黄选中行 |

## 工程结构

    aura/
    ├── settings.gradle.kts / build.gradle.kts / gradle.properties
    ├── gradle/libs.versions.toml
    ├── gradlew / gradlew.bat / gradle/wrapper   (Gradle 9.8.1)
    ├── screenshots/                              # 12 张真机截图
    ├── tools/subset_font.py                      # 从设备 NotoSerifCJK 生成子集
    └── app/src/main/
        ├── assets/licenses/                      # Playfair OFL + Noto 子集 NOTICE
        ├── res/font/playfair_400|600|700.ttf , noto_serif_sc.otf
        └── java/com/aura/chat/
            ├── MainActivity.kt / AuraApp.kt / AuraViewModel.kt
            ├── data/ Models.kt DemoChats.kt DemoBrain.kt ChatStore.kt DeepSeekClient.kt WebSearch.kt DocumentReader.kt
            └── ui/
                ├── theme/ Color.kt Type.kt Shape.kt Theme.kt
                ├── components/ Glyphs.kt Buttons.kt Art.kt Editorial.kt
                │             ConversationCards.kt ChatMessages.kt Composer.kt
                └── screens/ HomeScreen.kt ChatScreen.kt SettingsScreen.kt ModelSheet.kt

## 依赖与版本（不猜版本号）

| 依赖 | 版本 | 依据 |
|---|---|---|
| AGP / Gradle / Kotlin | 9.4.1 / 9.8.1 / 2.3.21 | KB 00-Metadata/version-tracking.md 2026-11-16 实证 |
| Compose BOM | compose-bom-alpha 2026.11.00 → material3 1.5.0-rc01 | 同上（官方 Maven POM） |
| compileSdk | 37 + minor 2 | Compose 1.13 要求 ≥ 37.1 |
| targetSdk / minSdk | 36 / 26 | 与 KB 示例一致 |
| core-ktx / activity-compose / lifecycle | 1.18.0 / 1.13.0 / 2.11.0 | KB 稳定版清单 |
| junit | 4.13.2 | AGP 9 需显式声明 |

不引入：navigation-compose、DataStore、OkHttp/Retrofit、material-icons-extended（图标 Canvas 自绘）。

## 主题与 Expressive

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        typography = AuraTypography,
        shapes = AuraShapes,
    )

- 置顶卡：featured / onFeatured / featuredGlow 三个令牌，深色主题固定为墨黑卡面（不跟随 primary 反色）。
- 颜色：语义色走 MaterialTheme.colorScheme（浅/深各一套）；编辑风强调色走 LocalAuraAccent：
  deep / onDeep / card / chip / onChip / ink / inkCard / inkSoft / lemon / onLemon /
  userBubble / cyan / text / muted / onInk / onInkMuted。
- 组件不出现 BorderStroke：卡片、胶囊、输入框、对话气泡全部实心填充（本轮的「纯色块」口径）。
- 字号全部来自 Typography；中文标题由 editorialFamily(text) 选择字族：
  含 CJK → SerifCJK（Noto Serif CJK SC 子集），纯 Latin → Playfair Display。
- 动效：MotionScheme.expressive()（spatial damping 0.8 / stiffness 380，KB motion.md）。

Expressive 组件（全部来自 material3 1.5.0-rc01，KB expressive-components.md 已实证注解状态）：

| 组件 | 用法 | 备注 |
|---|---|---|
| HorizontalFloatingToolbar | 对话输入区 | expanded=true；FAB 槽位放发送钮 |
| ButtonGroup + ButtonGroupDefaults.connected*ButtonShapes() | 设置主题三档 | 与 ToggleButton 组合 |
| ToggleButton | 主题单选项 | 选中态用主题 primary 实心 |
| LinearWavyProgressIndicator | 流式回答卡 | 无实验注解 |
| MaterialExpressiveTheme / MotionScheme.expressive() | 全局 | 无实验注解 |
| ModalBottomSheet | 模型选择 | 深色实心，dragHandle 置空 |

已知限制（1.5.0-rc01 实测）：HorizontalFloatingToolbar 外层加 fillMaxWidth() 会触发内部
FloatingToolbarKt.HorizontalFloatingToolbarWithFabLayout 的约束冲突
（IllegalArgumentException: maxWidth must be >= minWidth），当前实现按内容宽居中，不要改回 fillMaxWidth。

## 中文衬线的生成方式（可复现）

1. 从设备取源字体：adb pull /system/fonts/NotoSerifCJK-Regular.ttc fonts/
2. 运行 python aura/tools/subset_font.py：选 TTC 中的 SC 字面，字符集 = ASCII + 常用标点 +
   GB2312 一二级全部汉字（行 0xB0–0xF7）+ 工程源码里出现的所有字符，共 7038 字。
3. 输出 app/src/main/res/font/noto_serif_sc.otf（4.5MB，CFF 轮廓）。
4. 许可：SIL OFL 1.1，NOTICE 随 assets 分发；子集外字符回退系统无衬线。

## 数据与状态

模型：ChatMessage / Conversation / AppSettings（同 0.1.0）。

持久化（SharedPreferences 名 aura_store）：conversations(JSON)、api_key、base_url、model、theme、
demo_mode、web_search。

状态编排（AuraViewModel）：send() 追加 user + 占位 assistant；有 Key 且非演示模式走
DeepSeekClient.stream(SSE)，否则 DemoBrain 逐块模拟（先 reasoning 再正文）；结束统一写耗时与
reasoningSeconds 并落盘；异常映射 401/402/429/5xx 为中文提示。

DeepSeek 协议：POST {baseUrl}/chat/completions，Bearer Key，stream=true；
解析 data: {...} 的 choices[0].delta.content / reasoning_content；data: [DONE] 结束。

## 联网检索与附件（0.3.0）

- WebSearch.kt：Tavily /search，读 results[].title/url/content，来源取 URL host。Key 存 SharedPreferences
  （search_key），设置页「检索 Key（Tavily）」。
- 发送流程：联网开 + 有检索 Key + 非演示模式 → 先检索 3 条 → 引用写进 assistant 消息 →
  把摘要拼进 user 内容一起发给模型；检索失败不阻断对话，只少了引用。
- 附件：ActivityResultContracts.OpenDocument 选 text/* json xml → 本机读 UTF-8 → 20k 字截断 →
  随下一条消息作为上下文发送；Composer 顶部出现附件胶囊，可移除。
- 已知约束：DeepSeek chat 模型无视觉输入，图片附件不支持；联网检索不是 DeepSeek 官方能力。

## 0.4.0 视觉与技术变更

- 调色：Brand #5B5BF2 / BrandDeep #4646D6 / Lemon #E9FF7A / 页面 #EDE5FF；深色主题主色仍为薰衣草。
- 置顶卡：featured = Brand，标题柠檬色衬线，右上角 2dp 柠檬弧线（Canvas drawArc）。
- 主题组：Expressive ToggleButton + ButtonGroupDefaults.connected*ButtonShapes()，
  colors 用 ToggleButtonDefaults.colors(containerColor = chip, contentColor = onChip,
  checkedContainerColor = lemon, checkedContentColor = onLemon)；选中态由组件自身形变，不再自绘。
- 主题动画：animateScheme() 对 15 个可见角色做 spring(1f, 380f) 插值。
- 设置页底部 120dp 渐隐。
- 检索：WebSearchClient 先试 Tavily（有 Key），否则 DuckDuckGo html 端点（解析 result__a / result__snippet）。
- 文档：DocumentReader 支持纯文本、.docx（Zip + XmlPullParser）、PDF（PdfRenderer 渲染前 5 页）、图片；
  OCR 用 ML Kit text-recognition 16.0.1（本地内置模型）。

## 0.6.3 调整

- Composer 胶囊内顺序：模型短标签（衬线）在左，随后 Spacer(weight 1f)，再是附件 / 语音 / 联网三个动作，贴右侧；发送钮仍在胶囊外右侧。
- 文档解析自检三条链路（图片 OCR / PDF / Word）已由用户手动确认全部通过。
- 版本 0.6.3（versionCode 11）。

## 0.6.2 调整

- ModelInfo 增加 shortLabel（Chat / Reasoner），Composer 用 shortLabel + serif 字族。
- Composer 输入框改为单一 BasicTextField + decorationBox：占位与输入共用 textStyle/基线/左边距。
- DocumentReader.readSamples() + assets/samples/{ocr-sample.png,sample.pdf,sample.docx}：
  自检依次跑 图片 OCR / PDF（PdfRenderer → OCR）/ Word（Zip + XML）。
- 版本 0.6.2（versionCode 10）。

## 0.6.1 调整

- Composer：Row { Surface(weight 1f, 胶囊) { 模型/附件/语音/联网 } + Spacer(10dp) + SendButton }，
  发送钮与胶囊分离、垂直居中、无阴影。
- ConnectedChoiceRow 新增 unselectedContainer / unselectedContent；设置页浅色传 16% 白 + onDeep。
- KB 复核：buttons.md 的 Toggle 未选中官方为 surfaceContainer/onSurfaceVariant，本轮为品牌偏离（16% 白）。

## 0.6.0 变更

- 图标：Glyphs.kt 全部改为 Material Symbols Rounded（Icons.Rounded.*），函数签名不变；
  依赖 androidx.compose.material:material-icons-extended（BOM 管理版本，Apache 2.0）。
- 输入区：去掉 HorizontalFloatingToolbar，改为无阴影扁平胶囊（Surface + Row），发送钮进同一行。
- 浅色配色：发送钮 Brand 紫底 + 柠檬箭头；联网开启态同理。
- 设置页：新增 SettingsPalette（card/onCard/eyebrow/field/onField/muted/danger），浅色整页 Brand 紫卡，
  深色沿用紫黑卡；字段为 16% 白、眉标柠檬。
- 版本 0.6.0（versionCode 8）。

## 0.5.0 变更

- ChatColors（ui/components/ChatColors.kt）：background / surface / surfaceAlt / text / muted /
  userBubble / onUserBubble / highlight / onSurfaceControl 九项，由 dark + AuraAccent + ColorScheme 派生。
- ChatScreen 接收 dark，CompositionLocalProvider 下发；浅色下 InkField 换成 LavenderField。
- 设置页顶栏 hero：Brand 紫卡 + 柠檬眉标 + 衬线标题，与首页置顶卡同源。
- 签名：release.keystore（4096 RSA，10950 天）+ keystore.properties（24 位随机口令，仅本机）。

## 0.4.1 修复要点

- proguard：-keep com.google.mlkit.** 与 mlkit_vision_text/common，解决选文件闪退（R8 剥离内部类 → NPE）。
- ModelSheet：hideThenDismiss() = sheetState.hide() → 完成后 onDismiss()；点遮罩也走同一路径。
- 深色色阶：brand #5B5BF2 的深色同族（#A9A6FF 主色、#262154 置顶卡、#1A1726 卡面、#0F0D17 背景）。
- 设置新增「文档解析自检（OCR）」，内置样张 assets/samples/ocr-sample.png。
- 提示条停留 5s。

## release 构建

    cd aura
    keytool -genkeypair ... -keystore release.keystore -alias aura   # 已有演示 keystore
    ./gradlew :app:assembleRelease

- signingConfigs.release 读取 ../release.keystore；口令明文在 app/build.gradle.kts（演示用，正式发布必须换）。
- R8 + shrinkResources 已开；release APK 5.5MB。签名与 debug 不同，覆盖安装前需 uninstall。
- proguard-rules.pro 只补了注解与 ViewModel 构造兜底。

## 验收记录

| 项 | 结果 |
|---|---|
| 单元测试 | 4/4 通过（DemoBrainTest） |
| 编译 | assembleDebug 通过（AGP 9.4.1 / Gradle 9.8.1 / JDK 21） |
| 真机 | Redmi 23013RK75C（API 37）安装运行；12 张截图 |
| 对话 | 打开会话、思考展开、流式回答（含波浪进度）、停止/重发/复制、模型切换、持久化 |
| 网络错误 | 无效 Key → 真机 401 → 中文错误写进气泡并弹提示 |
| 主题 | 浅色/深色/跟随系统；深色截图见 08 |
| 宽屏 | 横屏限宽 560/600dp 居中，不崩（09） |
| 字号 | 系统 1.3× 下首页不截断（10）；2.0× 未验证 |
| 置顶卡 | 深色主题固定墨黑（08），浅色仍是深紫（01） |
| 主题组 | 三段等分满宽（07） |
| 破坏性操作 | 清空对话二次确认（12） |
| 附件 | 文件选择器可打开（13）；文本读取后显示胶囊（未真机选中样本文件） |
| 联网检索 | 设置页可填 Tavily Key（06/14）；真实检索需用户 Key，未联调 |
| release | 签名 + R8 包真机安装运行（15/16） |
| 主题组形变 | 选中项按 checked shape 变形（07） |
| 主题动画 | 颜色插值代码已接入，未逐帧录屏验证 |
| 文档解析 | 图片 OCR / PDF / Word 三条链路自检全部通过（用户手动确认） |
| 选文件闪退 | 已修：R8 剥离 ML Kit 内部类导致 NPE，加 keep 规则后 release 复测不再崩 |
| 深色主题 | 与 Brand 同源的新深色阶（背景 #0F0D17 / 卡面 #1A1726 / 置顶 #262154） |
| 模型弹层 | 选中项先播 hide() 收起动画再 dismiss |
| 包体 | release 48.5MB（ML Kit 模型占大头） |
| 无障碍 | 图标按钮 contentDescription、触控 ≥ 48dp、装饰图形不朗读 |

## 边界

1. 中文衬线是 GB2312 子集（7038 字），GB2312 之外的生僻字仍回退无衬线。
2. 联网检索 / 附件 / 语音为 UI 占位；引用来自演示数据。
3. 富文本仅加粗与换行。
4. 未做平板/折叠屏双栏（本机无平板可验证）；横屏仅限宽居中。
5. Debug 构建未开 R8；release 需补签名。
6. 会话持久化为整表 JSON 覆写，规模大时应换 Room。
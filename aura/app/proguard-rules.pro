# Compose 与协程均由官方 consumer rules 覆盖
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.**
-keep class com.aura.chat.AuraViewModel { <init>(...); }

# ML Kit 本地文字识别：R8 混淆后曾在选图时 NPE，这里显式保留
-dontwarn com.google.android.gms.**
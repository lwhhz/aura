#!/usr/bin/env bash
# UI 静态门禁：统计页面层裸色值/裸圆角/裸字号（theme 除外）
set -euo pipefail
cd "$(dirname "$0")/.."
SRC="aura/app/src/main/java/com/aura/chat"
echo "== 裸色值（theme 之外） =="
grep -rn "Color(0x" "$SRC" --include='*.kt' | grep -v "/ui/theme/" | wc -l
echo "== 裸圆角（theme 之外） =="
grep -rn "RoundedCornerShape(" "$SRC" --include='*.kt' | grep -v "/ui/theme/" | wc -l
echo "== 裸字号 =="
grep -rn "fontSize\s*=" "$SRC" --include='*.kt' | grep -v "/ui/theme/" | wc -l
echo "== 非 8 倍数间距（抽样） =="
grep -rn "\(padding\|spacedBy\)([0-9]*\.dp" "$SRC" --include='*.kt' | grep -Ev "([048]|12|16|20|24|32|40|48|64)\.dp" | head -20 || true

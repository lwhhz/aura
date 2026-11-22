#!/usr/bin/env bash
cd "$(dirname "$0")/.."
SRC="aura/app/src/main/java/com/aura/chat"
echo "== raw colors (outside theme) =="
grep -rn "Color(0x" "$SRC" --include='*.kt' 2>/dev/null | grep -v "/ui/theme/" | wc -l
echo "== raw corner radius =="
grep -rn "RoundedCornerShape(" "$SRC" --include='*.kt' 2>/dev/null | grep -v "/ui/theme/" | wc -l
echo "== raw font sizes =="
grep -rn "fontSize\s*=" "$SRC" --include='*.kt' 2>/dev/null | grep -v "/ui/theme/" | grep -v "ui-lint: allow" | wc -l
echo "== non-8sp spacing (sample) =="
grep -rn "padding([0-9]*\.dp" "$SRC" --include='*.kt' 2>/dev/null | grep -Ev "([048]|12|16|20|24|32|40|48|64)\.dp" | head -10 || true

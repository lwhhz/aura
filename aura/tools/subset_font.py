import os, sys
from fontTools.ttLib import TTCollection

TTC = r"D:/AI项目/第三方dsapp/fonts/NotoSerifCJK-Regular.ttc"
ttc = TTCollection(TTC)
faces = []
for i, f in enumerate(ttc.fonts):
    try:
        fam = f["name"].getDebugName(1)
        full = f["name"].getDebugName(4)
    except Exception:
        fam, full = "?", "?"
    faces.append((i, fam, full))
print("faces:", faces)

# 选择简体（SC）那一份
sc_index = None
for i, fam, full in faces:
    if fam and ("SC" in fam or "Simplified" in full):
        sc_index = i
        break
if sc_index is None:
    sc_index = 0
print("chosen:", sc_index, faces[sc_index][1])

# 字符集：ASCII + 常用标点 + GB2312 一二级全部汉字（6763）+ 项目源码里的所有字符
chars = set(chr(c) for c in range(0x20, 0x7F))
chars.update("　、。〃々〈〉《》「」『』【】〜〝〞·—–…‘’“”※◆○●◇〆￥")
chars.update(chr(c) for c in range(0x3000, 0x3040))
chars.update(chr(c) for c in range(0xFF01, 0xFF61))
for hi in range(0xB0, 0xF8):
    for lo in range(0xA1, 0xFF):
        try:
            ch = bytes([hi, lo]).decode("gb2312")
        except Exception:
            continue
        if "\u4e00" <= ch <= "\u9fff":
            chars.add(ch)

root = r"D:/AI项目/第三方dsapp/aura/app/src"
for dirpath, _, filenames in os.walk(root):
    for fn in filenames:
        if fn.endswith((".kt", ".xml", ".md")):
            try:
                with open(os.path.join(dirpath, fn), encoding="utf-8", errors="ignore") as fh:
                    chars.update(fh.read())
            except Exception:
                pass
for extra in [r"D:/AI项目/第三方dsapp/README.md", r"D:/AI项目/第三方dsapp/aura/README.md"]:
    if os.path.exists(extra):
        with open(extra, encoding="utf-8", errors="ignore") as fh:
            chars.update(fh.read())

chars = {c for c in chars if c.isprintable() or c == " "}
print("chars:", len(chars))

from fontTools import subset
font = ttc.fonts[sc_index]
opts = subset.Options()
opts.name_IDs = ["*"]
opts.name_languages = ["*"]
opts.name_legacy = True
opts.notdef_outline = True
opts.recalc_bounds = True
opts.drop_tables = ["BASE", "JSTF", "DSIG", "EBDT", "EBLC", "SVG "]
opts.layout_features = ["*"]
opts.hinting = True
sub = subset.Subsetter(options=opts)
sub.populate(unicodes={ord(c) for c in chars})
sub.subset(font)
out = r"D:/AI项目/第三方dsapp/aura/app/src/main/res/font/noto_serif_sc.otf"
os.makedirs(os.path.dirname(out), exist_ok=True)
font.save(out)
print("bytes:", os.path.getsize(out))
from fontTools.ttLib import TTFont
f = TTFont(r"D:/AI项目/第三方dsapp/aura/app/src/main/res/font/noto_serif_sc.otf")
cmap = f.getBestCmap()
tests = ["饕", "蓊", "鹣", "薰", "黎", "霓", "髓", "觊"]
print({t: (ord(t) in cmap) for t in tests})
print("glyphs:", len(cmap))

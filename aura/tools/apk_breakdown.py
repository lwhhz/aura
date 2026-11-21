import zipfile, collections
p = r"D:/AI项目/第三方dsapp/dist/aura-1.0.0-release.apk"
z = zipfile.ZipFile(p)
tot = 0
groups = collections.Counter()
for i in z.infolist():
    tot += i.file_size
    top = i.filename.split("/")[0]
    groups[top] += i.file_size
print("total_uncompressed", tot)
for k, v in groups.most_common(10):
    print(k, v)
libs = [n for n in z.namelist() if n.startswith("lib/")]
print("libs", len(libs))
assets = sorted([(i.file_size, i.filename) for i in z.infolist() if i.filename.startswith("assets/")], reverse=True)[:5]
print("assets_top", assets)
raw = sorted([(i.file_size, i.filename) for i in z.infolist() if i.filename.startswith("res/")], reverse=True)[:5]
print("res_top", raw)

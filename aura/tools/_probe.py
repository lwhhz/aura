import zipfile, os
aar = os.path.expanduser("~/.gradle/caches/modules-2/files-2.1/androidx.compose.material3/material3-android/1.5.0-rc01/8bc885c11f45d76f7faed3b4f8ea26d3901aafbd/material3.aar")
out = r"D:/AI项目/第三方dsapp/aura/tools/_m3classes.jar"
os.makedirs(os.path.dirname(out), exist_ok=True)
with zipfile.ZipFile(aar) as z, z.open("classes.jar") as s, open(out, "wb") as d:
    d.write(s.read())
print("ok")

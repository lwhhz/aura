import secrets, string, os
alphabet = string.ascii_letters + string.digits
pwd = "".join(secrets.choice(alphabet) for _ in range(24))
root = r"D:/AI项目/第三方dsapp/aura"
with open(os.path.join(root, "keystore.properties"), "w", encoding="utf-8") as f:
    f.write("storeFile=release.keystore\n")
    f.write("storePassword=" + pwd + "\n")
    f.write("keyAlias=aura\n")
    f.write("keyPassword=" + pwd + "\n")
print("password length", len(pwd))

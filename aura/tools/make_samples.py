from PIL import Image, ImageDraw, ImageFont
import os, zipfile
root = r"D:/AI项目/第三方dsapp/aura/app/src/main/assets/samples"
os.makedirs(root, exist_ok=True)
font = None
for p in [r"C:/Windows/Fonts/msyh.ttc", r"C:/Windows/Fonts/simsun.ttc", r"C:/Windows/Fonts/simhei.ttf"]:
    try:
        font = ImageFont.truetype(p, 40); break
    except Exception:
        pass
img = Image.new("RGB", (900, 500), "white")
d = ImageDraw.Draw(img)
d.text((50, 60), "Aura PDF 样张", fill="black", font=font)
d.text((50, 160), "Echo Bridge 10:30 pm", fill="black", font=font)
d.text((50, 260), "二十把椅子，一盏灯", fill="black", font=font)
img.save(os.path.join(root, "sample.pdf"), "PDF", resolution=150.0)

doc_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>
<w:p><w:r><w:t>Aura Word 样张</w:t></w:r></w:p>
<w:p><w:r><w:t>Echo Bridge 10:30 pm</w:t></w:r></w:p>
<w:p><w:r><w:t>二十把椅子，一盏灯</w:t></w:r></w:p>
</w:body></w:document>"""
with zipfile.ZipFile(os.path.join(root, "sample.docx"), "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""")
    z.writestr("word/document.xml", doc_xml)
print("samples ready", os.listdir(root))

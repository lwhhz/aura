from PIL import Image, ImageDraw, ImageFont
img = Image.new("RGB", (900, 420), (255, 255, 255))
d = ImageDraw.Draw(img)
font = None
for path in [r"C:/Windows/Fonts/msyh.ttc", r"C:/Windows/Fonts/simsun.ttc", r"C:/Windows/Fonts/simhei.ttf"]:
    try:
        font = ImageFont.truetype(path, 44); break
    except Exception:
        pass
d.text((40, 60), "Aura OCR 测试", fill=(20, 20, 20), font=font)
d.text((40, 150), "Echo Bridge 10:30 pm", fill=(20, 20, 20), font=font)
d.text((40, 240), "二十把椅子，一盏灯", fill=(20, 20, 20), font=font)
img.save(r"D:/AI项目/第三方dsapp/aura/screenshots/ocr-test.png")
print("saved")

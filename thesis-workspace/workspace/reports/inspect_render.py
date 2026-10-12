from pathlib import Path
from pdf2image import convert_from_path
from pypdf import PdfReader
from PIL import Image,ImageOps,ImageDraw
import json
root=Path(r'G:\project\CareerNova\thesis-workspace\workspace\reports\render')
pdf=root/'proposal.pdf'
reader=PdfReader(str(pdf))
pages=convert_from_path(str(pdf),dpi=110)
for i,p in enumerate(pages,1): p.save(root/f'page-{i}.png')
thumbs=[]
for i,p in enumerate(pages,1):
    p.thumbnail((350,500))
    tile=Image.new('RGB',(370,535),'#dddddd')
    tile.paste(p,((370-p.width)//2,25))
    ImageDraw.Draw(tile).text((15,8),f'Page {i}',fill='black')
    thumbs.append(tile)
for start in range(0,len(thumbs),6):
    subset=thumbs[start:start+6]
    canvas=Image.new('RGB',(1110,535*((len(subset)+2)//3)),'white')
    for j,t in enumerate(subset):canvas.paste(t,((j%3)*370,(j//3)*535))
    canvas.save(root/f'contact-{start//6+1}.png')
info=[]
for i,p in enumerate(reader.pages,1):
    text=p.extract_text()
    info.append({'page':i,'characters':len(text),'start':text[:110],'end':text[-100:]})
(root/'page-summary.json').write_text(json.dumps(info,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(info,ensure_ascii=False))

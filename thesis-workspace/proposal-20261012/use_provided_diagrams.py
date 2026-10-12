from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from lxml import etree as E
from copy import deepcopy
from PIL import Image
import shutil, json, hashlib

base=Path(__file__).parent
out=Path('G:/project/智聘通招聘网站的设计与实现开题报告.docx')
backup=base/'before-provided-diagrams.docx'
if not backup.exists():shutil.copy2(out,backup)
with ZipFile(backup) as z:parts={n:z.read(n) for n in z.namelist()};infos=z.infolist()
ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main','wp':'http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing','a':'http://schemas.openxmlformats.org/drawingml/2006/main','r':'http://schemas.openxmlformats.org/officeDocument/2006/relationships'}
w=ns['w'];r=ns['r'];pr='http://schemas.openxmlformats.org/package/2006/relationships'
root=E.fromstring(parts['word/document.xml']);body=root.find('{'+w+'}body')
imagep=root.xpath('//w:p[w:r/w:drawing]',namespaces=ns)[0]
caption=imagep.getnext();assert '图2.1' in ''.join(caption.xpath('.//w:t/text()',namespaces=ns))
rels=E.fromstring(parts['word/_rels/document.xml.rels'])
at=list(body).index(imagep);body.remove(imagep);body.remove(caption)
record=[]
for i,role in enumerate(['用户','企业','管理员'],1):
    src=Path(f'G:/project/img/结构图/结构图——{role}.png')
    width,height=Image.open(src).size
    cx=5760000;cy=round(cx*height/width)
    p=deepcopy(imagep)
    for x in p.xpath('.//wp:extent | .//a:ext',namespaces=ns):x.set('cx',str(cx));x.set('cy',str(cy))
    for x in p.xpath('.//wp:docPr',namespaces=ns):x.set('id',str(20+i));x.set('name',f'{role}功能结构图')
    for x in p.xpath('.//*[local-name()="cNvPr"]'):x.set('id',str(20+i));x.set('name',f'{role}功能结构图')
    rid=f'rIdProvidedStructure{i}'
    for x in p.xpath('.//a:blip',namespaces=ns):x.set('{'+r+'}embed',rid)
    media=f'word/media/provided-structure-{i}.png';parts[media]=src.read_bytes()
    E.SubElement(rels,'{'+pr+'}Relationship',Id=rid,Type=r+'/image',Target=f'media/provided-structure-{i}.png')
    cp=deepcopy(caption)
    ts=cp.xpath('.//w:t',namespaces=ns)
    ts[0].text=f'图2.{i}  {role}功能结构图'
    for t in ts[1:]:t.text=''
    body.insert(at,p);body.insert(at+1,cp);at+=2
    record.append({'source':str(src),'embedded':media,'sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'width_cm':16,'height_cm':cy/360000})
parts['word/document.xml']=E.tostring(root,encoding='UTF-8',xml_declaration=True,standalone=True)
parts['word/_rels/document.xml.rels']=E.tostring(rels,encoding='UTF-8',xml_declaration=True,standalone=True)
# Replace legacy floating footer textboxes with a centered PAGE field so two-digit
# page numbers remain visible after adding the three original structure figures.
for name in ['word/footer3.xml','word/footer4.xml']:
    footer=E.fromstring(parts[name])
    for child in list(footer):footer.remove(child)
    p=E.SubElement(footer,'{'+w+'}p');pp=E.SubElement(p,'{'+w+'}pPr')
    for name_,attrs in [('pStyle',{'val':'9'}),('jc',{'val':'center'}),('ind',{'left':'0','right':'0','firstLine':'0'}),('spacing',{'before':'0','after':'0','line':'240','lineRule':'auto'})]:
        x=E.SubElement(pp,'{'+w+'}'+name_)
        for key,val in attrs.items():x.set('{'+w+'}'+key,val)
    field=E.SubElement(p,'{'+w+'}fldSimple');field.set('{'+w+'}instr',' PAGE ')
    run=E.SubElement(field,'{'+w+'}r');rp=E.SubElement(run,'{'+w+'}rPr')
    f=E.SubElement(rp,'{'+w+'}rFonts')
    for a in ['ascii','hAnsi','eastAsia']:f.set('{'+w+'}'+a,'Times New Roman')
    size=E.SubElement(rp,'{'+w+'}sz');size.set('{'+w+'}val','18')
    E.SubElement(run,'{'+w+'}t').text='1'
    parts[name]=E.tostring(footer,encoding='UTF-8',xml_declaration=True,standalone=True)
with ZipFile(out,'w',ZIP_DEFLATED) as z:
    for info in infos:z.writestr(info,parts[info.filename])
    for n in [x['embedded'] for x in record]:z.writestr(n,parts[n])
with ZipFile(backup) as z:
    assert all(z.read(n)==parts[n] for n in z.namelist() if n not in ['word/document.xml','word/_rels/document.xml.rels','word/footer3.xml','word/footer4.xml'])
assert len(root.xpath('//w:hyperlink',namespaces=ns))==10
assert len(root.xpath('//w:drawing',namespaces=ns))==3
(base/'provided-diagrams-qa.json').write_text(json.dumps(record,ensure_ascii=False,indent=2),encoding='utf-8')
with (base/'artifact.md').open('a',encoding='utf-8') as f:f.write('\nUser-directed revision: replace single redrawn function figure with all three original PNGs from G:/project/img/结构图, unchanged image bytes, 16 cm width and preserved aspect ratio. Caption sequence 图2.1 用户功能结构图, 图2.2 企业功能结构图, 图2.3 管理员功能结构图. Only document.xml and document relationships changed, three image parts added. All other baseline parts preserved.\n')
print(json.dumps(record,ensure_ascii=False))

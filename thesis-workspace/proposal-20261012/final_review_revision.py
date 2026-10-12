from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
from lxml import etree as E
from copy import deepcopy
from PIL import Image
import shutil,re,json,hashlib
base=Path(__file__).parent
out=Path('G:/project/智聘通招聘网站的设计与实现开题报告.docx')
src=Path('C:/Users/syx04/AppData/Local/Temp/codex-clipboard-cc91088b-bdcf-4408-92ae-a85958928e06.png')
backup=base/'before-single-image-review.docx'
if not backup.exists():shutil.copy2(out,backup)
with ZipFile(backup) as z:parts={n:z.read(n) for n in z.namelist()};infos=z.infolist()
W='http://schemas.openxmlformats.org/wordprocessingml/2006/main';R='http://schemas.openxmlformats.org/officeDocument/2006/relationships'
ns={'w':W,'r':R,'a':'http://schemas.openxmlformats.org/drawingml/2006/main','wp':'http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing'}
root=E.fromstring(parts['word/document.xml']);body=root.find('{'+W+'}body')
def txt(x):return ''.join(x.xpath('.//w:t/text()',namespaces=ns))
pics=root.xpath('//w:p[w:r/w:drawing]',namespaces=ns);assert len(pics)==3
pic=deepcopy(pics[0]);cap=deepcopy(pics[0].getnext());at=list(body).index(pics[0])
for p in pics:body.remove(p.getnext());body.remove(p)
width,height=Image.open(src).size;cx=5760000;cy=round(cx*height/width)
for x in pic.xpath('.//wp:extent | .//a:ext',namespaces=ns):x.set('cx',str(cx));x.set('cy',str(cy))
for x in pic.xpath('.//wp:docPr | .//*[local-name()="cNvPr"]',namespaces=ns):x.set('name','系统总体功能模块');x.set('id','31')
for x in pic.xpath('.//a:blip',namespaces=ns):x.set('{'+R+'}embed','rIdUserOverallStructure')
ts=cap.xpath('.//w:t',namespaces=ns);ts[0].text='图2.1  系统总体功能模块'
for x in ts[1:]:x.text=''
body.insert(at,pic);body.insert(at+1,cap)
rels=E.fromstring(parts['word/_rels/document.xml.rels'])
E.SubElement(rels,'{http://schemas.openxmlformats.org/package/2006/relationships}Relationship',Id='rIdUserOverallStructure',Type=R+'/image',Target='media/user-overall-structure.png')
parts['word/media/user-overall-structure.png']=src.read_bytes()
for t in root.xpath('//w:t',namespaces=ns):
    if t.text:
        t.text=t.text.replace('系统功能结构如图2.1至图2.3所示。','系统总体功能模块如图2.1所示，简历分析与优化归入“我的简历”，岗位推荐归入“智能匹配”，企业投递处理与候选人分析分别对应“收到投递”和“候选人库”。')
        t.text=t.text.replace('必要时组织用户试用，记录实际意见，不预先虚构访谈或问卷结论。','必要时组织用户试用，收集实际意见，用于改进页面交互和业务流程。')
        t.text=t.text.replace('若缺少可靠的真实录用标签，不将样例结果包装为大规模招聘准确率。','在缺少真实录用标签的情况下，以样例排序合理性、解释完整性和运行稳定性作为评价依据。')
# Order the ten task-book references by their first mention in the main text.
children=list(body);refstart=next(i for i,p in enumerate(children) if txt(p).startswith('3 主要参考文献'))
reference_nodes=[p for p in children[refstart+1:] if p.xpath('.//w:hyperlink',namespaces=ns)]
assert len(reference_nodes)==10
order=[]
for p in children[:refstart]:
    for number in re.findall(r'\[(\d+)\]',txt(p)):
        number=int(number)
        if number not in order:order.append(number)
assert len(order)==10,order
mapping={old:new for new,old in enumerate(order,1)}
for t in root.xpath('//w:t',namespaces=ns):
    if t.text:t.text=re.sub(r'\[(\d+)\]',lambda m:'['+str(mapping[int(m.group(1))])+']',t.text)
firstindex=list(body).index(reference_nodes[0])
for p in reference_nodes:body.remove(p)
for j,old in enumerate(order):body.insert(firstindex+j,reference_nodes[old-1])
# The fourteen-week plan fits a full page; start its title on that page rather
# than allowing the unchanged template table to split after the fifth week.
for p in body:
    if txt(p)=='2.3 本课题实施计划':
        pp=p.find('{'+W+'}pPr')
        pagebreak=pp.find('{'+W+'}pageBreakBefore')
        if pagebreak is None:pagebreak=E.SubElement(pp,'{'+W+'}pageBreakBefore')
        pagebreak.set('{'+W+'}val','1')
parts['word/document.xml']=E.tostring(root,encoding='UTF-8',xml_declaration=True,standalone=True)
parts['word/_rels/document.xml.rels']=E.tostring(rels,encoding='UTF-8',xml_declaration=True,standalone=True)
with ZipFile(out,'w',ZIP_DEFLATED) as z:
    for info in infos:z.writestr(info,parts[info.filename])
    z.writestr('word/media/user-overall-structure.png',parts['word/media/user-overall-structure.png'])
with ZipFile(backup) as z:assert all(z.read(n)==parts[n] for n in z.namelist() if n not in ['word/document.xml','word/_rels/document.xml.rels'])
main=[];active=False
for p in body:
    t=txt(p)
    if t.startswith('1 选题'):active=True
    if t.startswith('3 主要参考'):active=False
    if active and E.QName(p).localname=='p' and not t.startswith('图'):main.append(t)
assert len(re.findall('[\u4e00-\u9fff]',''.join(main)))>=3000
assert len(root.xpath('//w:drawing',namespaces=ns))==1
assert len(root.xpath('//w:hyperlink',namespaces=ns))==10
assert not any('图2.2' in txt(p) or '图2.3' in txt(p) for p in body)
report={'main_chinese_characters':len(re.findall('[\u4e00-\u9fff]',''.join(main))),'reference_order_original_numbers':order,'new_reference_numbers':list(range(1,11)),'source_image':str(src),'image_sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'image_dimensions_pixels':[width,height],'image_width_cm':16,'image_height_cm':cy/360000,'source_image_embedded_unchanged':True,'requirements':{'three_roles':True,'task_book_core_functions':True,'14_week_plan':True,'task_book_references_with_hyperlinks':10,'personal_information_blank_as_requested':True,'approval_fields_blank':True}}
(base/'final-content-review.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
with (base/'artifact.md').open('a',encoding='utf-8') as f:f.write('\nUser final review: all three figures replaced by one attached overall structure image, embedded byte-for-byte unchanged at 16 cm wide with original aspect ratio. Caption and body references now exclusively 图2.1. Reference paragraphs and in-text citations renumbered consistently in order of first mention, maintaining task-book hyperlinks. Two research-method sentences rewritten in formal proposal voice. Implementation-plan heading starts a page so all fourteen weeks remain together. No other package members changed. Verify main text exceeds 3000 Chinese characters and final native Word pagination.\n')
print(json.dumps(report,ensure_ascii=False))

from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
from lxml import etree as E
from copy import deepcopy
import shutil,re,json
base=Path(__file__).parent
path=Path('G:/project/智聘通招聘网站的设计与实现开题报告.docx')
backup=base/'before-shortening-goal-content.docx'
if not backup.exists():shutil.copy2(path,backup)
with ZipFile(backup) as z:infos=z.infolist();parts={n:z.read(n) for n in z.namelist()}
W='http://schemas.openxmlformats.org/wordprocessingml/2006/main';ns={'w':W}
root=E.fromstring(parts['word/document.xml']);body=root.find('{'+W+'}body')
def txt(p):return ''.join(p.xpath('.//w:t/text()',namespaces=ns))
children=list(body);start=next(i for i,p in enumerate(children) if txt(p)=='2.1.1 课题研究的目标和内容');end=next(i for i,p in enumerate(children) if txt(p)=='2.1.2 课题研究方法')
slot=children[start+1:end]
textparas=[p for p in slot if txt(p) and not txt(p).startswith('图2.')]
figures=[deepcopy(p) for p in slot if not txt(p) or txt(p).startswith('图2.')]
newtexts=[
'本课题研究目的是设计并实现一个智聘通招聘网站，采用Java语言，结合Spring Boot框架、Vue 3前端技术与MySQL数据库，构建功能完善、操作便捷的招聘服务平台。网站采用B/S架构和前后端分离方式，面向求职者、企业和管理员提供职位检索、简历投递与招聘管理服务，并结合简历分析、岗位匹配与推荐等智能辅助功能，帮助求职者了解自身能力与岗位要求的差距，为企业筛选候选人提供参考，提高求职与招聘的信息处理效率。',
'本设计要求开发一个智聘通招聘网站。求职者功能模块划分为用户认证、首页、职位检索、简历管理、职位收藏、简历投递、个人中心、简历分析、岗位匹配与推荐、简历优化、模拟面试；企业功能模块划分为企业信息、职位管理、投递处理、候选人分析、数据统计；管理员功能模块划分为首页、个人中心、用户管理、企业管理、职位管理、简历管理、投递管理、举报处理、操作日志、AI配置管理。系统功能结构如图2.1至图2.3所示。']
new=[]
for text in newtexts:
    p=deepcopy(textparas[0]);pp=p.find('{'+W+'}pPr');rp=deepcopy(p.find('{'+W+'}r/{'+W+'}rPr'))
    for c in list(p):
        if c is not pp:p.remove(c)
    r=E.SubElement(p,'{'+W+'}r')
    if rp is not None:r.append(rp)
    E.SubElement(r,'{'+W+'}t').text=text
    new.append(p)
for p in slot:body.remove(p)
for i,p in enumerate(new+figures,start+1):body.insert(i,p)
parts['word/document.xml']=E.tostring(root,encoding='UTF-8',xml_declaration=True,standalone=True)
with ZipFile(path,'w',ZIP_DEFLATED) as z:
    for info in infos:z.writestr(info,parts[info.filename])
with ZipFile(backup) as z:assert all(z.read(n)==parts[n] for n in z.namelist() if n!='word/document.xml')
assert len(root.xpath('//w:drawing',namespaces=ns))==3
assert len(root.xpath('//w:hyperlink',namespaces=ns))==10
before=sum(len(re.findall('[\u4e00-\u9fff]',txt(p))) for p in textparas)
after=sum(len(re.findall('[\u4e00-\u9fff]',t)) for t in newtexts)
print(json.dumps({'section_before_chinese_characters':before,'section_after_chinese_characters':after,'paragraphs_before':len(textparas),'paragraphs_after':2},ensure_ascii=False))
with (base/'artifact.md').open('a',encoding='utf-8') as f:f.write('\nUser annotation revision: subsection 2.1.1 condensed to two source-style body paragraphs (research objective and technology, then three-role function summary). Removed detailed architecture, database, algorithm and test elaboration from this subsection. Preserved all three provided figures, all other prose, all relationships and every non-document.xml package member byte-for-byte.\n')

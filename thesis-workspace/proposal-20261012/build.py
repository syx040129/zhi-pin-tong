from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from copy import deepcopy
from lxml import etree as E
from PIL import Image, ImageDraw, ImageFont
import hashlib, json, re, html

BASE=Path(__file__).parent
REF=Path('D:/xwechat_files/wxid_bhj0j1xkaxh822_06a6/msg/file/2026-10/3.开题报告.docx')
TASK=REF.with_name('2.任务书(1).docx')
OUT=Path('G:/project/智聘通招聘网站的设计与实现开题报告.docx')
W='http://schemas.openxmlformats.org/wordprocessingml/2006/main'
R='http://schemas.openxmlformats.org/officeDocument/2006/relationships'
NS={'w':W,'r':R,'a':'http://schemas.openxmlformats.org/drawingml/2006/main','wp':'http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing'}
def tag(x):return '{'+W+'}'+x
def prop(parent,name,**attrs):
    x=parent.find(tag(name))
    if x is None:x=E.SubElement(parent,tag(name))
    for k,v in attrs.items():x.set(tag(k),str(v))
    return x
def text(p):return ''.join(p.xpath('.//w:t/text()',namespaces=NS))
with ZipFile(REF) as z:parts={n:z.read(n) for n in z.namelist()}; infos=z.infolist()
root=E.fromstring(parts['word/document.xml']); body=root.find(tag('body')); old=list(body)
pars=body.findall(tag('p'))
inventory={n:{'size':len(v),'sha256':hashlib.sha256(v).hexdigest()} for n,v in parts.items()}
(BASE/'source-inventory.json').write_text(json.dumps({'reference_sha256':hashlib.sha256(REF.read_bytes()).hexdigest(),'parts':inventory},ensure_ascii=False,indent=2),encoding='utf-8')

def para(template,content,kind='body'):
    p=deepcopy(template)
    rp=p.find('.//'+tag('rPr'))
    rp=deepcopy(rp) if rp is not None else E.Element(tag('rPr'))
    pp=p.find(tag('pPr'))
    for x in list(p):
        if x is not pp:p.remove(x)
    if pp is None:pp=E.SubElement(p,tag('pPr'))
    for x in pp.findall(tag('numPr')):pp.remove(x)
    prop(pp,'widowControl',val=1)
    if kind=='heading':prop(pp,'keepNext',val=1);prop(pp,'keepLines',val=1)
    if kind=='body':
        prop(rp,'rFonts',ascii='Times New Roman',hAnsi='Times New Roman',eastAsia='宋体')
        f=rp.find(tag('rFonts'))
        for a in list(f.attrib):
            if 'Theme' in a or 'theme' in a:del f.attrib[a]
        prop(rp,'sz',val=24);prop(rp,'color',val='000000');prop(rp,'b',val=0)
        prop(pp,'spacing',line=360,lineRule='auto',after=0,before=0)
        prop(pp,'ind',firstLine=480,left=0,right=0)
    r=E.SubElement(p,tag('r'));r.append(rp);t=E.SubElement(r,tag('t'));t.text=content
    return p

content=[
('h1','1 选题的意义和研究现状'),
('h2','1.1 选题的意义'),
('b','招聘网站连接求职者与用人单位，其服务质量不仅取决于职位信息的数量，也取决于岗位要求能否被准确理解、简历能否清晰表达个人能力，以及投递后的处理过程能否及时反馈。对于缺少求职经验的学生，单纯依据职位名称和关键词筛选，容易忽略技能、学历、工作经历与实际岗位职责之间的差异；对于企业，简历信息分散、筛选标准不一致和处理记录不完整，也会增加招聘管理成本。围绕这些问题，本课题拟设计并实现智聘通招聘网站，形成职位检索、简历管理、投递处理与智能辅助相结合的服务流程。'),
('b','本课题以求职者、企业和管理员为服务对象。求职者能够集中维护个人简历、查看职位要求并跟踪投递状态；企业能够维护招聘岗位、处理候选人申请并了解招聘数据；管理员能够维护平台业务信息、处理举报和查看操作日志。通过统一的数据结构与明确的权限边界，将招聘活动中相互分离的信息组织起来，使岗位发布、申请提交、企业处理和结果查看具有连续性，为后续分析提供可追溯的数据基础。'),
('b','在普通招聘业务之上，引入简历分析、人岗匹配、岗位推荐、简历优化和模拟面试，可帮助用户理解自身资料与目标岗位之间的差距。系统将给出维度评分、匹配依据与改进建议，使智能辅助结果能够与具体简历和岗位信息对应。课题的实践意义在于探索适合本科毕业设计规模的智能招聘实现路径，并通过软件工程方法验证系统的功能完整性、权限正确性与异常处理能力，形成可运行、可解释、可维护的应用原型。'),
('h2','1.2 本课题所涉及问题在国内外设计或研究的现状'),
('h3','1.2.1 国外现状'),
('b','国际人岗匹配研究关注如何从求职者资料与岗位描述中提取有效特征，并将这些特征用于匹配排序。仅比较简历与职位文本的相似度，可能遗漏求职者在职业经历中表现出的长期偏好。Gong等在AAAI 2024发表的研究将职业路径纳入人岗匹配，分析经历中的一致性、相似性与连续性，说明历史职业经历可以为岗位推荐提供额外依据[10]。该研究为本课题组织工作、项目和实习经历提供参考，但其模型和论文实验结果不能直接视为本系统的实现能力或测试结论。'),
('b','从上述研究可以看出，招聘推荐需要同时理解岗位需求与个人经历，而不能只依赖孤立关键词。本课题拟在结构化简历中保留多类经历，构造统一的简历文本和岗位文本，并将语义相似度与技能、经验、学历等条件结合。考虑到毕业设计缺少大规模带标签招聘数据，研究重点放在数据组织、评分解释和业务集成上，采用现有模型接口与可复现规则形成对照，不将训练大型人岗匹配模型作为必要前提。'),
('h3','1.2.2 国内现状'),
('b','国内相关研究从招聘流程优化和匹配算法两个方向展开。王晓莹围绕企业人员招聘体系的优化，讨论招聘规划、流程和评价机制等问题[2]，为平台业务流程设计提供参考。黄翠研究基于模糊综合评价的多岗位招聘模块[1]，体现了从多个能力维度组织候选人评价的思路。这些研究提示，招聘系统需要将岗位条件、候选人资料和处理过程统一考虑，使评价结果服务于实际招聘管理。'),
('b','在人岗匹配方法方面，任书仪等提出协同注意力网络方法，将结构化数值信息、文本语义与历史招聘特征融合[3]；张茜等针对医院招聘，研究属性级语义表示与自注意力组合[4]；王雅娟等将知识图谱与注意力机制用于岗位匹配预测[5]；啜子涵等研究半结构化多元属性的键值表示和特征融合[7]。这些工作共同表明，简历与岗位的多属性信息值得保留，而不能在建模前被压缩为单一文本或标签。'),
('b','在就业服务应用方面，郭立群等以高校就业支持体系为对象，讨论人工智能与简历优化、模拟面试、人岗匹配等服务的结合[6]，为本课题扩展求职辅助功能提供方向。工程实现方面，朱芊慧等的Spring Boot与Vue后台设计[8]、吴晓云等的Spring Boot在线考试系统[9]，为分层开发、角色管理和业务测试提供了可借鉴的技术实例。结合这些文献，本课题将以常规招聘业务为基础，重点解决三角色协同、匹配结果解释与智能服务异常回退问题。'),
('h1','2 课题设计或研究的内容、预期目标和实施计划'),
('h2','2.1 设计或研究的主要内容方案论证分析'),
('h3','2.1.1 课题研究的目标和内容'),
('b','本课题拟基于现有CareerNova项目代码，完成智聘通招聘网站的设计、完善与验证。系统采用B/S架构和前后端分离方式，前端以Vue 3、TypeScript和Vite组织页面，配合Vue Router、Pinia、Axios、Element Plus与ECharts实现路由、状态管理、接口调用和数据展示；后端采用Java 17与Spring Boot 3，使用MyBatis-Plus访问MySQL数据库，通过Spring Security、JWT和BCrypt实现身份认证与密码保护。开发工具采用Visual Studio Code和IntelliJ IDEA，Redis作为可选缓存组件。'),
('b','（1）求职者功能。完成注册登录、首页浏览、职位检索、职位详情、个人中心、简历管理、职位收藏与简历投递。简历由基本信息、技能及教育、工作、项目、实习经历组成，附件用于补充资料。职位查询支持城市、薪资、学历和经验等条件；投递操作绑定具体岗位与简历，用户可在投递列表中查看处理状态，避免重复申请。智能辅助模块包括简历分析、岗位匹配与推荐、简历优化和模拟面试，分别提供资料评价、适配依据、修改建议及逐题训练反馈。'),
('b','（2）企业功能。完成企业信息维护、职位发布与上下架、岗位技能维护、投递列表查询、候选人查看、投递状态处理、候选人分析和招聘数据统计。企业只能操作本企业发布的岗位与收到的申请，不能通过修改请求参数读取其他企业的候选人资料。候选人分析结合目标岗位展示优势、差距和建议，供招聘人员辅助判断；数据统计围绕岗位和投递记录生成，用于观察招聘进度和处理情况。'),
('b','（3）管理员功能。完成后台首页、个人中心、用户管理、企业管理、职位管理、简历管理、投递管理、举报处理、操作日志和AI配置管理。管理员维护账户状态与业务信息，记录重要操作，并对举报进行查询和处理。AI管理页面展示运行模式、模型配置与连通性测试结果，密钥只展示是否已配置，避免明文回显。项目现有招聘助手作为补充功能，为用户提供招聘相关问答与历史会话查看，不改变任务书规定的核心业务范围。'),
('figure',''),
('caption','图2.1  系统总体功能模块'),
('b','（4）总体架构与接口设计。前端按求职者门户、企业工作台和管理后台组织页面；后端按认证、用户、企业、岗位、简历、投递、收藏、举报、日志和智能服务划分业务模块，通过Controller、Service和Mapper分层处理请求。接口采用HTTP与JSON进行交互，统一响应结构和异常提示。权限控制既检查登录身份与角色，也检查简历、岗位和投递记录的归属，避免仅依赖前端按钮隐藏实现授权。'),
('b','（5）数据库设计。以项目SQL脚本为依据，组织用户、企业、岗位、岗位技能、简历、简历经历、投递、收藏、简历分析、岗位推荐、模拟面试、面试题目、举报、日志、助手会话和助手消息共16张业务表。通过用户与企业、企业与岗位、简历与经历、岗位与投递等主外键字段表达业务联系；利用唯一索引限制重复收藏和重复投递，通过状态字段记录岗位及申请的变化。设计阶段同时检查查询索引、字段约束、逻辑删除与历史记录保留方式，保障数据的一致性。'),
('b','（6）岗位匹配与推荐。依据项目JobMatchService实现，设语义分、技能分、经验分和学历分均为0至100，总匹配分按“语义分×0.50＋技能分×0.25＋经验分×0.15＋学历分×0.10”计算。技能分结合岗位技能权重与简历技能覆盖情况，其他维度依据岗位条件计算，并返回匹配技能、缺失技能与解释。推荐服务从可用岗位中计算和排序，保存用户与岗位的推荐结果。这些权重是现有工程方案的初始参数，需通过样例对照检验其合理性，不能直接解释为录用概率。'),
('b','语义计算拟支持外部Embedding模型与本地向量两条路径。真实模式调用配置的向量接口；接口不可用或未启用时，使用项目已有的字符二元组哈希向量与余弦相似度，保证演示结果可复现。简历分析、优化和面试通过Spring AI组织模型调用，并对结构化输出进行解析。模型调用失败时采用相应规则、题库或本地计算回退，同时呈现结果来源。报告与测试将分别记录真实模型和规则模式，避免将离线规则结果表述为已验证的真实模型推理效果。'),
('b','（7）测试与交付设计。围绕注册登录、岗位检索、简历维护、收藏投递、企业处理、举报处置和AI辅助构建完整业务用例。重点验证未登录访问、跨用户简历访问、跨企业投递访问、重复申请、已下架岗位、空简历和外部服务异常等边界。前端进行类型检查与构建验证，后端开展业务单元测试和接口检查，并在独立测试数据上核对数据变化。系统部署、真实模型调用与性能测试应分别保留环境、参数和结果记录，形成代码、数据库脚本、运行说明与测试报告。'),
('h3','2.1.2 课题研究方法'),
('b','（1）文献分析法。以任务书所列招聘体系、人岗匹配、就业服务和Web系统开发文献为主要依据，梳理多属性评价、语义特征融合及智能求职辅助思路。区分文献提出的研究模型与本课题采用的工程方案，提取对数据结构、评分解释和业务流程有价值的结论，形成与系统功能对应的参考依据。'),
('b','（2）需求分析与原型迭代法。根据任务书逐项建立三角色功能清单，结合现有路由、接口、实体和数据库脚本核对业务边界。通过岗位查询、简历投递、企业筛选和管理员处理等使用场景，明确输入、输出、前置条件与异常分支；在指导教师反馈基础上迭代页面和流程。必要时组织用户试用，记录实际意见，不预先虚构访谈或问卷结论。'),
('b','（3）软件工程与实验对照法。按照需求分析、总体设计、模块实现、联调测试与部署整理的顺序推进。使用脱敏或构造样例建立岗位与简历对照集，比较仅使用规则评分与加入语义评分后的推荐排序、技能缺口说明和结果稳定性。通过人工评阅、Top-K结果检查、响应时间与失败回退记录分析系统表现；若缺少可靠的真实录用标签，不将样例结果包装为大规模招聘准确率。'),
('h3','2.1.3 可行性分析'),
('b','（1）经济可行性。课题以现有项目和个人开发环境为基础，主要成本为开发、调试与资料整理时间。Spring Boot、Vue和MySQL能够满足毕业设计的开发需要，规则模式不依赖付费模型服务，可用于基础功能演示。真实模型验证阶段需根据调用量控制API费用，服务器与域名按实际部署需要配置，避免将持续付费服务作为完成基本业务的必要条件。'),
('b','（2）技术可行性。项目已有三角色页面、业务接口、16张数据表及智能服务代码，为课题提供了可检查的实现基础。Java与Spring Boot承担业务处理，Vue负责交互展示，MyBatis-Plus与MySQL承担持久化，技术职责清晰。主要技术难点在于资源归属校验、投递状态一致性、附件解析、评分解释和外部模型失败处理。通过分层设计、数据库约束、专项用例和规则回退，可在本科毕业设计周期内逐步完成验证；真实模型效果及生产环境稳定性仍需单独测试。'),
('b','（3）操作可行性。用户通过浏览器访问系统，无需安装专用客户端。求职者以职位列表、简历编辑和投递记录为主要操作入口，企业以岗位与候选人列表开展招聘工作，管理员以分类管理页面处理业务信息。界面采用统一表单、筛选、分页、状态标签和反馈提示，减少重复输入。对于智能建议，展示具体依据和修改前后内容，便于用户理解并自主选择，避免将辅助建议直接作为企业录用结论。'),
('h2','2.2 本课题选题特色及预期的目标'),
('b','本课题的特色是将完整招聘业务与可解释的智能辅助结合。首先，以三角色和资源归属约束组织业务，贯通岗位发布、简历维护、收藏投递、企业处理和平台治理；其次，保留简历经历与岗位技能的结构化信息，将语义、技能、经验和学历评分共同用于匹配，展示分项得分及技能差距；再次，提供真实模型与规则回退两条运行路径，使外部服务不可用时仍能完成基本演示，并在界面和测试记录中说明结果来源。'),
('b','预期完成一套能够在明确配置环境中运行的智聘通招聘网站。求职者能够完成“查找岗位—维护简历—获取分析与推荐—投递—查看状态”的流程，企业能够完成“维护信息—发布岗位—查看候选人—处理申请—查看统计”的流程，管理员能够完成数据管理、举报处理、日志查询与AI配置检查。核心功能、权限边界和异常路径应有测试证据，智能模块应返回可读的维度评分、建议或面试反馈，并保证这些结果与相应用户资料关联。'),
('b','预期成果包括系统源代码、数据库建表与演示数据脚本、需求与总体设计说明、功能模块图、数据库关系说明、接口和部署说明、测试记录以及毕业设计论文和答辩材料。匹配质量、真实模型效果与响应性能均以最终实验和测试结果为准，不预设已实现的提升比例。按照任务书完成不少于3000字的开题报告、外文资料翻译及后续论文写作，通过指导教师审阅持续修订，最终形成可演示、可复核、可归档的毕业设计成果。'),
('h2','2.3 本课题实施计划'),
('b','本课题按14周安排实施，具体起止日期以学校毕业设计工作安排为准。各阶段结合现有代码开展核对、完善和测试，并同步整理设计依据与论文材料。'),
]

styles={'h1':old[15],'h2':old[19],'h3':old[20],'b':old[35],'caption':old[33]}
new=[]
for k,t in content:
    if k=='figure':
        p=deepcopy(old[32]); pp=p.find(tag('pPr'));prop(pp,'keepNext',val=1);prop(pp,'jc',val='center');prop(pp,'ind',firstLine=0,left=0,right=0)
        for x in pp.findall(tag('ind')):
            for a in list(x.attrib):
                if a.endswith('Chars'):del x.attrib[a]
        for x in p.xpath('.//a:srcRect',namespaces=NS):x.getparent().remove(x)
        for x in p.xpath('.//wp:extent',namespaces=NS):x.set('cx','5760000');x.set('cy','3520000')
        for x in p.xpath('.//a:ext',namespaces=NS):x.set('cx','5760000');x.set('cy','3520000')
    else:
        p=para(styles[k],t,'heading' if k.startswith('h') else ('body' if k=='b' else 'caption'))
        if k=='h1' and t.startswith('2 '):prop(p.find(tag('pPr')),'pageBreakBefore',val=1)
        if k=='caption':prop(p.find(tag('pPr')),'ind',firstLine=0,firstLineChars=0)
    new.append(p)

# Cover: clear unconfirmed personal details, retain institute and add subject.
cover=old[:15]
cover[8]=para(old[2],'智聘通招聘网站的设计与实现','cover')
prop(cover[8].find(tag('pPr')),'jc',val='center')
prop(cover[8].find('.//'+tag('rPr')),'sz',val=36)
for row in cover[12].findall(tag('tr'))[1:]:
    tc=row.findall(tag('tc'))[1]
    for x in tc.xpath('.//w:t',namespaces=NS):x.text=''

plan=[
'确定研究范围，收集招聘网站案例，核对任务书与项目代码',
'梳理研究现状与需求，完成开题报告，准备开题答辩',
'研读人岗匹配外文资料，完成翻译，整理研究与技术依据',
'细化三角色需求、权限与接口，确定开发环境和验证计划',
'完成总体架构、数据库、业务流程与功能模块设计',
'完善认证、职位检索、简历、收藏及投递等求职者功能',
'完善企业信息、岗位管理、投递处理和招聘统计功能',
'完善管理员功能、举报处理、日志和AI配置管理',
'完善简历分析、匹配推荐、优化及模拟面试，开展模式对照',
'完成前后端联调、业务与权限测试，记录结果并修复问题',
'验证运行与部署，整理接口和测试资料，完成论文初稿',
'根据教师意见修订论文，检查格式、引用并完成查重检测',
'整理演示环境和答辩PPT，完善论文答辩稿与相关材料',
'参加毕业设计答辩，修订终稿并归档代码、文档及数据脚本']
table=deepcopy(old[45]); rows=table.findall(tag('tr'))
def celltext(tc,value):
    ps=tc.findall(tag('p')); p=para(ps[0],value,'cell');
    for x in ps:tc.remove(x)
    tc.append(p)
celltext(rows[0].findall(tag('tc'))[1],'智聘通招聘网站的设计与实现')
for i,v in enumerate(plan,1):celltext(rows[i].findall(tag('tc'))[1],v)
new.append(table)

# Read each reference and its actual hyperlink from the task book.
with ZipFile(TASK) as z:
    tr=E.fromstring(z.read('word/document.xml')); rel=E.fromstring(z.read('word/_rels/document.xml.rels'))
linkmap={x.get('Id'):x.get('Target') for x in rel}
refs=[]
for p in tr.xpath('//w:p',namespaces=NS):
    tt=text(p)
    links=p.xpath('.//w:hyperlink',namespaces=NS)
    if links and ('[J]' in tt or '[C]' in tt):refs.append((tt,linkmap[links[0].get('{'+R+'}id')]))
assert len(refs)==10,len(refs)
rels=E.fromstring(parts['word/_rels/document.xml.rels'])
p=para(old[47],'3 主要参考文献','heading');prop(p.find(tag('pPr')),'pageBreakBefore',val=1);new.append(p)
for i,(tt,url) in enumerate(refs,1):
    p=para(old[48],f'[{i}] {tt}','reference')
    pp=p.find(tag('pPr'));prop(pp,'keepLines',val=1);prop(pp,'wordWrap',val=1)
    ind=prop(pp,'ind',left=480,hanging=480)
    for a in list(ind.attrib):
        if a.endswith('Chars'):del ind.attrib[a]
    r=p.find(tag('r')); rid=f'rIdProposalRef{i}'
    h=E.Element(tag('hyperlink'));h.set('{'+R+'}id',rid);p.remove(r);h.append(r);p.append(h)
    rp=r.find(tag('rPr'));prop(rp,'color',val='000000');prop(rp,'u',val='none')
    E.SubElement(rels,'{http://schemas.openxmlformats.org/package/2006/relationships}Relationship',Id=rid,Type=R+'/hyperlink',Target=url,TargetMode='External')
    new.append(p)

# Keep the original approval form geometry while removing old endorsements.
p=para(old[68],'','empty');prop(p.find(tag('pPr')),'pageBreakBefore',val=1);new.append(p)
approval=deepcopy(old[69])
for i,tc in enumerate(approval.xpath('./w:tr/w:tc',namespaces=NS)):
    ps=tc.findall(tag('p')); patterns=[deepcopy(x) for x in ps]
    for x in ps:tc.remove(x)
    values=['指导教师意见：','','','','','指导教师：','年    月    日'] if i==0 else ['教研室意见：','','','','审查结果：□同意开题        □不同意开题','教研室主任：','年    月    日']
    for j,v in enumerate(values):
        p=para(patterns[min(j,len(patterns)-1)],v,'approval')
        for rp in p.xpath('.//w:rPr',namespaces=NS):prop(rp,'b',val=1 if j==0 else 0)
        if j>=5:prop(p.find(tag('pPr')),'jc',val='right')
        tc.append(p)
new.append(approval);new.append(deepcopy(old[70]));new.append(deepcopy(old[71]))
for x in list(body):body.remove(x)
for x in cover+new:body.append(x)

# Redraw diagram with SVG source and high-resolution PNG for Word compatibility.
img=Image.new('RGB',(1800,1100),'white');draw=ImageDraw.Draw(img)
fontpath='C:/Windows/Fonts/simsun.ttc'
ft=ImageFont.truetype(fontpath,40);fh=ImageFont.truetype('C:/Windows/Fonts/simhei.ttf',44)
svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1800" height="1100" viewBox="0 0 1800 1100"><rect width="1800" height="1100" fill="white"/>']
def line(xy):draw.line(xy,fill='black',width=3);svg.append(f'<path d="M {xy[0]} {xy[1]} L {xy[2]} {xy[3]}" stroke="black" stroke-width="3"/>')
def box(x,y,w,h,s,head=False):
    draw.rectangle((x,y,x+w,y+h),outline='black',width=3)
    draw.text((x+w/2,y+h/2),s,font=fh if head else ft,fill='black',anchor='mm')
    svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="white" stroke="black" stroke-width="3"/><text x="{x+w/2}" y="{y+h/2}" text-anchor="middle" dominant-baseline="central" font-family="SimSun,宋体" font-size="{44 if head else 40}">{html.escape(s)}</text>')
box(480,15,840,80,'智聘通招聘网站',True)
line((900,95,900,130));line((290,130,1510,130))
groups=[('求职者',['用户认证与首页','职位检索与详情','简历管理','职位收藏','简历投递与状态','个人中心','简历分析','岗位匹配与推荐','简历优化','模拟面试']),('企业',['企业信息','职位管理','投递处理','候选人分析','数据统计']),('管理员',['后台首页与个人中心','用户管理','企业管理','职位管理','简历管理','投递管理','举报处理','操作日志','AI配置管理'])]
for c,(name,items) in enumerate(groups):
    x=30+c*610;cx=x+260
    line((cx,130,cx,170));box(x,170,520,75,name,True)
    line((cx,245,cx,280))
    for j,s in enumerate(items):box(x,280+j*66,520,58,s)
box(330,1000,1140,75,'补充功能  招聘智能助手与历史会话')
svg.append('</svg>');(BASE/'系统总体功能模块.svg').write_text('\n'.join(svg),encoding='utf-8')
img.save(BASE/'系统总体功能模块.png');parts['word/media/image1.png']=(BASE/'系统总体功能模块.png').read_bytes()
parts['word/document.xml']=E.tostring(root,encoding='UTF-8',xml_declaration=True,standalone=True)
parts['word/_rels/document.xml.rels']=E.tostring(rels,encoding='UTF-8',xml_declaration=True,standalone=True)
with ZipFile(OUT,'w',ZIP_DEFLATED) as z:
    for info in infos:z.writestr(info,parts[info.filename])
bodytext='\n'.join(t for k,t in content if k=='b')
report={'output':str(OUT),'body_chinese_characters':len(re.findall('[\u4e00-\u9fff]',bodytext)),'reference_count':len(refs),'hyperlinks':refs,'unchanged_parts':[n for n,v in inventory.items() if hashlib.sha256(parts[n]).hexdigest()==v['sha256']], 'changed_parts':[n for n,v in inventory.items() if hashlib.sha256(parts[n]).hexdigest()!=v['sha256']]}
assert report['body_chinese_characters']>=3000
assert report['changed_parts']==['word/_rels/document.xml.rels','word/document.xml','word/media/image1.png'] or set(report['changed_parts'])=={'word/_rels/document.xml.rels','word/document.xml','word/media/image1.png'}
assert hashlib.sha256(REF.read_bytes()).hexdigest()==json.loads((BASE/'source-inventory.json').read_text(encoding='utf-8'))['reference_sha256']
(BASE/'qa.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
(BASE/'report-content.txt').write_text('\n'.join(t for k,t in content if t),encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k in ['output','body_chinese_characters','reference_count','changed_parts']},ensure_ascii=False))

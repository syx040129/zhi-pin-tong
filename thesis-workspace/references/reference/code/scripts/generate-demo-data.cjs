// Rebuild the reviewed SQL assets; no database connection is made here.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const q = value => value === null ? 'NULL' : typeof value === 'number' ? String(value) : `'${String(value).replaceAll("'", "''")}'`;
const sql = ['-- CareerNova 扩展演示数据（虚构企业与求职者），MySQL 8.0',
  '-- 可重复执行；只新增缺失记录，不覆盖已有资料。所有 cn_demo_* 账号密码：123456。',
  'USE smart_recruitment;', 'SET NAMES utf8mb4;',
  'DROP PROCEDURE IF EXISTS cn_seed_demo;', 'DELIMITER $$',
  'CREATE PROCEDURE cn_seed_demo()', 'BEGIN',
  '  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;',
  '  START TRANSACTION;'];
const raw = value => ({ raw: value });
const v = value => value && typeof value === 'object' ? value.raw : q(value);
function insert(table, values, exists) {
  sql.push(`  INSERT INTO ${table} (${Object.keys(values).join(', ')}) SELECT ${Object.values(values).map(v).join(', ')} WHERE NOT EXISTS (SELECT 1 FROM ${table} WHERE ${exists});`);
}
const date = days => raw(`DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ${days} DAY)`);
const password = '$2b$10$Y3X9mUabtpfrCn/3mJJuKOFzq1reUPcqPXhmZ.c3X1LkbT6kGBKMC';
const cities = ['上海', '北京', '深圳', '杭州', '广州', '成都', '武汉', '南京'];
const firms = ['澄川云计算', '知禾数据', '远山智能', '晴川软件', '星桥数字', '青岚科技', '江城信息', '金陵智造'];
const tracks = [
  ['Java开发工程师', '后端开发', 'Java,Spring Boot,MySQL,Redis', 16, 28],
  ['Vue前端工程师', '前端开发', 'Vue,TypeScript,JavaScript,CSS', 14, 25],
  ['数据分析师', '数据分析', 'SQL,Python,Excel,数据可视化', 12, 22],
  ['AI应用工程师', '算法工程师', 'Python,PyTorch,NLP,RAG', 22, 40],
  ['测试开发工程师', '测试开发', 'Python,自动化测试,SQL,接口测试', 12, 24],
  ['云平台运维工程师', '运维开发', 'Linux,Docker,Kubernetes,CI/CD', 16, 30],
  ['产品经理', '产品经理', '需求分析,Axure,SQL,产品设计', 15, 28],
  ['全栈开发工程师', '全栈开发', 'Java,Vue,MySQL,TypeScript', 18, 32],
];
const schools = ['上海大学', '北京工业大学', '深圳大学', '浙江工业大学', '广州大学', '成都信息工程大学', '武汉理工大学', '南京邮电大学'];
const names = ['林知夏', '陈泽宇', '许清禾', '周景行', '苏念安', '沈予辰', '叶嘉宁', '陆星然', '唐书言', '顾可欣', '程远航', '宋雨桐', '何亦然', '赵思齐', '吴若溪', '郑明轩', '梁安宁', '徐文博', '谢梓涵', '韩沐阳', '邓诗涵', '冯承宇', '魏书宁', '蒋云舒', '蔡嘉禾', '潘景明', '卢佳音', '袁子墨', '罗清越', '马天朗'];
for (let i = 0; i < firms.length; i++) {
  const username = `cn_demo_company_${i + 1}`;
  insert('sys_user', { username, password, nickname: firms[i], role: 'COMPANY', status: 'ENABLED', email: `hr${i + 1}@careernova.example`, create_time: date(45 - i), update_time: date(45 - i) }, `username=${q(username)}`);
  sql.push(`  SET @cn_company_user_${i} = (SELECT id FROM sys_user WHERE username=${q(username)});`);
  insert('company', { user_id: raw(`@cn_company_user_${i}`), company_name: `${cities[i]}${firms[i]}有限公司（演示）`, industry: ['云计算', '大数据/数据服务', '人工智能', '互联网/软件开发'][i % 4], scale: i % 2 ? '100-499人' : '500-999人', address: `${cities[i]}市科技园创新中心`, description: `${firms[i]}是虚构演示企业，专注企业数字化与软件产品研发。团队采用代码评审、自动化测试和持续交付，提供导师培养、弹性工作与年度培训。`, website: `https://company${i + 1}.example`, create_time: date(45 - i), update_time: date(45 - i) }, `user_id=@cn_company_user_${i}`);
  sql.push(`  SET @cn_company_${i} = (SELECT id FROM company WHERE user_id=@cn_company_user_${i});`);
}
for (let i = 0; i < 64; i++) {
  const t = tracks[i % 8], c = Math.floor(i / 8);
  const entry = i % 9 === 0;
  const title = `${t[0]}${entry ? '（校招）' : ''} · ${firms[c]}演示`;
  insert('job', { company_id: raw(`@cn_company_${c}`), job_name: title, job_type: t[1], salary_min: entry ? 8 : t[3], salary_max: entry ? 14 : t[4], city: cities[c], education: i % 8 === 3 ? '硕士' : '本科', experience: entry ? '应届' : i % 3 ? '1-3年' : '3-5年', description: `参与${t[1]}方向的产品建设，负责需求评审、方案设计、功能交付与上线质量。与产品、研发和业务团队协作，通过监控指标持续优化用户体验与运行效率。提供完整导师培养和技术分享机制。`, requirements: `熟悉 ${t[2]}；具备良好的沟通与问题分析能力，能够说明项目方案、技术选择及量化成果。${entry ? '欢迎应届毕业生，需提供课程或实习项目。' : '具有完整项目交付经验，重视工程质量和团队协作。'}`, status: i % 13 === 0 ? 'OFFLINE' : 'ONLINE', view_count: 80 + i * 37 % 1800, create_time: date(1 + i % 29), update_time: date(1 + i % 29) }, `company_id=@cn_company_${c} AND job_name=${q(title)}`);
  sql.push(`  SET @cn_job_${i} = (SELECT id FROM job WHERE company_id=@cn_company_${c} AND job_name=${q(title)} LIMIT 1);`);
  for (const skill of t[2].split(',')) insert('job_skill', { job_id: raw(`@cn_job_${i}`), skill_name: skill, required_level: entry ? 2 : 3, weight: 2 }, `job_id=@cn_job_${i} AND skill_name=${q(skill)}`);
}
for (let i = 0; i < names.length; i++) {
  const username = `cn_demo_user_${String(i + 1).padStart(2, '0')}`, t = tracks[i % 8];
  insert('sys_user', { username, password, nickname: names[i], email: `candidate${i + 1}@careernova.example`, role: 'USER', status: 'ENABLED', create_time: date(1 + i % 28), update_time: date(1 + i % 28) }, `username=${q(username)}`);
  sql.push(`  SET @cn_user_${i} = (SELECT id FROM sys_user WHERE username=${q(username)});`);
  insert('resume', { user_id: raw(`@cn_user_${i}`), name: names[i], email: `candidate${i + 1}@careernova.example`, education: i % 8 === 3 ? '硕士' : '本科', school: schools[i % 8], major: i % 8 === 6 ? '信息管理与信息系统' : '计算机科学与技术', skills: t[2], summary: `关注${t[1]}方向，熟悉 ${t[2]}。完成团队协作项目，参与需求分析、设计与交付，能够结合日志和指标定位问题，期待在${cities[i % 8]}发展。`, completion_score: 80 + i % 20, create_time: date(1 + i % 28), update_time: date(1 + i % 28) }, `user_id=@cn_user_${i}`);
  sql.push(`  SET @cn_resume_${i} = (SELECT id FROM resume WHERE user_id=@cn_user_${i} ORDER BY id DESC LIMIT 1);`);
  const experiences = [ ['EDUCATION', '本科阶段学习', schools[i % 8], '完成数据结构、数据库、软件工程等课程，参与学院团队实践项目。', '2020-09', '2024-06'], ['PROJECT', `${t[1]}实践项目`, 'CareerNova 演示项目组', `负责${t[1]}相关模块的设计与实现，补充自动化验证，优化流程后关键接口耗时降低约30%，形成技术复盘文档。`, '2024-09', '2025-03'], ['WORK', t[0], '演示数字技术团队', '参与迭代开发、代码评审与故障排查，推动监控与质量检查落地，按时完成季度交付。', '2025-04', '至今'] ];
  experiences.forEach((e, sort) => insert('resume_experience', { resume_id: raw(`@cn_resume_${i}`), type: e[0], title: e[1], company: e[2], content: e[3], start_date: e[4], end_date: e[5], sort }, `resume_id=@cn_resume_${i} AND type=${q(e[0])} AND title=${q(e[1])}`));
  insert('ai_resume_analysis', { resume_id: raw(`@cn_resume_${i}`), overall_score: 75 + i % 20, skill_score: 80 + i % 16, experience_score: 70 + i % 25, education_score: 85, completeness_score: 80 + i % 20, strengths: JSON.stringify(['技术栈与目标方向相关', '项目包含具体改进成果']), weaknesses: JSON.stringify(['项目规模与个人职责可以描述得更清晰']), suggestions: JSON.stringify(['补充关键指标的测量方法', '针对目标岗位突出对应技能']), analysis_result: `${t[1]}方向（演示分析）`, create_time: date(1) }, `resume_id=@cn_resume_${i}`);
  for (let n = 0; n < 4; n++) {
    const j = (i % 8) + ((i + n) % 8) * 8, score = 78 + (i + n) % 20;
    insert('job_application', { job_id: raw(`@cn_job_${j}`), user_id: raw(`@cn_user_${i}`), resume_id: raw(`@cn_resume_${i}`), status: ['PENDING', 'VIEWED', 'INTERVIEW', 'REJECTED', 'HIRED'][(i + n) % 5], cover_letter: `希望应聘${t[0]}，我的技能与项目经验与岗位相关，期待进一步沟通。（演示投递）`, match_score: score, create_time: date(Math.min(1 + (i + n) % 20, 1 + i % 28, 1 + j % 29)), update_time: date(1) }, `user_id=@cn_user_${i} AND job_id=@cn_job_${j}`);
    insert('ai_recommendation', { user_id: raw(`@cn_user_${i}`), job_id: raw(`@cn_job_${j}`), match_score: score, skill_score: score, experience_score: score - 3, education_score: 85, semantic_score: score - 2, recommend_reason: `技能 ${t[2]} 与岗位方向契合，建议结合工作年限和城市偏好进一步评估。（演示推荐）`, create_time: date(1) }, `user_id=@cn_user_${i} AND job_id=@cn_job_${j}`);
    if (n < 2) insert('favorite', { user_id: raw(`@cn_user_${i}`), job_id: raw(`@cn_job_${j}`), create_time: date(1 + i % 10) }, `user_id=@cn_user_${i} AND job_id=@cn_job_${j}`);
  }
  const completed = i % 3 !== 0, title = `${t[0]}模拟面试（扩展演示）`;
  const score = 80 + i % 10;
  insert('ai_interview', { user_id: raw(`@cn_user_${i}`), job_id: raw(`@cn_job_${i % 8}`), title, difficulty: '中等', question_count: 3, total_score: completed ? score : null, status: completed ? 'COMPLETED' : 'ONGOING', report: completed ? JSON.stringify({ dimensionScores: { '专业知识': score, '项目经验': score, '沟通表达': score }, strengths: ['技术表达有条理'], weaknesses: ['可以补充边界条件与失败案例'], suggestions: ['结合真实项目补充量化成果'] }) : null, create_time: date(1 + i % 7), update_time: date(1) }, `user_id=@cn_user_${i} AND title=${q(title)}`);
  sql.push(`  SET @cn_interview_${i} = (SELECT id FROM ai_interview WHERE user_id=@cn_user_${i} AND title=${q(title)} LIMIT 1);`);
  ['专业知识', '项目经验', '沟通表达'].forEach((type, sort) => insert('ai_interview_question', { interview_id: raw(`@cn_interview_${i}`), question: [`请结合场景介绍 ${t[2].split(',')[0]} 的使用经验。`, '请介绍一次项目性能或流程优化，说明问题、方案及结果。', '当需求出现冲突时，你会如何与团队沟通并推进交付？'][sort], question_type: type, difficulty: '中等', answer: completed ? '先明确业务目标与约束，定位问题后比较方案，进行验证并通过指标检查改进效果，最后向团队同步结论。（演示回答）' : null, score: completed ? score : null, feedback: completed ? '表达结构清楚，可进一步补充具体案例和技术细节。（演示点评）' : null, sort }, `interview_id=@cn_interview_${i} AND sort=${sort}`));
  if (i < 12) {
    const title = `如何准备${t[1]}面试？（扩展演示）`;
    insert('ai_chat_session', { user_id: raw(`@cn_user_${i}`), title, message_count: 2, create_time: date(1), update_time: date(1) }, `user_id=@cn_user_${i} AND title=${q(title)}`);
    sql.push(`  SET @cn_session_${i} = (SELECT id FROM ai_chat_session WHERE user_id=@cn_user_${i} AND title=${q(title)} LIMIT 1);`);
    insert('ai_chat_message', { session_id: raw(`@cn_session_${i}`), user_id: raw(`@cn_user_${i}`), role: 'USER', content: title, create_time: date(1) }, `session_id=@cn_session_${i} AND role='USER'`);
    insert('ai_chat_message', { session_id: raw(`@cn_session_${i}`), user_id: raw(`@cn_user_${i}`), role: 'ASSISTANT', content: `建议先梳理 ${t[2]} 的核心知识，再准备两个项目案例，说明目标、方案和结果，最后进行模拟面试。（演示对话）`, suggestions: JSON.stringify(['如何描述项目成果？', '如何安排复习计划？']), source: 'RULE', create_time: date(1) }, `session_id=@cn_session_${i} AND role='ASSISTANT'`);
  }
  insert('sys_log', { user_id: raw(`@cn_user_${i}`), username, type: 'OPERATE', module: '演示数据', action: '导入演示资料', detail: '用于本地开发与功能展示的虚构记录', ip: '127.0.0.1', cost_ms: 20 + i, create_time: date(1 + i % 10) }, `username=${q(username)} AND module='演示数据'`);
  if (i < 9) insert('report', { user_id: raw(`@cn_user_${i}`), target_type: 'JOB', target_id: raw(`@cn_job_${i}`), reason: '演示：岗位信息待核实', detail: '此条仅用于演示举报处理流程。', status: ['PENDING', 'HANDLED', 'IGNORED'][i % 3], handle_remark: i % 3 ? '演示处理：已完成信息核实。' : null, create_time: date(1 + i) }, `user_id=@cn_user_${i} AND reason='演示：岗位信息待核实'`);
}
sql.push('  COMMIT;', 'END$$', 'DELIMITER ;', 'CALL cn_seed_demo();', 'DROP PROCEDURE cn_seed_demo;', '');
fs.writeFileSync(path.join(root, 'sql/demo-data.sql'), sql.join('\n'));
console.log('Generated sql/demo-data.sql');

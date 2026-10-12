const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
// Equality filters precede the sort/range column. Existing single-column indexes
// are retained for compatibility; these additions cover actual service queries.
const indexes = {
  sys_user: { idx_user_list: '(deleted, role, status, id)', idx_user_trend: '(deleted, create_time)' },
  company: { idx_company_list: '(deleted, status, id)' },
  job: { idx_job_latest: '(deleted, status, create_time)', idx_job_hot: '(deleted, status, view_count)', idx_job_salary: '(deleted, status, salary_max)', idx_job_type_latest: '(deleted, status, job_type, create_time)', idx_company_jobs: '(company_id, deleted, create_time)', idx_job_trend: '(deleted, create_time)' },
  resume: { idx_resume_owner: '(user_id, deleted, id)' },
  resume_experience: { idx_experience_sort: '(resume_id, sort)' },
  job_application: { idx_application_user: '(user_id, deleted, id)', idx_application_job: '(job_id, deleted, status, id)', idx_application_admin: '(deleted, status, id)', idx_application_trend: '(deleted, create_time)' },
  favorite: { idx_favorite_user: '(user_id, id)' },
  ai_recommendation: { idx_recommendation_rank: '(user_id, match_score)' },
  ai_interview: { idx_interview_history: '(user_id, id)' },
  ai_interview_question: { idx_question_sort: '(interview_id, sort)' },
  report: { idx_report_list: '(deleted, status, id)' },
  sys_log: { idx_log_time: '(create_time)', idx_log_type_time: '(type, create_time)' },
  ai_chat_session: { idx_chat_history: '(user_id, id)' },
  ai_chat_message: { idx_chat_messages: '(session_id, id)' },
};
const checks = {
  job: { chk_job_salary: '(salary_min IS NULL OR salary_min >= 0) AND (salary_max IS NULL OR salary_max >= 0) AND (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)', chk_job_views: 'view_count IS NULL OR view_count >= 0' },
  job_skill: { chk_skill_level: 'required_level IS NULL OR required_level BETWEEN 1 AND 5', chk_skill_weight: 'weight IS NULL OR weight > 0' },
  resume: { chk_resume_completion: 'completion_score IS NULL OR completion_score BETWEEN 0 AND 100' },
  job_application: { chk_application_score: 'match_score IS NULL OR match_score BETWEEN 0 AND 100' },
  ai_resume_analysis: { chk_analysis_scores: ['overall_score','skill_score','experience_score','education_score','completeness_score'].map(c => `(${c} IS NULL OR ${c} BETWEEN 0 AND 100)`).join(' AND ') },
  ai_recommendation: { chk_recommendation_scores: ['match_score','skill_score','experience_score','education_score','semantic_score'].map(c => `(${c} IS NULL OR ${c} BETWEEN 0 AND 100)`).join(' AND ') },
  ai_interview: { chk_interview_score: 'total_score IS NULL OR total_score BETWEEN 0 AND 100', chk_interview_count: 'question_count IS NULL OR question_count >= 0' },
  ai_interview_question: { chk_question_score: 'score IS NULL OR score BETWEEN 0 AND 100' },
  ai_chat_session: { chk_chat_count: 'message_count IS NULL OR message_count >= 0' },
};
for (const table of ['sys_user','company','job','resume','job_application','report']) {
  checks[table] ??= {};
  checks[table][`chk_${table}_deleted`] = 'deleted IS NOT NULL AND deleted IN (0, 1)';
}
let schema = fs.readFileSync(path.join(root, 'sql/schema.sql'), 'utf8');
for (const table of new Set([...Object.keys(indexes), ...Object.keys(checks)])) {
  const pattern = new RegExp(`(CREATE TABLE ${table} \\([\\s\\S]*?)(\\n\\) ENGINE)`);
  schema = schema.replace(pattern, (_, body, end) => {
    for (const [name, columns] of Object.entries(indexes[table] || {})) if (!body.includes(name)) body += `,\n  KEY ${name} ${columns}`;
    for (const [name, expression] of Object.entries(checks[table] || {})) if (!body.includes(name)) body += `,\n  CONSTRAINT ${name} CHECK (${expression})`;
    return body + end;
  });
}
fs.writeFileSync(path.join(root, 'sql/schema.sql'), schema);
const sql = ['-- CareerNova 数据库增量优化，MySQL >= 8.0.16',
  '-- 不删除业务数据；重复运行跳过已有索引和约束。DDL 自动提交，失败后修复数据再重跑。',
  'USE smart_recruitment;', 'SET NAMES utf8mb4;',
  'DROP PROCEDURE IF EXISTS cn_add_db_object;', 'DELIMITER $$',
  'CREATE PROCEDURE cn_add_db_object(IN p_table VARCHAR(64), IN p_name VARCHAR(64), IN p_kind VARCHAR(20), IN p_definition TEXT)',
  'BEGIN',
  "  IF (p_kind='INDEX' AND NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=p_table AND INDEX_NAME=p_name))",
  "    OR (p_kind='CHECK' AND NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME=p_table AND CONSTRAINT_NAME=p_name)) THEN",
  "    SET @cn_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD ', IF(p_kind='INDEX', 'INDEX ', 'CONSTRAINT '), '`', p_name, '` ', p_definition);",
  '    PREPARE cn_stmt FROM @cn_ddl;', '    EXECUTE cn_stmt;', '    DEALLOCATE PREPARE cn_stmt;',
  '  END IF;', 'END$$', 'DELIMITER ;'];
for (const [table, items] of Object.entries(indexes)) for (const [name, columns] of Object.entries(items)) sql.push(`CALL cn_add_db_object('${table}', '${name}', 'INDEX', '${columns}');`);
for (const [table, items] of Object.entries(checks)) for (const [name, expression] of Object.entries(items)) sql.push(`CALL cn_add_db_object('${table}', '${name}', 'CHECK', 'CHECK (${expression})');`);
sql.push('DROP PROCEDURE cn_add_db_object;', '');
fs.writeFileSync(path.join(root, 'sql/upgrade-database.sql'), sql.join('\n'));
console.log('Updated schema.sql and generated upgrade-database.sql');

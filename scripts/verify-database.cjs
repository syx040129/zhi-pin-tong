// MYSQL_PWD must be supplied through the environment; never print credentials.
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const mysql = process.env.MYSQL_BIN || 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe';
const username = process.env.MYSQL_USERNAME || 'root';
const scratch = `careernova_check_${Date.now()}`;
function run(sql, expectFailure = false) {
  const result = spawnSync(mysql, ['-h', process.env.MYSQL_HOST || 'localhost', '-P', process.env.MYSQL_PORT || '3306', '-u', username, '--default-character-set=utf8mb4', '-N', '-B'], { input: sql, encoding: 'utf8', maxBuffer: 10 * 1024 * 1024 });
  if (expectFailure) { assert.notEqual(result.status, 0, 'Invalid data should be rejected'); assert.match(result.stderr, /3819.*violated/s); }
  else if (result.status !== 0) throw new Error(result.stderr || String(result.error));
  return result.stdout.trim();
}
function asset(name) { return fs.readFileSync(path.join(root, 'sql', name), 'utf8').replaceAll('smart_recruitment', scratch); }
const tables = ['sys_user','company','job','job_skill','resume','resume_experience','job_application','favorite','ai_resume_analysis','ai_recommendation','ai_interview','ai_interview_question','report','sys_log','ai_chat_session','ai_chat_message'];
const counts = db => run(`USE \`${db}\`;` + tables.map(t => `SELECT '${t}', COUNT(*) FROM ${t};`).join('\n'));
try {
  run(asset('schema.sql') + '\n' + asset('data.sql') + '\n' + asset('demo-data.sql'));
  const before = counts(scratch);
  run(asset('upgrade-database.sql') + '\n' + asset('demo-data.sql'));
  assert.equal(counts(scratch), before, 'Second import must leave row counts unchanged');
  const orphans = [
    ['company','user_id','sys_user'], ['job','company_id','company'], ['job_skill','job_id','job'],
    ['resume','user_id','sys_user'], ['resume_experience','resume_id','resume'],
    ['job_application','job_id','job'], ['job_application','user_id','sys_user'], ['job_application','resume_id','resume'],
    ['favorite','user_id','sys_user'], ['favorite','job_id','job'],
    ['ai_resume_analysis','resume_id','resume'], ['ai_recommendation','user_id','sys_user'], ['ai_recommendation','job_id','job'],
    ['ai_interview','user_id','sys_user'], ['ai_interview','job_id','job'], ['ai_interview_question','interview_id','ai_interview'],
    ['ai_chat_session','user_id','sys_user'], ['ai_chat_message','session_id','ai_chat_session'], ['ai_chat_message','user_id','sys_user'],
  ];
  for (const [child, column, parent] of orphans) assert.equal(run(`USE ${scratch}; SELECT COUNT(*) FROM ${child} c LEFT JOIN ${parent} p ON p.id=c.${column} WHERE c.${column} IS NOT NULL AND p.id IS NULL;`), '0', `${child}.${column} orphan`);
  assert.equal(run(`USE ${scratch}; SELECT COUNT(*) FROM job_application a JOIN resume r ON r.id=a.resume_id WHERE r.user_id<>a.user_id;`), '0');
  assert.equal(run(`USE ${scratch}; SELECT COUNT(*) FROM ai_chat_session s WHERE s.message_count<>(SELECT COUNT(*) FROM ai_chat_message m WHERE m.session_id=s.id);`), '0');
  assert.equal(run(`USE ${scratch}; SELECT COUNT(*) FROM ai_interview i WHERE i.question_count<>(SELECT COUNT(*) FROM ai_interview_question q WHERE q.interview_id=i.id);`), '0');
  for (const statement of ["UPDATE job SET salary_min=50,salary_max=10 WHERE id=1", "UPDATE resume SET completion_score=101 WHERE id=1", "UPDATE job_skill SET required_level=6 WHERE id=1", "UPDATE sys_user SET deleted=2 WHERE id=1"]) run(`USE ${scratch}; START TRANSACTION; ${statement}; ROLLBACK;`, true);
  console.log('PASS fresh initialization, repeat imports, relation ownership, chat/interview counts, four invalid-value constraints');
  console.log(before);
  console.log('Query plans:');
  console.log(run(`USE ${scratch}; EXPLAIN SELECT * FROM job WHERE deleted=0 AND status='ONLINE' ORDER BY create_time DESC LIMIT 10; EXPLAIN SELECT * FROM job_application WHERE user_id=20 AND deleted=0 ORDER BY id DESC LIMIT 10; EXPLAIN SELECT * FROM ai_chat_message WHERE session_id=2 ORDER BY id DESC LIMIT 20;`));
} finally {
  assert.match(scratch, /^careernova_check_\d+$/);
  run(`DROP DATABASE IF EXISTS \`${scratch}\`;`);
}

// Run against a local Mock-mode backend: node scripts/smoke-upgrade.cjs
const assert = require('node:assert/strict');
const base = process.env.SMOKE_API || 'http://localhost:8081/api';
const password = process.env.SMOKE_PASSWORD || '123456';
const cleanup = [];
const results = [];
async function call(path, token, method = 'GET', body, expected = 200) {
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json';
  const response = await fetch(base + path, { method, headers,
    body: body ? body instanceof FormData ? body : JSON.stringify(body) : undefined });
  const json = await response.json();
  assert.equal(json.code, expected, `${method} ${path}: ${json.message}`);
  return json.data;
}
async function login(username) { return call('/auth/login', null, 'POST', { username, password }); }
function passed(label) { results.push(label); console.log('PASS ' + label); }
(async () => {
  const admin = await login('admin');
  const company = await login('company');
  const otherCompany = await login('company2');
  const suffix = Date.now().toString().slice(-10);
  let user;
  try {
    const account = await call('/auth/register', null, 'POST', {
      username: 'qa_' + suffix, password, nickname: '升级回归测试', role: 'USER',
    });
    cleanup.push(() => call(`/admin/users/${account.id}`, admin.token, 'DELETE'));
    user = await login('qa_' + suffix);
    const jobForm = { jobName: '升级回归测试岗位 ' + suffix, jobType: '后端开发', salaryMin: 10, salaryMax: 20,
      city: '上海', education: '本科', experience: '应届', description: 'Java 开发', requirements: 'Java Spring Boot',
      skills: [{ skillName: 'Java', weight: 1 }, { skillName: 'Spring Boot', weight: 1 }] };
    const job = await call('/company/jobs', company.token, 'POST', jobForm);
    cleanup.push(() => call(`/company/jobs/${job.id}`, company.token, 'DELETE'));
    const form = { name: '原始简历', education: '本科', skills: ['Java', 'Spring Boot'], summary: '回归测试原始简介', experiences: [] };
    const resume = await call('/resume', user.token, 'POST', form);
    cleanup.push(() => call(`/resume/${resume.id}`, user.token, 'DELETE'));
    const upload = new FormData(); upload.append('file', new Blob(['姓名：解析预览\n电话：13800138000\n技能：Java Spring Boot'], { type: 'text/plain' }), 'preview.txt');
    const uploaded = await call(`/resume/${resume.id}/file`, user.token, 'POST', upload);
    const preview = await call(`/resume/${resume.id}/parse`, user.token, 'POST');
    assert.equal(preview.filled, false);
    assert.equal((await call(`/resume/${resume.id}`, user.token)).name, form.name);
    passed('解析预览不覆盖已保存简历');
    const match = await call('/ai/match', user.token, 'POST', { jobId: job.id, resumeId: resume.id });
    assert.equal(match.semanticSource, 'LOCAL'); assert.equal(match.source, 'RULE'); assert.equal(match.skillScore, 100);
    passed('匹配使用所选简历并返回实际来源与维度分数');
    await call(`/resume/${resume.id}`, otherCompany.token, 'GET', undefined, 403);
    const privatePath = uploaded.resumeFileUrl.slice(4);
    await call(privatePath, null, 'GET', undefined, 401);
    await call(privatePath, otherCompany.token, 'GET', undefined, 404);
    passed('匿名及未接收投递的企业无法获取简历附件');
    const application = await call('/applications', user.token, 'POST', { jobId: job.id, resumeId: resume.id });
    cleanup.push(() => call(`/admin/applications/${application.id}`, admin.token, 'DELETE'));
    await call('/applications', user.token, 'POST', { jobId: job.id, resumeId: resume.id }, 409);
    await call(`/company/applications/${application.id}`, otherCompany.token, 'GET', undefined, 403);
    await call(`/company/applications/${application.id}/status`, otherCompany.token, 'PUT', { status: 'HIRED' }, 403);
    await call(`/company/applications/${application.id}/status`, company.token, 'PUT', { status: 'INTERVIEW' });
    assert.equal((await call(`/applications/${application.id}`, user.token)).status, 'INTERVIEW');
    const download = await fetch(base + privatePath, { headers: { Authorization: `Bearer ${company.token}` } });
    assert.equal(download.headers.get('content-type'), 'application/octet-stream');
    assert.match(await download.text(), /Java/);
    passed('发布→投递→企业处理→求职者查看闭环与跨企业隔离');
    await call('/ai/recommendations/refresh', user.token, 'POST');
    const fresh = await call('/ai/recommendations', user.token);
    assert(fresh.find(item => item.jobId === job.id));
    await call(`/resume/${resume.id}`, user.token, 'PUT', { ...form, skills: ['Rust'] });
    const afterResumeEdit = await call('/ai/recommendations', user.token);
    assert(afterResumeEdit.every(item => item.matchedSkills.every(skill => skill.toLowerCase() === 'rust')));
    await call(`/resume/${resume.id}`, user.token, 'PUT', form);
    await call(`/company/jobs/${job.id}`, company.token, 'PUT', { ...jobForm, skills: [{ skillName: 'Rust', weight: 1 }] });
    const changed = await call('/ai/match', user.token, 'POST', { jobId: job.id, resumeId: resume.id });
    assert.equal(changed.skillScore, 0);
    const afterJobEdit = await call('/ai/recommendations', user.token);
    const editedRecommendation = afterJobEdit.find(item => item.jobId === job.id);
    if (editedRecommendation) assert.equal(editedRecommendation.skillScore, 0);
    await call(`/company/jobs/${job.id}/status`, company.token, 'PUT', { status: 'OFFLINE' });
    assert(!(await call('/ai/recommendations', user.token)).some(item => item.jobId === job.id));
    await call('/applications', user.token, 'POST', { jobId: job.id, resumeId: resume.id }, 404);
    passed('简历/岗位修改后刷新，已下架岗位不可推荐或投递');
    await call(`/admin/users/${account.id}/status`, admin.token, 'PUT', { status: 'DISABLED' });
    await call('/resume/list', user.token, 'GET', undefined, 401);
    await call(`/admin/users/${account.id}/status`, admin.token, 'PUT', { status: 'ENABLED' });
    passed('账号禁用后已签发 Token 立即失效');
  } finally {
    const failures = [];
    for (const action of cleanup.reverse()) try { await action(); } catch (error) { failures.push(error.message); }
    if (failures.length) throw new Error('测试数据清理失败: ' + failures.join('; '));
  }
  console.log(`Completed ${results.length} smoke checks; test business records cleaned up.`);
})().catch(error => { console.error(error.message); process.exitCode = 1; });

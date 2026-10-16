/*
 * 独立临时库端到端回归（不修改 smart_recruitment）。先运行 mvn package 和前端 npm run build。
 * MYSQL_CLIENT 指向 mysql 可执行文件；MYSQL_PASSWORD 默认为本机开发配置 123456。
 * NODE_PATH 可指向安装了 Playwright 的 node_modules。报告和截图输出到 backend/target。
 */
const fs = require('node:fs');
const path = require('node:path');
const net = require('node:net');
const assert = require('node:assert/strict');
const { spawn, spawnSync } = require('node:child_process');
const { chromium } = require('playwright');

const root = path.resolve(__dirname, '../..');
const target = path.join(root, 'backend/target');
const database = `careernova_verify_${Date.now()}`;
const backendPort = Number(process.env.VERIFY_BACKEND_PORT || 8081);
const frontendPort = Number(process.env.VERIFY_FRONTEND_PORT || 5175);
const apiBase = `http://127.0.0.1:${backendPort}/api`;
const uiBase = `http://127.0.0.1:${frontendPort}`;
const mysqlEnv = { ...process.env, MYSQL_PWD: process.env.MYSQL_PASSWORD || '123456' };
const checks = [];
const children = [];
const logs = [];
let browser;
let databaseCreated = false;

function sql(statement) {
  const result = spawnSync(process.env.MYSQL_CLIENT || 'mysql', [
    '--host=127.0.0.1', '--port=3306', `--user=${process.env.MYSQL_USERNAME || 'root'}`,
    '--default-character-set=utf8mb4', '--batch', '--skip-column-names',
  ], { input: statement, encoding: 'utf8', env: mysqlEnv, windowsHide: true });
  if (result.error || result.status !== 0) throw new Error(result.error?.message || result.stderr);
  return result.stdout.trim();
}

async function freePort(port) {
  await new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once('error', reject);
    server.listen(port, '127.0.0.1', () => server.close(resolve));
  });
}

function launch(command, args, cwd, env, name) {
  const log = fs.createWriteStream(path.join(target, `optimization-${name}.log`));
  logs.push(log);
  const child = spawn(command, args, { cwd, env: { ...process.env, ...env }, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
  children.push(child);
  child.stdout.pipe(log); child.stderr.pipe(log);
  child.on('error', error => log.write(error.message));
  return child;
}

async function ready(url) {
  for (let i = 0; i < 100; i++) {
    if (children.some(child => child.exitCode !== null)) throw new Error('验证服务启动失败，请检查 target/optimization-*.log');
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(3000) });
      if (response.ok) return;
    } catch {}
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  throw new Error(`服务启动超时: ${url}`);
}

async function api(endpoint, token, method = 'GET', body, expected = 200) {
  const response = await fetch(apiBase + endpoint, { method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(15000) });
  const result = await response.json();
  assert.equal(result.code, expected, `${method} ${endpoint}: ${result.message}`);
  return result.data;
}

function pass(name) { checks.push(name); console.log(`PASS ${name}`); }

async function uiChecks(user, resumeId, jobId) {
  browser = await chromium.launch({ channel: 'chrome', headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' });
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', error => { errors.push(error.message); console.error('Browser error:', error.message); });
  await page.goto(uiBase + '/login');
  await page.waitForLoadState('networkidle');
  await page.locator('input[name=username]').fill('user');
  await page.locator('input[name=password]').fill('123456');
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await page.waitForURL(uiBase + '/', { timeout: 10000 });
  await page.goto(uiBase + '/recommend');
  await page.getByRole('button', { name: '按意向推荐', exact: true }).waitFor();
  await page.locator('.rec-card').first().waitFor();
  await page.locator('.el-loading-mask').first().waitFor({ state: 'hidden' });
  await page.locator('.preference-form .el-select').click();
  await page.getByRole('option', { name: '回归测试简历', exact: true }).click();
  await page.getByPlaceholder('如：北京').fill('北京');
  await page.getByPlaceholder('如：后端开发').fill('后端开发');
  const recommendationResponse = page.waitForResponse(response => response.url().includes('/api/ai/recommendations?'));
  await page.getByRole('button', { name: '按意向推荐', exact: true }).click();
  const payload = await (await recommendationResponse).json();
  assert.equal(payload.code, 200);
  assert(payload.data.length > 0);
  assert(payload.data.every(item => item.resumeId === resumeId && item.city === '北京' && item.jobType === '后端开发'));
  await page.locator('.rec-card').first().getByText('本地规则计算', { exact: true }).waitFor();
  await page.screenshot({ path: path.join(target, 'optimization-recommend-desktop.png'), fullPage: true });
  pass('UI selects resume/preferences and shows execution source');

  await page.locator('.rec-card').first().getByRole('button', { name: '匹配分析', exact: true }).click();
  await page.waitForURL(`**/recommend/*?resumeId=${resumeId}`);
  await page.locator('.match-head .el-select').getByText('回归测试简历', { exact: true }).waitFor();
  pass('UI carries selected resume into match analysis');

  await page.route('**/api/resume/list', route => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 503, message: '模拟失败' }) }));
  await page.goto(uiBase + '/recommend');
  await page.getByText('简历加载失败', { exact: true }).waitFor();
  assert.equal(await page.getByText('还没有简历，无法进行智能匹配', { exact: true }).count(), 0);
  await page.unroute('**/api/resume/list');
  await page.getByRole('button', { name: '重新加载', exact: true }).click();
  await page.locator('.rec-card').first().waitFor();
  pass('UI distinguishes resume failure and recovers on retry');

  await page.route('**/api/ai/recommendations', route => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 503, message: '模拟失败' }) }));
  await page.reload();
  await page.getByText('推荐加载失败', { exact: true }).waitFor();
  await page.unroute('**/api/ai/recommendations');
  await page.getByRole('button', { name: '重新加载', exact: true }).click();
  await page.locator('.rec-card').first().waitFor();
  pass('UI distinguishes recommendation failure and recovers on retry');

  await page.route('**/api/applications?*', route => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 503, message: '模拟失败' }) }));
  await page.goto(uiBase + '/applications');
  await page.getByText('投递记录加载失败', { exact: true }).waitFor();
  await page.unroute('**/api/applications?*');
  await page.getByRole('button', { name: '重新加载', exact: true }).click();
  await page.locator('.app-card').first().waitFor();
  pass('UI application records retry');

  for (const width of [1440, 768, 375]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto(uiBase + '/recommend');
    await page.locator('.rec-card').first().waitFor();
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false, `recommend overflow at ${width}`);
  }
  await page.screenshot({ path: path.join(target, 'optimization-recommend-mobile.png'), fullPage: true });
  pass('UI responsive recommendation page at desktop/tablet/mobile widths');
  assert.deepEqual(errors, []);
  await context.close();

  for (const [account, routes] of [
    ['user', ['/', '/jobs', `/jobs/${jobId}`, '/resume', '/resume/analysis', '/recommend', `/recommend/${jobId}`, '/applications', '/ai/interview']],
    ['company', ['/company', '/company/profile', '/company/jobs', '/company/jobs/create', '/company/applications', '/company/candidates']],
    ['admin', ['/admin', '/admin/users', '/admin/companies', '/admin/jobs', '/admin/resumes', '/admin/applications', '/admin/reports', '/admin/logs', '/admin/settings']],
  ]) {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const page = await context.newPage();
    page.on('pageerror', error => errors.push(error.message));
    await page.goto(uiBase + '/login');
    await page.waitForLoadState('networkidle');
    await page.locator('input[name=username]').fill(account);
    await page.locator('input[name=password]').fill('123456');
    await page.getByRole('button', { name: '登录', exact: true }).click();
    await page.waitForURL(url => !url.pathname.includes('/login'));
    for (const route of routes) {
      await page.goto(uiBase + route);
      await page.waitForLoadState('networkidle');
      assert.equal(new URL(page.url()).pathname, route);
      assert.equal(await page.locator('.el-message--error').count(), 0, `${account} ${route} API error`);
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false, `${account} ${route} overflow`);
    }
    await context.close();
    pass(`UI ${account} portal routes (${routes.length})`);
  }
  assert.deepEqual(errors, []);
}

(async () => {
  try {
    await freePort(backendPort); await freePort(frontendPort);
    // 此脚本只允许创建/清理带时间戳的验证数据库。
    assert.match(database, /^careernova_verify_\d+$/);
    sql(`CREATE DATABASE ${database} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`);
    databaseCreated = true;
    for (const name of ['schema.sql', 'data.sql', 'demo-data.sql']) {
      sql(fs.readFileSync(path.join(root, 'sql', name), 'utf8').replaceAll('smart_recruitment', database));
    }
    launch('java', ['-jar', path.join(target, 'smart-recruitment-backend.jar'),
      `--server.port=${backendPort}`, '--spring.config.import=', '--ai.enabled=false', '--ai.mock=true',
      `--app.cors.allowed-origins=${uiBase}`,
      `--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/${database}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai`,
      `--spring.datasource.username=${process.env.MYSQL_USERNAME || 'root'}`,
    ], path.join(root, 'backend'), { SPRING_DATASOURCE_PASSWORD: mysqlEnv.MYSQL_PWD }, 'backend');
    launch(process.execPath, [path.join(root, 'frontend/node_modules/vite/bin/vite.js'), 'preview', '--host', '127.0.0.1', '--port', String(frontendPort), '--strictPort'],
      path.join(root, 'frontend'), { VITE_PROXY_TARGET: `http://127.0.0.1:${backendPort}` }, 'frontend');
    await ready(apiBase + '/common/health'); await ready(uiBase);
    const health = await api('/common/health'); assert.equal(health.mysql, true); assert.equal(health.ai.mode, 'MOCK');
    pass('packaged backend and production frontend start against isolated MySQL');
    const login = async username => (await api('/auth/login', null, 'POST', { username, password: '123456' })).token;
    const user = await login('user'), company = await login('company'), admin = await login('admin');
    await api('/auth/register', null, 'POST', { username: 'verify_other', password: '123456', nickname: '其他企业', role: 'COMPANY' });
    const other = await login('verify_other');
    const resume = await api('/resume', user, 'POST', { name: '回归测试简历', education: '本科', skills: ['Java', 'Spring Boot', 'MySQL'], summary: '熟悉 Java 后端开发与数据库设计。', experiences: [] });
    const job = await api('/company/jobs', company, 'POST', { jobName: '回归测试岗位', jobType: '后端开发', salaryMin: 15, salaryMax: 25, city: '北京', education: '本科', experience: '应届', description: '负责 Java 后端开发', requirements: '掌握 Spring Boot 与 MySQL', skills: [{ skillName: 'Java', weight: 2 }, { skillName: 'Spring Boot', weight: 1 }] });
    const proxiedJob = await (await fetch(uiBase + `/api/jobs/${job.id}`)).json();
    assert.equal(proxiedJob.data?.jobName, '回归测试岗位', 'preview proxy must use isolated backend');
    pass('company publishes job and user creates resume');
    const recommendations = await api(`/ai/recommendations?resumeId=${resume.id}&city=${encodeURIComponent('北京')}&jobType=${encodeURIComponent('后端开发')}&salaryMin=20&salaryMax=30`, user);
    assert(recommendations.some(item => item.jobId === job.id));
    assert(recommendations.every(item => item.resumeId === resume.id && item.city === '北京' && item.salaryMax >= 20 && item.salaryMin <= 30));
    assert(recommendations.every(item => item.source === 'RULE' && item.semanticSource === 'LOCAL'));
    await api('/ai/recommendations?salaryMin=30&salaryMax=10', user, 'GET', undefined, 400);
    await api('/ai/recommendations?resumeId=-1', user, 'GET', undefined, 400);
    await api('/ai/recommendations?resumeId=9999999', user, 'GET', undefined, 404);
    pass('personalized recommendations, provenance and invalid query validation');
    const foreignResumeId = Number(sql(`USE ${database}; SELECT id FROM resume WHERE user_id <> 2 LIMIT 1;`));
    assert(foreignResumeId > 0);
    await api(`/ai/recommendations?resumeId=${foreignResumeId}`, user, 'GET', undefined, 404);
    pass('foreign resume cannot be used for recommendation');
    await api('/ai/recommendations/refresh', user, 'POST');
    const cached = await api('/ai/recommendations', user);
    assert(cached.length > 0 && cached.every(item => item.source === 'CACHED'));
    pass('default recommendation cache remains usable');
    const before = Number(sql(`USE ${database}; SELECT view_count FROM job WHERE id=${job.id};`));
    await Promise.all(Array.from({ length: 20 }, () => api(`/jobs/${job.id}`)));
    assert.equal(Number(sql(`USE ${database}; SELECT view_count FROM job WHERE id=${job.id};`)), before + 20);
    assert.equal((await api(`/jobs/${job.id}`)).jobName, '回归测试岗位');
    pass('20 concurrent detail requests increment views without lost updates');
    await api(`/company/jobs/${job.id}/status`, company, 'PUT', { status: 'OFFLINE' });
    await api(`/jobs/${job.id}`, null, 'GET', undefined, 404);
    await api(`/jobs/${job.id}`, user, 'GET', undefined, 404);
    await api(`/jobs/${job.id}`, other, 'GET', undefined, 404);
    await api(`/jobs/${job.id}`, company); await api(`/jobs/${job.id}`, admin);
    await api(`/company/jobs/${job.id}/status`, company, 'PUT', { status: 'ONLINE' });
    pass('offline job visibility for guest/user/owner/other company/admin');
    const application = await api('/applications', user, 'POST', { jobId: job.id, resumeId: resume.id });
    await api('/applications', user, 'POST', { jobId: job.id, resumeId: resume.id }, 409);
    await api(`/company/applications/${application.id}/status`, other, 'PUT', { status: 'HIRED' }, 403);
    await api(`/company/applications/${application.id}/status`, company, 'PUT', { status: 'HIRED' }, 409);
    for (const status of ['VIEWED', 'INTERVIEW', 'HIRED', 'HIRED']) {
      await api(`/company/applications/${application.id}/status`, company, 'PUT', { status });
    }
    await api(`/company/applications/${application.id}/status`, company, 'PUT', { status: 'VIEWED' }, 409);
    assert.equal((await api(`/applications/${application.id}`, user)).status, 'HIRED');
    await api(`/company/jobs/${job.id}/status`, company, 'PUT', { status: 'OFFLINE' });
    await api(`/jobs/${job.id}`, user);
    await api(`/company/jobs/${job.id}/status`, company, 'PUT', { status: 'ONLINE' });
    pass('publish/apply/process/view workflow, duplicate prevention and historical access');
    await api(`/ai/resume/analyze/${resume.id}`, user, 'POST');
    const interview = await api('/ai/interview/start', user, 'POST', { jobId: job.id, difficulty: '简单', questionCount: 3 });
    assert(interview.questions.length > 0);
    await api('/ai/chat/ask', user, 'POST', { content: '如何改进我的简历？' });
    pass('Mock resume analysis, interview and chat remain functional');
    await uiChecks(user, resume.id, job.id);
    fs.writeFileSync(path.join(target, 'optimization-verification.json'), JSON.stringify({ passed: checks.length, checks, testedAt: new Date().toISOString() }, null, 2));
    console.log(`ALL PASSED: ${checks.length} checks`);
  } catch (error) {
    if (browser) {
      for (const context of browser.contexts()) for (const page of context.pages()) {
        console.error('UI URL:', page.url(), 'Messages:', await page.locator('.el-message,.el-form-item__error').allTextContents());
        await page.screenshot({ path: path.join(target, 'optimization-ui-failure.png'), fullPage: true }).catch(() => {});
      }
    }
    console.error(error);
    process.exitCode = 1;
  } finally {
    if (browser) await browser.close();
    for (const child of children.reverse()) {
      if (child.exitCode === null) {
        child.kill();
        await new Promise(resolve => { child.once('exit', resolve); setTimeout(resolve, 3000); });
      }
    }
    logs.forEach(log => log.end());
    if (databaseCreated) { assert.match(database, /^careernova_verify_\d+$/); sql(`DROP DATABASE ${database};`); console.log('Temporary database removed'); }
  }
})();

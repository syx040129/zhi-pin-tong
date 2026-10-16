/*
 * 企业搜索端到端验证：先 mvn package、前端 npm run build，再执行本脚本。
 * 需要 MySQL CLI 和 Playwright（可用 MYSQL_CLIENT / NODE_PATH 指定位置）。
 * 仅创建并清理 careernova_search_verify_<时间戳> 临时数据库，不修改开发库。
 */
const fs = require('node:fs');
const path = require('node:path');
const net = require('node:net');
const assert = require('node:assert/strict');
const { spawn, spawnSync } = require('node:child_process');
const { chromium } = require('playwright');

const root = path.resolve(__dirname, '../..');
const target = path.join(root, 'backend/target');
const database = `careernova_search_verify_${Date.now()}`;
const backendPort = Number(process.env.VERIFY_BACKEND_PORT || 8082);
const frontendPort = Number(process.env.VERIFY_FRONTEND_PORT || 5176);
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
  const log = fs.createWriteStream(path.join(target, `company-search-${name}.log`));
  logs.push(log);
  const child = spawn(command, args, { cwd, env: { ...process.env, ...env }, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
  children.push(child);
  child.stdout.pipe(log); child.stderr.pipe(log);
  child.on('error', error => log.write(error.message));
}

async function ready(url) {
  const deadline = Date.now() + 90000;
  while (Date.now() < deadline) {
    try { if ((await fetch(url)).ok) return; } catch {}
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  throw new Error(`服务启动超时: ${url}`);
}

async function api(endpoint, expected = 200) {
  const response = await fetch(apiBase + endpoint, { signal: AbortSignal.timeout(15000) });
  const result = await response.json();
  assert.equal(result.code, expected, `${endpoint}: ${result.message}`);
  return result.data;
}

function pass(name) { checks.push(name); console.log(`PASS ${name}`); }

function fixtures() {
  const statements = [];
  const company = (id, name, status = 'ACTIVE', deleted = 0) => statements.push(
    `INSERT INTO company (id,user_id,company_name,industry,scale,description,status,deleted) VALUES (${id},${id},'${name}','验收行业','20-99人','企业搜索验证介绍','${status}',${deleted});`);
  const job = (id, companyId, status = 'ONLINE', deleted = 0) => statements.push(
    `INSERT INTO job (id,company_id,job_name,job_type,salary_min,salary_max,city,education,experience,status,deleted) VALUES (${id},${companyId},'平台工程师${id}','后端开发',15,25,'上海','本科','1-3年','${status}',${deleted});`);
  company(9001, '搜索验收甲企业');
  company(9002, '搜索验收甲企业分部');
  company(9003, '搜索验收待招聘企业');
  company(9004, '搜索验收禁用企业', 'DISABLED');
  company(9005, '搜索验收删除企业', 'ACTIVE', 1);
  job(9101, 9001); job(9102, 9001, 'OFFLINE'); job(9103, 9001, 'ONLINE', 1);
  job(9104, 9002); job(9105, 9004); job(9106, 9005);
  job(9107, 9003, 'OFFLINE'); job(9108, 9003, 'ONLINE', 1);
  for (let i = 0; i < 10; i++) job(9200 + i, 9001);
  for (let i = 0; i < 14; i++) { company(9010 + i, `分页验收企业${i}`); job(9300 + i, 9010 + i); }
  statements.push("INSERT INTO job_skill (job_id,skill_name) VALUES (9101,'验收技能Omega'),(9101,'验收技能OmegaPlus');");
  sql(`USE ${database};\n` + statements.join('\n'));
}

async function apiChecks() {
  const list = await api('/companies?keyword=搜索验收');
  assert.deepEqual(list.records.map(c => c.id), [9002, 9001]);
  assert.deepEqual(list.records.map(c => c.jobCount), [1, 11]);
  pass('Company name search hides disabled/deleted/non-hiring companies and counts only online jobs');

  const includeEmpty = await api('/companies?keyword=搜索验收&includeInactiveHiring=true');
  assert.deepEqual(includeEmpty.records.map(c => c.id), [9003, 9002, 9001]);
  assert.equal(includeEmpty.records[0].jobCount, 0);
  pass('Include non-hiring option retains normal companies with zero online jobs');

  const filtered = await api('/companies?keyword=分页验收&industry=验收行业&scale=20-99人&size=3&page=2');
  assert.equal(filtered.total, 14); assert.equal(filtered.page, 2); assert.equal(filtered.records.length, 3);
  assert.deepEqual(filtered.records.map(c => c.id), [9020, 9019, 9018]);
  assert.equal((await api('/companies?keyword=搜索验收&industry=不存在的行业')).total, 0);
  assert.equal((await api('/companies?keyword=搜索验收&scale=不存在的规模')).total, 0);
  assert.equal((await api('/companies?keyword=分页验收&page=100')).records.length, 0);
  pass('Company filters combine with AND; pagination has stable totals and empty pages');

  const detail = await api('/companies/9001');
  assert.equal(detail.jobCount, 11);
  for (const company of [...list.records, detail]) {
    for (const field of ['userId', 'username', 'contactPhone', 'status', 'deleted']) assert.equal(field in company, false);
  }
  for (const id of [9004, 9005, 99999999]) await api(`/companies/${id}`, 404);
  pass('Public detail and list omit account fields; disabled/deleted/missing details return 404');

  const filters = await api('/companies/filters');
  assert(filters.industries.includes('验收行业')); assert(filters.scales.includes('20-99人'));
  for (const query of ['page=0', 'size=0', 'size=101']) await api(`/companies?${query}`, 400);
  await api('/jobs/page?companyId=-1', 400);
  pass('Public filter options and request validation');

  const nameJobs = await api('/jobs/page?keyword=搜索验收&size=100');
  assert.equal(nameJobs.total, 12); assert(nameJobs.records.every(j => [9001, 9002].includes(j.companyId)));
  const exact = await api('/jobs/page?keyword=搜索验收&companyId=9001&city=上海&education=本科&experience=1-3年&salaryMin=20&size=100');
  assert.equal(exact.total, 11); assert(exact.records.every(j => j.companyId === 9001));
  const jobPages = await Promise.all([1, 2].map(page => api(`/jobs/page?companyId=9001&size=8&page=${page}`)));
  const pagedIds = jobPages.flatMap(result => result.records.map(j => j.id));
  assert.equal(new Set(pagedIds).size, 11);
  assert.deepEqual([...pagedIds].sort((a, b) => a - b), exact.records.map(j => j.id).sort((a, b) => a - b));
  assert.equal((await api('/jobs/page?keyword=搜索验收&city=北京')).total, 0);
  assert.equal((await api('/jobs/page?keyword=搜索验收&salaryMin=30')).total, 0);
  for (const id of [9004, 9005]) assert.equal((await api(`/jobs/page?companyId=${id}`)).total, 0);
  pass('Job company-name matching combines with company ID/city/education/experience/salary and visibility');

  const skillJobs = await api('/jobs/page?keyword=验收技能Omega');
  assert.equal(skillJobs.total, 1); assert.equal(skillJobs.records[0].id, 9101);
  for (const endpoint of ['/companies', '/jobs/page']) {
    assert.equal((await api(endpoint + '?keyword=' + encodeURIComponent("' OR 1=1 --"))).total, 0);
  }
  pass('Skill search returns each job once; quoted SQL-like input stays a bound search value');
}

async function uiChecks() {
  browser = await chromium.launch({ channel: 'chrome', headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 }, reducedMotion: 'reduce' });
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  const visit = async route => { await page.goto(uiBase + route); await page.waitForLoadState('networkidle'); };
  const count = async (selector, expected) => {
    await page.waitForFunction(({ selector, expected }) => document.querySelectorAll(selector).length === expected, { selector, expected });
    assert.equal(await page.locator(selector).count(), expected);
  };
  await visit('/');
  await page.getByRole('textbox', { name: '职位、公司或技能关键词' }).fill('搜索验收');
  await page.getByRole('button', { name: '搜索职位', exact: true }).click();
  await page.waitForURL('**/jobs?keyword=*');
  await page.locator('.job-card__name').first().waitFor();
  assert((await page.locator('.job-card__company').evaluateAll(links => links.map(a => a.getAttribute('href'))))
    .every(href => ['/companies/9001', '/companies/9002'].includes(href)));
  await visit('/');
  await page.locator('.search-type .el-radio-button').filter({ hasText: '公司' }).click();
  assert(await page.getByRole('radio', { name: '公司', exact: true }).isChecked());
  assert.equal(await page.getByRole('combobox', { name: '工作城市' }).count(), 0);
  await page.getByRole('textbox', { name: '公司名称', exact: true }).fill('搜索验收');
  await page.getByRole('button', { name: '搜索公司', exact: true }).click();
  await page.waitForURL('**/companies?keyword=*');
  await page.locator('.company-result').first().waitFor();
  await count('.company-result', 2);
  pass('Homepage job/company switch routes searches to working result pages');

  await page.getByText('包含暂无在招职位的企业', { exact: true }).click();
  await page.waitForURL('**/companies?*includeInactiveHiring=true');
  await page.getByRole('heading', { name: '搜索验收待招聘企业', exact: true }).waitFor();
  await page.reload();
  await page.getByRole('checkbox', { name: '包含暂无在招职位的企业' }).waitFor({ state: 'attached' });
  assert(await page.getByRole('checkbox', { name: '包含暂无在招职位的企业' }).isChecked());
  await page.getByRole('heading', { name: '搜索验收待招聘企业', exact: true }).click();
  await page.waitForURL('**/companies/9003');
  await page.getByText('暂无在招职位', { exact: true }).waitFor();
  await page.goBack();
  await page.getByRole('heading', { name: '搜索验收甲企业', exact: true }).waitFor();
  assert(await page.getByRole('checkbox', { name: '包含暂无在招职位的企业' }).isChecked());
  pass('Include-empty selection survives reload/back and opens the no-jobs detail state');

  await visit('/companies?keyword=分页验收');
  await page.locator('.company-result').first().waitFor();
  await page.locator('.el-pager').getByText('2', { exact: true }).click();
  await page.waitForURL('**/companies?*page=2');
  await page.waitForLoadState('networkidle');
  await count('.company-result', 2);
  await page.reload();
  await page.locator('.company-result').first().waitFor();
  assert.equal(await page.locator('.el-pager .is-active').innerText(), '2');
  await page.getByRole('combobox', { name: '企业行业' }).click();
  await page.getByRole('option', { name: '验收行业', exact: true }).click();
  await page.waitForURL(url => url.searchParams.get('industry') === '验收行业' && !url.searchParams.has('page'));
  await page.waitForLoadState('networkidle');
  await count('.company-result', 12);
  pass('Company pagination persists on reload; changing industry resets page one');

  await visit('/companies/9001');
  await page.locator('.position-card').first().waitFor();
  await count('.position-card', 8);
  await page.locator('.el-pager').getByText('2', { exact: true }).click();
  await page.waitForURL('**/companies/9001?page=2');
  await page.waitForLoadState('networkidle');
  await count('.position-card', 3);
  await page.getByRole('link', { name: '筛选企业职位' }).click();
  await page.waitForURL('**/jobs?companyId=9001');
  await page.locator('.job-card__name').first().waitFor();
  await page.locator('.filter-card .el-select').first().click();
  await page.getByRole('option', { name: '上海', exact: true }).click();
  await page.waitForURL(url => url.searchParams.get('city') === '上海' && url.searchParams.get('companyId') === '9001');
  await page.locator('.job-card__name').first().waitFor();
  assert((await page.locator('.job-card__company').evaluateAll(links => links.map(a => a.getAttribute('href'))))
    .every(href => href === '/companies/9001'));
  await page.locator('.job-card__company').first().click();
  await page.waitForURL('**/companies/9001');
  pass('Company job pagination and exact company filter survive further job filtering');

  await page.route('**/api/companies?**', r => r.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 503, message: '模拟企业加载失败' }) }));
  await visit('/companies');
  await page.getByText('企业加载失败，请重新尝试', { exact: true }).waitFor();
  await page.unroute('**/api/companies?**');
  await page.getByRole('button', { name: '重新加载', exact: true }).click();
  await page.locator('.company-result').first().waitFor();
  await visit('/companies?keyword=无匹配企业XYZ');
  await page.getByText('没有找到符合条件的企业', { exact: true }).waitFor();
  await page.getByRole('button', { name: '清空筛选条件' }).click();
  await page.locator('.company-result').first().waitFor();
  await page.route('**/api/jobs/page?**', r => r.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 503, message: '模拟职位加载失败' }) }));
  await visit('/companies/9001');
  await page.getByText('职位加载失败，请重新尝试', { exact: true }).waitFor();
  await page.unroute('**/api/jobs/page?**');
  await page.getByRole('button', { name: '重新加载职位' }).click();
  await page.locator('.position-card').first().waitFor();
  await visit('/companies/9004');
  await page.getByText('企业不存在、已停用或暂时无法加载', { exact: true }).waitFor();
  pass('List and company jobs distinguish failures from empty states and recover on retry');

  for (const width of [1440, 1024, 768, 375]) {
    await page.setViewportSize({ width, height: 900 });
    for (const route of ['/', '/companies', '/companies/9001', '/jobs?companyId=9001']) {
      await visit(route);
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1), false, `${route}: overflow at ${width}`);
      if (route === '/companies' || route === '/companies/9001') await page.screenshot({ path: path.join(target, `company-search-${width}-${route === '/companies' ? 'list' : 'detail'}.png`), fullPage: true });
    }
  }
  await page.getByRole('button', { name: '展开导航', exact: true }).click();
  await page.locator('#mobile-navigation').getByRole('link', { name: '找公司', exact: true }).click();
  await page.waitForURL(uiBase + '/companies');
  assert.equal(await page.locator('#mobile-navigation').count(), 0);
  assert.deepEqual(errors, []);
  pass('Desktop/tablet/mobile layouts and mobile company navigation have no overflow or runtime errors');
}

(async () => {
  let failure;
  try {
    await freePort(backendPort); await freePort(frontendPort);
    assert.match(database, /^careernova_search_verify_\d+$/);
    sql(`CREATE DATABASE ${database} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`);
    databaseCreated = true;
    for (const name of ['schema.sql', 'data.sql', 'demo-data.sql']) {
      sql(fs.readFileSync(path.join(root, 'sql', name), 'utf8').replaceAll('smart_recruitment', database));
    }
    fixtures();
    launch('java', ['-jar', path.join(target, 'smart-recruitment-backend.jar'),
      `--server.port=${backendPort}`, '--spring.config.import=', '--ai.enabled=false', '--ai.mock=true',
      `--app.cors.allowed-origins=${uiBase}`,
      `--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/${database}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai`,
      `--spring.datasource.username=${process.env.MYSQL_USERNAME || 'root'}`,
    ], path.join(root, 'backend'), { SPRING_DATASOURCE_PASSWORD: mysqlEnv.MYSQL_PWD }, 'backend');
    launch(process.execPath, [path.join(root, 'frontend/node_modules/vite/bin/vite.js'), 'preview', '--host', '127.0.0.1', '--port', String(frontendPort), '--strictPort'],
      path.join(root, 'frontend'), { VITE_PROXY_TARGET: `http://127.0.0.1:${backendPort}` }, 'frontend');
    await ready(apiBase + '/common/health'); await ready(uiBase);
    await apiChecks();
    await uiChecks();
  } catch (error) {
    failure = error;
    console.error(error.stack || error.message);
    if (browser) for (const context of browser.contexts()) for (const page of context.pages()) {
      await page.screenshot({ path: path.join(target, 'company-search-failure.png'), fullPage: true }).catch(() => {});
    }
  } finally {
    if (browser) await browser.close();
    for (const child of children) child.kill();
    if (databaseCreated) {
      assert.match(database, /^careernova_search_verify_\d+$/);
      sql(`DROP DATABASE ${database};`);
    }
    for (const log of logs) log.end();
    fs.writeFileSync(path.join(target, 'company-search-verification.json'), JSON.stringify({
      passed: !failure, database, isolatedDatabaseRemoved: databaseCreated, checks,
      error: failure?.message,
    }, null, 2));
    if (failure) process.exitCode = 1;
  }
})();

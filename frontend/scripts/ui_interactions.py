"""Supplementary read-only interaction and tablet/landscape checks."""
from pathlib import Path
import argparse,json
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright
parser=argparse.ArgumentParser()
parser.add_argument('--base-url',default='http://127.0.0.1:5174')
base=parser.parse_args().base_url
out=Path(__file__).resolve().parents[2]/'docs/ui-preview'
checks=[]
with sync_playwright() as p:
    browser=p.chromium.launch(channel='chrome',headless=True)
    context=browser.new_context(viewport={'width':1440,'height':1000},reduced_motion='reduce')
    page=context.new_page()
    errors=[]
    page.on('pageerror',lambda e:errors.append(str(e)))
    def logged_in():
        page.goto(base+'/login')
        page.wait_for_load_state('networkidle')
        page.locator('input[name=username]').fill('user')
        page.locator('input[name=password]').fill('123456')
        page.get_by_role('button',name='登录',exact=True).click()
        page.wait_for_url(base+'/')
        page.wait_for_load_state('networkidle')
    page.goto(base+'/')
    page.wait_for_load_state('networkidle')
    page.keyboard.press('Tab')
    assert page.locator('.skip-link').evaluate('(e)=>e===document.activeElement')
    checks.append('keyboard skip link')
    page.goto(base+'/resume')
    page.wait_for_url('**/login?redirect=/resume')
    checks.append('guest route protection')
    page.get_by_role('button',name='登录',exact=True).click()
    page.locator('.el-form-item__error').first.wait_for()
    checks.append('login inline validation')
    logged_in()
    for width,height in [(768,1024),(844,390)]:
        page.set_viewport_size({'width':width,'height':height})
        for path in ['/','/jobs','/recommend','/resume/analysis','/ai/interview']:
            page.goto(base+path)
            page.wait_for_load_state('networkidle')
            assert urlparse(page.url).path==path
            assert not page.evaluate('document.documentElement.scrollWidth > innerWidth + 1'),path
            checks.append(f'{width}x{height} {path}')
    page.route('**/api/resume/list',lambda r:r.fulfill(status=200,content_type='application/json',body=json.dumps({'code':403,'message':'无访问权限'})))
    page.goto(base+'/resume')
    page.wait_for_load_state('networkidle')
    assert urlparse(page.url).path=='/resume'
    assert page.evaluate("Boolean(localStorage.getItem('sr_token'))")
    checks.append('403 retains login state')
    page.unroute('**/api/resume/list')
    page.route('**/api/resume/list',lambda r:r.fulfill(status=401,content_type='application/json',body=json.dumps({'code':401,'message':'登录已失效'})))
    page.reload()
    page.wait_for_url('**/login?redirect=/resume')
    assert page.evaluate("localStorage.getItem('sr_token')===null && localStorage.getItem('sr_user')===null")
    checks.append('401 clears login and preserves redirect')
    browser.close()
report={'baseUrl':base,'checks':len(checks),'passed':len(checks),'results':checks,'browserIssues':errors}
(out/'interaction-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
if errors:raise SystemExit(1)

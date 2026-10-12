"""Read-only route verification against a running frontend/backend and seeded demo accounts.
Run: python frontend/scripts/ui_smoke.py --base-url http://localhost:5173
Requires Python Playwright and Google Chrome. Does not publish jobs or submit applications.
"""
from pathlib import Path
import argparse, json
from playwright.sync_api import sync_playwright
parser=argparse.ArgumentParser()
parser.add_argument('--base-url',default='http://localhost:5173')
args=parser.parse_args()
root=Path(__file__).resolve().parents[2]
out=root/'docs'/'ui-preview'
out.mkdir(parents=True,exist_ok=True)
results=[]
issues=[]
expected_failure=[False]
with sync_playwright() as p:
    browser=p.chromium.launch(channel='chrome',headless=True)
    def make_page():
        context=browser.new_context(viewport={'width':1440,'height':1000}, reduced_motion='reduce')
        page=context.new_page()
        page.on('pageerror',lambda e:issues.append({'type':'pageerror','url':page.url,'message':str(e)}))
        page.on('console',lambda m:issues.append({'type':m.type,'url':page.url,'message':m.text}) if (m.type=='error' or 'Failed to resolve component' in m.text) and not expected_failure[0] else None)
        return context,page
    def visit(page,path,name,screenshot=False):
        page.goto(args.base_url+path)
        page.wait_for_load_state('networkidle')
        page.locator('.el-loading-mask').wait_for(state='hidden',timeout=15000) if page.locator('.el-loading-mask').count()==1 else None
        overflow=page.evaluate('document.documentElement.scrollWidth > innerWidth + 1')
        ok=not overflow and page.locator('main').count()>0 and '/login' not in page.url if path!='/login' else not overflow
        messages=page.locator('.el-message--error').all_text_contents()
        results.append({'route':path,'viewport':page.viewport_size,'ok':bool(ok) and not messages,'overflow':overflow,'apiErrors':messages,'name':name})
        (out/'verification-progress.json').write_text(json.dumps({'routes':results,'browserIssues':issues},ensure_ascii=False,indent=2),encoding='utf-8')
        if screenshot: page.screenshot(path=str(out/(name+'.png')),full_page=True)
    def login(page,account):
        page.goto(args.base_url+'/login')
        page.wait_for_load_state('networkidle')
        page.locator('input[name=username]').fill(account)
        page.locator('input[name=password]').fill('123456')
        page.get_by_role('button',name='登录',exact=True).click()
        page.wait_for_url(lambda u:'/login' not in u,timeout=15000)
        page.wait_for_load_state('networkidle')
    context,page=make_page()
    visit(page,'/','home-desktop',True)
    # Real search and URL-backed pagination.
    page.get_by_role('textbox',name='职位或技能关键词').fill('Java')
    page.get_by_role('button',name='搜索职位',exact=True).click()
    page.wait_for_url('**/jobs?keyword=Java')
    page.wait_for_load_state('networkidle')
    page.locator('.job-card__name').first.wait_for(state='visible')
    visit(page,'/jobs?page=2','jobs-page2')
    assert page.locator('.el-pager .is-active').inner_text()=='2'
    visit(page,'/jobs/1','job-detail-guest',True)
    visit(page,'/register?role=COMPANY','register-desktop',True)
    assert page.get_by_role('radio',name='企业',exact=True).is_checked()
    page.set_viewport_size({'width':375,'height':812})
    visit(page,'/','home-mobile',True)
    page.get_by_role('button',name='展开导航',exact=True).click()
    page.locator('#mobile-navigation').get_by_role('link',name='找工作',exact=True).click()
    page.wait_for_url('**/jobs')
    assert not page.locator('#mobile-navigation').count()
    # Failure and empty states are deliberately exercised without altering server data.
    expected_failure[0]=True
    page.route('**/api/jobs/page?**',lambda r:r.fulfill(status=503,content_type='application/json',body=json.dumps({'code':503,'message':'测试加载失败'})))
    page.goto(args.base_url+'/jobs')
    page.wait_for_load_state('networkidle')
    assert page.get_by_text('职位加载失败，请重新尝试',exact=True).count()==1
    page.unroute('**/api/jobs/page?**')
    expected_failure[0]=False
    page.get_by_role('button',name='重新加载',exact=True).click()
    page.wait_for_load_state('networkidle')
    page.locator('.job-card__name').first.wait_for(state='visible')
    page.goto(args.base_url+'/jobs?keyword=__no_matching_job__')
    page.wait_for_load_state('networkidle')
    assert page.locator('.el-empty').count()>0
    context.close()
    for account,paths in [
      ('user',['/','/profile','/resume','/resume/edit','/resume/analysis','/recommend','/recommend/1','/jobs','/jobs/1','/applications','/favorites','/ai/interview']),
      ('company',['/company','/company/profile','/company/jobs','/company/jobs/create','/company/applications','/company/candidates']),
      ('admin',['/admin','/admin/users','/admin/companies','/admin/jobs','/admin/resumes','/admin/applications','/admin/reports','/admin/logs','/admin/settings'])]:
        context,page=make_page()
        login(page,account)
        if account=='user':
            token=page.evaluate("localStorage.getItem('sr_token')")
            headers={'Authorization':'Bearer '+token}
            resumes=page.request.get(args.base_url+'/api/resume/list',headers=headers).json()['data']
            interviews=page.request.get(args.base_url+'/api/ai/interview/list',headers=headers).json()['data']
            paths += ['/resume/edit/'+str(resumes[0]['id'])] if resumes else []
            for interview in interviews[:2]:
                paths.append('/ai/interview/'+str(interview['id']))
                if interview['status']=='COMPLETED': paths.append('/ai/interview/'+str(interview['id'])+'/report')
        for width,height,suffix in [(1440,1000,'desktop'),(375,812,'mobile')]:
            page.set_viewport_size({'width':width,'height':height})
            for path in paths:
                name=account+'-'+path.strip('/').replace('/','-')+'-'+suffix
                visit(page,path,name,True)
            if account!='user' and width==375:
                page.get_by_role('button',name='展开导航',exact=True).click()
                assert page.locator('.workspace-sidebar').is_visible()
                page.locator('.workspace-nav a').nth(1).click()
                page.wait_for_load_state('networkidle')
                page.locator('.workspace-sidebar.is-open').wait_for(state='hidden')
        if account=='user':
            page.get_by_role('button',name='打开 AI 智能助手',exact=True).click()
            page.wait_for_load_state('networkidle')
            assert page.locator('.ai-chat__panel').is_visible()
            page.get_by_role('textbox',name='向求职助手提问').focus()
            page.keyboard.press('Escape')
            page.locator('.ai-chat__panel').wait_for(state='hidden')
        context.close()
    browser.close()
report={'baseUrl':args.base_url,'checks':len(results),'passed':sum(r['ok'] for r in results),'routes':results,'browserIssues':issues}
(out/'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'checks':report['checks'],'passed':report['passed'],'failures':[r for r in results if not r['ok']],'browserIssues':issues},ensure_ascii=False))
if report['passed']!=report['checks'] or issues: raise SystemExit(1)

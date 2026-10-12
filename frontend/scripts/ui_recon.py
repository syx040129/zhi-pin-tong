from pathlib import Path
import json
from playwright.sync_api import sync_playwright
root = Path(__file__).resolve().parents[2]
out = root / 'docs' / 'ui-preview'
out.mkdir(parents=True, exist_ok=True)
with sync_playwright() as p:
    browser = p.chromium.launch(channel='chrome', headless=True)
    page = browser.new_page(viewport={'width':1440,'height':1000}, device_scale_factor=1)
    errors=[]
    page.on('pageerror',lambda error:errors.append(str(error)))
    for path,name in [('/','home-desktop'),('/login','login-desktop'),('/register?role=COMPANY','register-desktop')]:
        page.goto('http://localhost:5173'+path)
        page.wait_for_load_state('networkidle')
        page.screenshot(path=str(out/(name+'.png')),full_page=True)
        print(name, page.locator('h1,h2').all_text_contents())
    page.goto('http://localhost:5173/login')
    page.wait_for_load_state('networkidle')
    page.locator('input[name=username]').fill('user')
    page.locator('input[name=password]').fill('123456')
    page.get_by_role('button',name='登录',exact=True).click()
    page.wait_for_url('http://localhost:5173/')
    page.wait_for_load_state('networkidle')
    for path,name in [('/recommend','recommend-desktop'),('/resume/analysis','analysis-desktop'),('/jobs','jobs-desktop')]:
        page.goto('http://localhost:5173'+path)
        page.wait_for_load_state('networkidle')
        page.screenshot(path=str(out/(name+'.png')),full_page=True)
        print(name, page.locator('h1,h2').all_text_contents())
    page.set_viewport_size({'width':375,'height':812})
    for path,name in [('/','home-mobile'),('/recommend','recommend-mobile'),('/jobs','jobs-mobile'),('/resume/analysis','analysis-mobile')]:
        page.goto('http://localhost:5173'+path)
        page.wait_for_load_state('networkidle')
        page.screenshot(path=str(out/(name+'.png')),full_page=True)
        overflow=page.evaluate('document.documentElement.scrollWidth > innerWidth')
        print(name,'overflow=',overflow)
    print('PAGE_ERRORS',json.dumps(errors,ensure_ascii=False))
    browser.close()

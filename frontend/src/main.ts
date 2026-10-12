import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { registerUi } from './plugins/ui'
import 'element-plus/dist/index.css'
import './styles/index.css'
import './styles/pages.css'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { setAuthExpiredHandler } from './utils/request'

const app = createApp(App)

registerUi(app)

const pinia = createPinia()
app.use(pinia)
app.use(router)

// 刷新页面时恢复登录态；有 token 但本地无用户信息时静默拉取最新资料
const auth = useAuthStore(pinia)
auth.init()
setAuthExpiredHandler(() => {
  const current = router.currentRoute.value
  auth.logout()
  if (current.path !== '/login') {
    void router.replace({ path: '/login', query: { redirect: current.fullPath } })
  }
})
if (auth.token && !auth.user) {
  auth.fetchMe().catch(() => undefined)
}

app.mount('#app')

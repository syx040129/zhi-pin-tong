import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { UserRole } from '@/types/api'
import type { LoginResponse, UserProfileVO } from '@/types/api'
import { me } from '@/api/auth'
import {
  clearToken,
  clearUser,
  getToken,
  getUser,
  setToken,
  setUser as persistUser,
} from '@/utils/auth'

/** 全局登录态：token 与用户信息双写到 pinia + localStorage */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const user = ref<UserProfileVO | null>(getUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const role = computed<UserRole | null>(() => user.value?.role ?? null)

  /** 各角色默认首页地址 */
  function roleHome(): string {
    if (role.value === 'COMPANY') return '/company/dashboard'
    if (role.value === 'ADMIN') return '/admin/dashboard'
    return '/'
  }

  /** 从 localStorage 恢复登录态（刷新页面时调用） */
  function init(): void {
    token.value = getToken()
    user.value = getUser()
  }

  /** 登录成功后写入 token + 用户信息 */
  function setLogin(resp: LoginResponse): void {
    token.value = resp.token
    user.value = resp.user
    setToken(resp.token)
    persistUser(resp.user)
  }

  function setUserInfo(info: UserProfileVO): void {
    user.value = info
    persistUser(info)
  }

  /** 拉取最新用户资料（如头像更新后保持导航栏同步） */
  async function fetchMe(): Promise<UserProfileVO> {
    const info = await me()
    setUserInfo(info)
    return info
  }

  function logout(): void {
    token.value = null
    user.value = null
    clearToken()
    clearUser()
  }

  return {
    token,
    user,
    isLoggedIn,
    role,
    roleHome,
    init,
    setLogin,
    setUser: setUserInfo,
    fetchMe,
    logout,
  }
})

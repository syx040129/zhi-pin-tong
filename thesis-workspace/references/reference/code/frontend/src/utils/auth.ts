import type { UserProfileVO } from '@/types/api'

/** localStorage 键名（与后端 /api/auth/me 返回的用户信息配合使用） */
const TOKEN_KEY = 'sr_token'
const USER_KEY = 'sr_user'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUser(): UserProfileVO | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserProfileVO
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export function setUser(user: UserProfileVO | null): void {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

export function clearUser(): void {
  localStorage.removeItem(USER_KEY)
}

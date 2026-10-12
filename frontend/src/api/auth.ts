import request from '@/utils/request'
import type { LoginResponse, RegisterRequest, UserProfileVO } from '@/types/api'

/** 登录：返回 token + 用户信息 */
export function login(username: string, password: string): Promise<LoginResponse> {
  return request.post<LoginResponse>('/auth/login', { username, password })
}

/** 注册（USER / COMPANY），成功返回用户信息但不含 token */
export function register(payload: RegisterRequest): Promise<UserProfileVO> {
  return request.post<UserProfileVO>('/auth/register', payload)
}

/** 当前登录用户信息 */
export function me(): Promise<UserProfileVO> {
  return request.get<UserProfileVO>('/auth/me')
}

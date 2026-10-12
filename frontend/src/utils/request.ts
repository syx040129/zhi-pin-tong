import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { clearToken, getToken } from '@/utils/auth'

/** 后端统一响应包裹 { code, message, data } */
interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/**
 * 精简版 axios 实例类型：所有方法都已解包 data，
 * 因此 request.get<T>(url) 直接 resolve 为 T。
 */
export interface Request {
  <T = unknown>(config: AxiosRequestConfig): Promise<T>
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const service = axios.create({
  // 允许通过 VITE_API_BASE 指定网关地址；未配置时使用开发代理 /api
  baseURL: (import.meta.env.VITE_API_BASE ?? '') || '/api',
  timeout: 20000,
})

// 请求拦截：附加 Bearer token
service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let onAuthExpired: (() => void) | undefined

/** 由应用入口接管登录失效，同时清除 Pinia 与持久化状态。 */
export function setAuthExpiredHandler(handler: () => void): void {
  onAuthExpired = handler
}

function redirectToLogin(): void {
  if (onAuthExpired) onAuthExpired()
  else window.location.href = '/login'
}

// 同时支持 Result 业务码与 HTTP 非 2xx 状态。
service.interceptors.response.use(
  ((response: AxiosResponse<ApiResponse>) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        // 解包：业务层拿到的就是 data
        return body.data as unknown as AxiosResponse
      }
      const message = body.message || '请求失败，请稍后重试'
      if (body.code === 401) {
        clearToken()
        ElMessage.error(message)
        redirectToLogin()
      } else {
        ElMessage.error(message)
      }
      return Promise.reject(new Error(message))
    }
    // 非 Result 包裹（理论不会出现），原样透传
    return response
  }) as (response: AxiosResponse<ApiResponse>) => AxiosResponse | Promise<AxiosResponse>,
  (error) => {
    // 网络层 / HTTP 非 2xx 错误
    const status = error?.response?.status as number | undefined
    const message =
      error?.response?.data?.message || error?.message || '网络异常，请检查网络后重试'
    ElMessage.error(message)
    if (status === 401) {
      clearToken()
      redirectToLogin()
    }
    return Promise.reject(new Error(message))
  },
)

const request = service as unknown as Request

export default request

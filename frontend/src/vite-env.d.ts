/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 可选：后端接口基础地址；留空时默认走开发代理 /api */
  readonly VITE_API_BASE?: string
  /** AI 请求等待时间（毫秒），默认 120000 */
  readonly VITE_AI_TIMEOUT_MS?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

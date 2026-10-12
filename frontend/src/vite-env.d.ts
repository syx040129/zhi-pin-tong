/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 可选：后端接口基础地址；留空时默认走开发代理 /api */
  readonly VITE_API_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

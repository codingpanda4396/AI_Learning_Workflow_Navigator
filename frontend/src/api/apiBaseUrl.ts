/**
 * 与 Vite 开发代理一致：开发环境走同源 ''。
 * 生产可通过 VITE_API_BASE 覆盖；`.env.production` 里写 `VITE_API_BASE=` 表示同源 `/api`（空字符串会被 Vite 注入，必须按「已配置」处理）。
 * 未设置 VITE_API_BASE 时，生产默认直连本机后端（本地预览 JAR 用）。
 */
export function getApiBaseUrl(): string {
  const fromEnv = import.meta.env.VITE_API_BASE as string | undefined
  if (fromEnv !== undefined) {
    return fromEnv.replace(/\/$/, '')
  }
  return import.meta.env.DEV ? '' : 'http://localhost:8080'
}

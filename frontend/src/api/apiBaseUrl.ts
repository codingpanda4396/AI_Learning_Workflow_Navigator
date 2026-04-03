/**
 * 与 Vite 开发代理一致：开发环境走同源 ''，生产构建默认直连后端。
 * 可通过 VITE_API_BASE 覆盖（需在 env 中配置）。
 */
export function getApiBaseUrl(): string {
  const fromEnv = import.meta.env.VITE_API_BASE as string | undefined
  if (fromEnv !== undefined && fromEnv !== '') {
    return fromEnv.replace(/\/$/, '')
  }
  return import.meta.env.DEV ? '' : 'http://localhost:8080'
}

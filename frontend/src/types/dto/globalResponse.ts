export interface GlobalResponse<T> {
  code: string
  message: string
  data: T | null
}

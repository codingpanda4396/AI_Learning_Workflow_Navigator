/**
 * 设计系统 tokens
 */
export const colors = {
  primary: '#4F46E5',
  primaryHover: '#4338CA',
  secondary: '#6366F1',
  background: '#F8FAFC',
  cardBg: '#FFFFFF',
  textPrimary: '#0F172A',
  textSecondary: '#475569',
  border: '#E2E8F0',
  success: '#059669',
  warning: '#D97706',
  error: '#DC2626',
} as const

export const spacing = {
  page: 24,
  card: 20,
  cardLarge: 24,
  formItem: 16,
} as const

export const radius = {
  card: 16,
  input: 12,
  button: 12,
} as const

export const shadow = {
  card: '0 1px 3px rgba(0,0,0,.08)',
  cardHover: '0 4px 12px rgba(0,0,0,.1)',
} as const

export const typography = {
  title: { fontSize: 32, fontWeight: 700, lineHeight: 1.3 },
  section: { fontSize: 20, fontWeight: 600, lineHeight: 1.4 },
  body: { fontSize: 15, fontWeight: 400, lineHeight: 1.6 },
  caption: { fontSize: 13, fontWeight: 400, lineHeight: 1.5 },
} as const

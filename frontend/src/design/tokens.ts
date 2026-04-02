/**
 * 星火学航设计系统 tokens（与 style.css :root、tailwind extend 保持一致）
 */
export const colors = {
  primary: '#1E3A5F',
  primaryHover: '#152E4A',
  primaryMuted: '#E8EEF5',
  secondary: '#6366F1',
  accent: '#D97706',
  accentHover: '#B45309',
  accentMuted: '#FEF3C7',
  background: '#F1F5F9',
  cardBg: '#FFFFFF',
  textPrimary: '#0F172A',
  textSecondary: '#475569',
  textMuted: '#94A3B8',
  border: '#E2E8F0',
  borderStrong: '#CBD5E1',
  success: '#059669',
  warning: '#B45309',
  danger: '#DC2626',
  error: '#DC2626',
  /** 执行四阶段识别色（仅标签/细条，不作大背景） */
  phaseStructure: '#2563EB',
  phaseUnderstanding: '#7C3AED',
  phaseTraining: '#D97706',
  phaseReflection: '#059669',
} as const

export const phaseColors = {
  structure: colors.phaseStructure,
  understanding: colors.phaseUnderstanding,
  training: colors.phaseTraining,
  reflection: colors.phaseReflection,
} as const

export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  page: 24,
  card: 20,
  cardLarge: 24,
  formItem: 16,
} as const

export const radius = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  card: 16,
  input: 12,
  button: 12,
} as const

export const shadow = {
  card: '0 1px 2px rgba(15, 23, 42, 0.06)',
  cardHover: '0 8px 24px rgba(15, 23, 42, 0.08)',
  popover: '0 12px 32px rgba(15, 23, 42, 0.12)',
} as const

export const typography = {
  title: { fontSize: 32, fontWeight: 700, lineHeight: 1.2 },
  section: { fontSize: 20, fontWeight: 600, lineHeight: 1.35 },
  body: { fontSize: 15, fontWeight: 400, lineHeight: 1.6 },
  caption: { fontSize: 13, fontWeight: 400, lineHeight: 1.45 },
  button: { fontSize: 15, fontWeight: 600, lineHeight: 1.2 },
} as const

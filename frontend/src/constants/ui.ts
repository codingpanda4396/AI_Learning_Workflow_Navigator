export const uiTokens = {
  colors: {
    bg: '#060a14',
    bgElevated: '#0d1528',
    bgSurface: '#111c34',
    text: '#eaf0ff',
    textSecondary: '#91a4d1',
    border: '#213153',
    primary: '#3e8cff',
    primaryHover: '#63a3ff',
    primaryActive: '#2f79e8',
    primaryAlpha: 'rgba(62, 140, 255, 0.2)',
    success: '#2bc58d',
    warning: '#ffb04f',
    error: '#ff6b7a',
  },
  radius: {
    sm: '8px',
    md: '12px',
    lg: '16px',
    xl: '20px',
  },
  spacing: {
    xs: '4px',
    sm: '8px',
    md: '12px',
    lg: '16px',
    xl: '24px',
    xxl: '32px',
  },
  shadow: {
    sm: '0 6px 14px rgba(0, 0, 0, 0.16)',
    md: '0 14px 36px rgba(0, 0, 0, 0.28)',
  },
  fontSize: {
    xs: '12px',
    sm: '14px',
    md: '16px',
    lg: '20px',
    xl: '28px',
  },
} as const

export function applyUiTokens() {
  const root = document.documentElement

  root.style.setProperty('--color-bg', uiTokens.colors.bg)
  root.style.setProperty('--color-bg-elevated', uiTokens.colors.bgElevated)
  root.style.setProperty('--color-bg-surface', uiTokens.colors.bgSurface)
  root.style.setProperty('--color-text', uiTokens.colors.text)
  root.style.setProperty('--color-text-secondary', uiTokens.colors.textSecondary)
  root.style.setProperty('--color-border', uiTokens.colors.border)
  root.style.setProperty('--color-primary', uiTokens.colors.primary)
  root.style.setProperty('--color-primary-hover', uiTokens.colors.primaryHover)
  root.style.setProperty('--color-primary-active', uiTokens.colors.primaryActive)
  root.style.setProperty('--color-primary-alpha', uiTokens.colors.primaryAlpha)
  root.style.setProperty('--color-success', uiTokens.colors.success)
  root.style.setProperty('--color-warning', uiTokens.colors.warning)
  root.style.setProperty('--color-error', uiTokens.colors.error)

  root.style.setProperty('--radius-sm', uiTokens.radius.sm)
  root.style.setProperty('--radius-md', uiTokens.radius.md)
  root.style.setProperty('--radius-lg', uiTokens.radius.lg)
  root.style.setProperty('--radius-xl', uiTokens.radius.xl)

  root.style.setProperty('--space-xs', uiTokens.spacing.xs)
  root.style.setProperty('--space-sm', uiTokens.spacing.sm)
  root.style.setProperty('--space-md', uiTokens.spacing.md)
  root.style.setProperty('--space-lg', uiTokens.spacing.lg)
  root.style.setProperty('--space-xl', uiTokens.spacing.xl)
  root.style.setProperty('--space-xxl', uiTokens.spacing.xxl)

  root.style.setProperty('--shadow-sm', uiTokens.shadow.sm)
  root.style.setProperty('--shadow-md', uiTokens.shadow.md)

  root.style.setProperty('--font-size-xs', uiTokens.fontSize.xs)
  root.style.setProperty('--font-size-sm', uiTokens.fontSize.sm)
  root.style.setProperty('--font-size-md', uiTokens.fontSize.md)
  root.style.setProperty('--font-size-lg', uiTokens.fontSize.lg)
  root.style.setProperty('--font-size-xl', uiTokens.fontSize.xl)
}

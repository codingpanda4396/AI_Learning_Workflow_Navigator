/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#1E3A5F',
          hover: '#152E4A',
          muted: '#E8EEF5',
        },
        /** 琥珀金强调色：CTA、当前节点、轻量「点亮」 */
        accent: {
          DEFAULT: '#D97706',
          hover: '#B45309',
          muted: '#FEF3C7',
        },
        secondary: '#6366F1',
        background: '#F1F5F9',
        'text-primary': '#0F172A',
        'text-secondary': '#475569',
        'text-muted': '#94A3B8',
        border: '#E2E8F0',
        'border-strong': '#CBD5E1',
        'primary-hover': '#152E4A',
        success: '#059669',
        /** 语义注意态（略深于 accent，避免与主 CTA 完全同色） */
        warning: '#B45309',
        danger: '#DC2626',
        phase: {
          structure: '#2563EB',
          understanding: '#7C3AED',
          training: '#D97706',
          reflection: '#059669',
        },
      },
      borderRadius: {
        sm: '8px',
        md: '12px',
        lg: '16px',
        xl: '20px',
        card: '16px',
        input: '12px',
      },
      boxShadow: {
        card: '0 1px 2px rgba(15, 23, 42, 0.06)',
        'card-hover': '0 8px 24px rgba(15, 23, 42, 0.08)',
        popover: '0 12px 32px rgba(15, 23, 42, 0.12)',
      },
      maxWidth: {
        content: '1200px',
        execution: '1280px',
      },
    },
  },
  plugins: [],
}

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#1E3A5F',
          hover: '#152e4a',
          muted: '#E8EEF5',
        },
        accent: {
          DEFAULT: '#EA580C',
          hover: '#C2410C',
        },
        secondary: '#6366F1',
        background: '#F1F5F9',
        'text-primary': '#0F172A',
        'text-secondary': '#475569',
        'text-muted': '#94A3B8',
        border: '#E2E8F0',
        'border-strong': '#CBD5E1',
        'primary-hover': '#152e4a',
        success: '#059669',
        warning: '#D97706',
        danger: '#DC2626',
        phase: {
          structure: '#2563EB',
          understanding: '#7C3AED',
          training: '#EA580C',
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

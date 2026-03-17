/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#4F46E5',
          hover: '#4338CA',
        },
        secondary: '#6366F1',
        background: '#F8FAFC',
        'text-primary': '#0F172A',
        'text-secondary': '#475569',
        border: '#E2E8F0',
        'primary-hover': '#4338CA',
      },
      borderRadius: {
        card: '16px',
        input: '12px',
      },
      boxShadow: {
        card: '0 1px 3px rgba(0,0,0,.08)',
      },
    },
  },
  plugins: [],
}

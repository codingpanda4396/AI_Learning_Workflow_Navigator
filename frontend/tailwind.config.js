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
          DEFAULT: '#0891B2',
          light: '#22D3EE',
          dark: '#164E63'
        },
        accent: '#22C55E',
        background: '#ECFEFF',
        surface: '#FFFFFF',
        'surface-glass': 'rgba(255, 255, 255, 0.7)'
      },
      fontFamily: {
        heading: ['Fira Code', 'monospace'],
        body: ['Fira Sans', 'sans-serif']
      },
      boxShadow: {
        'glass': '0 8px 32px rgba(0, 0, 0, 0.1)',
        'glass-hover': '0 12px 40px rgba(0, 0, 0, 0.15)'
      }
    },
  },
  plugins: [],
}

/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'primary-black': '#000000',
        'secondary-black': '#1a1a1a',
        'accent-black': '#333333',
        'primary-red': '#dc2626',
        'secondary-red': '#b91c1c',
        'accent-red': '#ef4444',
        'light-red': '#fca5a5',
      }
    },
  },
  plugins: [],
}

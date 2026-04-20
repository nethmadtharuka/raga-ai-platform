import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const backendStatic = '../src/main/resources/static'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: backendStatic,
    emptyOutDir: true,
  },
})

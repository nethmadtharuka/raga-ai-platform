import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const backendStatic = '../src/main/resources/static'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // The click sounds live in the project's root sounds/ folder.
    fs: { allow: ['.', '../sounds'] },
    proxy: {
      '/api': {
        // Local backend from scripts/dev-backend.ps1 (or Docker on RAGA_PORT). Override with RAGA_API_TARGET.
        target: process.env.RAGA_API_TARGET ?? 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: backendStatic,
    emptyOutDir: true,
  },
})

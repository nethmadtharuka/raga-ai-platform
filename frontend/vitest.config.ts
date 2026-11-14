import { defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config'

// Component tests (jsdom). The plain-Node adapter checks in tests/ run separately via `npm test`.
export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: 'jsdom',
      include: ['src/**/*.test.tsx'],
    },
  }),
)

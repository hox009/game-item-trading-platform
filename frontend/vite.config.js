import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server proxies API calls to the gateway (8080) and AI service (8087)
// so the browser can use same-origin relative paths and avoid CORS in dev.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/assistant': {
        target: process.env.VITE_ASSISTANT_URL || 'http://localhost:8087',
        changeOrigin: true,
      },
      '/api': {
        target: process.env.VITE_GATEWAY_URL || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});

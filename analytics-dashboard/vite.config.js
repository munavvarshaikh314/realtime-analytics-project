import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return undefined
          }

          if (
            id.includes('node_modules/recharts') ||
            id.includes('node_modules/recharts-scale') ||
            id.includes('node_modules/react-smooth') ||
            id.includes('node_modules/victory-vendor') ||
            id.includes('node_modules/d3-')
          ) {
            return 'charts'
          }

          if (id.includes('node_modules/@stomp/stompjs')) {
            return 'realtime'
          }

          if (id.includes('node_modules/lucide-react')) {
            return 'icons'
          }

          if (
            id.includes('node_modules/@radix-ui') ||
            id.includes('node_modules/class-variance-authority') ||
            id.includes('node_modules/clsx') ||
            id.includes('node_modules/tailwind-merge')
          ) {
            return 'ui'
          }

          return undefined
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})

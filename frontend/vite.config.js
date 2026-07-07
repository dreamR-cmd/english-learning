 import { defineConfig } from 'vite'
 import vue from '@vitejs/plugin-vue'
 
 export default defineConfig({
   plugins: [vue()],
   server: {
     port: 3000,
     proxy: {
       '/api': {
         target: process.env.VITE_API_TARGET || 'http://localhost:8080',
         changeOrigin: true
       },
       '/swagger-ui.html': {
         target: process.env.VITE_API_TARGET || 'http://localhost:8080',
         changeOrigin: true
       },
       '/swagger-ui': {
         target: process.env.VITE_API_TARGET || 'http://localhost:8080',
         changeOrigin: true
       },
       '/v3': {
         target: process.env.VITE_API_TARGET || 'http://localhost:8080',
         changeOrigin: true
       }
     }
   }
 })

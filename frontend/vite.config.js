 import { defineConfig } from 'vite'
 import vue from '@vitejs/plugin-vue'
//代理端口
 const port = 'http://localhost:8081'
 
 export default defineConfig({
   plugins: [vue()],
   server: {
     port: 3000,
     proxy: {
       '/api': {
         target: process.env.VITE_API_TARGET || port,
         changeOrigin: true
       },
       '/swagger-ui.html': {
         target: process.env.VITE_API_TARGET || port,
         changeOrigin: true
       },
       '/swagger-ui': {
         target: process.env.VITE_API_TARGET || port,
         changeOrigin: true
       },
       '/v3': {
         target: process.env.VITE_API_TARGET || port,
         changeOrigin: true
       }
     }
   }
 })

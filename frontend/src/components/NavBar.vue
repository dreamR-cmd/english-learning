 <template>
   <nav class="navbar">
     <div class="navbar-inner">
       <div class="navbar-left">
         <router-link to="/modules" class="logo">📚 英语学习平台</router-link>
       </div>
       <div class="navbar-right">
         <span class="user-info" v-if="user">
           <span class="user-icon">👤</span>
           {{ user.nickname || user.username }}
         </span>
         <button class="btn-logout" @click="handleLogout">退出登录</button>
       </div>
     </div>
   </nav>
 </template>
 
 <script setup>
 import { computed } from 'vue'
 import { useRouter } from 'vue-router'
 
 const router = useRouter()
 const user = computed(() => {
   try {
     return JSON.parse(sessionStorage.getItem('currentUser'))
   } catch {
     return null
   }
 })
 
 function handleLogout() {
   sessionStorage.removeItem('currentUser')
   router.push('/login')
 }
 </script>
 
 <style scoped>
 .navbar {
   background: #fff;
   box-shadow: 0 2px 8px rgba(0,0,0,0.08);
   position: sticky;
   top: 0;
   z-index: 100;
 }
 .navbar-inner {
   max-width: 1200px;
   margin: 0 auto;
   padding: 0 24px;
   height: 60px;
   display: flex;
   align-items: center;
   justify-content: space-between;
 }
 .logo {
   font-size: 20px;
   font-weight: 700;
   color: #1a73e8;
   text-decoration: none;
 }
 .navbar-right {
   display: flex;
   align-items: center;
   gap: 16px;
 }
 .user-info {
   font-size: 14px;
   color: #555;
 }
 .user-icon {
   margin-right: 4px;
 }
 .btn-logout {
   padding: 6px 16px;
   border: 1px solid #ddd;
   border-radius: 6px;
   background: #fff;
   color: #666;
   cursor: pointer;
   transition: all 0.2s;
   font-size: 13px;
 }
 .btn-logout:hover {
   border-color: #1a73e8;
   color: #1a73e8;
 }
 </style>

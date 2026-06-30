 <template>
   <div class="login-page">
     <div class="login-card">
       <div class="login-header">
         <h1>📚 英语学习平台</h1>
         <p>English Learning Platform</p>
       </div>
       <div class="login-tabs">
         <button :class="{ active: isLogin }" @click="isLogin = true">登录</button>
         <button :class="{ active: !isLogin }" @click="isLogin = false">注册</button>
       </div>
       <form @submit.prevent="handleSubmit" class="login-form">
         <div class="form-group">
           <label>用户名</label>
           <input v-model="form.username" type="text" placeholder="请输入用户名" required />
         </div>
         <div class="form-group">
           <label>密码</label>
           <input v-model="form.password" type="password" placeholder="请输入密码" required />
         </div>
         <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
         <button type="submit" class="btn-submit" :disabled="loading">
           {{ loading ? '处理中...' : (isLogin ? '登录' : '注册') }}
         </button>
       </form>
     </div>
   </div>
 </template>
 
<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, register } from '../utils/api'
import { setCurrentUser } from '../utils/currentUser'
 
 const router = useRouter()
 const isLogin = ref(true)
 const loading = ref(false)
 const errorMsg = ref('')
 const form = reactive({ username: '', password: '' })
 
 async function handleSubmit() {
   loading.value = true
   errorMsg.value = ''
   try {
      const res = isLogin.value
        ? await login(form.username, form.password)
        : await register(form.username, form.password)
      if (res.data.code === 200) {
        setCurrentUser(res.data.data)
        router.push('/modules')
      } else {
       errorMsg.value = res.data.message
     }
   } catch (e) {
     errorMsg.value = e.response?.data?.message || '网络错误，请稍后重试'
   } finally {
     loading.value = false
   }
 }
 </script>
 
 <style scoped>
 .login-page {
   min-height: 100vh;
   display: flex;
   align-items: center;
   justify-content: center;
   background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
 }
 .login-card {
   background: #fff;
   border-radius: 16px;
   padding: 40px;
   width: 400px;
   max-width: 90vw;
   box-shadow: 0 20px 60px rgba(0,0,0,0.15);
 }
 .login-header {
   text-align: center;
   margin-bottom: 30px;
 }
 .login-header h1 {
   font-size: 28px;
   color: #1a73e8;
   margin-bottom: 8px;
 }
 .login-header p {
   color: #999;
   font-size: 14px;
 }
 .login-tabs {
   display: flex;
   border-bottom: 2px solid #eee;
   margin-bottom: 24px;
 }
 .login-tabs button {
   flex: 1;
   padding: 12px;
   border: none;
   background: none;
   font-size: 16px;
   color: #999;
   cursor: pointer;
   transition: all 0.2s;
 }
 .login-tabs button.active {
   color: #1a73e8;
   border-bottom: 2px solid #1a73e8;
   margin-bottom: -2px;
   font-weight: 600;
 }
 .login-form .form-group {
   margin-bottom: 20px;
 }
 .login-form label {
   display: block;
   font-size: 14px;
   color: #666;
   margin-bottom: 6px;
 }
 .login-form input {
   width: 100%;
   padding: 12px 16px;
   border: 1px solid #ddd;
   border-radius: 8px;
   font-size: 15px;
   transition: border-color 0.2s;
 }
 .login-form input:focus {
   outline: none;
   border-color: #1a73e8;
 }
 .error-msg {
   color: #e74c3c;
   font-size: 14px;
   margin-bottom: 12px;
 }
 .btn-submit {
   width: 100%;
   padding: 12px;
   background: #1a73e8;
   color: #fff;
   border: none;
   border-radius: 8px;
   font-size: 16px;
   cursor: pointer;
   transition: background 0.2s;
 }
 .btn-submit:hover { background: #1557b0; }
 .btn-submit:disabled { background: #93b8f0; cursor: not-allowed; }
 </style>

<template>
  <div class="login-page">
    <main class="login-shell">
      <section class="study-visual" aria-label="English study">
        <div class="visual-topline">
          <span>English Learning</span>
          <strong>CEFR · CET · IELTS</strong>
        </div>
        <div class="book-scene">
          <div class="book-page left-page">
            <span class="page-label">Reading</span>
            <p>The right word can open a new chapter.</p>
            <div class="sentence-lines">
              <i></i>
              <i></i>
              <i></i>
            </div>
          </div>
          <div class="book-page right-page">
            <span class="page-label">Vocabulary</span>
            <dl>
              <dt>fluent</dt>
              <dd>/ˈfluːənt/</dd>
            </dl>
            <div class="word-chips">
              <span>listen</span>
              <span>review</span>
              <span>master</span>
            </div>
          </div>
        </div>
        <div class="audio-strip">
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span></span>
          <span></span>
        </div>
      </section>

      <section class="login-panel">
        <div class="login-header">
          <p class="eyebrow">Welcome Back</p>
          <h1>英语学习平台</h1>
          <span>Read. Listen. Remember.</span>
        </div>

        <div class="login-tabs" role="tablist">
          <button type="button" :class="{ active: isLogin }" @click="isLogin = true">登录</button>
          <button type="button" :class="{ active: !isLogin }" @click="isLogin = false">注册</button>
        </div>

        <form @submit.prevent="handleSubmit" class="login-form">
          <div class="form-group">
            <label>用户名</label>
            <input v-model="form.username" type="text" placeholder="请输入用户名" autocomplete="username" required />
          </div>
          <div class="form-group">
            <label>密码</label>
            <input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              required
            />
          </div>
          <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>
          <button type="submit" class="btn-submit" :disabled="loading">
            {{ loading ? '处理中...' : (isLogin ? '登录' : '注册') }}
          </button>
        </form>
      </section>
    </main>
  </div>
</template>
 
<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, register } from '../../utils/api'
import { setCurrentUser } from '../../utils/currentUser'
 
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
        router.push(res.data.data?.roleCode === 'ADMIN' ? '/admin' : '/modules')
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
  display: grid;
  place-items: center;
  padding: 32px;
  background:
    linear-gradient(115deg, rgba(13, 148, 136, 0.12), transparent 34%),
    linear-gradient(245deg, rgba(245, 158, 11, 0.16), transparent 32%),
    #eef3f5;
}
.login-shell {
  width: min(1040px, 100%);
  min-height: 620px;
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  overflow: hidden;
  border: 1px solid #d7e0e6;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.16);
}
.study-visual {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 34px;
  color: #f8fafc;
  background:
    linear-gradient(rgba(14, 68, 78, 0.82), rgba(15, 66, 57, 0.88)),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='900' height='700' viewBox='0 0 900 700'%3E%3Crect width='900' height='700' fill='%23234f5c'/%3E%3Cpath d='M0 105C160 40 290 90 430 54s300-86 470-15v661H0z' fill='%232f766d'/%3E%3Cpath d='M72 588c105-82 206-84 326-35 158 65 292 58 430-34v181H72z' fill='%23d99f37' opacity='.78'/%3E%3Ccircle cx='738' cy='102' r='82' fill='%23f6c85f' opacity='.72'/%3E%3C/svg%3E");
  background-size: cover;
}
.visual-topline {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 13px;
}
.visual-topline span {
  color: #dff7f2;
}
.visual-topline strong {
  color: #ffe8a3;
}
.book-scene {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  align-items: stretch;
  margin: auto 0;
  transform: rotate(-1deg);
}
.book-page {
  min-height: 260px;
  padding: 26px;
  color: #17313b;
  background: #fffaf0;
  border: 1px solid rgba(83, 54, 24, 0.16);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.18);
}
.left-page {
  border-radius: 8px 3px 3px 8px;
}
.right-page {
  border-radius: 3px 8px 8px 3px;
}
.page-label {
  display: inline-block;
  margin-bottom: 18px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}
.book-page p {
  max-width: 210px;
  color: #334155;
  font-size: 24px;
  line-height: 1.28;
}
.sentence-lines {
  display: grid;
  gap: 10px;
  margin-top: 32px;
}
.sentence-lines i {
  height: 8px;
  border-radius: 4px;
  background: #d9c9a4;
}
.sentence-lines i:nth-child(2) {
  width: 82%;
}
.sentence-lines i:nth-child(3) {
  width: 58%;
}
.book-page dl {
  margin: 0 0 28px;
}
.book-page dt {
  color: #0f172a;
  font-size: 42px;
  font-weight: 800;
}
.book-page dd {
  margin-top: 4px;
  color: #64748b;
}
.word-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.word-chips span {
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  padding: 7px 10px;
  font-size: 12px;
  font-weight: 700;
}
.audio-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 42px;
}
.audio-strip span {
  width: 8px;
  border-radius: 999px;
  background: #fcd34d;
}
.audio-strip span:nth-child(1) { height: 18px; }
.audio-strip span:nth-child(2) { height: 34px; }
.audio-strip span:nth-child(3) { height: 24px; }
.audio-strip span:nth-child(4) { height: 40px; }
.audio-strip span:nth-child(5) { height: 28px; }
.audio-strip span:nth-child(6) { height: 20px; }
.audio-strip span:nth-child(7) { height: 32px; }
.login-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 52px 46px;
}
.login-header {
  margin-bottom: 30px;
}
.eyebrow {
  margin-bottom: 10px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}
.login-header h1 {
  margin-bottom: 8px;
  color: #102a43;
  font-size: 34px;
  line-height: 1.15;
}
.login-header span {
  color: #64748b;
  font-size: 15px;
}
.login-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  padding: 6px;
  margin-bottom: 24px;
  border: 1px solid #dbe4ea;
  border-radius: 8px;
  background: #f8fafc;
}
.login-tabs button {
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  padding: 10px 12px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}
.login-tabs button.active {
  background: #0f766e;
  color: #ffffff;
  box-shadow: 0 8px 18px rgba(15, 118, 110, 0.22);
}
.login-form .form-group {
  margin-bottom: 18px;
}
.login-form label {
  display: block;
  margin-bottom: 7px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}
.login-form input {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 13px 14px;
  color: #0f172a;
  font-size: 15px;
  background: #ffffff;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.login-form input:focus {
  outline: none;
  border-color: #0f766e;
  box-shadow: 0 0 0 3px rgba(15, 118, 110, 0.14);
}
.error-msg {
  margin-bottom: 12px;
  border-radius: 8px;
  background: #fef2f2;
  color: #b91c1c;
  padding: 10px 12px;
  font-size: 14px;
}
.btn-submit {
  width: 100%;
  border: none;
  border-radius: 8px;
  background: #102a43;
  color: #fff;
  padding: 13px;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}
.btn-submit:hover {
  background: #0f766e;
  transform: translateY(-1px);
}
.btn-submit:disabled {
  background: #94a3b8;
  cursor: not-allowed;
  transform: none;
}
@media (max-width: 820px) {
  .login-page {
    padding: 18px;
  }
  .login-shell {
    grid-template-columns: 1fr;
  }
  .study-visual {
    min-height: 300px;
    padding: 24px;
  }
  .book-scene {
    transform: none;
  }
  .book-page {
    min-height: 180px;
    padding: 18px;
  }
  .book-page p {
    font-size: 18px;
  }
  .book-page dt {
    font-size: 30px;
  }
  .login-panel {
    padding: 34px 24px;
  }
}
@media (max-width: 520px) {
  .book-scene {
    grid-template-columns: 1fr;
  }
  .right-page {
    display: none;
  }
  .visual-topline {
    flex-direction: column;
  }
}
</style>

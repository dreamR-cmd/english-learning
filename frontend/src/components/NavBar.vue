<template>
  <nav class="navbar">
    <div class="navbar-inner">
      <div class="navbar-left">
        <router-link to="/modules" class="logo">📚 英语学习平台</router-link>
        <router-link to="/shop" class="nav-link">商城</router-link>
      </div>

      <div class="navbar-right">
        <router-link v-if="user" to="/profile" class="user-info">
          <span class="user-icon">{{ user.avatar || '👤' }}</span>
          <span class="user-name">{{ user.nickname || user.username }}</span>
        </router-link>

        <button class="btn-logout" @click="handleLogout">退出登录</button>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { clearCurrentUser, currentUser } from '../utils/currentUser'

const router = useRouter()
const user = currentUser

function handleLogout() {
  clearCurrentUser()
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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

.navbar-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.nav-link {
  color: #555;
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
  padding: 6px 10px;
  border-radius: 999px;
  transition: all 0.2s;
}

.nav-link:hover,
.nav-link.router-link-active {
  background: #fff7ed;
  color: #b45309;
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  font-size: 14px;
  color: #555;
  text-decoration: none;
  display: flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  transition: all 0.2s;
}

.user-info:hover {
  background: #f0f7ff;
  color: #1a73e8;
}

.user-icon {
  margin-right: 6px;
  font-size: 18px;
}

.user-name {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

@media (max-width: 768px) {
  .navbar-inner {
    padding: 0 16px;
  }

  .logo {
    font-size: 16px;
  }

  .navbar-left {
    gap: 8px;
  }

  .nav-link {
    font-size: 13px;
    padding: 5px 8px;
  }

  .navbar-right {
    gap: 10px;
  }

  .user-name {
    max-width: 96px;
  }
}
</style>

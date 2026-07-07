import { createRouter, createWebHistory } from 'vue-router'
import { readStoredCurrentUser } from '../utils/currentUser'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/Login.vue')
  },
  {
    path: '/modules',
    name: 'Modules',
    component: () => import('../views/modules/Modules.vue')
  },
  {
    path: '/module/:code',
    name: 'ModuleDetail',
    component: () => import('../views/modules/ModuleDetail.vue'),
    props: true
  },
  {
    path: '/practice/words/:moduleCode',
    name: 'WordPractice',
    component: () => import('../views/practice/WordPractice.vue'),
    props: true
  },
  {
    path: '/practice/readings/:moduleCode',
    name: 'ReadingPractice',
    component: () => import('../views/practice/ReadingPractice.vue'),
    props: true
  },
  {
    path: '/practice/listenings/:moduleCode',
    name: 'ListeningPractice',
    component: () => import('../views/practice/ListeningPractice.vue'),
    props: true
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/user/Profile.vue')
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/user/Settings.vue')
  },
  {
    path: '/shop',
    name: 'Shop',
    component: () => import('../views/shop/Shop.vue')
  },
  {
    path: '/selected-readings',
    name: 'SelectedReadings',
    component: () => import('../views/reading/SelectedReadings.vue')
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/shop/Orders.vue')
  },
  {
    path: '/wrong-records',
    name: 'WrongRecords',
    component: () => import('../views/user/WrongRecords.vue')
  },
  {
    path: '/review-words',
    name: 'ReviewWords',
    component: () => import('../views/user/ReviewWords.vue')
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('../views/user/Favorites.vue')
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../views/admin/AdminDashboard.vue'),
    meta: { admin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = readStoredCurrentUser()
  if (to.name !== 'Login' && !user) {
    next({ name: 'Login' })
  } else if (to.meta.admin && user?.roleCode !== 'ADMIN') {
    next('/modules')
  } else if (to.name === 'Login' && user) {
    next(user.roleCode === 'ADMIN' ? '/admin' : '/modules')
  } else {
    next()
  }
})

export default router

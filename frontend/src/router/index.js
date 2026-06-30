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
    component: () => import('../views/Login.vue')
  },
  {
    path: '/modules',
    name: 'Modules',
    component: () => import('../views/Modules.vue')
  },
  {
    path: '/module/:code',
    name: 'ModuleDetail',
    component: () => import('../views/ModuleDetail.vue'),
    props: true
  },
  {
    path: '/practice/words/:moduleCode',
    name: 'WordPractice',
    component: () => import('../views/WordPractice.vue'),
    props: true
  },
  {
    path: '/practice/readings/:moduleCode',
    name: 'ReadingPractice',
    component: () => import('../views/ReadingPractice.vue'),
    props: true
  },
  {
    path: '/practice/listenings/:moduleCode',
    name: 'ListeningPractice',
    component: () => import('../views/ListeningPractice.vue'),
    props: true
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue')
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue')
  },
  {
    path: '/shop',
    name: 'Shop',
    component: () => import('../views/Shop.vue')
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/Orders.vue')
  },
  {
    path: '/wrong-records',
    name: 'WrongRecords',
    component: () => import('../views/WrongRecords.vue')
  },
  {
    path: '/review-words',
    name: 'ReviewWords',
    component: () => import('../views/ReviewWords.vue')
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('../views/Favorites.vue')
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
  } else {
    next()
  }
})

export default router

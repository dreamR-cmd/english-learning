 import { createRouter, createWebHistory } from 'vue-router'
 
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
   }
 ]
 
 const router = createRouter({
   history: createWebHistory(),
   routes
 })
 
 router.beforeEach((to, from, next) => {
   const user = sessionStorage.getItem('currentUser')
   if (to.name !== 'Login' && !user) {
     next({ name: 'Login' })
   } else {
     next()
   }
 })
 
 export default router

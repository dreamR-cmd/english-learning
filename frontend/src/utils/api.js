import axios from 'axios'
import { clearCurrentUser, isCurrentUserExpired, readStoredCurrentUser } from './currentUser'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  if (isCurrentUserExpired()) {
    clearCurrentUser()
    if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
    return config
  }

  const user = readStoredCurrentUser()
  if (user?.token) {
    config.headers.Authorization = `Bearer ${user.token}`
  }
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    const status = error.response?.status
    if ((status === 401 || status === 403) && error.config?.url?.startsWith('/admin')) {
      clearCurrentUser()
      if (typeof window !== 'undefined') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export function login(username, password) {
  return api.post('/auth/login', { username, password })
}

export function register(username, password) {
  return api.post('/auth/register', { username, password })
}

export function getModules() {
  return api.get('/modules')
}

export function getModuleByCode(code) {
  return api.get(`/modules/${code}`)
}

export function getWordsByModule(moduleCode) {
  return api.get(`/practice/words/${moduleCode}`)
}

export function getWordsByModuleForUser(moduleCode, userId) {
  return api.get(`/practice/words/${moduleCode}`)
}

export function getDailyWords(userId) {
  return api.get('/practice/words/daily')
}

export function getReadingsByModule(moduleCode) {
  return api.get(`/practice/readings/${moduleCode}`)
}

export function getListeningsByModule(moduleCode) {
  return api.get(`/practice/listenings/${moduleCode}`)
}

export function updateProfile(userId, nickname, dailyWordTarget) {
  return api.put('/user/profile', { nickname, dailyWordTarget })
}

export function submitWrongRecord(data) {
  return api.post('/user/wrong-records', data)
}

export function getWrongRecords(userId) {
  return api.get('/user/wrong-records')
}

export function removeWrongRecord(userId, wrongRecordId) {
  return api.delete(`/user/wrong-records/${wrongRecordId}`)
}

export function addFavorite(userId, readingId) {
  return api.post('/user/favorites', { readingId })
}

export function removeFavorite(userId, readingId) {
  return api.delete(`/user/favorites/${readingId}`)
}

export function getFavorites(userId) {
  return api.get('/user/favorites')
}

export function checkFavorite(userId, readingId) {
  return api.get('/user/favorites/check', { params: { readingId } })
}

export function markWordKnown(userId, wordId) {
  return api.post('/user/word-progress/known', { wordId })
}

export function resetWordProgress(userId, wordId) {
  return api.post('/user/word-progress/reset', { wordId })
}

export function getReviewWords(userId) {
  return api.get('/user/word-progress/review')
}

export function getShopProducts() {
  return api.get('/shop/products')
}

export function createShopOrderToken(userId, productId) {
  return api.post('/shop/order-tokens', { productId })
}

export function createShopOrder(userId, productId, requestId) {
  return api.post('/shop/orders', { productId, requestId })
}

export function createSeckillShopOrder(userId, productId, requestId) {
  return api.post('/shop/seckill-orders', { productId, requestId })
}

export function getSeckillShopOrderResult(userId, requestId) {
  return api.get('/shop/orders/result', { params: { requestId } })
}

export function getShopOrders(userId, status = 'all') {
  return api.get('/shop/orders', { params: { status } })
}

export function payShopOrder(userId, orderId) {
  return api.post(`/shop/orders/${orderId}/pay`, {})
}

export function getSelectedReadings(userId) {
  return api.get('/selected-readings')
}

export function addSelectedReadingFavorite(userId, selectedReadingId) {
  return api.post('/selected-readings/favorites', { selectedReadingId })
}

export function removeSelectedReadingFavorite(userId, selectedReadingId) {
  return api.delete(`/selected-readings/favorites/${selectedReadingId}`)
}

export function getAdminOrders() {
  return api.get('/admin/orders')
}

export function updateAdminOrderStatus(orderId, status, confirmText) {
  return api.put(`/admin/orders/${orderId}/status`, { status, confirmText })
}

export function getAdminModules() {
  return api.get('/admin/modules')
}

export function createAdminModule(data) {
  return api.post('/admin/modules', data)
}

export function updateAdminModule(moduleId, data) {
  return api.put(`/admin/modules/${moduleId}`, data)
}

export function deleteAdminModule(moduleId, confirmText) {
  return api.delete(`/admin/modules/${moduleId}`, { data: { confirmText } })
}

export function getAdminUsers() {
  return api.get('/admin/users')
}

export function updateAdminUserRole(userId, roleId, confirmText) {
  return api.put(`/admin/users/${userId}/role`, { roleId, confirmText })
}

export function deleteAdminUser(userId, confirmText) {
  return api.delete(`/admin/users/${userId}`, { data: { confirmText } })
}

export function getAdminRoles() {
  return api.get('/admin/roles')
}

export function createAdminRole(data) {
  return api.post('/admin/roles', data)
}

export function updateAdminRole(roleId, data) {
  return api.put(`/admin/roles/${roleId}`, data)
}

export function deleteAdminRole(roleId, confirmText) {
  return api.delete(`/admin/roles/${roleId}`, { data: { confirmText } })
}

export function getAdminPermissions() {
  return api.get('/admin/permissions')
}

export function getAdminRolePermissions(roleId) {
  return api.get(`/admin/roles/${roleId}/permissions`)
}

export function assignAdminRolePermissions(roleId, permissionIds, confirmText) {
  return api.put(`/admin/roles/${roleId}/permissions`, { permissionIds, confirmText })
}

export function getAdminOperationLogs() {
  return api.get('/admin/audit/operations')
}

export function getAdminPermissionChangeLogs() {
  return api.get('/admin/audit/permission-changes')
}

export default api

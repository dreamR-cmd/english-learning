import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

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
  return api.get(`/practice/words/${moduleCode}`, { params: { userId } })
}

export function getDailyWords(userId) {
  return api.get('/practice/words/daily', { params: { userId } })
}

export function getReadingsByModule(moduleCode) {
  return api.get(`/practice/readings/${moduleCode}`)
}

export function getListeningsByModule(moduleCode) {
  return api.get(`/practice/listenings/${moduleCode}`)
}

export function updateProfile(userId, nickname, dailyWordTarget) {
  return api.put('/user/profile', { userId, nickname, dailyWordTarget })
}

export function submitWrongRecord(data) {
  return api.post('/user/wrong-records', data)
}

export function getWrongRecords(userId) {
  return api.get('/user/wrong-records', { params: { userId } })
}

export function removeWrongRecord(userId, wrongRecordId) {
  return api.delete(`/user/wrong-records/${wrongRecordId}`, { params: { userId } })
}

export function addFavorite(userId, readingId) {
  return api.post('/user/favorites', { userId, readingId })
}

export function removeFavorite(userId, readingId) {
  return api.delete(`/user/favorites/${readingId}`, { params: { userId } })
}

export function getFavorites(userId) {
  return api.get('/user/favorites', { params: { userId } })
}

export function checkFavorite(userId, readingId) {
  return api.get('/user/favorites/check', { params: { userId, readingId } })
}

export function markWordKnown(userId, wordId) {
  return api.post('/user/word-progress/known', { userId, wordId })
}

export function resetWordProgress(userId, wordId) {
  return api.post('/user/word-progress/reset', { userId, wordId })
}

export function getReviewWords(userId) {
  return api.get('/user/word-progress/review', { params: { userId } })
}

export function getShopProducts() {
  return api.get('/shop/products')
}

export function createShopOrder(userId, productId) {
  return api.post('/shop/orders', { userId, productId })
}

export function getShopOrders(userId, status = 'all') {
  return api.get('/shop/orders', { params: { userId, status } })
}

export function payShopOrder(userId, orderId) {
  return api.post(`/shop/orders/${orderId}/pay`, { userId })
}

export function getSelectedReadings(userId) {
  return api.get('/selected-readings', { params: userId ? { userId } : {} })
}

export function addSelectedReadingFavorite(userId, selectedReadingId) {
  return api.post('/selected-readings/favorites', { userId, selectedReadingId })
}

export function removeSelectedReadingFavorite(userId, selectedReadingId) {
  return api.delete(`/selected-readings/favorites/${selectedReadingId}`, { params: { userId } })
}

export default api

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

export function getReadingsByModule(moduleCode) {
  return api.get(`/practice/readings/${moduleCode}`)
}

export function getListeningsByModule(moduleCode) {
  return api.get(`/practice/listenings/${moduleCode}`)
}

export function updateProfile(userId, nickname) {
  return api.put('/user/profile', { userId, nickname })
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

export default api

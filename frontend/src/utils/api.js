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
 
 export default api

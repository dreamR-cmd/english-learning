import { ref } from 'vue'

function parseCurrentUser(rawUser) {
  if (!rawUser) return null

  try {
    return JSON.parse(rawUser)
  } catch {
    return null
  }
}

export function readStoredCurrentUser() {
  if (typeof window === 'undefined') return null
  return parseCurrentUser(window.sessionStorage.getItem('currentUser'))
}

export const currentUser = ref(readStoredCurrentUser())

export function setCurrentUser(nextUser) {
  currentUser.value = nextUser || null

  if (typeof window === 'undefined') return

  if (nextUser) {
    window.sessionStorage.setItem('currentUser', JSON.stringify(nextUser))
    return
  }

  window.sessionStorage.removeItem('currentUser')
}

export function clearCurrentUser() {
  setCurrentUser(null)
}

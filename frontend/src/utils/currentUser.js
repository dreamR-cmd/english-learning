import { ref } from 'vue'

const LOGIN_EXPIRE_MS = 2 * 60 * 60 * 1000

export const currentUser = ref(null)

function parseCurrentUser(rawUser) {
  if (!rawUser) return null

  try {
    return JSON.parse(rawUser)
  } catch {
    return null
  }
}

function isExpired(user) {
  return Boolean(user?.expiresAt && Date.now() > user.expiresAt)
}

export function readStoredCurrentUser() {
  if (typeof window === 'undefined') return null

  const storedUser = parseCurrentUser(window.sessionStorage.getItem('currentUser'))
  if (!storedUser) return null

  if (isExpired(storedUser)) {
    window.sessionStorage.removeItem('currentUser')
    currentUser.value = null
    return null
  }

  return storedUser
}

currentUser.value = readStoredCurrentUser()

export function setCurrentUser(nextUser, options = {}) {
  if (typeof window === 'undefined') return

  if (nextUser) {
    const storedUser = readStoredCurrentUser()
    const mergedUser = storedUser
      ? { ...storedUser, ...nextUser }
      : nextUser
    const expiresAt = options.refreshExpiry || !storedUser?.expiresAt
      ? Date.now() + LOGIN_EXPIRE_MS
      : storedUser.expiresAt
    const userWithExpiry = { ...mergedUser, expiresAt }

    currentUser.value = userWithExpiry
    window.sessionStorage.setItem('currentUser', JSON.stringify(userWithExpiry))
    return
  }

  currentUser.value = null
  window.sessionStorage.removeItem('currentUser')
}

export function clearCurrentUser() {
  setCurrentUser(null)
}

export function isCurrentUserExpired() {
  if (typeof window === 'undefined') return false

  const storedUser = parseCurrentUser(window.sessionStorage.getItem('currentUser'))
  return Boolean(storedUser && isExpired(storedUser))
}

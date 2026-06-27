<template>
  <div class="profile-page">
    <NavBar />
    <div class="profile-content">
      <!-- User Info Card -->
      <div class="user-card">
        <div class="avatar-section">
          <div class="avatar-display" @click="showAvatarPicker = !showAvatarPicker">
            {{ currentAvatar }}
          </div>
          <div v-if="showAvatarPicker" class="avatar-picker">
            <span
              v-for="a in avatarOptions"
              :key="a"
              class="avatar-option"
              :class="{ active: a === currentAvatar }"
              @click="selectAvatar(a)"
            >{{ a }}</span>
          </div>
        </div>
        <div class="user-details">
          <div class="nickname-row">
            <template v-if="editingNickname">
              <input
                v-model="nicknameInput"
                class="nickname-input"
                maxlength="20"
                @keyup.enter="saveNickname"
              />
              <button class="btn-save" @click="saveNickname">保存</button>
              <button class="btn-cancel" @click="editingNickname = false">取消</button>
            </template>
            <template v-else>
              <span class="nickname-text">{{ user?.nickname || user?.username }}</span>
              <button class="btn-edit" @click="startEditNickname">✏️</button>
            </template>
          </div>
          <span class="username-tag">@{{ user?.username }}</span>
        </div>
      </div>

      <!-- Tab Switch -->
      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'wrong' }"
          @click="activeTab = 'wrong'"
        >
          📝 错题本
          <span v-if="wrongRecords.length" class="tab-count">{{ wrongRecords.length }}</span>
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'favorites' }"
          @click="activeTab = 'favorites'"
        >
          ⭐ 收藏夹
          <span v-if="favorites.length" class="tab-count">{{ favorites.length }}</span>
        </button>
      </div>

      <!-- Wrong Records Tab -->
      <div v-if="activeTab === 'wrong'" class="tab-content">
        <div v-if="loadingWrong" class="loading">加载错题...</div>
        <div v-else-if="wrongRecords.length === 0" class="empty">
          <div class="empty-icon">🎉</div>
          <p>暂无错题记录</p>
          <p class="empty-hint">去练习模块做几道题吧，做错的题目会自动收录到这里！</p>
        </div>
        <div v-else class="wrong-list">
          <div v-for="group in groupedWrongRecords" :key="group.moduleCode" class="wrong-group">
            <div class="group-header">
              <span>{{ moduleNameMap[group.moduleCode] || group.moduleCode.toUpperCase() }}</span>
              <span class="group-badge">{{ group.records.length }}题</span>
            </div>
            <div v-for="rec in group.records" :key="rec.id" class="wrong-item">
              <div class="wrong-item-header">
                <span class="wrong-type-badge" :class="'type-' + rec.questionType.toLowerCase()">
                  {{ typeLabel(rec.questionType) }}
                </span>
                <span v-if="rec.contentTitle" class="wrong-content-title">{{ rec.contentTitle }}</span>
                <span class="wrong-time">{{ formatTime(rec.createdAt) }}</span>
              </div>
              <p class="wrong-question">{{ rec.questionText }}</p>
              <div class="wrong-answers">
                <span class="answer-label wrong-answer">
                  ✗ 我的答案：{{ rec.userAnswer }}
                </span>
                <span class="answer-label correct-answer">
                  ✓ 正确答案：{{ rec.correctAnswer }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Favorites Tab -->
      <div v-if="activeTab === 'favorites'" class="tab-content">
        <div v-if="loadingFav" class="loading">加载收藏...</div>
        <div v-else-if="favorites.length === 0" class="empty">
          <div class="empty-icon">📚</div>
          <p>暂无收藏</p>
          <p class="empty-hint">在模块详情页的精选读物中点击 ⭐ 即可收藏喜欢的文章！</p>
        </div>
        <div v-else class="favorite-list">
          <div v-for="fav in favorites" :key="fav.id" class="favorite-item"
               @click="goToReading(fav.reading)">
            <div class="fav-main">
              <h4 class="fav-title">{{ fav.reading?.title }}</h4>
              <p class="fav-preview">{{ fav.reading?.content?.substring(0, 120) }}...</p>
              <div class="fav-meta">
                <span class="fav-module">{{ getModuleName(fav.reading) }}</span>
                <span class="fav-time">{{ formatTime(fav.createdAt) }}</span>
              </div>
            </div>
            <button class="btn-unfav" @click.stop="handleRemoveFavorite(fav.readingId)">
              取消收藏
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import {
  updateProfile,
  getWrongRecords,
  getFavorites,
  removeFavorite,
  getModules
} from '../utils/api'

const router = useRouter()

const user = computed(() => {
  try {
    return JSON.parse(sessionStorage.getItem('currentUser'))
  } catch {
    return null
  }
})

const activeTab = ref('wrong')
const currentAvatar = ref('👤')
const showAvatarPicker = ref(false)
const editingNickname = ref(false)
const nicknameInput = ref('')
const wrongRecords = ref([])
const favorites = ref([])
const loadingWrong = ref(true)
const loadingFav = ref(true)
const moduleNameMap = ref({})

const avatarOptions = [
  '👤', '😊', '🎓', '📚', '🌟', '💪', '🧠', '🐱',
  '🐶', '🦊', '🐼', '🐨', '🌻', '🍀', '🎯', '🚀'
]

const typeLabelMap = { READING: '阅读理解', LISTENING: '听力', WORD: '单词' }
function typeLabel(type) {
  return typeLabelMap[type] || type
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const groupedWrongRecords = computed(() => {
  const groups = {}
  for (const r of wrongRecords.value) {
    const key = r.moduleCode || 'unknown'
    if (!groups[key]) groups[key] = { moduleCode: key, records: [] }
    groups[key].records.push(r)
  }
  return Object.values(groups)
})

function getModuleName(reading) {
  if (!reading || !reading.module) return ''
  const mod = reading.module
  return mod.name || mod.code || ''
}

function selectAvatar(a) {
  currentAvatar.value = a
  showAvatarPicker.value = false
  try {
    const u = JSON.parse(sessionStorage.getItem('currentUser'))
    u.avatar = a
    sessionStorage.setItem('currentUser', JSON.stringify(u))
  } catch {}
}

function startEditNickname() {
  nicknameInput.value = user.value?.nickname || user.value?.username || ''
  editingNickname.value = true
}

async function saveNickname() {
  const name = nicknameInput.value.trim()
  if (!name || !user.value) return
  try {
    const res = await updateProfile(user.value.id, name)
    if (res.data.code === 200 && res.data.data) {
      const u = { ...res.data.data, avatar: currentAvatar.value }
      sessionStorage.setItem('currentUser', JSON.stringify(u))
    }
    editingNickname.value = false
  } catch (e) {
    console.error('Failed to update nickname', e)
  }
}

async function handleRemoveFavorite(readingId) {
  if (!user.value) return
  try {
    await removeFavorite(user.value.id, readingId)
    favorites.value = favorites.value.filter(f => f.readingId !== readingId)
  } catch (e) {
    console.error('Failed to remove favorite', e)
  }
}

function goToReading(reading) {
  if (!reading || !reading.module) return
  const code = reading.module.code
  router.push(`/practice/readings/${code}`)
}

onMounted(async () => {
  if (!user.value) return
  const uid = user.value.id

  // Restore avatar
  if (user.value.avatar) currentAvatar.value = user.value.avatar

  // Load module name map
  try {
    const modRes = await getModules()
    const modules = modRes.data.data || []
    for (const m of modules) {
      moduleNameMap.value[m.code] = m.name
    }
  } catch {}

  // Load wrong records
  try {
    const wrRes = await getWrongRecords(uid)
    wrongRecords.value = wrRes.data.data || []
  } catch {} finally {
    loadingWrong.value = false
  }

  // Load favorites
  try {
    const favRes = await getFavorites(uid)
    favorites.value = favRes.data.data || []
  } catch {} finally {
    loadingFav.value = false
  }
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #f0f2f5;
}
.profile-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

/* User Card */
.user-card {
  background: linear-gradient(135deg, #1a73e8 0%, #4a90d9 50%, #764ba2 100%);
  border-radius: 16px;
  padding: 32px;
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(26, 115, 232, 0.25);
}
.avatar-section {
  position: relative;
  flex-shrink: 0;
}
.avatar-display {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  cursor: pointer;
  transition: all 0.2s;
  border: 3px solid rgba(255,255,255,0.5);
}
.avatar-display:hover {
  background: rgba(255,255,255,0.35);
  transform: scale(1.05);
}
.avatar-picker {
  position: absolute;
  top: 90px;
  left: 0;
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  z-index: 10;
  min-width: 200px;
}
.avatar-option {
  font-size: 28px;
  padding: 6px;
  text-align: center;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.15s;
}
.avatar-option:hover {
  background: #f0f7ff;
}
.avatar-option.active {
  background: #e3f0ff;
  box-shadow: inset 0 0 0 2px #1a73e8;
}
.user-details {
  color: #fff;
  flex: 1;
  min-width: 0;
}
.nickname-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.nickname-text {
  font-size: 24px;
  font-weight: 700;
}
.btn-edit {
  background: rgba(255,255,255,0.2);
  border: none;
  color: #fff;
  border-radius: 6px;
  padding: 4px 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}
.btn-edit:hover {
  background: rgba(255,255,255,0.35);
}
.nickname-input {
  padding: 6px 12px;
  border: 2px solid rgba(255,255,255,0.5);
  border-radius: 8px;
  font-size: 18px;
  background: rgba(255,255,255,0.15);
  color: #fff;
  outline: none;
  width: 180px;
}
.nickname-input::placeholder {
  color: rgba(255,255,255,0.6);
}
.nickname-input:focus {
  border-color: #fff;
}
.btn-save, .btn-cancel {
  padding: 6px 14px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}
.btn-save {
  background: #fff;
  color: #1a73e8;
  font-weight: 600;
}
.btn-save:hover {
  background: #e8f0fe;
}
.btn-cancel {
  background: rgba(255,255,255,0.2);
  color: #fff;
}
.btn-cancel:hover {
  background: rgba(255,255,255,0.35);
}
.username-tag {
  font-size: 14px;
  opacity: 0.75;
}

/* Tabs */
.tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.tab-btn {
  flex: 1;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: #fff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #666;
}
.tab-btn.active {
  background: #1a73e8;
  color: #fff;
  box-shadow: 0 4px 12px rgba(26, 115, 232, 0.3);
}
.tab-count {
  background: rgba(255,255,255,0.3);
  border-radius: 12px;
  padding: 2px 10px;
  font-size: 13px;
  font-weight: 600;
}
.tab-btn.active .tab-count {
  background: rgba(255,255,255,0.3);
}

/* Tab Content */
.tab-content {
  min-height: 200px;
}
.loading {
  text-align: center;
  color: #999;
  padding: 60px;
  font-size: 15px;
}
.empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}
.empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
}
.empty p {
  font-size: 16px;
  margin-bottom: 8px;
}
.empty-hint {
  font-size: 13px !important;
  color: #bbb !important;
}

/* Wrong Records */
.wrong-group {
  margin-bottom: 24px;
}
.group-header {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  padding: 12px 16px;
  background: #fff;
  border-radius: 10px 10px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid #f0f2f5;
}
.group-badge {
  font-size: 12px;
  background: #f0f7ff;
  color: #1a73e8;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 500;
}
.wrong-item {
  background: #fff;
  padding: 16px 20px;
  border-bottom: 1px solid #f5f5f5;
}
.wrong-item:last-child {
  border-radius: 0 0 10px 10px;
  border-bottom: none;
}
.wrong-item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.wrong-type-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}
.type-reading { background: #fef3c7; color: #92400e; }
.type-listening { background: #ede9fe; color: #5b21b6; }
.type-word { background: #dbeafe; color: #1e40af; }
.wrong-content-title {
  font-size: 13px;
  color: #888;
  font-weight: 500;
}
.wrong-time {
  font-size: 12px;
  color: #bbb;
  margin-left: auto;
}
.wrong-question {
  font-size: 14px;
  color: #444;
  margin-bottom: 8px;
  line-height: 1.5;
}
.wrong-answers {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.answer-label {
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 6px;
  font-weight: 500;
}
.wrong-answer {
  background: #fef2f2;
  color: #dc2626;
}
.correct-answer {
  background: #f0fdf4;
  color: #16a34a;
}

/* Favorites */
.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.favorite-item {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 16px;
}
.favorite-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}
.fav-main {
  flex: 1;
  min-width: 0;
}
.fav-title {
  font-size: 16px;
  color: #333;
  margin-bottom: 6px;
}
.fav-preview {
  font-size: 13px;
  color: #999;
  line-height: 1.6;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.fav-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}
.fav-module {
  font-size: 12px;
  padding: 2px 8px;
  background: #f0f7ff;
  color: #1a73e8;
  border-radius: 4px;
}
.fav-time {
  font-size: 12px;
  color: #bbb;
}
.btn-unfav {
  flex-shrink: 0;
  padding: 6px 14px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: #fff;
  color: #999;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}
.btn-unfav:hover {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}
</style>

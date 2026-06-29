<template>
  <div class="profile-page">
    <NavBar />

    <div class="profile-content">
      <div class="user-card">
        <div class="avatar-section">
          <div class="avatar-display" @click="showAvatarPicker = !showAvatarPicker">
            {{ currentAvatar }}
          </div>

          <div v-if="showAvatarPicker" class="avatar-picker">
            <span
              v-for="avatar in avatarOptions"
              :key="avatar"
              class="avatar-option"
              :class="{ active: avatar === currentAvatar }"
              @click="selectAvatar(avatar)"
            >
              {{ avatar }}
            </span>
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
              <input
                v-model.number="dailyWordTargetInput"
                class="daily-target-input"
                type="number"
                min="1"
                max="100"
              />
              <button class="btn-save" type="button" @click="saveNickname">保存</button>
              <button class="btn-cancel" type="button" @click="editingNickname = false">取消</button>
            </template>
            <template v-else>
              <span class="nickname-text">{{ user?.nickname || user?.username }}</span>
              <button class="btn-edit" type="button" @click="startEditNickname">编辑</button>
            </template>
          </div>

          <span class="username-tag">@{{ user?.username }}</span>
          <span class="daily-target-text">每日单词练习：{{ user?.dailyWordTarget || 20 }} 个</span>
        </div>
      </div>

      <div class="tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'wrong' }"
          type="button"
          @click="activeTab = 'wrong'"
        >
          错题本
          <span v-if="wrongRecords.length" class="tab-count">{{ wrongRecords.length }}</span>
        </button>

        <button
          class="tab-btn"
          :class="{ active: activeTab === 'review' }"
          type="button"
          @click="activeTab = 'review'"
        >
          复习模块
          <span v-if="reviewWords.length" class="tab-count">{{ reviewWords.length }}</span>
        </button>

        <button
          class="tab-btn"
          :class="{ active: activeTab === 'favorites' }"
          type="button"
          @click="activeTab = 'favorites'"
        >
          收藏夹
          <span v-if="favorites.length" class="tab-count">{{ favorites.length }}</span>
        </button>
      </div>

      <div v-if="activeTab === 'wrong'" class="tab-content">
        <div v-if="loadingWrong" class="loading">加载错题...</div>

        <div v-else-if="wrongRecords.length === 0" class="empty">
          <div class="empty-icon">📘</div>
          <p>暂无错题记录</p>
          <p class="empty-hint">去练习模块做几道题吧，做错的题目会自动收录到这里。</p>
        </div>

        <div v-else class="wrong-list">
          <div v-for="group in groupedWrongRecords" :key="group.moduleCode" class="wrong-group">
            <div class="group-header">
              <span>{{ moduleNameMap[group.moduleCode] || formatModuleCode(group.moduleCode) }}</span>
              <span class="group-badge">{{ group.records.length }} 题</span>
            </div>

            <div
              v-for="record in group.records"
              :key="record.id"
              class="wrong-item"
              :class="{ clickable: canOpenWrongDetail(record) }"
              @click="handleWrongRecordClick(record)"
            >
              <div class="wrong-item-header">
                <span class="wrong-type-badge" :class="'type-' + String(record.questionType || '').toLowerCase()">
                  {{ typeLabel(record.questionType) }}
                </span>
                <span v-if="record.contentTitle" class="wrong-content-title">{{ record.contentTitle }}</span>
                <span class="wrong-time">{{ formatTime(record.createdAt) }}</span>
              </div>

              <p class="wrong-question">{{ record.questionText }}</p>

              <div class="wrong-item-footer">
                <div class="wrong-answers">
                  <span class="answer-label wrong-answer">我的答案：{{ record.userAnswer || '-' }}</span>
                  <span class="answer-label correct-answer">正确答案：{{ record.correctAnswer || '-' }}</span>
                </div>

                <button
                  v-if="canOpenWrongDetail(record)"
                  class="btn-view-question"
                  type="button"
                  @click.stop="openWrongDetail(record)"
                >
                  {{ detailActionLabel(record) }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'review'" class="tab-content">
        <div v-if="loadingReview" class="loading">加载复习单词...</div>

        <div v-else-if="reviewWords.length === 0" class="empty">
          <div class="empty-icon">🧠</div>
          <p>暂无复习单词</p>
          <p class="empty-hint">单词在练习中累计认识 4 次后，会自动进入这里。</p>
        </div>

        <div v-else class="review-list">
          <div
            v-for="item in reviewWords"
            :key="item.id"
            class="review-item"
          >
            <div class="review-main">
              <div class="review-header">
                <h4 class="review-word">{{ item.word?.word || item.wordId }}</h4>
                <span class="review-badge">已认识 {{ item.knownCount }}/4 次</span>
              </div>
              <p v-if="item.word?.phonetic" class="review-phonetic">{{ item.word.phonetic }}</p>
              <p class="review-meaning">{{ item.word?.meaning || '暂无释义' }}</p>
              <p v-if="item.word?.example" class="review-example">{{ item.word.example }}</p>
            </div>
            <button class="review-reset-btn" type="button" @click="markReviewWordUnknown(item)">
              不认识
            </button>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'favorites'" class="tab-content">
        <div v-if="loadingFav" class="loading">加载收藏...</div>

        <div v-else-if="favorites.length === 0" class="empty">
          <div class="empty-icon">📚</div>
          <p>暂无收藏</p>
          <p class="empty-hint">在模块详情页的精选阅读中点击收藏即可保存喜欢的文章。</p>
        </div>

        <div v-else class="favorite-list">
          <div
            v-for="favorite in favorites"
            :key="favorite.id"
            class="favorite-item"
            @click="goToReading(favorite.reading)"
          >
            <div class="fav-main">
              <h4 class="fav-title">{{ favorite.reading?.title }}</h4>
              <p class="fav-preview">{{ favorite.reading?.content?.substring(0, 120) }}...</p>
              <div class="fav-meta">
                <span class="fav-module">{{ getModuleName(favorite.reading) }}</span>
                <span class="fav-time">{{ formatTime(favorite.createdAt) }}</span>
              </div>
            </div>

            <button
              class="btn-unfav"
              type="button"
              @click.stop="handleRemoveFavorite(favorite.readingId)"
            >
              取消收藏
            </button>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="wrongDetailVisible" class="detail-overlay" @click.self="closeWrongDetail">
        <div class="detail-dialog">
          <div class="detail-header">
            <div>
              <p class="detail-kicker">{{ detailKicker }}</p>
              <h3>{{ detailTitle }}</h3>
            </div>

            <button class="detail-close" type="button" @click="closeWrongDetail">×</button>
          </div>

          <div v-if="detailLoading" class="detail-state">加载详情...</div>
          <div v-else-if="detailError" class="detail-state detail-state-error">{{ detailError }}</div>
          <div v-else-if="detailRecord" class="detail-body">
            <div v-if="detailNotice" class="detail-notice">{{ detailNotice }}</div>

            <div v-if="!detailIsWord" class="detail-summary">
              <span class="answer-label wrong-answer">我的答案：{{ detailRecord.userAnswer || '-' }}</span>
              <span class="answer-label correct-answer">正确答案：{{ detailRecord.correctAnswer || '-' }}</span>
            </div>

            <div v-if="detailIsWord" class="detail-section">
              <div class="detail-word-card-container">
                <div class="detail-word-card" :class="{ flipped: detailWordFlipped }" @click="toggleDetailWordCard">
                  <div class="detail-word-card-inner">
                    <div class="detail-word-face detail-word-front">
                      <p class="detail-word-text">{{ detailWord?.word || detailRecord.contentTitle }}</p>
                      <p v-if="detailWord?.phonetic" class="detail-word-phonetic">{{ detailWord.phonetic }}</p>
                      <p class="detail-word-tip">点击卡片查看详细信息</p>
                    </div>

                    <div class="detail-word-face detail-word-back">
                      <div class="detail-word-block">
                        <div class="detail-word-label">释义</div>
                        <p class="detail-word-meaning">{{ detailWord?.meaning || detailRecord.correctAnswer }}</p>
                      </div>

                      <div v-if="detailWord?.example" class="detail-word-block">
                        <div class="detail-word-label">例句</div>
                        <p class="detail-word-example">{{ detailWord.example }}</p>
                      </div>

                      <p class="detail-word-tip">点击卡片回到正面</p>
                    </div>
                  </div>
                </div>
              </div>

              <div class="detail-word-actions">
                <button
                  class="detail-word-btn detail-word-btn-known"
                  type="button"
                  :disabled="wordKnownLoading"
                  @click="markWordKnown"
                >
                  {{ wordKnownLoading ? '处理中...' : '认识' }}
                </button>
                <button
                  class="detail-word-btn detail-word-btn-unknown"
                  type="button"
                  @click="markWordUnknown"
                >
                  不认识
                </button>
              </div>

              <p v-if="wordMarkedUnknown" class="detail-word-feedback unknown">
                已标记为不认识，建议稍后再复习这个单词。
              </p>
            </div>

            <div v-if="detailIsListening" class="detail-section">
              <div class="detail-section-title">音频播放</div>

              <div class="detail-audio-panel">
                <audio
                  v-if="detailAudioUrl"
                  ref="detailAudioRef"
                  class="detail-audio"
                  :src="detailAudioUrl"
                  controls
                  preload="none"
                />

                <template v-else>
                  <div class="detail-audio-fallback">
                    <div class="detail-audio-copy">
                      <div class="detail-audio-title">原文朗读</div>
                      <p class="detail-audio-hint">
                        当前没有上传音频文件，已切换为浏览器语音朗读模式。
                      </p>
                    </div>

                    <div class="detail-audio-actions">
                      <button
                        class="detail-audio-btn primary"
                        type="button"
                        :disabled="!detailTranscript || !speechSupported"
                        @click="playTranscriptAudio"
                      >
                        播放
                      </button>
                      <button
                        class="detail-audio-btn"
                        type="button"
                        :disabled="speechState !== 'playing'"
                        @click="pauseTranscriptAudio"
                      >
                        暂停
                      </button>
                      <button
                        class="detail-audio-btn"
                        type="button"
                        :disabled="speechState !== 'paused'"
                        @click="resumeTranscriptAudio"
                      >
                        继续
                      </button>
                      <button
                        class="detail-audio-btn"
                        type="button"
                        :disabled="speechState === 'idle'"
                        @click="stopAudioPlayback"
                      >
                        停止
                      </button>
                    </div>

                    <p v-if="speechError" class="detail-audio-error">{{ speechError }}</p>
                    <p v-else-if="!speechSupported" class="detail-audio-error">
                      当前浏览器不支持语音朗读。
                    </p>
                  </div>
                </template>
              </div>
            </div>

            <div v-if="!detailIsWord" class="detail-section">
              <div class="detail-section-title">{{ detailPassageTitle }}</div>
              <div class="detail-passage">{{ detailPassageText || detailPassageEmptyText }}</div>
            </div>

            <div v-if="!detailIsWord" class="detail-section">
              <div class="detail-section-title">题目</div>
              <p class="detail-question">{{ detailQuestion?.q || detailRecord.questionText }}</p>

              <div v-if="detailOptions.length" class="detail-options">
                <div
                  v-for="(option, index) in detailOptions"
                  :key="index"
                  class="detail-option"
                  :class="detailOptionClass(index)"
                >
                  <span class="detail-option-label">{{ optionLetter(index) }}</span>
                  <span class="detail-option-text">{{ option }}</span>
                  <span v-if="index === detailUserAnswerIndex" class="detail-pill detail-pill-user">你的选择</span>
                  <span v-if="index === detailCorrectAnswerIndex" class="detail-pill detail-pill-correct">正确答案</span>
                </div>
              </div>

              <div v-else class="detail-empty-options">
                这条错题记录没有保留完整选项，已展示题干和答案。
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import {
  getFavorites,
  getListeningsByModule,
  getReviewWords,
  getModules,
  markWordKnown as saveWordKnownProgress,
  resetWordProgress,
  getReadingsByModule,
  getWordsByModule,
  getWrongRecords,
  removeWrongRecord,
  removeFavorite,
  updateProfile
} from '../utils/api'

const router = useRouter()

function readCurrentUser() {
  try {
    return JSON.parse(sessionStorage.getItem('currentUser'))
  } catch {
    return null
  }
}

function persistUser(nextUser) {
  user.value = nextUser
  sessionStorage.setItem('currentUser', JSON.stringify(nextUser))
}

function parseQuestions(questions) {
  try {
    return JSON.parse(questions || '[]')
  } catch {
    return []
  }
}

function normalizeText(text) {
  return String(text || '').replace(/\s+/g, ' ').trim()
}

function answerToIndex(answer) {
  const normalized = String(answer || '').trim().toUpperCase()
  if (!normalized) return -1

  const code = normalized.charCodeAt(0)
  if (code >= 65 && code <= 90) {
    return code - 65
  }

  const parsed = Number(normalized)
  return Number.isInteger(parsed) ? parsed : -1
}

function questionAnswerIndex(question) {
  const parsed = Number(question?.answer)
  return Number.isInteger(parsed) ? parsed : -1
}

function optionLetter(index) {
  return String.fromCharCode(65 + index)
}

function findMatchingQuestion(questions, record) {
  const targetText = normalizeText(record.questionText)
  const targetAnswer = answerToIndex(record.correctAnswer)

  const exactMatches = questions.filter(question => normalizeText(question?.q) === targetText)
  if (exactMatches.length === 1) {
    return exactMatches[0]
  }
  if (exactMatches.length > 1 && targetAnswer >= 0) {
    return exactMatches.find(question => questionAnswerIndex(question) === targetAnswer) || exactMatches[0]
  }

  const fuzzyMatches = questions.filter(question => {
    const currentText = normalizeText(question?.q)
    return currentText && (currentText.includes(targetText) || targetText.includes(currentText))
  })
  if (fuzzyMatches.length === 1) {
    return fuzzyMatches[0]
  }
  if (fuzzyMatches.length > 1 && targetAnswer >= 0) {
    return fuzzyMatches.find(question => questionAnswerIndex(question) === targetAnswer) || fuzzyMatches[0]
  }

  return null
}

function browserSupportsSpeech() {
  return typeof window !== 'undefined'
    && 'speechSynthesis' in window
    && 'SpeechSynthesisUtterance' in window
}

const user = ref(readCurrentUser())
const activeTab = ref('wrong')
const currentAvatar = ref('👤')
const showAvatarPicker = ref(false)
const editingNickname = ref(false)
const nicknameInput = ref('')
const dailyWordTargetInput = ref(20)
const wrongRecords = ref([])
const favorites = ref([])
const reviewWords = ref([])
const loadingWrong = ref(true)
const loadingReview = ref(true)
const loadingFav = ref(true)
const moduleNameMap = ref({})

const wrongDetailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailNotice = ref('')
const detailRecord = ref(null)
const detailQuestion = ref(null)
const detailReading = ref(null)
const detailListening = ref(null)
const detailWord = ref(null)
const detailAudioRef = ref(null)
const speechState = ref('idle')
const speechError = ref('')
const wordKnownLoading = ref(false)
const wordMarkedUnknown = ref(false)
const detailWordFlipped = ref(false)

const readingCache = new Map()
const listeningCache = new Map()
const wordCache = new Map()

const avatarOptions = [
  '👤', '😊', '🎓', '📚', '🌟', '💪', '🧠', '🐱',
  '🐶', '🦊', '🐼', '🐨', '🌻', '🍀', '🎯', '🚀'
]

const typeLabelMap = {
  READING: '阅读理解',
  LISTENING: '听力',
  WORD: '单词'
}

const speechSupported = computed(() => browserSupportsSpeech())
const detailIsReading = computed(() => detailRecord.value?.questionType === 'READING')
const detailIsListening = computed(() => detailRecord.value?.questionType === 'LISTENING')
const detailIsWord = computed(() => detailRecord.value?.questionType === 'WORD')
const detailTitle = computed(() => {
  if (detailIsWord.value) {
    return detailWord.value?.word || detailRecord.value?.contentTitle || '单词卡片'
  }

  return detailReading.value?.title
    || detailListening.value?.title
    || detailRecord.value?.contentTitle
    || typeLabel(detailRecord.value?.questionType)
})
const detailKicker = computed(() => {
  if (detailIsWord.value) return '单词错题卡片'
  return detailIsListening.value ? '听力错题详情' : '阅读错题详情'
})
const detailPassageTitle = computed(() => detailIsListening.value ? '听力原文' : '阅读原文')
const detailPassageText = computed(() => {
  if (detailIsListening.value) return detailListening.value?.transcript || ''
  return detailReading.value?.content || ''
})
const detailPassageEmptyText = computed(() => {
  return detailIsListening.value ? '未找到听力原文。' : '未找到阅读原文。'
})
const detailAudioUrl = computed(() => detailListening.value?.audioUrl || '')
const detailTranscript = computed(() => detailListening.value?.transcript || '')
const groupedWrongRecords = computed(() => {
  const groups = {}

  for (const record of wrongRecords.value) {
    const key = record.moduleCode || 'unknown'
    if (!groups[key]) {
      groups[key] = { moduleCode: key, records: [] }
    }
    groups[key].records.push(record)
  }

  return Object.values(groups)
})
const detailOptions = computed(() => {
  return Array.isArray(detailQuestion.value?.options) ? detailQuestion.value.options : []
})
const detailUserAnswerIndex = computed(() => answerToIndex(detailRecord.value?.userAnswer))
const detailCorrectAnswerIndex = computed(() => answerToIndex(detailRecord.value?.correctAnswer))

function typeLabel(type) {
  return typeLabelMap[type] || type || '题目'
}

function detailActionLabel(record) {
  if (record?.questionType === 'LISTENING') return '查看原文'
  if (record?.questionType === 'READING') return '查看原题'
  if (record?.questionType === 'WORD') return '查看卡片'
  return '查看详情'
}

function formatModuleCode(moduleCode) {
  return String(moduleCode || 'UNKNOWN').toUpperCase()
}

function formatTime(dateStr) {
  if (!dateStr) return ''

  const date = new Date(dateStr)
  const pad = value => String(value).padStart(2, '0')

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function getModuleName(reading) {
  if (!reading || !reading.module) return ''
  return reading.module.name || reading.module.code || ''
}

function selectAvatar(avatar) {
  currentAvatar.value = avatar
  showAvatarPicker.value = false

  if (user.value) {
    persistUser({ ...user.value, avatar })
  }
}

function startEditNickname() {
  nicknameInput.value = user.value?.nickname || user.value?.username || ''
  dailyWordTargetInput.value = user.value?.dailyWordTarget || 20
  editingNickname.value = true
}

async function saveNickname() {
  const nickname = nicknameInput.value.trim()
  if (!nickname || !user.value) return

  const normalizedDailyTarget = Math.max(1, Math.min(100, Number(dailyWordTargetInput.value) || 20))

  try {
    const response = await updateProfile(user.value.id, nickname, normalizedDailyTarget)
    if (response.data.code === 200 && response.data.data) {
      persistUser({ ...response.data.data, avatar: currentAvatar.value })
    }
    editingNickname.value = false
  } catch (error) {
    console.error('Failed to update nickname', error)
  }
}

async function markReviewWordUnknown(item) {
  if (!user.value || !item?.wordId) return

  try {
    await resetWordProgress(user.value.id, item.wordId)
    reviewWords.value = reviewWords.value.filter(word => word.wordId !== item.wordId)
  } catch (error) {
    console.error('Failed to reset review word', error)
  }
}

async function handleRemoveFavorite(readingId) {
  if (!user.value) return

  try {
    await removeFavorite(user.value.id, readingId)
    favorites.value = favorites.value.filter(item => item.readingId !== readingId)
  } catch (error) {
    console.error('Failed to remove favorite', error)
  }
}

function goToReading(reading) {
  if (!reading || !reading.module) return
  router.push(`/practice/readings/${reading.module.code}`)
}

function canOpenWrongDetail(record) {
  return ['READING', 'LISTENING', 'WORD'].includes(record?.questionType)
    && !!record?.moduleCode
    && !!record?.contentId
}

function handleWrongRecordClick(record) {
  if (canOpenWrongDetail(record)) {
    openWrongDetail(record)
  }
}

async function ensureReadings(moduleCode) {
  if (readingCache.has(moduleCode)) {
    return readingCache.get(moduleCode)
  }

  const response = await getReadingsByModule(moduleCode)
  const readings = response.data.data || []
  readingCache.set(moduleCode, readings)
  return readings
}

async function ensureListenings(moduleCode) {
  if (listeningCache.has(moduleCode)) {
    return listeningCache.get(moduleCode)
  }

  const response = await getListeningsByModule(moduleCode)
  const listenings = response.data.data || []
  listeningCache.set(moduleCode, listenings)
  return listenings
}

async function ensureWords(moduleCode) {
  if (wordCache.has(moduleCode)) {
    return wordCache.get(moduleCode)
  }

  const response = await getWordsByModule(moduleCode)
  const words = response.data.data || []
  wordCache.set(moduleCode, words)
  return words
}

function setMatchedQuestion(record, questions) {
  const matchedQuestion = findMatchingQuestion(questions, record)

  if (matchedQuestion) {
    detailQuestion.value = matchedQuestion
    return
  }

  detailNotice.value = '已找到原文内容，但没有精确定位到这道题，先展示记录中的题干和答案。'
  detailQuestion.value = {
    q: record.questionText,
    options: []
  }
}

async function openWrongDetail(record) {
  stopAudioPlayback()

  wrongDetailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detailNotice.value = ''
  detailRecord.value = record
  detailQuestion.value = null
  detailReading.value = null
  detailListening.value = null
  detailWord.value = null
  speechError.value = ''
  wordKnownLoading.value = false
  wordMarkedUnknown.value = false
  detailWordFlipped.value = false

  try {
    if (record.questionType === 'READING') {
      const readings = await ensureReadings(record.moduleCode)
      const matchedReading = readings.find(item => String(item.id) === String(record.contentId))

      if (!matchedReading) {
        throw new Error('没有找到这道错题对应的阅读原文。')
      }

      detailReading.value = matchedReading
      setMatchedQuestion(record, parseQuestions(matchedReading.questions))
    } else if (record.questionType === 'LISTENING') {
      const listenings = await ensureListenings(record.moduleCode)
      const matchedListening = listenings.find(item => String(item.id) === String(record.contentId))

      if (!matchedListening) {
        throw new Error('没有找到这道错题对应的听力原文。')
      }

      detailListening.value = matchedListening
      setMatchedQuestion(record, parseQuestions(matchedListening.questions))
    } else if (record.questionType === 'WORD') {
      const words = await ensureWords(record.moduleCode)
      const matchedWord = words.find(item => String(item.id) === String(record.contentId))

      if (!matchedWord) {
        throw new Error('没有找到这条错题对应的单词信息。')
      }

      detailWord.value = matchedWord
    } else {
      throw new Error('暂不支持打开这类错题详情。')
    }
  } catch (error) {
    detailError.value = error?.message || '加载详情失败，请稍后重试。'
  } finally {
    detailLoading.value = false
  }
}

function stopAudioPlayback() {
  if (detailAudioRef.value) {
    detailAudioRef.value.pause()
    detailAudioRef.value.currentTime = 0
  }

  if (browserSupportsSpeech()) {
    window.speechSynthesis.cancel()
  }

  speechState.value = 'idle'
}

function playTranscriptAudio() {
  speechError.value = ''

  if (!detailTranscript.value) {
    speechError.value = '当前没有可播放的听力原文。'
    return
  }

  if (!speechSupported.value) {
    speechError.value = '当前浏览器不支持语音朗读。'
    return
  }

  stopAudioPlayback()

  const utterance = new window.SpeechSynthesisUtterance(detailTranscript.value)
  utterance.lang = 'en-US'
  utterance.rate = 0.95
  utterance.pitch = 1

  utterance.onstart = () => {
    speechState.value = 'playing'
  }
  utterance.onpause = () => {
    speechState.value = 'paused'
  }
  utterance.onresume = () => {
    speechState.value = 'playing'
  }
  utterance.onend = () => {
    speechState.value = 'idle'
  }
  utterance.onerror = () => {
    speechState.value = 'idle'
    speechError.value = '语音播放失败，请稍后重试。'
  }

  window.speechSynthesis.speak(utterance)
}

function pauseTranscriptAudio() {
  if (!speechSupported.value) return
  window.speechSynthesis.pause()
  speechState.value = 'paused'
}

function resumeTranscriptAudio() {
  if (!speechSupported.value) return
  window.speechSynthesis.resume()
  speechState.value = 'playing'
}

function closeWrongDetail() {
  stopAudioPlayback()
  detailWord.value = null
  wordKnownLoading.value = false
  wordMarkedUnknown.value = false
  detailWordFlipped.value = false
  wrongDetailVisible.value = false
}

async function markWordKnown() {
  if (!user.value || !detailRecord.value?.id || !detailRecord.value?.contentId) return

  wordKnownLoading.value = true
  try {
    await saveWordKnownProgress(user.value.id, detailRecord.value.contentId)
    await removeWrongRecord(user.value.id, detailRecord.value.id)
    wrongRecords.value = wrongRecords.value.filter(item => item.id !== detailRecord.value.id)
    const reviewResponse = await getReviewWords(user.value.id)
    reviewWords.value = reviewResponse.data.data || []
    closeWrongDetail()
  } catch (error) {
    console.error('Failed to remove wrong record', error)
  } finally {
    wordKnownLoading.value = false
  }
}

async function markWordUnknown() {
  wordMarkedUnknown.value = true
  if (!user.value || !detailRecord.value?.contentId) return

  try {
    await resetWordProgress(user.value.id, detailRecord.value.contentId)
    reviewWords.value = reviewWords.value.filter(item => item.wordId !== detailRecord.value.contentId)
  } catch (error) {
    console.error('Failed to reset word progress', error)
  }
}

function toggleDetailWordCard() {
  if (!detailIsWord.value) return
  detailWordFlipped.value = !detailWordFlipped.value
}

function detailOptionClass(index) {
  return {
    'detail-option-user': index === detailUserAnswerIndex.value,
    'detail-option-correct': index === detailCorrectAnswerIndex.value
  }
}

onBeforeUnmount(() => {
  stopAudioPlayback()
})

onMounted(async () => {
  if (!user.value) return

  if (user.value.avatar) {
    currentAvatar.value = user.value.avatar
  }
  dailyWordTargetInput.value = user.value.dailyWordTarget || 20

  try {
    const moduleResponse = await getModules()
    const modules = moduleResponse.data.data || []
    for (const module of modules) {
      moduleNameMap.value[module.code] = module.name
    }
  } catch {
    // Ignore module label loading failures.
  }

  try {
    const wrongResponse = await getWrongRecords(user.value.id)
    wrongRecords.value = wrongResponse.data.data || []
  } catch {
    // Ignore wrong record loading failures.
  } finally {
    loadingWrong.value = false
  }

  try {
    const reviewResponse = await getReviewWords(user.value.id)
    reviewWords.value = reviewResponse.data.data || []
  } catch {
    // Ignore review word loading failures.
  } finally {
    loadingReview.value = false
  }

  try {
    const favoriteResponse = await getFavorites(user.value.id)
    favorites.value = favoriteResponse.data.data || []
  } catch {
    // Ignore favorite loading failures.
  } finally {
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
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44px;
  cursor: pointer;
  transition: all 0.2s;
  border: 3px solid rgba(255, 255, 255, 0.5);
}

.avatar-display:hover {
  background: rgba(255, 255, 255, 0.35);
  transform: scale(1.05);
}

.avatar-picker {
  position: absolute;
  top: 90px;
  left: 0;
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
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
  flex-wrap: wrap;
}

.daily-target-text {
  display: inline-block;
  margin-top: 8px;
  font-size: 13px;
  opacity: 0.9;
}

.nickname-text {
  font-size: 24px;
  font-weight: 700;
}

.btn-edit {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #fff;
  border-radius: 6px;
  padding: 4px 10px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.2s;
}

.btn-edit:hover {
  background: rgba(255, 255, 255, 0.35);
}

.nickname-input {
  padding: 6px 12px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 8px;
  font-size: 18px;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  outline: none;
  width: 180px;
}

.daily-target-input {
  padding: 6px 12px;
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 8px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  outline: none;
  width: 130px;
}

.nickname-input::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.nickname-input:focus {
  border-color: #fff;
}

.daily-target-input:focus {
  border-color: #fff;
}

.btn-save,
.btn-cancel {
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
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.35);
}

.username-tag {
  font-size: 14px;
  opacity: 0.75;
}

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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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
  background: rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  padding: 2px 10px;
  font-size: 13px;
  font-weight: 600;
}

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
  font-size: 13px;
  color: #bbb;
}

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

.wrong-item.clickable {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.wrong-item.clickable:hover {
  transform: translateY(-1px);
  box-shadow: inset 0 0 0 1px rgba(26, 115, 232, 0.15);
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

.type-reading {
  background: #fef3c7;
  color: #92400e;
}

.type-listening {
  background: #ede9fe;
  color: #5b21b6;
}

.type-word {
  background: #dbeafe;
  color: #1e40af;
}

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
  margin-bottom: 10px;
  line-height: 1.6;
}

.wrong-item-footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.wrong-answers {
  display: flex;
  gap: 10px;
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

.btn-view-question {
  border: none;
  background: #e8f0fe;
  color: #1a73e8;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, transform 0.2s;
}

.btn-view-question:hover {
  background: #d7e7ff;
  transform: translateY(-1px);
}

.favorite-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-item {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.review-word {
  font-size: 18px;
  color: #1f2937;
  margin: 0;
}

.review-badge {
  font-size: 12px;
  padding: 4px 10px;
  background: #ecfdf5;
  color: #15803d;
  border-radius: 999px;
  font-weight: 700;
}

.review-phonetic {
  margin: 0 0 8px;
  font-size: 14px;
  color: #64748b;
}

.review-meaning {
  margin: 0 0 8px;
  font-size: 14px;
  color: #334155;
  line-height: 1.7;
}

.review-example {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.7;
}

.review-main {
  flex: 1;
}

.review-reset-btn {
  flex-shrink: 0;
  padding: 8px 14px;
  border: 1px solid #fca5a5;
  border-radius: 999px;
  background: #fff;
  color: #dc2626;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  transition: all 0.2s;
}

.review-reset-btn:hover {
  background: #fef2f2;
}

.favorite-item {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 16px;
}

.favorite-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
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

.detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 999;
}

.detail-dialog {
  width: min(960px, 100%);
  max-height: min(88vh, 900px);
  overflow: hidden;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.28);
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px 18px;
  border-bottom: 1px solid #eef2f7;
}

.detail-kicker {
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #1a73e8;
  text-transform: uppercase;
}

.detail-header h3 {
  margin: 0;
  font-size: 24px;
  color: #1f2937;
}

.detail-close {
  border: none;
  background: #f3f4f6;
  color: #374151;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.2s;
}

.detail-close:hover {
  background: #e5e7eb;
}

.detail-state {
  padding: 56px 24px;
  text-align: center;
  color: #6b7280;
  font-size: 15px;
}

.detail-state-error {
  color: #dc2626;
}

.detail-body {
  padding: 24px 28px 28px;
  overflow-y: auto;
}

.detail-notice {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 13px;
}

.detail-summary {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.detail-section {
  margin-bottom: 22px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 12px;
}

.detail-word-card-container {
  perspective: 1000px;
  margin-bottom: 18px;
}

.detail-word-card {
  height: 320px;
  cursor: pointer;
}

.detail-word-card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.5s ease;
  transform-style: preserve-3d;
}

.detail-word-card.flipped .detail-word-card-inner {
  transform: rotateY(180deg);
}

.detail-word-face {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
  background: linear-gradient(160deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #dbeafe;
  border-radius: 18px;
  box-shadow: 0 12px 32px rgba(37, 99, 235, 0.08);
  padding: 28px;
}

.detail-word-front {
  text-align: center;
}

.detail-word-back {
  transform: rotateY(180deg);
  align-items: flex-start;
}

.detail-word-text {
  margin: 0 0 4px;
  font-size: 32px;
  line-height: 1.1;
  color: #0f172a;
  text-align: center;
}

.detail-word-phonetic {
  font-size: 16px;
  color: #64748b;
  text-align: center;
}

.detail-word-block + .detail-word-block {
  margin-top: 16px;
}

.detail-word-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #2563eb;
  margin-bottom: 8px;
}

.detail-word-meaning,
.detail-word-example {
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: #334155;
}

.detail-word-tip {
  margin: 6px 0 0;
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
}

.detail-word-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.detail-word-btn {
  min-width: 120px;
  padding: 10px 18px;
  border-radius: 999px;
  background: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, background 0.2s;
}

.detail-word-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.detail-word-btn-known {
  border: 1px solid #86efac;
  color: #15803d;
}

.detail-word-btn-known:hover {
  background: #f0fdf4;
}

.detail-word-btn-unknown {
  border: 1px solid #fca5a5;
  color: #dc2626;
}

.detail-word-btn-unknown:hover {
  background: #fef2f2;
}

.detail-word-feedback {
  margin: 14px 0 0;
  font-size: 13px;
  font-weight: 600;
}

.detail-word-feedback.known {
  color: #15803d;
}

.detail-word-feedback.unknown {
  color: #dc2626;
}

.detail-audio-panel {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px;
}

.detail-audio {
  width: 100%;
}

.detail-audio-fallback {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-audio-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-audio-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.detail-audio-hint {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.detail-audio-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-audio-btn {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.detail-audio-btn.primary {
  background: #1a73e8;
  border-color: #1a73e8;
  color: #fff;
}

.detail-audio-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.detail-audio-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.detail-audio-error {
  margin: 0;
  color: #dc2626;
  font-size: 13px;
}

.detail-passage {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 18px;
  color: #334155;
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-line;
  max-height: 280px;
  overflow-y: auto;
}

.detail-question {
  margin: 0 0 14px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.7;
}

.detail-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-option {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 14px 16px;
  background: #fff;
}

.detail-option-user {
  border-color: #fca5a5;
  background: #fff1f2;
}

.detail-option-correct {
  border-color: #86efac;
  background: #f0fdf4;
}

.detail-option-label {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #eff6ff;
  color: #1d4ed8;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.detail-option-text {
  flex: 1;
  color: #374151;
  font-size: 14px;
  line-height: 1.6;
}

.detail-pill {
  flex-shrink: 0;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 700;
}

.detail-pill-user {
  background: #fee2e2;
  color: #b91c1c;
}

.detail-pill-correct {
  background: #dcfce7;
  color: #15803d;
}

.detail-empty-options {
  font-size: 13px;
  color: #6b7280;
  background: #f8fafc;
  border-radius: 12px;
  padding: 14px 16px;
}

@media (max-width: 768px) {
  .profile-content {
    padding: 16px;
  }

  .user-card {
    padding: 24px 18px;
    flex-direction: column;
    align-items: flex-start;
  }

  .tabs {
    flex-direction: column;
  }

  .favorite-item {
    padding: 16px;
    flex-direction: column;
    align-items: flex-start;
  }

  .btn-unfav {
    width: 100%;
  }

  .detail-overlay {
    padding: 12px;
  }

  .detail-header,
  .detail-body {
    padding-left: 18px;
    padding-right: 18px;
  }

  .detail-header h3 {
    font-size: 20px;
  }

  .detail-option {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .detail-word-card {
    height: 340px;
  }

  .detail-word-face {
    padding: 20px;
  }

  .detail-word-text {
    font-size: 26px;
  }

  .detail-word-actions {
    flex-direction: column;
  }

  .detail-word-btn {
    width: 100%;
  }
}
</style>

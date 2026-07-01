<template>
  <div class="sub-page">
    <NavBar />
    <main class="sub-content">
      <section class="sub-hero">
        <div>
          <p class="sub-kicker">Wrong Book</p>
          <h1>错题本</h1>
          <p>单词错题和题目错题分开管理，复习路径更清晰。</p>
        </div>
        <button class="back-btn" type="button" @click="back">返回个人中心</button>
      </section>

      <div v-if="loading" class="state-card">加载错题...</div>
      <div v-else-if="wrongRecords.length === 0" class="state-card empty">
        <div class="empty-icon">📘</div>
        <p>暂无错题记录</p>
        <small>练习中答错的题目会自动收录到这里。</small>
      </div>

      <section v-else class="wrong-sections">
        <section class="wrong-section">
          <div class="section-heading">
            <div>
              <p class="section-kicker">Words</p>
              <h2>单词错题</h2>
            </div>
            <div class="section-actions">
              <span class="count-pill">{{ wordRecords.length }} 条</span>
              <button class="collapse-btn" type="button" @click="wordCollapsed = !wordCollapsed">
                {{ wordCollapsed ? '展开' : '折叠' }}
              </button>
            </div>
          </div>

          <Transition name="section-fold" mode="out-in">
            <div v-if="wordCollapsed" key="word-collapsed" class="mini-empty collapsed">单词错题已折叠</div>
            <div v-else-if="wordRecords.length === 0" key="word-empty" class="mini-empty">暂无单词错题</div>
            <div v-else key="word-list" class="record-list fold-content">
              <article
                v-for="record in wordRecords"
                :key="record.id"
                class="record-card word-record"
                :class="{ clickable: canOpenDetail(record) }"
                @click="openDetail(record)"
              >
                <div class="record-header">
                  <span class="type-badge word">单词</span>
                  <span class="record-title">{{ record.contentTitle || record.questionText || '单词卡片' }}</span>
                  <span class="record-time">{{ formatTime(record.createdAt) }}</span>
                </div>
                <p class="record-question">{{ record.questionText }}</p>
                <div class="answer-row">
                  <span class="correct-answer">释义：{{ record.correctAnswer || '-' }}</span>
                  <button v-if="canOpenDetail(record)" class="view-btn" type="button" @click.stop="openDetail(record)">
                    查看单词
                  </button>
                </div>
              </article>
            </div>
          </Transition>
        </section>

        <section class="wrong-section">
          <div class="section-heading">
            <div>
              <p class="section-kicker">Questions</p>
              <h2>题目错题</h2>
            </div>
            <div class="section-actions">
              <span class="count-pill">{{ questionRecords.length }} 条</span>
              <button class="collapse-btn" type="button" @click="questionCollapsed = !questionCollapsed">
                {{ questionCollapsed ? '展开' : '折叠' }}
              </button>
            </div>
          </div>

          <Transition name="section-fold" mode="out-in">
            <div v-if="questionCollapsed" key="question-collapsed" class="mini-empty collapsed">题目错题已折叠</div>
            <div v-else-if="questionRecords.length === 0" key="question-empty" class="mini-empty">暂无阅读或听力错题</div>
            <div v-else key="question-list" class="record-list fold-content">
              <article
                v-for="record in questionRecords"
                :key="record.id"
                class="record-card"
                :class="{ clickable: canOpenDetail(record) }"
                @click="openDetail(record)"
              >
                <div class="record-header">
                  <span class="type-badge">{{ typeLabel(record.questionType) }}</span>
                  <span class="record-title">{{ record.contentTitle || '练习题目' }}</span>
                  <span class="record-time">{{ formatTime(record.createdAt) }}</span>
                </div>
                <p class="record-question">{{ record.questionText }}</p>
                <div class="answer-row">
                  <span class="wrong-answer">我的答案：{{ record.userAnswer || '-' }}</span>
                  <span class="correct-answer">正确答案：{{ record.correctAnswer || '-' }}</span>
                  <button v-if="canOpenDetail(record)" class="view-btn" type="button" @click.stop="openDetail(record)">
                    {{ detailActionLabel(record) }}
                  </button>
                </div>
              </article>
            </div>
          </Transition>
        </section>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="detailVisible" class="detail-overlay" @click.self="closeDetail">
        <div class="detail-dialog">
          <div class="detail-header">
            <div>
              <p class="sub-kicker">{{ detailKicker }}</p>
              <h2>{{ detailTitle }}</h2>
            </div>
            <button class="close-btn" type="button" @click="closeDetail">×</button>
          </div>

          <div v-if="detailLoading" class="detail-state">加载详情...</div>
          <div v-else-if="detailError" class="detail-state error">{{ detailError }}</div>
          <div v-else-if="detailRecord" class="detail-body">
            <template v-if="detailIsWord">
              <div class="word-detail-card">
                <h3>{{ detailWord?.word || detailRecord.contentTitle }}</h3>
                <p v-if="detailWord?.phonetic" class="phonetic">{{ detailWord.phonetic }}</p>
                <p class="word-meaning">{{ detailWord?.meaning || detailRecord.correctAnswer }}</p>
                <p v-if="detailWord?.example" class="word-example">{{ detailWord.example }}</p>
              </div>

              <div class="word-actions">
                <button class="word-action known" type="button" :disabled="wordActionLoading" @click="markDetailWordKnown">
                  {{ wordActionLoading === 'known' ? '处理中...' : '认识' }}
                </button>
                <button class="word-action unknown" type="button" :disabled="wordActionLoading" @click="markDetailWordUnknown">
                  {{ wordActionLoading === 'unknown' ? '处理中...' : '不认识' }}
                </button>
              </div>

              <p v-if="wordActionMessage" class="word-action-message">{{ wordActionMessage }}</p>
            </template>

            <template v-else>
              <section class="detail-section">
                <h3>{{ detailIsListening ? '听力原文' : '阅读原文' }}</h3>
                <p class="passage">{{ detailPassageText || '未找到原文。' }}</p>
              </section>

              <section class="detail-section">
                <h3>原题</h3>
                <p class="question">{{ detailQuestion?.q || detailRecord.questionText }}</p>
                <div v-if="detailOptions.length" class="options">
                  <div
                    v-for="(option, index) in detailOptions"
                    :key="index"
                    class="option"
                    :class="optionClass(index)"
                  >
                    <span>{{ optionLetter(index) }}</span>
                    <p>{{ option }}</p>
                    <strong v-if="index === userAnswerIndex">你的选择</strong>
                    <strong v-if="index === correctAnswerIndex">正确答案</strong>
                  </div>
                </div>
              </section>
            </template>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import {
  getListeningsByModule,
  getReadingsByModule,
  getWordsByModule,
  getWrongRecords,
  markWordKnown,
  removeWrongRecord,
  resetWordProgress
} from '../utils/api'
import { currentUser } from '../utils/currentUser'

const router = useRouter()
const user = currentUser
const loading = ref(true)
const wrongRecords = ref([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailRecord = ref(null)
const detailReading = ref(null)
const detailListening = ref(null)
const detailWord = ref(null)
const detailQuestion = ref(null)
const wordActionLoading = ref('')
const wordActionMessage = ref('')
const wordCollapsed = ref(true)
const questionCollapsed = ref(true)

const readingCache = new Map()
const listeningCache = new Map()
const wordCache = new Map()

const typeMap = {
  READING: '阅读理解',
  LISTENING: '听力',
  WORD: '单词'
}

function typeLabel(type) {
  return typeMap[type] || type || '题目'
}

const detailIsWord = computed(() => detailRecord.value?.questionType === 'WORD')
const detailIsListening = computed(() => detailRecord.value?.questionType === 'LISTENING')
const detailTitle = computed(() => (
  detailWord.value?.word
  || detailReading.value?.title
  || detailListening.value?.title
  || detailRecord.value?.contentTitle
  || '错题详情'
))
const detailKicker = computed(() => {
  if (detailRecord.value?.questionType === 'WORD') return '单词卡片'
  if (detailRecord.value?.questionType === 'LISTENING') return '听力错题'
  return '阅读错题'
})
const detailPassageText = computed(() => (
  detailIsListening.value ? detailListening.value?.transcript : detailReading.value?.content
))
const detailOptions = computed(() => (
  Array.isArray(detailQuestion.value?.options) ? detailQuestion.value.options : []
))
const userAnswerIndex = computed(() => answerToIndex(detailRecord.value?.userAnswer))
const correctAnswerIndex = computed(() => answerToIndex(detailRecord.value?.correctAnswer))
const wordRecords = computed(() => wrongRecords.value.filter(record => record.questionType === 'WORD'))
const questionRecords = computed(() => wrongRecords.value.filter(record => record.questionType !== 'WORD'))

function formatTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function back() {
  router.push('/profile')
}

function canOpenDetail(record) {
  return ['READING', 'LISTENING', 'WORD'].includes(record?.questionType)
    && record?.moduleCode
    && record?.contentId
}

function detailActionLabel(record) {
  if (record?.questionType === 'WORD') return '查看单词'
  if (record?.questionType === 'LISTENING') return '查看原文'
  return '查看原题'
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
  if (code >= 65 && code <= 90) return code - 65

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
  const matches = questions.filter(question => normalizeText(question?.q) === targetText)

  if (matches.length === 1) return matches[0]
  if (matches.length > 1 && targetAnswer >= 0) {
    return matches.find(question => questionAnswerIndex(question) === targetAnswer) || matches[0]
  }

  return questions.find(question => {
    const currentText = normalizeText(question?.q)
    return currentText && (currentText.includes(targetText) || targetText.includes(currentText))
  }) || null
}

async function ensureReadings(moduleCode) {
  if (readingCache.has(moduleCode)) return readingCache.get(moduleCode)

  const response = await getReadingsByModule(moduleCode)
  const readings = response.data.data || []
  readingCache.set(moduleCode, readings)
  return readings
}

async function ensureListenings(moduleCode) {
  if (listeningCache.has(moduleCode)) return listeningCache.get(moduleCode)

  const response = await getListeningsByModule(moduleCode)
  const listenings = response.data.data || []
  listeningCache.set(moduleCode, listenings)
  return listenings
}

async function ensureWords(moduleCode) {
  if (wordCache.has(moduleCode)) return wordCache.get(moduleCode)

  const response = await getWordsByModule(moduleCode)
  const words = response.data.data || []
  wordCache.set(moduleCode, words)
  return words
}

function resetDetail(record) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detailRecord.value = record
  detailReading.value = null
  detailListening.value = null
  detailWord.value = null
  detailQuestion.value = null
  wordActionLoading.value = ''
  wordActionMessage.value = ''
}

async function openDetail(record) {
  if (!canOpenDetail(record)) return
  resetDetail(record)

  try {
    if (record.questionType === 'READING') {
      const readings = await ensureReadings(record.moduleCode)
      detailReading.value = readings.find(item => String(item.id) === String(record.contentId))
      if (!detailReading.value) throw new Error('没有找到这道错题对应的阅读原文。')
      detailQuestion.value = findMatchingQuestion(parseQuestions(detailReading.value.questions), record)
        || { q: record.questionText, options: [] }
    } else if (record.questionType === 'LISTENING') {
      const listenings = await ensureListenings(record.moduleCode)
      detailListening.value = listenings.find(item => String(item.id) === String(record.contentId))
      if (!detailListening.value) throw new Error('没有找到这道错题对应的听力原文。')
      detailQuestion.value = findMatchingQuestion(parseQuestions(detailListening.value.questions), record)
        || { q: record.questionText, options: [] }
    } else {
      const words = await ensureWords(record.moduleCode)
      detailWord.value = words.find(item => String(item.id) === String(record.contentId))
      if (!detailWord.value) throw new Error('没有找到这条错题对应的单词信息。')
    }
  } catch (error) {
    detailError.value = error?.message || '加载详情失败，请稍后重试。'
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detailVisible.value = false
}

async function markDetailWordKnown() {
  if (!user.value || !detailRecord.value?.id || !detailRecord.value?.contentId || wordActionLoading.value) return

  wordActionLoading.value = 'known'
  wordActionMessage.value = ''
  try {
    await markWordKnown(user.value.id, detailRecord.value.contentId)
    await removeWrongRecord(user.value.id, detailRecord.value.id)
    wrongRecords.value = wrongRecords.value.filter(item => item.id !== detailRecord.value.id)
    closeDetail()
  } catch (error) {
    console.error('Failed to mark word known from wrong records', error)
    wordActionMessage.value = '处理失败，请稍后重试'
  } finally {
    wordActionLoading.value = ''
  }
}

async function markDetailWordUnknown() {
  if (!user.value || !detailRecord.value?.contentId || wordActionLoading.value) return

  wordActionLoading.value = 'unknown'
  wordActionMessage.value = ''
  try {
    await resetWordProgress(user.value.id, detailRecord.value.contentId)
    wordActionMessage.value = '已标记为不认识，建议稍后继续复习'
  } catch (error) {
    console.error('Failed to reset word progress from wrong records', error)
    wordActionMessage.value = '处理失败，请稍后重试'
  } finally {
    wordActionLoading.value = ''
  }
}

function optionClass(index) {
  return {
    user: index === userAnswerIndex.value,
    correct: index === correctAnswerIndex.value
  }
}

onMounted(async () => {
  if (!user.value) return

  try {
    const response = await getWrongRecords(user.value.id)
    wrongRecords.value = response.data.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.sub-page { min-height: 100vh; background: #f3f6fb; }
.sub-content { max-width: 920px; margin: 0 auto; padding: 32px 24px 48px; }
.sub-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 22px; }
.sub-kicker { color: #1a73e8; font-size: 12px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; margin-bottom: 8px; }
.sub-hero h1 { font-size: 32px; color: #172033; margin-bottom: 8px; }
.sub-hero p { color: #667085; }
.back-btn { border: none; border-radius: 999px; background: #172033; color: #fff; padding: 11px 18px; font-weight: 800; cursor: pointer; }
.state-card { background: #fff; border-radius: 18px; padding: 46px 20px; text-align: center; color: #64748b; border: 1px solid #e2e8f0; }
.empty-icon { font-size: 48px; margin-bottom: 10px; }
.empty small { color: #94a3b8; }
.wrong-sections { display: flex; flex-direction: column; gap: 24px; }
.wrong-section { background: #fff; border: 1px solid #e2e8f0; border-radius: 22px; padding: 20px; box-shadow: 0 12px 26px rgba(15, 23, 42, 0.05); }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.section-kicker { margin: 0 0 4px; color: #1a73e8; font-size: 12px; font-weight: 900; letter-spacing: 0.14em; text-transform: uppercase; }
.section-heading h2 { margin: 0; color: #172033; font-size: 22px; }
.section-actions { display: inline-flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.count-pill { border-radius: 999px; background: #f1f5f9; color: #475569; padding: 6px 12px; font-size: 13px; font-weight: 800; white-space: nowrap; }
.collapse-btn { border: 1px solid #dbeafe; border-radius: 999px; background: #fff; color: #1a73e8; padding: 6px 12px; font-size: 13px; font-weight: 800; cursor: pointer; }
.collapse-btn:hover { background: #edf4ff; }
.mini-empty { border: 1px dashed #cbd5e1; border-radius: 16px; padding: 24px; color: #94a3b8; text-align: center; background: #f8fafc; }
.mini-empty.collapsed { color: #64748b; background: #f1f5f9; }
.fold-content { overflow: hidden; }
.section-fold-enter-active,
.section-fold-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease, max-height 0.28s ease;
  overflow: hidden;
}
.section-fold-enter-from,
.section-fold-leave-to {
  opacity: 0;
  transform: translateY(-8px);
  max-height: 0;
}
.section-fold-enter-to,
.section-fold-leave-from {
  opacity: 1;
  transform: translateY(0);
  max-height: 1200px;
}
.record-list { display: flex; flex-direction: column; gap: 14px; }
.record-card { background: #fff; border-radius: 18px; padding: 18px 20px; border: 1px solid #e2e8f0; box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05); }
.wrong-section .record-card { box-shadow: none; }
.word-record { background: linear-gradient(160deg, #ffffff 0%, #f8fbff 100%); border-color: #dbeafe; }
.record-card.clickable { cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
.record-card.clickable:hover { transform: translateY(-2px); box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08); }
.record-header { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 10px; }
.type-badge { background: #edf4ff; color: #1a73e8; border-radius: 999px; padding: 4px 10px; font-size: 12px; font-weight: 800; }
.type-badge.word { background: #ecfdf5; color: #15803d; }
.record-title { color: #334155; font-weight: 700; }
.record-time { margin-left: auto; color: #94a3b8; font-size: 12px; }
.record-question { color: #334155; line-height: 1.7; margin-bottom: 12px; }
.answer-row { display: flex; gap: 10px; flex-wrap: wrap; }
.wrong-answer, .correct-answer { border-radius: 8px; padding: 6px 10px; font-size: 13px; font-weight: 700; }
.wrong-answer { background: #fef2f2; color: #dc2626; }
.correct-answer { background: #f0fdf4; color: #16a34a; }
.view-btn { border: none; border-radius: 999px; background: #edf4ff; color: #1a73e8; padding: 6px 12px; font-size: 13px; font-weight: 800; cursor: pointer; }
.detail-overlay { position: fixed; inset: 0; background: rgba(15, 23, 42, 0.55); display: flex; align-items: center; justify-content: center; padding: 24px; z-index: 999; }
.detail-dialog { width: min(920px, 100%); max-height: min(88vh, 900px); overflow: hidden; background: #fff; border-radius: 22px; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.28); display: flex; flex-direction: column; }
.detail-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 24px 28px 18px; border-bottom: 1px solid #eef2f7; }
.detail-header h2 { margin: 0; color: #172033; font-size: 24px; }
.close-btn { border: none; background: #f3f4f6; color: #374151; width: 36px; height: 36px; border-radius: 50%; font-size: 24px; cursor: pointer; }
.detail-state { padding: 56px 24px; text-align: center; color: #64748b; }
.detail-state.error { color: #dc2626; }
.detail-body { padding: 24px 28px 28px; overflow-y: auto; }
.detail-section { margin-bottom: 22px; }
.detail-section h3 { color: #172033; font-size: 16px; margin-bottom: 10px; }
.passage { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 14px; padding: 16px; color: #334155; line-height: 1.9; white-space: pre-line; max-height: 260px; overflow-y: auto; }
.question { color: #172033; line-height: 1.7; font-weight: 700; margin-bottom: 12px; }
.options { display: flex; flex-direction: column; gap: 10px; }
.option { display: flex; align-items: center; gap: 12px; border: 1px solid #e5e7eb; border-radius: 14px; padding: 12px 14px; }
.option span { width: 28px; height: 28px; border-radius: 50%; background: #eff6ff; color: #1d4ed8; display: inline-flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 800; flex-shrink: 0; }
.option p { flex: 1; color: #334155; line-height: 1.6; }
.option strong { border-radius: 999px; padding: 4px 9px; font-size: 12px; }
.option.user { border-color: #fca5a5; background: #fff1f2; }
.option.correct { border-color: #86efac; background: #f0fdf4; }
.word-detail-card { border: 1px solid #dbeafe; border-radius: 18px; background: linear-gradient(160deg, #fff 0%, #f8fbff 100%); padding: 30px; text-align: center; }
.word-detail-card h3 { color: #172033; font-size: 34px; margin-bottom: 8px; }
.phonetic { color: #64748b; margin-bottom: 18px; }
.word-meaning { color: #334155; font-size: 18px; line-height: 1.8; }
.word-example { color: #64748b; line-height: 1.8; margin-top: 16px; }
.word-actions { display: flex; justify-content: center; gap: 12px; margin-top: 18px; flex-wrap: wrap; }
.word-action { min-width: 120px; border-radius: 999px; background: #fff; padding: 10px 18px; font-size: 15px; font-weight: 800; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s, background 0.2s; }
.word-action:disabled { cursor: not-allowed; opacity: 0.65; }
.word-action.known { border: 1px solid #86efac; color: #15803d; }
.word-action.known:hover:not(:disabled) { background: #f0fdf4; }
.word-action.unknown { border: 1px solid #fca5a5; color: #dc2626; }
.word-action.unknown:hover:not(:disabled) { background: #fef2f2; }
.word-action-message { margin-top: 12px; text-align: center; color: #15803d; font-size: 13px; font-weight: 700; }
@media (max-width: 768px) {
  .sub-content { padding: 24px 16px 36px; }
  .sub-hero { flex-direction: column; align-items: flex-start; }
  .back-btn { width: 100%; }
  .section-heading { align-items: flex-start; }
  .section-actions { flex-direction: column; align-items: flex-end; }
  .record-time { margin-left: 0; width: 100%; }
  .detail-overlay { padding: 12px; }
  .detail-header, .detail-body { padding-left: 18px; padding-right: 18px; }
  .option { align-items: flex-start; flex-wrap: wrap; }
  .word-action { width: 100%; }
}
</style>

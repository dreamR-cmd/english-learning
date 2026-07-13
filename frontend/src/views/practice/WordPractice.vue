<template>
  <div class="word-practice">
    <NavBar />
    <div class="practice-content">
      <div class="breadcrumb" @click="back">← 返回模块</div>
      <h1>📚 单词练习</h1>

      <div v-if="loading" class="loading">加载今日单词中...</div>
      <div v-else-if="words.length === 0" class="empty-tip">
        今日没有可练习的单词，已完成的单词会在复习模块中查看。
      </div>
      <div v-else class="word-card-container">
        <div class="word-progress">
          今日随机单词 {{ words.length }} 个
          <span class="progress-meta">当前词库来源：{{ currentWord.moduleCode?.toUpperCase() || '全词库' }}</span>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
        </div>

        <div class="word-card" :class="{ flipped: isFlipped }" @click="flipCard">
          <div class="card-inner">
            <div class="card-front">
              <p class="word-text">{{ currentWord.word }}</p>
              <p v-if="currentWord.phonetic" class="word-phonetic">{{ currentWord.phonetic }}</p>
              <p class="tap-hint">点击翻转查看释义</p>
            </div>
            <div class="card-back">
              <p class="word-meaning">{{ currentWord.meaning }}</p>
              <p v-if="currentWord.example" class="word-example">
                <strong>例句：</strong>{{ currentWord.example }}
              </p>
              <p class="tap-hint">点击回到正面</p>
            </div>
          </div>
        </div>

        <div class="word-actions">
          <button class="word-action-btn word-action-btn-known" :disabled="saving" @click="markAsKnown">
            {{ savingAction === 'known' ? '保存中...' : (currentWord.knownCount >= 3 ? '认识并完成' : '认识') }}
          </button>
          <button class="word-action-btn word-action-btn-blur" :disabled="saving" @click="markAsBlur">
            模糊
          </button>
          <button class="word-action-btn word-action-btn-unknown" :disabled="saving" @click="markAsUnknown">
            {{ savingAction === 'unknown' ? '保存中...' : '不认识' }}
          </button>
        </div>

        <p v-if="saveMessage" class="save-hint">{{ saveMessage }}</p>
        <p v-else-if="saveError" class="save-error">{{ saveError }}</p>
        <p v-else-if="unknownMarked" class="unknown-hint">已记录到错题本，并重置认识次数</p>
        <p v-else-if="blurMarked" class="blur-hint">已标记为模糊，本次不计认识次数</p>
        <p class="known-hint">当前累计认识 {{ currentWord.knownCount || 0 }} / 4 次</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../../components/NavBar.vue'
import { getDailyWords, markWordKnown, resetWordProgress, submitWrongRecord } from '../../utils/api'
import { currentUser } from '../../utils/currentUser'

const router = useRouter()
const user = currentUser

const words = ref([])
const loading = ref(true)
const savingAction = ref('')
const saveMessage = ref('')
const saveError = ref('')
const isFlipped = ref(false)
const unknownMarked = ref(false)
const blurMarked = ref(false)
const currentWord = ref({})
const saving = computed(() => Boolean(savingAction.value))
const progressPercent = computed(() =>
  words.value.length > 0 ? Math.min(100, ((currentWord.value.knownCount || 0) / 4) * 100) : 0
)

onMounted(async () => {
  await loadDailyWords()
})

async function loadDailyWords() {
  if (!user.value) {
    loading.value = false
    return
  }

  try {
    const res = await getDailyWords(user.value.id)
    words.value = res.data.data || []
    pickRandomWord(true)
  } catch (error) {
    console.error('Failed to load daily words', error)
  } finally {
    loading.value = false
  }
}

function back() {
  router.push('/modules')
}

function flipCard() {
  isFlipped.value = !isFlipped.value
}

function pickRandomWord(resetHints = false) {
  if (words.value.length === 0) {
    currentWord.value = {}
    unknownMarked.value = false
    blurMarked.value = false
    return
  }

  const randomIndex = Math.floor(Math.random() * words.value.length)
  currentWord.value = words.value[randomIndex]
  isFlipped.value = false
  if (resetHints) {
    unknownMarked.value = false
    blurMarked.value = false
  }
}

function removeCurrentWordFromPool() {
  words.value = words.value.filter(item => String(item.id) !== String(currentWord.value.id))
}

function updateCurrentWordKnownCount(knownCount) {
  words.value = words.value.map(item => (
    String(item.id) === String(currentWord.value.id)
      ? { ...item, knownCount }
      : item
  ))
  currentWord.value = { ...currentWord.value, knownCount }
}

async function markAsKnown() {
  if (!user.value || !currentWord.value?.id || saving.value) return

  unknownMarked.value = false
  blurMarked.value = false
  saveMessage.value = ''
  saveError.value = ''
  savingAction.value = 'known'

  try {
    const response = await markWordKnown(user.value.id, currentWord.value.id)
    const progress = response.data.data
    const nextKnownCount = progress?.knownCount ?? ((currentWord.value.knownCount || 0) + 1)

    if (progress?.reviewReady) {
      removeCurrentWordFromPool()
    } else {
      updateCurrentWordKnownCount(nextKnownCount)
    }

    saveMessage.value = progress?.reviewReady ? '已保存，单词进入复习列表' : '已保存认识进度'
    pickRandomWord(true)
  } catch (error) {
    console.error('Failed to mark word known', error)
    saveError.value = '保存失败，请稍后重试'
  } finally {
    savingAction.value = ''
  }
}

function markAsBlur() {
  if (!currentWord.value?.id || saving.value) return
  unknownMarked.value = false
  blurMarked.value = true
  saveMessage.value = ''
  saveError.value = ''
  pickRandomWord(false)
}

async function markAsUnknown() {
  if (!user.value || !currentWord.value?.id || saving.value) return

  blurMarked.value = false
  unknownMarked.value = true
  saveMessage.value = ''
  saveError.value = ''
  savingAction.value = 'unknown'

  try {
    await submitWrongRecord({
      questionType: 'WORD',
      contentId: currentWord.value.id,
      contentTitle: currentWord.value.word,
      questionText: '单词 ' + currentWord.value.word + ' - ' + (currentWord.value.phonetic || ''),
      userAnswer: '不认识',
      correctAnswer: currentWord.value.meaning,
      moduleCode: currentWord.value.moduleCode
    })

    if ((currentWord.value.knownCount || 0) > 0) {
      const response = await resetWordProgress(user.value.id, currentWord.value.id)
      const progress = response.data.data
      updateCurrentWordKnownCount(progress?.knownCount ?? 0)
    }

    saveMessage.value = '已保存到错题本'
    pickRandomWord(false)
  } catch (error) {
    console.error('Failed to mark word unknown', error)
    unknownMarked.value = false
    saveError.value = '保存失败，请稍后重试'
  } finally {
    savingAction.value = ''
  }
}
</script>

<style scoped>
.word-practice { min-height: 100vh; background: #f0f2f5; }
.practice-content { max-width: 700px; margin: 0 auto; padding: 24px; }
.breadcrumb { font-size: 14px; color: #1a73e8; cursor: pointer; margin-bottom: 16px; }
.breadcrumb:hover { text-decoration: underline; }
.practice-content h1 { font-size: 26px; color: #1a1a1a; margin-bottom: 24px; }
.loading, .empty-tip { text-align: center; color: #999; padding: 60px; font-size: 16px; }
.word-progress { margin-bottom: 24px; font-size: 14px; color: #666; display: flex; flex-direction: column; gap: 8px; }
.progress-meta { font-size: 12px; color: #94a3b8; }
.progress-bar {
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
}
.progress-fill {
  height: 100%;
  background: #1a73e8;
  border-radius: 3px;
  transition: width 0.3s ease;
}
.word-card-container { perspective: 1000px; margin-bottom: 24px; }
.word-card {
  height: 280px;
  cursor: pointer;
}
.card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transition: transform 0.5s ease;
  transform-style: preserve-3d;
}
.word-card.flipped .card-inner { transform: rotateY(180deg); }
.card-front, .card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  padding: 32px;
}
.card-back { transform: rotateY(180deg); }
.word-text { font-size: 36px; font-weight: 700; color: #1a1a1a; margin-bottom: 8px; }
.word-phonetic { font-size: 18px; color: #888; margin-bottom: 12px; }
.word-meaning { font-size: 24px; color: #333; margin-bottom: 16px; }
.word-example { font-size: 15px; color: #666; text-align: center; line-height: 1.6; max-width: 500px; }
.tap-hint { font-size: 13px; color: #bbb; margin-top: 20px; }
.word-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.word-action-btn {
  min-width: 120px;
  padding: 10px 18px;
  border-radius: 999px;
  background: #fff;
  font-size: 15px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, background 0.2s;
  font-weight: 700;
}
.word-action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}
.word-action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
  transform: none;
  box-shadow: none;
}
.word-action-btn-known {
  border: 1px solid #86efac;
  color: #15803d;
}
.word-action-btn-known:hover {
  background: #f0fdf4;
}
.word-action-btn-blur {
  border: 1px solid #93c5fd;
  color: #2563eb;
}
.word-action-btn-blur:hover {
  background: #eff6ff;
}
.word-action-btn-unknown {
  border: 1px solid #fca5a5;
  color: #dc2626;
}
.word-action-btn-unknown:hover {
  background: #fef2f2;
}
.unknown-hint,
.blur-hint,
.save-hint,
.save-error,
.known-hint {
  text-align: center;
  font-size: 13px;
  margin-top: 8px;
}
.unknown-hint { color: #dc2626; }
.blur-hint { color: #2563eb; }
.save-hint { color: #15803d; }
.save-error { color: #dc2626; }
.known-hint { color: #15803d; }
@media (max-width: 768px) {
  .word-action-btn {
    width: 100%;
  }
}
</style>

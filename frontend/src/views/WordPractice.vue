<template>
  <div class="word-practice">
    <NavBar />
    <div class="practice-content">
      <div class="breadcrumb" @click="back">← 返回模块</div>
      <h1>📖 单词练习</h1>

      <div v-if="loading" class="loading">加载单词中...</div>
      <div v-else-if="words.length === 0" class="empty-tip">暂无单词数据</div>
      <div v-else class="word-card-container">
        <div class="word-progress">
          第 {{ currentIndex + 1 }} / {{ words.length }} 个单词
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
          <button class="btn-unknown" @click="markAsUnknown">
            🤔 不认识
          </button>
          <p v-if="unknownMarked" class="unknown-hint">已记录到错题本</p>
        </div>

        <div class="word-nav">
          <button @click="prevWord" :disabled="currentIndex === 0">← 上一个</button>
          <button @click="nextWord" :disabled="currentIndex === words.length - 1">下一个 →</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import { getWordsByModule, submitWrongRecord } from '../utils/api'

const route = useRoute()
const router = useRouter()
const props = defineProps({ moduleCode: String })
const moduleCode = computed(() => props.moduleCode || route.params.moduleCode)

const words = ref([])
const loading = ref(true)
const currentIndex = ref(0)
const isFlipped = ref(false)
const unknownMarked = ref(false)
const currentWord = computed(() => words.value[currentIndex.value] || {})
const progressPercent = computed(() =>
  words.value.length > 0 ? ((currentIndex.value + 1) / words.value.length) * 100 : 0
)

onMounted(async () => {
  try {
    const res = await getWordsByModule(moduleCode.value)
    words.value = res.data.data || []
  } catch (e) {
    console.error('Failed to load words', e)
  } finally {
    loading.value = false
  }
})

function back() { router.push(`/module/${moduleCode.value}`) }
function flipCard() { isFlipped.value = !isFlipped.value }
function nextWord() {
  if (currentIndex.value < words.value.length - 1) {
    currentIndex.value++
    isFlipped.value = false
    unknownMarked.value = false
  }
}
function prevWord() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    isFlipped.value = false
    unknownMarked.value = false
  }
}
function markAsUnknown() {
  unknownMarked.value = true
  try {
    const user = JSON.parse(sessionStorage.getItem('currentUser'))
    if (user) {
      submitWrongRecord({
        userId: user.id,
        questionType: 'WORD',
        contentId: currentWord.value.id,
        contentTitle: currentWord.value.word,
        questionText: '单词 ' + currentWord.value.word + ' - ' + (currentWord.value.phonetic || ''),
        userAnswer: '不认识',
        correctAnswer: currentWord.value.meaning,
        moduleCode: moduleCode.value
      })
    }
  } catch {}
}
</script>

<style scoped>
.word-practice { min-height: 100vh; background: #f0f2f5; }
.practice-content { max-width: 700px; margin: 0 auto; padding: 24px; }
.breadcrumb { font-size: 14px; color: #1a73e8; cursor: pointer; margin-bottom: 16px; }
.breadcrumb:hover { text-decoration: underline; }
.practice-content h1 { font-size: 26px; color: #1a1a1a; margin-bottom: 24px; }
.loading, .empty-tip { text-align: center; color: #999; padding: 60px; font-size: 16px; }
.word-progress { margin-bottom: 24px; font-size: 14px; color: #666; }
.progress-bar {
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  margin-top: 8px;
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
  text-align: center;
  margin-bottom: 20px;
}
.btn-unknown {
  padding: 10px 28px;
  border: 2px solid #f59e0b;
  border-radius: 10px;
  background: #fff;
  color: #f59e0b;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
}
.btn-unknown:hover {
  background: #fffbeb;
  border-color: #d97706;
  color: #d97706;
}
.unknown-hint {
  font-size: 13px;
  color: #16a34a;
  margin-top: 8px;
}
.word-nav {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.word-nav button {
  flex: 1;
  padding: 14px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  cursor: pointer;
  background: #fff;
  color: #333;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transition: all 0.2s;
}
.word-nav button:hover:not(:disabled) {
  background: #1a73e8;
  color: #fff;
}
.word-nav button:disabled { opacity: 0.4; cursor: not-allowed; }
</style>

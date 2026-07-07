<template>
  <div class="reading-practice">
    <NavBar />
    <div class="practice-content">
      <div class="breadcrumb" @click="back">← 返回模块</div>
      <h1>📖 阅读理解</h1>

      <div v-if="loading" class="loading">加载文章...</div>
      <div v-else-if="readingQueue.length === 0" class="empty-tip">暂无阅读题目</div>
      <div v-else>
        <div class="reading-progress">
          <span>随机阅读文章 {{ currentReadingIndex + 1 }} / {{ readingQueue.length }}</span>
          <span>{{ currentReading.title }}</span>
        </div>

        <div class="reading-card">
          <h2>{{ currentReading.title }}</h2>
          <div class="reading-text">{{ currentReading.content }}</div>
        </div>

        <div class="quiz-section">
          <h3>📝 阅读理解题</h3>
          <div v-for="(question, qi) in currentQuestions" :key="qi" class="question-item">
            <p class="question-text">{{ qi + 1 }}. {{ question.q }}</p>
            <div class="options">
              <label v-for="(opt, oi) in question.options" :key="oi" class="option"
                :class="{
                  correct: completed && oi === question.answer,
                  wrong: completed && oi === selectedAnswers[qi] && oi !== question.answer,
                  selected: selectedAnswers[qi] === oi
                }"
              >
                <input
                  type="radio"
                  :name="'reading-question-' + qi"
                  :value="oi"
                  v-model="selectedAnswers[qi]"
                  :disabled="completed"
                />
                <span>{{ String.fromCharCode(65 + oi) }}. {{ opt }}</span>
              </label>
            </div>

            <p v-if="completed" class="answer-feedback"
              :class="selectedAnswers[qi] === question.answer ? 'correct-text' : 'wrong-text'">
              {{ selectedAnswers[qi] === question.answer ? '✓ 回答正确' : '✗ 回答错误。正确答案是 ' + optionLetter(question.answer) }}
            </p>
          </div>

          <p v-if="!completed && !allAnswered" class="completion-hint">请完成本文全部题目后再提交。</p>

          <div class="quiz-actions">
            <button
              v-if="!completed"
              class="quiz-btn primary"
              type="button"
              :disabled="!allAnswered"
              @click="completeReading"
            >
              完成
            </button>
            <button
              v-else
              class="quiz-btn primary"
              type="button"
              @click="nextReading"
            >
              {{ hasNextReading ? '下一篇文章' : '重新随机练习' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '../../components/NavBar.vue'
import { getReadingsByModule, submitWrongRecord } from '../../utils/api'

const route = useRoute()
const router = useRouter()
const props = defineProps({ moduleCode: String })
const moduleCode = computed(() => props.moduleCode || route.params.moduleCode)

const readings = ref([])
const readingQueue = ref([])
const loading = ref(true)
const currentReadingIndex = ref(0)
const selectedAnswers = ref([])
const completed = ref(false)

const currentReading = computed(() => readingQueue.value[currentReadingIndex.value] || {})
const currentQuestions = computed(() => parseQuestions(currentReading.value.questions))
const hasNextReading = computed(() => currentReadingIndex.value < readingQueue.value.length - 1)
const allAnswered = computed(() => (
  currentQuestions.value.length > 0
  && currentQuestions.value.every((_, index) => selectedAnswers.value[index] !== null && selectedAnswers.value[index] !== undefined)
))

onMounted(async () => {
  try {
    const res = await getReadingsByModule(moduleCode.value)
    readings.value = res.data.data || []
    buildReadingQueue()
  } catch (e) {
    console.error('Failed to load readings', e)
  } finally {
    loading.value = false
  }
})

function back() { router.push(`/module/${moduleCode.value}`) }

function buildReadingQueue() {
  readingQueue.value = shuffle(readings.value.filter(reading => parseQuestions(reading.questions).length > 0))
  currentReadingIndex.value = 0
  resetCurrentReading()
}

function parseQuestions(questions) {
  try {
    return JSON.parse(questions || '[]')
  } catch {
    return []
  }
}

function shuffle(items) {
  const nextItems = [...items]
  for (let i = nextItems.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[nextItems[i], nextItems[j]] = [nextItems[j], nextItems[i]]
  }
  return nextItems
}

function resetCurrentReading() {
  selectedAnswers.value = new Array(currentQuestions.value.length).fill(null)
  completed.value = false
}

function optionLetter(index) {
  return String.fromCharCode(65 + index)
}

function completeReading() {
  if (!allAnswered.value || completed.value) return

  completed.value = true
  currentQuestions.value.forEach((q, index) => {
    const selected = selectedAnswers.value[index]
    if (selected === q.answer) return

    try {
      const user = JSON.parse(sessionStorage.getItem('currentUser'))
      if (user) {
        submitWrongRecord({
          userId: user.id,
          questionType: 'READING',
          contentId: currentReading.value.id,
          contentTitle: currentReading.value.title,
          questionText: q.q,
          userAnswer: optionLetter(selected),
          correctAnswer: optionLetter(q.answer),
          moduleCode: moduleCode.value
        })
      }
    } catch {}
  })
}

function nextReading() {
  if (!completed.value) return

  if (hasNextReading.value) {
    currentReadingIndex.value += 1
    resetCurrentReading()
    return
  }

  buildReadingQueue()
}
</script>

<style scoped>
.reading-practice { min-height: 100vh; background: #f0f2f5; }
.practice-content { max-width: 800px; margin: 0 auto; padding: 24px; }
.breadcrumb { font-size: 14px; color: #1a73e8; cursor: pointer; margin-bottom: 16px; }
.breadcrumb:hover { text-decoration: underline; }
.practice-content h1 { font-size: 26px; color: #1a1a1a; margin-bottom: 24px; }
.loading, .empty-tip { text-align: center; color: #999; padding: 60px; font-size: 16px; }
.reading-progress {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  background: #fff;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 13px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  color: #64748b;
  flex-wrap: wrap;
}
.reading-progress span:first-child {
  color: #1a73e8;
  font-weight: 700;
}
.reading-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.reading-card h2 { font-size: 20px; color: #1a1a1a; margin-bottom: 16px; }
.reading-text {
  font-size: 15px;
  line-height: 1.9;
  color: #444;
  white-space: pre-line;
}
.quiz-section { margin-bottom: 40px; }
.quiz-section h3 { font-size: 18px; margin-bottom: 20px; color: #333; }
.question-item { margin-bottom: 24px; }
.question-text { font-size: 15px; color: #333; margin-bottom: 12px; font-weight: 500; }
.options { display: flex; flex-direction: column; gap: 8px; }
.option {
  padding: 10px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.option:hover:not(.correct):not(.wrong) { border-color: #1a73e8; background: #f0f7ff; }
.option.correct { border-color: #22c55e; background: #f0fdf4; }
.option.wrong { border-color: #ef4444; background: #fef2f2; }
.option input { accent-color: #1a73e8; }
.answer-feedback {
  margin-top: 8px;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 6px;
}
.correct-text { color: #16a34a; }
.wrong-text { color: #dc2626; }
.completion-hint {
  color: #94a3b8;
  font-size: 13px;
  text-align: right;
}
.quiz-actions { margin-top: 18px; display: flex; justify-content: flex-end; }
.quiz-btn {
  border: none;
  border-radius: 999px;
  padding: 10px 22px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}
.quiz-btn.primary {
  background: #1a73e8;
  color: #fff;
}
.quiz-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
@media (max-width: 768px) {
  .reading-progress {
    flex-direction: column;
  }
  .quiz-btn {
    width: 100%;
  }
}
</style>

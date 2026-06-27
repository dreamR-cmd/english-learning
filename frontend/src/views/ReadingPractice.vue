<template>
  <div class="reading-practice">
    <NavBar />
    <div class="practice-content">
      <div class="breadcrumb" @click="back">← 返回模块</div>
      <h1>📄 阅读理解</h1>

      <div v-if="loading" class="loading">加载文章...</div>
      <div v-else-if="readings.length === 0" class="empty-tip">暂无文章</div>
      <div v-else>
        <div class="reading-nav">
          <span
            v-for="(r, i) in readings"
            :key="r.id"
            class="reading-tab"
            :class="{ active: currentIndex === i }"
            @click="currentIndex = i; resetQuiz()"
          >{{ r.title.substring(0, 12) }}{{ r.title.length > 12 ? '...' : '' }}</span>
        </div>

        <div class="reading-card">
          <h2>{{ currentReading.title }}</h2>
          <div class="reading-text">{{ currentReading.content }}</div>
        </div>

        <div v-if="currentReading.questions" class="quiz-section">
          <h3>📝 阅读理解题</h3>
          <div v-for="(q, qi) in parsedQuestions" :key="qi" class="question-item">
            <p class="question-text">{{ qi + 1 }}. {{ q.q }}</p>
            <div class="options">
              <label v-for="(opt, oi) in q.options" :key="oi" class="option"
                :class="{
                  correct: answered[qi] && oi === q.answer,
                  wrong: answered[qi] && oi === selectedAnswers[qi] && oi !== q.answer,
                  selected: selectedAnswers[qi] === oi
                }"
              >
                <input
                  type="radio"
                  :name="'q' + qi"
                  :value="oi"
                  v-model="selectedAnswers[qi]"
                  @change="checkAnswer(qi)"
                  :disabled="answered[qi]"
                />
                <span>{{ String.fromCharCode(65 + oi) }}. {{ opt }}</span>
              </label>
            </div>
            <p v-if="answered[qi]" class="answer-feedback"
              :class="selectedAnswers[qi] === q.answer ? 'correct-text' : 'wrong-text'">
              {{ selectedAnswers[qi] === q.answer ? '✓ 回答正确！' : '✗ 回答错误。正确答案是 ' + String.fromCharCode(65 + q.answer) }}
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import { getReadingsByModule, submitWrongRecord } from '../utils/api'

const route = useRoute()
const router = useRouter()
const props = defineProps({ moduleCode: String })
const moduleCode = computed(() => props.moduleCode || route.params.moduleCode)

const readings = ref([])
const loading = ref(true)
const currentIndex = ref(0)
const selectedAnswers = ref([])
const answered = ref([])

const currentReading = computed(() => readings.value[currentIndex.value] || {})
const parsedQuestions = computed(() => {
  try {
    return JSON.parse(currentReading.value.questions || '[]')
  } catch { return [] }
})

onMounted(async () => {
  try {
    const res = await getReadingsByModule(moduleCode.value)
    readings.value = res.data.data || []
    resetQuiz()
  } catch (e) {
    console.error('Failed to load readings', e)
  } finally {
    loading.value = false
  }
})

function resetQuiz() {
  selectedAnswers.value = new Array(parsedQuestions.value.length).fill(null)
  answered.value = new Array(parsedQuestions.value.length).fill(false)
}
function back() { router.push(`/module/${moduleCode.value}`) }
function checkAnswer(qi) {
  answered.value[qi] = true
  const q = parsedQuestions.value[qi]
  if (!q) return
  const selected = selectedAnswers.value[qi]
  if (selected !== q.answer) {
    try {
      const user = JSON.parse(sessionStorage.getItem('currentUser'))
      if (user) {
        submitWrongRecord({
          userId: user.id,
          questionType: 'READING',
          contentId: currentReading.value.id,
          contentTitle: currentReading.value.title,
          questionText: q.q,
          userAnswer: String.fromCharCode(65 + (selected ?? -1)),
          correctAnswer: String.fromCharCode(65 + q.answer),
          moduleCode: moduleCode.value
        })
      }
    } catch {}
  }
}
</script>

<style scoped>
.reading-practice { min-height: 100vh; background: #f0f2f5; }
.practice-content { max-width: 800px; margin: 0 auto; padding: 24px; }
.breadcrumb { font-size: 14px; color: #1a73e8; cursor: pointer; margin-bottom: 16px; }
.breadcrumb:hover { text-decoration: underline; }
.practice-content h1 { font-size: 26px; color: #1a1a1a; margin-bottom: 24px; }
.loading, .empty-tip { text-align: center; color: #999; padding: 60px; font-size: 16px; }
.reading-nav {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  overflow-x: auto;
  padding-bottom: 8px;
}
.reading-tab {
  padding: 8px 16px;
  background: #fff;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  transition: all 0.2s;
}
.reading-tab.active {
  background: #1a73e8;
  color: #fff;
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
</style>

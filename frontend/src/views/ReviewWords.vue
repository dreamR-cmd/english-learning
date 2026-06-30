<template>
  <div class="sub-page">
    <NavBar />
    <main class="sub-content">
      <section class="sub-hero">
        <div>
          <p class="sub-kicker">Review Words</p>
          <h1>复习模块</h1>
          <p>集中复习已累计认识 4 次的单词。</p>
        </div>
        <button class="back-btn" type="button" @click="back">返回个人中心</button>
      </section>

      <div v-if="loading" class="state-card">加载复习单词...</div>
      <div v-else-if="reviewWords.length === 0" class="state-card empty">
        <div class="empty-icon">🧠</div>
        <p>暂无复习单词</p>
        <small>单词在练习中累计认识 4 次后，会进入这里。</small>
      </div>

      <section v-else class="word-list">
        <article v-for="item in reviewWords" :key="item.id" class="word-card">
          <div class="word-main">
            <div class="word-header">
              <h2>{{ item.word?.word || item.wordId }}</h2>
              <span>已认识 {{ item.knownCount }}/4 次</span>
            </div>
            <p v-if="item.word?.phonetic" class="phonetic">{{ item.word.phonetic }}</p>
            <p class="meaning">{{ item.word?.meaning || '暂无释义' }}</p>
            <p v-if="item.word?.example" class="example">{{ item.word.example }}</p>
          </div>
          <button class="reset-btn" type="button" @click="markUnknown(item)">不认识</button>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import { getReviewWords, resetWordProgress } from '../utils/api'
import { currentUser } from '../utils/currentUser'

const router = useRouter()
const user = currentUser
const loading = ref(true)
const reviewWords = ref([])

function back() {
  router.push('/profile')
}

async function markUnknown(item) {
  if (!user.value || !item?.wordId) return

  await resetWordProgress(user.value.id, item.wordId)
  reviewWords.value = reviewWords.value.filter(word => word.wordId !== item.wordId)
}

onMounted(async () => {
  if (!user.value) return

  try {
    const response = await getReviewWords(user.value.id)
    reviewWords.value = response.data.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.sub-page { min-height: 100vh; background: #f3f6fb; }
.sub-content { max-width: 920px; margin: 0 auto; padding: 32px 24px 48px; }
.sub-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 22px; }
.sub-kicker { color: #15803d; font-size: 12px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; margin-bottom: 8px; }
.sub-hero h1 { font-size: 32px; color: #172033; margin-bottom: 8px; }
.sub-hero p { color: #667085; }
.back-btn { border: none; border-radius: 999px; background: #172033; color: #fff; padding: 11px 18px; font-weight: 800; cursor: pointer; }
.state-card { background: #fff; border-radius: 18px; padding: 46px 20px; text-align: center; color: #64748b; border: 1px solid #e2e8f0; }
.empty-icon { font-size: 48px; margin-bottom: 10px; }
.empty small { color: #94a3b8; }
.word-list { display: flex; flex-direction: column; gap: 14px; }
.word-card { background: #fff; border-radius: 18px; padding: 20px; border: 1px solid #e2e8f0; box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05); display: flex; align-items: flex-start; gap: 16px; }
.word-main { flex: 1; min-width: 0; }
.word-header { display: flex; align-items: center; gap: 10px; justify-content: space-between; flex-wrap: wrap; margin-bottom: 8px; }
.word-header h2 { color: #172033; font-size: 20px; margin: 0; }
.word-header span { background: #ecfdf5; color: #15803d; border-radius: 999px; padding: 5px 10px; font-size: 12px; font-weight: 800; }
.phonetic { color: #64748b; margin-bottom: 8px; }
.meaning, .example { color: #334155; line-height: 1.7; }
.example { color: #64748b; margin-top: 8px; }
.reset-btn { flex-shrink: 0; border: 1px solid #fca5a5; border-radius: 999px; background: #fff; color: #dc2626; padding: 9px 15px; font-weight: 800; cursor: pointer; }
.reset-btn:hover { background: #fef2f2; }
@media (max-width: 768px) {
  .sub-content { padding: 24px 16px 36px; }
  .sub-hero, .word-card { flex-direction: column; align-items: flex-start; }
  .back-btn, .reset-btn { width: 100%; }
}
</style>

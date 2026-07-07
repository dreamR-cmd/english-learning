<template>
  <div class="sub-page">
    <NavBar />
    <main class="sub-content">
      <section class="sub-hero">
        <div>
          <p class="sub-kicker">Favorites</p>
          <h1>收藏夹</h1>
          <p>集中管理已收藏的精选阅读内容。</p>
        </div>
        <button class="back-btn" type="button" @click="back">返回个人中心</button>
      </section>

      <div v-if="loading" class="state-card">加载收藏...</div>
      <div v-else-if="favorites.length === 0" class="state-card empty">
        <div class="empty-icon">★</div>
        <p>暂无收藏</p>
        <small>在精选阅读中点击收藏即可保存文章。</small>
      </div>

      <section v-else class="favorite-list">
        <article
          v-for="favorite in favorites"
          :key="favorite.id"
          class="favorite-card"
          @click="goToReading(favorite.reading)"
        >
          <div class="favorite-main">
            <h2>{{ favorite.reading?.title || '精选阅读' }}</h2>
            <p>{{ favorite.reading?.content?.substring(0, 140) }}...</p>
            <div class="favorite-meta">
              <span>{{ getModuleName(favorite.reading) }}</span>
              <span>{{ formatTime(favorite.createdAt) }}</span>
            </div>
          </div>
          <button class="remove-btn" type="button" @click.stop="removeItem(favorite.readingId)">
            取消收藏
          </button>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../../components/NavBar.vue'
import { getFavorites, removeFavorite } from '../../utils/api'
import { currentUser } from '../../utils/currentUser'

const router = useRouter()
const user = currentUser
const loading = ref(true)
const favorites = ref([])

function back() {
  router.push('/profile')
}

function getModuleName(reading) {
  if (!reading || !reading.module) return ''
  return reading.module.name || reading.module.code || ''
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function goToReading(reading) {
  if (!reading?.module?.code) return
  router.push(`/practice/readings/${reading.module.code}`)
}

async function removeItem(readingId) {
  if (!user.value) return

  await removeFavorite(user.value.id, readingId)
  favorites.value = favorites.value.filter(item => item.readingId !== readingId)
}

onMounted(async () => {
  if (!user.value) return

  try {
    const response = await getFavorites(user.value.id)
    favorites.value = response.data.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.sub-page { min-height: 100vh; background: #f3f6fb; }
.sub-content { max-width: 920px; margin: 0 auto; padding: 32px 24px 48px; }
.sub-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 22px; }
.sub-kicker { color: #b45309; font-size: 12px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; margin-bottom: 8px; }
.sub-hero h1 { font-size: 32px; color: #172033; margin-bottom: 8px; }
.sub-hero p { color: #667085; }
.back-btn { border: none; border-radius: 999px; background: #172033; color: #fff; padding: 11px 18px; font-weight: 800; cursor: pointer; }
.state-card { background: #fff; border-radius: 18px; padding: 46px 20px; text-align: center; color: #64748b; border: 1px solid #e2e8f0; }
.empty-icon { font-size: 48px; margin-bottom: 10px; }
.empty small { color: #94a3b8; }
.favorite-list { display: flex; flex-direction: column; gap: 14px; }
.favorite-card { background: #fff; border-radius: 18px; padding: 20px; border: 1px solid #e2e8f0; box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05); display: flex; align-items: center; gap: 16px; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
.favorite-card:hover { transform: translateY(-2px); box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08); }
.favorite-main { flex: 1; min-width: 0; }
.favorite-main h2 { color: #172033; font-size: 18px; margin-bottom: 8px; }
.favorite-main p { color: #64748b; line-height: 1.7; margin-bottom: 10px; }
.favorite-meta { display: flex; gap: 12px; flex-wrap: wrap; color: #94a3b8; font-size: 12px; }
.favorite-meta span:first-child { color: #1a73e8; background: #edf4ff; border-radius: 6px; padding: 3px 8px; }
.remove-btn { flex-shrink: 0; border: 1px solid #e5e7eb; border-radius: 999px; background: #fff; color: #64748b; padding: 9px 15px; font-weight: 800; cursor: pointer; }
.remove-btn:hover { border-color: #ef4444; color: #dc2626; background: #fef2f2; }
@media (max-width: 768px) {
  .sub-content { padding: 24px 16px 36px; }
  .sub-hero, .favorite-card { flex-direction: column; align-items: flex-start; }
  .back-btn, .remove-btn { width: 100%; }
}
</style>

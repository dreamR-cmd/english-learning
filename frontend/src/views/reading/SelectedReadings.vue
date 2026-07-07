<template>
  <div class="selected-page">
    <NavBar />
    <main class="selected-content">
      <section class="hero">
        <div class="hero-main">
          <div>
            <p class="eyebrow">Selected Reading</p>
            <h1>精选读物</h1>
            <p>
              这里整理的是适合英语学习者的外部阅读资源，不作为阅读理解刷题文章。
              可以按难度和兴趣选择材料，重点训练长期阅读能力。
            </p>
          </div>
          <button class="back-btn" type="button" @click="backToModules">返回等级考试模块</button>
        </div>
      </section>

      <div v-if="loading" class="state-card">加载精选读物...</div>
      <div v-else-if="errorMessage" class="state-card error">{{ errorMessage }}</div>

      <section class="source-note">
        <strong>来源说明</strong>
        <span>优先选择官方学习网站和公版读物入口；部分经典读物来自 Project Gutenberg，可免费在线阅读。</span>
      </section>

      <section v-if="!loading && !errorMessage" class="reading-grid">
        <article v-for="item in readings" :key="item.title" class="reading-card">
          <div class="card-top">
            <span class="source-pill">{{ item.source }}</span>
            <span class="level-pill">{{ item.level }}</span>
          </div>
          <h2>{{ item.title }}</h2>
          <p class="desc">{{ item.description }}</p>
          <div class="meta-row">
            <span>{{ item.type }}</span>
            <span>{{ item.suggestedFor }}</span>
          </div>
          <div class="card-actions">
            <button class="open-link" type="button" @click="openReading(item)">开始阅读</button>
            <button
              class="favorite-btn"
              :class="{ favorited: item.favorited }"
              type="button"
              :disabled="favoriteLoadingId === item.id"
              @click="toggleFavorite(item)"
            >
              {{ favoriteLoadingId === item.id ? '处理中...' : (item.favorited ? '已收藏' : '收藏') }}
            </button>
          </div>
        </article>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="activeReading" class="reader-overlay" @click.self="closeReading">
        <article class="reader-dialog">
          <header class="reader-header">
            <div>
              <p class="eyebrow">{{ activeReading.source }} · {{ activeReading.level }}</p>
              <h2>{{ activeReading.title }}</h2>
            </div>
            <button class="reader-close" type="button" @click="closeReading">×</button>
          </header>
          <div class="reader-meta">
            <span>{{ activeReading.type }}</span>
            <span>{{ activeReading.suggestedFor }}</span>
          </div>
          <div class="reader-body">
            <p
              v-for="paragraph in readingParagraphs"
              :key="paragraph"
            >
              {{ paragraph }}
            </p>
          </div>
        </article>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../../components/NavBar.vue'
import {
  addSelectedReadingFavorite,
  getSelectedReadings,
  removeSelectedReadingFavorite
} from '../../utils/api'
import { currentUser } from '../../utils/currentUser'

const router = useRouter()
const user = currentUser
const readings = ref([])
const loading = ref(true)
const errorMessage = ref('')
const favoriteLoadingId = ref(null)
const activeReading = ref(null)
const readingParagraphs = computed(() =>
  String(activeReading.value?.content || '').split(/\n+/).map(item => item.trim()).filter(Boolean)
)

function backToModules() {
  router.push('/modules')
}

async function loadReadings() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await getSelectedReadings(user.value?.id)
    readings.value = response.data.data || []
  } catch (error) {
    console.error('Failed to load selected readings', error)
    errorMessage.value = '精选读物加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function toggleFavorite(item) {
  if (!user.value) {
    errorMessage.value = '请先登录后再收藏'
    return
  }

  favoriteLoadingId.value = item.id
  errorMessage.value = ''
  try {
    if (item.favorited) {
      await removeSelectedReadingFavorite(user.value.id, item.id)
      item.favorited = false
    } else {
      await addSelectedReadingFavorite(user.value.id, item.id)
      item.favorited = true
    }
  } catch (error) {
    console.error('Failed to toggle selected reading favorite', error)
    errorMessage.value = '收藏操作失败，请稍后重试'
  } finally {
    favoriteLoadingId.value = null
  }
}

function openReading(item) {
  activeReading.value = item
}

function closeReading() {
  activeReading.value = null
}

onMounted(loadReadings)
</script>

<style scoped>
.selected-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% 8%, rgba(34, 197, 94, 0.16), transparent 28%),
    radial-gradient(circle at 88% 10%, rgba(245, 158, 11, 0.16), transparent 26%),
    linear-gradient(180deg, #f7faf5 0%, #f5f7fb 100%);
}
.selected-content {
  max-width: 1180px;
  margin: 0 auto;
  padding: 34px 24px 56px;
}
.hero {
  position: relative;
  overflow: hidden;
  border-radius: 28px;
  padding: 38px;
  color: #102018;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(240, 253, 244, 0.9)),
    #fff;
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
}
.hero-main {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
}
.back-btn {
  border: none;
  border-radius: 999px;
  background: #102018;
  color: #fff;
  padding: 10px 16px;
  font-weight: 800;
  cursor: pointer;
  flex-shrink: 0;
}
.eyebrow {
  color: #15803d;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  margin-bottom: 10px;
}
.hero h1 {
  font-size: clamp(34px, 7vw, 72px);
  line-height: 0.95;
  margin-bottom: 18px;
}
.hero-main p:last-child {
  max-width: 700px;
  color: #526057;
  font-size: 16px;
  line-height: 1.9;
}
.source-note {
  display: flex;
  gap: 12px;
  align-items: center;
  margin: 22px 0;
  padding: 14px 18px;
  border: 1px solid #d9eadf;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  color: #526057;
}
.source-note strong {
  color: #15803d;
  white-space: nowrap;
}
.state-card {
  margin: 22px 0;
  border: 1px solid #d9eadf;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  color: #526057;
  padding: 24px;
  text-align: center;
}
.state-card.error {
  border-color: #fecaca;
  background: #fef2f2;
  color: #dc2626;
}
.reading-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}
.reading-card {
  display: flex;
  flex-direction: column;
  min-height: 310px;
  padding: 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e2e8f0;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
  transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
}
.reading-card:hover {
  transform: translateY(-5px);
  border-color: #86efac;
  box-shadow: 0 18px 40px rgba(22, 101, 52, 0.12);
}
.card-top,
.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.source-pill,
.level-pill {
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 800;
}
.source-pill {
  background: #ecfdf5;
  color: #15803d;
}
.level-pill {
  background: #eff6ff;
  color: #1d4ed8;
}
.reading-card h2 {
  margin: 18px 0 12px;
  color: #102018;
  font-size: 22px;
  line-height: 1.25;
}
.desc {
  color: #526057;
  line-height: 1.75;
  flex: 1;
}
.meta-row {
  margin: 18px 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}
.open-link {
  border: none;
  display: inline-flex;
  justify-content: center;
  flex: 1;
  border-radius: 999px;
  background: #15803d;
  color: #fff;
  padding: 11px 16px;
  text-decoration: none;
  font-weight: 900;
  cursor: pointer;
}
.open-link:hover {
  background: #166534;
}
.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.favorite-btn {
  flex: 1;
  border: 1px solid #bbf7d0;
  border-radius: 999px;
  background: #fff;
  color: #15803d;
  padding: 11px 16px;
  font-weight: 900;
  cursor: pointer;
}
.favorite-btn.favorited {
  background: #ecfdf5;
}
.favorite-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}
.reader-overlay {
  position: fixed;
  inset: 0;
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.56);
}
.reader-dialog {
  width: min(920px, 100%);
  max-height: min(88vh, 900px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.32);
}
.reader-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 28px 32px 18px;
  border-bottom: 1px solid #edf2f7;
}
.reader-header h2 {
  margin: 0;
  color: #102018;
  font-size: 30px;
  line-height: 1.2;
}
.reader-close {
  border: none;
  border-radius: 50%;
  width: 38px;
  height: 38px;
  background: #f1f5f9;
  color: #334155;
  font-size: 26px;
  cursor: pointer;
}
.reader-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: 18px 32px 0;
}
.reader-meta span {
  border-radius: 999px;
  background: #ecfdf5;
  color: #15803d;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 800;
}
.reader-body {
  padding: 24px 32px 34px;
  overflow-y: auto;
}
.reader-body p {
  margin: 0 0 18px;
  color: #334155;
  font-size: 17px;
  line-height: 2;
}
@media (max-width: 720px) {
  .selected-content {
    padding: 24px 16px 40px;
  }
  .hero {
    padding: 26px;
  }
  .hero-main {
    flex-direction: column;
    align-items: flex-start;
  }
  .back-btn {
    width: 100%;
  }
  .source-note {
    align-items: flex-start;
    flex-direction: column;
  }
  .reader-overlay {
    padding: 12px;
  }
  .reader-header,
  .reader-meta,
  .reader-body {
    padding-left: 20px;
    padding-right: 20px;
  }
  .reader-header h2 {
    font-size: 24px;
  }
}
</style>

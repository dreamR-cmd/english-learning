 <template>
   <div class="module-detail-page">
     <NavBar />
     <div class="detail-content">
       <div class="breadcrumb" @click="backToModules">← 返回等级选择</div>
       <div class="module-banner">
         <span class="banner-icon">{{ module?.icon }}</span>
         <h1>{{ module?.name }}</h1>
         <p>{{ module?.description }}</p>
       </div>
 
       <div class="section">
         <h2 class="section-title">📝 专项练习</h2>
         <div class="practice-grid">
           <div class="practice-card" @click="goToPractice('words')">
             <div class="card-icon">📖</div>
             <h3>单词练习</h3>
             <p>通过卡片记忆方式学习核心词汇</p>
           </div>
           <div class="practice-card" @click="goToPractice('readings')">
             <div class="card-icon">📄</div>
             <h3>阅读理解</h3>
             <p>阅读文章并完成理解题目</p>
           </div>
           <div class="practice-card" @click="goToPractice('listenings')">
             <div class="card-icon">🎧</div>
             <h3>听力练习</h3>
             <p>听录音材料并回答问题</p>
           </div>
         </div>
       </div>
 
       <div class="section">
         <h2 class="section-title">⭐ 精选读物</h2>
         <div v-if="readings.length === 0" class="empty-tip">暂无精选读物</div>
         <div v-else class="reading-list">
           <div v-for="r in readings" :key="r.id" class="reading-item" @click="goToReading(r)">
             <div class="reading-item-main">
               <h4>{{ r.title }}</h4>
               <p class="reading-preview">{{ r.content?.substring(0, 120) }}...</p>
             </div>
             <button class="btn-fav" :class="{ favorited: favoritedMap[r.id] }" @click.stop="toggleFavorite(r)">
               {{ favoritedMap[r.id] ? '⭐' : '☆' }}
             </button>
           </div>
         </div>
       </div>
     </div>
   </div>
 </template>
 
 <script setup>
 import { ref, onMounted, computed } from 'vue'
 import { useRoute, useRouter } from 'vue-router'
 import NavBar from '../components/NavBar.vue'
import { getModuleByCode, getReadingsByModule, addFavorite, removeFavorite, checkFavorite } from '../utils/api'
 
 const props = defineProps({ code: String })
 const route = useRoute()
 const router = useRouter()
 const module = ref({})
 const readings = ref([])
const favoritedMap = ref({})
 const moduleCode = computed(() => props.code || route.params.code)
 
 onMounted(async () => {
   try {
     const [modRes, readRes] = await Promise.all([
       getModuleByCode(moduleCode.value),
       getReadingsByModule(moduleCode.value)
     ])
     module.value = modRes.data.data || {}
    const allReadings = readRes.data.data || []
    readings.value = allReadings.filter(r => r.featured)
    // Check which readings are favorited
    try {
      const user = JSON.parse(sessionStorage.getItem('currentUser'))
      if (user) {
        for (const r of readings.value) {
          const favRes = await checkFavorite(user.id, r.id)
          if (favRes.data.data) favoritedMap.value[r.id] = true
        }
      }
    } catch {}
   } catch (e) {
     console.error('Failed to load module', e)
   }
 })
 
async function toggleFavorite(reading) {
  try {
    const user = JSON.parse(sessionStorage.getItem('currentUser'))
    if (!user) return
    if (favoritedMap.value[reading.id]) {
      await removeFavorite(user.id, reading.id)
      favoritedMap.value[reading.id] = false
    } else {
      await addFavorite(user.id, reading.id)
      favoritedMap.value[reading.id] = true
    }
  } catch {}
}
function goToReading(reading) {
  router.push(`/practice/readings/${moduleCode.value}`)
}

 function backToModules() { router.push('/modules') }
 function goToPractice(type) {
   router.push(`/practice/${type}/${moduleCode.value}`)
 }
 </script>
 
 <style scoped>
 .module-detail-page {
   min-height: 100vh;
   background: #f0f2f5;
 }
 .detail-content {
   max-width: 900px;
   margin: 0 auto;
   padding: 24px;
 }
 .breadcrumb {
   font-size: 14px;
   color: #1a73e8;
   cursor: pointer;
   margin-bottom: 16px;
   padding: 8px 0;
 }
 .breadcrumb:hover { text-decoration: underline; }
 .module-banner {
   background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
   border-radius: 16px;
   padding: 40px;
   color: #fff;
   text-align: center;
   margin-bottom: 32px;
 }
 .banner-icon { font-size: 64px; display: block; margin-bottom: 12px; }
 .module-banner h1 { font-size: 28px; margin-bottom: 8px; }
 .module-banner p { font-size: 14px; opacity: 0.85; }
 .section { margin-bottom: 40px; }
 .section-title {
   font-size: 22px;
   color: #1a1a1a;
   margin-bottom: 20px;
   padding-bottom: 12px;
   border-bottom: 2px solid #e8e8e8;
 }
 .practice-grid {
   display: grid;
   grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
   gap: 20px;
 }
 .practice-card {
   background: #fff;
   border-radius: 12px;
   padding: 28px 20px;
   text-align: center;
   cursor: pointer;
   transition: all 0.3s;
   box-shadow: 0 2px 8px rgba(0,0,0,0.06);
 }
 .practice-card:hover {
   transform: translateY(-3px);
   box-shadow: 0 8px 24px rgba(0,0,0,0.1);
 }
 .card-icon { font-size: 40px; display: block; margin-bottom: 12px; }
 .practice-card h3 { font-size: 18px; color: #333; margin-bottom: 8px; }
 .practice-card p { font-size: 13px; color: #999; line-height: 1.5; }
 .empty-tip { color: #999; text-align: center; padding: 40px; }
 .reading-list { display: flex; flex-direction: column; gap: 16px; }
 .reading-item {
   background: #fff;
   border-radius: 12px;
   padding: 20px 24px;
   box-shadow: 0 2px 8px rgba(0,0,0,0.06);
 }
 .reading-item-main { flex: 1; min-width: 0; }
.reading-item h4 { font-size: 16px; color: #333; margin-bottom: 8px; }
.reading-preview { font-size: 13px; color: #888; line-height: 1.6; }
 .btn-fav {
  flex-shrink: 0;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.2s;
  color: #ccc;
}
.btn-fav:hover {
  background: #fffbeb;
  transform: scale(1.2);
}
.btn-fav.favorited {
  color: #f59e0b;
}
</style>

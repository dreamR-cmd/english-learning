 <template>
   <div class="modules-page">
     <NavBar />
     <main class="modules-content">
       <!-- <section class="page-hero">
         <p class="page-kicker">Learning Hub</p>
         <h1>学习中心</h1>
         <p>等级考试、学习商城和精选读物统一入口，按目标选择学习路径。</p>
       </section> -->
       <div v-if="loading" class="loading">加载中...</div>
       <div v-else class="modules-grid">
         <div class="module-card shop-card" @click="goToShop">
           <span class="module-icon">🛒</span>
           <h3>学习商城</h3>
           <p class="module-desc">精选课程、真题资料与备考书籍，配合等级考试模块系统学习。</p>
           <div class="countdown-panel shop-panel">
             <div class="countdown-meta">
               <span class="countdown-label">精选商品</span>
               <span class="countdown-date">课程 / 图书</span>
             </div>
             <div class="countdown-value">进入商城</div>
           </div>
         </div>
         <div class="module-card selected-reading-card" @click="goToSelectedReadings">
           <span class="module-icon">📚</span>
           <h3>精选读物</h3>
           <p class="module-desc">独立于阅读理解题库，整理适合英语学习者的分级读物和公版经典。</p>
           <div class="countdown-panel reading-panel">
             <div class="countdown-meta">
               <span class="countdown-label">外部读物</span>
               <span class="countdown-date">分级 / 经典</span>
             </div>
             <div class="countdown-value">开始阅读</div>
           </div>
         </div>
         <div
           v-for="mod in modulesWithCountdown"
           :key="mod.id"
           class="module-card"
           @click="goToModule(mod.code)"
         >
           <span class="module-icon">{{ mod.icon }}</span>
           <h3>{{ mod.name }}</h3>
           <p class="module-desc">{{ mod.description }}</p>
           <div class="countdown-panel">
             <div class="countdown-meta">
               <span class="countdown-label">{{ mod.examCountdown.label }}</span>
               <span class="countdown-date">{{ mod.examCountdown.dateText }}</span>
             </div>
             <div
               class="countdown-value"
               :class="{ urgent: mod.examCountdown.urgent }"
             >
               {{ mod.examCountdown.countdownText }}
             </div>
           </div>
         </div>
       </div>
     </main>
   </div>
 </template>
 
 <script setup>
 import { computed, ref, onMounted } from 'vue'
 import { useRouter } from 'vue-router'
 import NavBar from '../components/NavBar.vue'
 import { getModules } from '../utils/api'

 const router = useRouter()
 const modules = ref([])
 const loading = ref(true)
 const fallbackCountdown = {
   label: '考试安排',
   dateText: '待更新',
   countdownText: '敬请期待',
   urgent: false
 }
 const modulesWithCountdown = computed(() =>
   modules.value.map(mod => ({
     ...mod,
     examCountdown: mod.examCountdown || fallbackCountdown
   }))
 )
 
 onMounted(async () => {
   try {
     const res = await getModules()
     modules.value = res.data.data || []
   } catch (e) {
     console.error('Failed to load modules', e)
   } finally {
     loading.value = false
   }
 })
 
 function goToModule(code) {
   router.push(`/module/${code}`)
 }

 function goToShop() {
   router.push('/shop')
 }

 function goToSelectedReadings() {
   router.push('/selected-readings')
 }
 </script>
 
 <style scoped>
 .modules-page {
   min-height: 100vh;
   background:
     radial-gradient(circle at 10% 8%, rgba(34, 197, 94, 0.16), transparent 28%),
     radial-gradient(circle at 88% 10%, rgba(245, 158, 11, 0.16), transparent 26%),
     linear-gradient(180deg, #f7faf5 0%, #f5f7fb 100%);
 }
 .modules-content {
   max-width: 1200px;
   margin: 0 auto;
   padding: 36px 24px 56px;
 }
 .page-hero {
   margin-bottom: 28px;
   border-radius: 28px;
   padding: 38px;
   background:
     linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(240, 253, 244, 0.88)),
     #fff;
   border: 1px solid rgba(148, 163, 184, 0.22);
   box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
 }
 .page-kicker {
   color: #15803d;
   font-size: 12px;
   font-weight: 900;
   letter-spacing: 0.18em;
   text-transform: uppercase;
   margin-bottom: 10px;
 }
 .page-hero h1 {
   color: #102018;
   font-size: clamp(34px, 7vw, 72px);
   line-height: 0.95;
   margin-bottom: 18px;
 }
 .page-hero p:last-child {
   max-width: 680px;
   color: #526057;
   font-size: 16px;
   line-height: 1.9;
 }
 .loading {
   text-align: center;
   color: #999;
   padding: 60px;
   font-size: 16px;
 }
 .modules-grid {
   display: grid;
   grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
   gap: 24px;
 }
 .module-card {
   background: rgba(255, 255, 255, 0.92);
   border-radius: 24px;
   padding: 32px 24px;
   display: flex;
   flex-direction: column;
   text-align: center;
   cursor: pointer;
   transition: all 0.3s ease;
   box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
   border: 1px solid rgba(226, 232, 240, 0.96);
 }
 .module-card:hover {
   transform: translateY(-5px);
   box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
   border-color: #86efac;
 }
 .shop-card {
   background:
     linear-gradient(135deg, rgba(255, 248, 229, 0.96), rgba(255, 255, 255, 0.98)),
     #fff;
   border-color: #f3d28b;
 }
 .shop-card:hover {
   border-color: #f59e0b;
   box-shadow: 0 14px 34px rgba(180, 83, 9, 0.16);
 }
 .module-icon {
   font-size: 48px;
   display: block;
   margin-bottom: 16px;
 }
 .module-card h3 {
   font-size: 20px;
   color: #102018;
   margin-bottom: 8px;
 }
 .module-desc {
   font-size: 13px;
   color: #526057;
   line-height: 1.6;
   min-height: 42px;
   margin-bottom: 18px;
 }
 .countdown-panel {
   margin-top: auto;
   padding: 14px 16px;
   border-radius: 16px;
   background: linear-gradient(135deg, #f8fbff 0%, #eef5ff 100%);
   border: 1px solid #dbe8ff;
 }
 .countdown-meta {
   display: flex;
   align-items: center;
   justify-content: space-between;
   gap: 12px;
   margin-bottom: 8px;
   font-size: 12px;
 }
 .countdown-label {
   color: #5f6b7a;
 }
 .countdown-date {
   color: #15803d;
   font-weight: 600;
 }
 .countdown-value {
   font-size: 18px;
   font-weight: 700;
   color: #102018;
 }
 .countdown-value.urgent {
   color: #d93025;
  }
 .shop-panel {
   background: linear-gradient(135deg, #fff7ed 0%, #fffbeb 100%);
   border-color: #fed7aa;
 }
 .shop-panel .countdown-date,
 .shop-panel .countdown-value {
   color: #b45309;
 }
 .selected-reading-card {
   background:
     radial-gradient(circle at top left, rgba(34, 197, 94, 0.16), transparent 34%),
     linear-gradient(135deg, rgba(240, 253, 244, 0.98), rgba(255, 255, 255, 0.98)),
     #fff;
   border-color: #bbf7d0;
 }
 .selected-reading-card:hover {
   border-color: #16a34a;
   box-shadow: 0 14px 34px rgba(22, 101, 52, 0.14);
 }
 .reading-panel {
   background: linear-gradient(135deg, #f0fdf4 0%, #ecfeff 100%);
   border-color: #bbf7d0;
 }
 .reading-panel .countdown-date,
 .reading-panel .countdown-value {
   color: #15803d;
 }
 @media (max-width: 720px) {
   .modules-content {
     padding: 24px 16px 40px;
   }
   .page-hero {
     padding: 26px;
   }
 }
 </style>

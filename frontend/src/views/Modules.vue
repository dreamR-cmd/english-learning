 <template>
   <div class="modules-page">
     <NavBar />
     <div class="modules-content">
       <div class="page-header">
         <h1>选择学习等级</h1>
         <p>Choose Your English Learning Level</p>
       </div>
       <div v-if="loading" class="loading">加载中...</div>
       <div v-else class="modules-grid">
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
               :class="{ urgent: mod.examCountdown.isUrgent }"
             >
               {{ mod.examCountdown.countdownText }}
             </div>
           </div>
         </div>
       </div>
     </div>
   </div>
 </template>
 
 <script setup>
 import { computed, ref, onMounted } from 'vue'
 import { useRouter } from 'vue-router'
 import NavBar from '../components/NavBar.vue'
 import { getModules } from '../utils/api'
 import { getExamCountdown } from '../utils/examCountdown'
 
 const router = useRouter()
 const modules = ref([])
 const loading = ref(true)
 const modulesWithCountdown = computed(() =>
   modules.value.map(mod => ({
     ...mod,
     examCountdown: getExamCountdown(mod.code)
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
 </script>
 
 <style scoped>
 .modules-page {
   min-height: 100vh;
   background: #f0f2f5;
 }
 .modules-content {
   max-width: 1200px;
   margin: 0 auto;
   padding: 40px 24px;
 }
 .page-header {
   text-align: center;
   margin-bottom: 48px;
 }
 .page-header h1 {
   font-size: 32px;
   color: #1a1a1a;
   margin-bottom: 8px;
 }
 .page-header p {
   color: #999;
   font-size: 16px;
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
   background: #fff;
   border-radius: 12px;
   padding: 32px 24px;
   display: flex;
   flex-direction: column;
   text-align: center;
   cursor: pointer;
   transition: all 0.3s ease;
   box-shadow: 0 2px 8px rgba(0,0,0,0.06);
   border: 2px solid transparent;
 }
 .module-card:hover {
   transform: translateY(-4px);
   box-shadow: 0 12px 32px rgba(0,0,0,0.1);
   border-color: #1a73e8;
 }
 .module-icon {
   font-size: 48px;
   display: block;
   margin-bottom: 16px;
 }
 .module-card h3 {
   font-size: 20px;
   color: #1a1a1a;
   margin-bottom: 8px;
 }
 .module-desc {
   font-size: 13px;
   color: #888;
   line-height: 1.6;
   min-height: 42px;
   margin-bottom: 18px;
 }
 .countdown-panel {
   margin-top: auto;
   padding: 14px 16px;
   border-radius: 10px;
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
   color: #1a73e8;
   font-weight: 600;
 }
 .countdown-value {
   font-size: 18px;
   font-weight: 700;
   color: #153a73;
 }
 .countdown-value.urgent {
   color: #d93025;
  }
 </style>

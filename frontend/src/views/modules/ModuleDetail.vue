 <template>
   <div class="module-detail-page">
     <NavBar />
     <main class="detail-content">
       <section class="module-hero">
         <div class="hero-main">
           <div>
             <p class="page-kicker">Exam Module</p>
             <div class="module-title-row">
               <span class="banner-icon">{{ module?.icon }}</span>
               <h1>{{ module?.name }}</h1>
             </div>
             <p>{{ module?.description }}</p>
           </div>
            <button class="back-btn" type="button" @click="backToModules">返回等级考试模块</button>
         </div>
       </section>
 
       <div class="section">
         <div class="section-heading">
           <p class="page-kicker">Practice Path</p>
            <h2>专项练习</h2>
         </div>
         <div class="practice-grid">
           <div class="practice-card" @click="goToPractice('words')">
              <div class="card-icon">📚</div>
              <h3>单词练习</h3>
              <p>通过卡片记忆方式学习核心词汇</p>
              <span class="card-action">开始练习</span>
           </div>
           <div class="practice-card" @click="goToPractice('readings')">
              <div class="card-icon">📖</div>
              <h3>阅读理解</h3>
              <p>阅读文章并完成理解题目</p>
              <span class="card-action">开始练习</span>
           </div>
           <div class="practice-card" @click="goToPractice('listenings')">
              <div class="card-icon">🎧</div>
              <h3>听力练习</h3>
              <p>听录音材料并回答问题</p>
              <span class="card-action">开始练习</span>
           </div>
         </div>
       </div>
     </main>
   </div>
 </template>
 
 <script setup>
 import { ref, onMounted, computed } from 'vue'
 import { useRoute, useRouter } from 'vue-router'
 import NavBar from '../../components/NavBar.vue'
import { getModuleByCode } from '../../utils/api'
 
 const props = defineProps({ code: String })
 const route = useRoute()
 const router = useRouter()
 const module = ref({})
 const moduleCode = computed(() => props.code || route.params.code)
 
 onMounted(async () => {
   try {
     const modRes = await getModuleByCode(moduleCode.value)
     module.value = modRes.data.data || {}
   } catch (e) {
     console.error('Failed to load module', e)
   }
 })
 
 function backToModules() { router.push('/modules') }
 function goToPractice(type) {
   router.push(`/practice/${type}/${moduleCode.value}`)
 }
 </script>
 
 <style scoped>
 .module-detail-page {
   min-height: 100vh;
   background:
     radial-gradient(circle at 10% 8%, rgba(34, 197, 94, 0.16), transparent 28%),
     radial-gradient(circle at 88% 10%, rgba(245, 158, 11, 0.16), transparent 26%),
     linear-gradient(180deg, #f7faf5 0%, #f5f7fb 100%);
 }
 .detail-content {
   max-width: 1180px;
   margin: 0 auto;
   padding: 36px 24px 56px;
 }
 .module-hero {
   margin-bottom: 28px;
   border-radius: 28px;
   padding: 38px;
   background:
     linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(240, 253, 244, 0.88)),
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
   padding: 11px 18px;
   font-weight: 800;
   cursor: pointer;
   flex-shrink: 0;
   transition: transform 0.2s, box-shadow 0.2s;
 }
 .back-btn:hover {
   transform: translateY(-1px);
   box-shadow: 0 12px 22px rgba(23, 32, 51, 0.2);
 }
 .page-kicker {
   color: #15803d;
   font-size: 12px;
   font-weight: 900;
   letter-spacing: 0.18em;
   text-transform: uppercase;
   margin-bottom: 10px;
 }
 .module-title-row {
   display: flex;
   align-items: center;
   gap: 18px;
   flex-wrap: wrap;
   margin-bottom: 18px;
 }
 .banner-icon {
   width: 74px;
   height: 74px;
   border-radius: 24px;
   display: inline-flex;
   align-items: center;
   justify-content: center;
   background: #ecfdf5;
   font-size: 42px;
   box-shadow: inset 0 0 0 1px #bbf7d0;
 }
 .module-hero h1 {
   color: #102018;
   font-size: clamp(34px, 7vw, 72px);
   line-height: 0.95;
 }
 .hero-main p:last-child {
   max-width: 700px;
   color: #526057;
   font-size: 16px;
   line-height: 1.9;
 }
 .section {
   margin-bottom: 40px;
 }
 .section-heading {
   margin-bottom: 18px;
 }
 .section-heading h2 {
   color: #102018;
   font-size: 28px;
 }
 .practice-grid {
   display: grid;
   grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
   gap: 20px;
 }
 .practice-card {
   display: flex;
   flex-direction: column;
   min-height: 250px;
   background: rgba(255, 255, 255, 0.92);
   border: 1px solid rgba(226, 232, 240, 0.96);
   border-radius: 24px;
   padding: 26px;
   cursor: pointer;
   transition: transform 0.24s ease, box-shadow 0.24s ease, border-color 0.24s ease;
   box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
 }
 .practice-card:hover {
   transform: translateY(-5px);
   border-color: #86efac;
   box-shadow: 0 18px 40px rgba(22, 101, 52, 0.12);
 }
 .card-icon {
   width: 58px;
   height: 58px;
   border-radius: 20px;
   display: inline-flex;
   align-items: center;
   justify-content: center;
   margin-bottom: 18px;
   background: #f0fdf4;
   font-size: 32px;
 }
 .practice-card h3 {
   color: #102018;
   font-size: 22px;
   margin-bottom: 10px;
 }
 .practice-card p {
   color: #526057;
   font-size: 14px;
   line-height: 1.7;
   flex: 1;
 }
 .card-action {
   display: inline-flex;
   justify-content: center;
   margin-top: 22px;
   border-radius: 999px;
   background: #15803d;
   color: #fff;
   padding: 11px 16px;
   font-weight: 900;
 }
 @media (max-width: 720px) {
   .detail-content {
     padding: 24px 16px 40px;
   }
   .module-hero {
     padding: 26px;
   }
   .hero-main {
     flex-direction: column;
     align-items: flex-start;
   }
   .back-btn {
     width: 100%;
   }
 }
</style>

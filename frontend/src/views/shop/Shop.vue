<template>
  <div class="shop-page">
    <NavBar />

    <main class="shop-content">
      <section class="shop-hero">
        <div>
          <p class="shop-kicker">Learning Shop</p>
          <h1>学习商城</h1>
          <p class="shop-subtitle">
            配套等级考试模块的精选课程与备考资料，适合 CET、考研英语等阶段复习。
          </p>
        </div>
        <button class="back-btn" type="button" @click="backToModules">返回等级考试模块</button>
      </section>

      <section class="category-strip" aria-label="商品分类">
        <span>精选课程</span>
        <span>真题资料</span>
        <span>备考书籍</span>
      </section>

      <section class="shop-search" aria-label="商品搜索">
        <label class="search-box">
          <span>搜索商品</span>
          <input
            v-model="searchKeyword"
            type="search"
            placeholder="输入课程、真题、词汇、考研..."
            autocomplete="off"
          />
        </label>
        <button v-if="searchKeyword" class="clear-search-btn" type="button" @click="clearSearch">清空</button>
        <span v-if="searching" class="searching-tip">搜索中...</span>
      </section>

      <section v-if="products.length" class="product-grid">
        <article
          v-for="product in products"
          :key="product.id"
          class="product-card"
          :class="product.tone"
        >
          <div class="product-cover">
            <span class="product-icon">{{ product.icon }}</span>
            <span class="product-tag">{{ product.tag }}</span>
          </div>
          <div class="product-body">
            <p class="product-category">{{ product.category }}</p>
            <h2>{{ product.title }}</h2>
            <p class="product-desc">{{ product.description }}</p>
            <ul class="product-points">
              <li v-for="point in product.points" :key="point">{{ point }}</li>
            </ul>
          </div>
          <div class="product-footer">
            <div>
              <span class="price">¥{{ product.price }}</span>
              <span v-if="product.originalPrice" class="original-price">¥{{ product.originalPrice }}</span>
              <span class="stock">库存 {{ product.stock }}</span>
            </div>
            <button class="buy-btn" type="button" :disabled="buyingProductId === product.id || product.stock <= 0" @click="selectProduct(product)">
              {{ buyingProductId === product.id ? '下单中...' : (product.stock <= 0 ? '已售罄' : '立即购买') }}
            </button>
          </div>
        </article>
      </section>
      <section v-else class="empty-products">
        <strong>{{ searchKeyword.trim() ? '没有找到相关商品' : '暂无商品' }}</strong>
        <span v-if="searchKeyword.trim()">换个关键词再试试</span>
      </section>

      <p v-if="message" class="selected-tip">{{ message }}</p>
      <p v-if="errorMessage" class="error-tip">{{ errorMessage }}</p>
    </main>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../../components/NavBar.vue'
import {
  createSeckillShopOrder,
  createShopOrderToken,
  getSeckillShopOrderResult,
  getShopProducts,
  searchShopProducts
} from '../../utils/api'
import { currentUser } from '../../utils/currentUser'

const router = useRouter()
const user = currentUser
const products = ref([])
const buyingProductId = ref(null)
const message = ref('')
const errorMessage = ref('')
const searchKeyword = ref('')
const searching = ref(false)
const pendingOrderRequestIds = new Map()
let searchTimer = null
let searchRequestSeq = 0

function backToModules() {
  router.push('/modules')
}

async function selectProduct(product) {
  if (buyingProductId.value) return

  if (!user.value) {
    errorMessage.value = '请先登录后再购买'
    return
  }

  if (!product?.id) {
    errorMessage.value = '商品信息异常，请刷新后重试'
    return
  }

  buyingProductId.value = product.id
  message.value = ''
  errorMessage.value = ''
  try {
    const requestId = await getOrderRequestId(product.id)
    const response = await createSeckillShopOrder(user.value.id, product.id, requestId)
    if (response.data.code !== 200) {
      errorMessage.value = response.data.message || '下单失败'
      return
    }
    const submitStatus = response.data.data?.status
    if (submitStatus === 'failed') {
      errorMessage.value = response.data.data?.message || '下单失败'
      return
    }
    if (submitStatus === 'success') {
      message.value = '订单已创建，请在有效期内支付'
      router.push({ path: '/orders', query: { status: 'pending' } })
      return
    }
    message.value = '订单已进入排队，请稍候...'
    await waitForSeckillResult(requestId)
  } catch (error) {
    console.error('Failed to create order', error)
    errorMessage.value = error.response?.data?.message || error.message || '下单失败，请稍后重试'
  } finally {
    pendingOrderRequestIds.delete(product.id)
    buyingProductId.value = null
  }
}

async function waitForSeckillResult(requestId) {
  for (let i = 0; i < 20; i++) {
    await sleep(1000)
    const response = await getSeckillShopOrderResult(user.value.id, requestId)
    if (response.data.code !== 200) {
      throw new Error(response.data.message || '查询订单结果失败')
    }
    const result = response.data.data
    if (result?.status === 'success') {
      message.value = '订单已创建，请在有效期内支付'
      router.push({ path: '/orders', query: { status: 'pending' } })
      return
    }
    if (result?.status === 'failed') {
      errorMessage.value = result.message || '下单失败'
      return
    }
    message.value = `订单排队中，请稍候...${i + 1}s`
  }
  message.value = '订单仍在排队，可稍后到订单页查看'
}

function sleep(ms) {
  return new Promise(resolve => window.setTimeout(resolve, ms))
}

async function getOrderRequestId(productId) {
  const existing = pendingOrderRequestIds.get(productId)
  if (existing) return existing

  const response = await createShopOrderToken(user.value.id, productId)
  if (response.data.code !== 200 || !response.data.data?.token) {
    throw new Error(response.data.message || '申请下单token失败')
  }
  const requestId = response.data.data.token
  pendingOrderRequestIds.set(productId, requestId)
  return requestId
}

function normalizeProduct(product) {
  return {
    ...product,
    points: String(product.points || '').split('|').filter(Boolean)
  }
}

async function loadProducts() {
  const keyword = searchKeyword.value.trim()
  const requestSeq = ++searchRequestSeq
  searching.value = Boolean(keyword)
  errorMessage.value = ''
  try {
    const response = keyword ? await searchShopProducts(keyword) : await getShopProducts()
    if (requestSeq !== searchRequestSeq) return
    products.value = (response.data.data || []).map(normalizeProduct)
  } catch (error) {
    console.error('Failed to load products', error)
    errorMessage.value = '商品加载失败，请稍后重试'
  } finally {
    if (requestSeq === searchRequestSeq) {
      searching.value = false
    }
  }
}

function scheduleProductSearch() {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
  searchTimer = window.setTimeout(() => {
    loadProducts()
  }, 300)
}

function clearSearch() {
  searchKeyword.value = ''
}

watch(searchKeyword, scheduleProductSearch)

onMounted(() => {
  loadProducts()
})

onBeforeUnmount(() => {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
})
</script>

<style scoped>
.shop-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% 8%, rgba(34, 197, 94, 0.16), transparent 28%),
    radial-gradient(circle at 88% 10%, rgba(245, 158, 11, 0.16), transparent 26%),
    linear-gradient(180deg, #f7faf5 0%, #f5f7fb 100%);
}

.shop-content {
  max-width: 1180px;
  margin: 0 auto;
  padding: 36px 24px 48px;
}

.shop-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
  border-radius: 28px;
  padding: 38px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(255, 251, 235, 0.88)),
    #fff;
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
}

.shop-kicker {
  color: #15803d;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.shop-hero h1 {
  color: #102018;
  font-size: clamp(34px, 7vw, 72px);
  line-height: 0.95;
  margin-bottom: 18px;
}

.shop-subtitle {
  max-width: 660px;
  color: #526057;
  font-size: 16px;
  line-height: 1.9;
}

.back-btn,
.buy-btn {
  border: none;
  border-radius: 999px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.back-btn {
  background: #102018;
  color: #fff;
  padding: 11px 18px;
}

.back-btn:hover,
.buy-btn:hover {
  transform: translateY(-1px);
}

.category-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 24px;
}

.category-strip span {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(226, 232, 240, 0.9);
  color: #526057;
  font-size: 13px;
  font-weight: 700;
}

.shop-search {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(226, 232, 240, 0.96);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
}

.search-box span {
  color: #15803d;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.search-box input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: #102018;
  font-size: 15px;
}

.search-box input::placeholder {
  color: #94a3b8;
}

.clear-search-btn {
  border: 1px solid rgba(148, 163, 184, 0.4);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.86);
  color: #475569;
  cursor: pointer;
  font-weight: 800;
  padding: 11px 14px;
}

.searching-tip {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 22px;
}

.product-card {
  overflow: hidden;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(226, 232, 240, 0.96);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
}

.product-cover {
  min-height: 138px;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.tone-blue .product-cover { background: linear-gradient(135deg, #dbeafe, #eff6ff); }
.tone-green .product-cover { background: linear-gradient(135deg, #dcfce7, #f0fdf4); }
.tone-orange .product-cover { background: linear-gradient(135deg, #ffedd5, #fff7ed); }
.tone-red .product-cover { background: linear-gradient(135deg, #fee2e2, #fff1f2); }

.product-icon {
  font-size: 46px;
}

.product-tag {
  padding: 7px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.product-body {
  padding: 20px 20px 12px;
  flex: 1;
}

.product-category {
  color: #15803d;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 8px;
}

.product-body h2 {
  color: #102018;
  font-size: 21px;
  margin-bottom: 10px;
}

.product-desc {
  color: #526057;
  font-size: 14px;
  line-height: 1.65;
}

.product-points {
  margin-top: 14px;
  padding-left: 18px;
  color: #475569;
  font-size: 13px;
  line-height: 1.8;
}

.product-footer {
  padding: 18px 20px 20px;
  border-top: 1px solid #edf2f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.price {
  color: #b45309;
  font-size: 24px;
  font-weight: 900;
}

.original-price {
  margin-left: 8px;
  color: #94a3b8;
  text-decoration: line-through;
  font-size: 13px;
}

.stock {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.buy-btn {
  background: #102018;
  color: #fff;
  padding: 10px 16px;
}

.buy-btn:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.buy-btn:hover {
  box-shadow: 0 12px 22px rgba(23, 32, 51, 0.22);
}

.selected-tip {
  margin-top: 22px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #bbf7d0;
}

.error-tip {
  margin-top: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.empty-products {
  min-height: 180px;
  border: 1px dashed rgba(148, 163, 184, 0.55);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  color: #64748b;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.empty-products strong {
  color: #334155;
  font-size: 18px;
}

@media (max-width: 768px) {
  .shop-content {
    padding: 28px 16px 36px;
  }

  .shop-hero {
    flex-direction: column;
    align-items: flex-start;
    padding: 26px;
  }

  .shop-hero h1 {
    font-size: 30px;
  }

  .back-btn {
    width: 100%;
  }

  .shop-search {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    align-items: flex-start;
    flex-direction: column;
  }

  .clear-search-btn {
    width: 100%;
  }
}
</style>

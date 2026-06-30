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

      <section class="product-grid">
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

      <p v-if="message" class="selected-tip">{{ message }}</p>
      <p v-if="errorMessage" class="error-tip">{{ errorMessage }}</p>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '../components/NavBar.vue'
import { createShopOrder, getShopProducts } from '../utils/api'
import { currentUser } from '../utils/currentUser'

const router = useRouter()
const user = currentUser
const products = ref([])
const buyingProductId = ref(null)
const message = ref('')
const errorMessage = ref('')

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
    const response = await createShopOrder(user.value.id, product.id)
    if (response.data.code !== 200) {
      errorMessage.value = response.data.message || '下单失败'
      return
    }
    message.value = '订单已创建，请在 30 分钟内支付'
    router.push({ path: '/orders', query: { status: 'pending' } })
  } catch (error) {
    console.error('Failed to create order', error)
    errorMessage.value = error.response?.data?.message || '下单失败，请稍后重试'
  } finally {
    buyingProductId.value = null
  }
}

function normalizeProduct(product) {
  return {
    ...product,
    points: String(product.points || '').split('|').filter(Boolean)
  }
}

onMounted(async () => {
  try {
    const response = await getShopProducts()
    products.value = (response.data.data || []).map(normalizeProduct)
  } catch (error) {
    console.error('Failed to load products', error)
    errorMessage.value = '商品加载失败，请稍后重试'
  }
})
</script>

<style scoped>
.shop-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 8% 10%, rgba(245, 158, 11, 0.18), transparent 28%),
    radial-gradient(circle at 90% 8%, rgba(26, 115, 232, 0.14), transparent 24%),
    linear-gradient(180deg, #fffaf1 0%, #f5f7fb 100%);
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
}

.shop-kicker {
  color: #b45309;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.shop-hero h1 {
  color: #172033;
  font-size: 38px;
  margin-bottom: 10px;
}

.shop-subtitle {
  max-width: 660px;
  color: #667085;
  line-height: 1.7;
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
  background: #fff;
  color: #1a73e8;
  border: 1px solid #d9e6fb;
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
  color: #475569;
  font-size: 13px;
  font-weight: 700;
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
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.08);
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
  color: #1a73e8;
  font-size: 12px;
  font-weight: 800;
  margin-bottom: 8px;
}

.product-body h2 {
  color: #172033;
  font-size: 21px;
  margin-bottom: 10px;
}

.product-desc {
  color: #64748b;
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
  background: #172033;
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

@media (max-width: 768px) {
  .shop-content {
    padding: 28px 16px 36px;
  }

  .shop-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .shop-hero h1 {
    font-size: 30px;
  }

  .back-btn {
    width: 100%;
  }
}
</style>

<template>
  <div class="orders-page">
    <NavBar />

    <main class="orders-content">
      <section class="orders-hero">
        <div>
          <p class="orders-kicker">My Orders</p>
          <h1>我的订单</h1>
          <p>查看商城课程和图书订单，按支付状态快速筛选。</p>
        </div>
        <button class="shop-link" type="button" @click="goToShop">去商城选购</button>
      </section>

      <section class="status-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="status-tab"
          :class="{ active: activeStatus === tab.key }"
          :disabled="loading && activeStatus === tab.key"
          @click="switchStatus(tab.key)"
        >
          {{ tab.label }}
          <span v-if="activeStatus === tab.key">{{ orders.length }}</span>
        </button>
      </section>

      <section v-if="orders.length" class="order-list">
        <article v-for="order in orders" :key="order.id" class="order-card">
          <div class="order-main">
            <div class="order-cover">{{ order.icon || '🛒' }}</div>
            <div class="order-info">
              <div class="order-title-row">
                <h2>{{ order.productName }}</h2>
                <span class="order-status" :class="order.status">{{ statusLabel(order.status) }}</span>
              </div>
              <p class="order-meta">订单号：{{ order.orderNo }}</p>
              <p class="order-meta">下单时间：{{ order.createdAt }}</p>
              <p v-if="order.status === 'pending'" class="order-countdown">
                订单关闭倒计时：{{ closeCountdown(order) }}
              </p>
            </div>
          </div>

          <div class="order-side">
            <span class="order-price">¥{{ order.amount }}</span>
            <button
              v-if="order.status === 'pending'"
              type="button"
              class="pay-btn"
              :disabled="payingOrderId === order.id"
              @click="markPaid(order)"
            >
              {{ payingOrderId === order.id ? '支付中...' : '去支付' }}
            </button>
            <button v-else type="button" class="detail-btn">查看详情</button>
          </div>
        </article>
      </section>

      <section v-else-if="loading" class="empty-orders">
        <div class="empty-icon">🛒</div>
        <p>加载订单...</p>
      </section>

      <section v-else class="empty-orders">
        <div class="empty-icon">🛒</div>
        <p>{{ emptyText }}</p>
        <button type="button" @click="goToShop">去商城看看</button>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '../../components/NavBar.vue'
import { getShopOrders, payShopOrder } from '../../utils/api'
import { currentUser } from '../../utils/currentUser'

const router = useRouter()
const route = useRoute()
const user = currentUser
const initialStatus = ['all', 'pending', 'paid'].includes(route.query.status)
  ? route.query.status
  : 'all'
const activeStatus = ref(initialStatus)
const orders = ref([])
const loading = ref(true)
const payingOrderId = ref(null)
const now = ref(Date.now())
let countdownTimer = null
let refreshAfterExpiredTimer = null

const tabs = [
  { key: 'all', label: '全部订单' },
  { key: 'pending', label: '待支付' },
  { key: 'paid', label: '已支付' }
]

const emptyText = computed(() => {
  if (activeStatus.value === 'pending') return '暂无待支付订单'
  if (activeStatus.value === 'paid') return '暂无已支付订单'
  return '暂无订单'
})

function statusLabel(status) {
  if (status === 'canceled') return '已取消'
  return status === 'paid' ? '已支付' : '待支付'
}

function parseOrderTime(value) {
  if (!value) return NaN
  return new Date(String(value).replace(' ', 'T')).getTime()
}

function remainingMillis(order) {
  return Math.max(0, parseOrderTime(order.expireAt) - now.value)
}

function closeCountdown(order) {
  const remaining = remainingMillis(order)
  if (!Number.isFinite(remaining) || remaining <= 0) return '等待关闭'

  const totalSeconds = Math.ceil(remaining / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}

function scheduleExpiredOrderRefresh() {
  const hasExpiredPending = orders.value.some(order => order.status === 'pending' && remainingMillis(order) <= 0)
  if (!hasExpiredPending || refreshAfterExpiredTimer) return

  refreshAfterExpiredTimer = window.setTimeout(async () => {
    refreshAfterExpiredTimer = null
    await loadOrders(activeStatus.value)
  }, 1200)
}

async function markPaid(order) {
  if (!user.value || !order?.id || payingOrderId.value) return

  payingOrderId.value = order.id
  try {
    const response = await payShopOrder(user.value.id, order.id)
    if (response.data.code === 200 && response.data.data) {
      await loadOrders(activeStatus.value)
    }
  } finally {
    payingOrderId.value = null
  }
}

function goToShop() {
  router.push('/shop')
}

async function loadOrders(status = activeStatus.value) {
  if (!user.value) return

  loading.value = true
  try {
    const response = await getShopOrders(user.value.id, status)
    orders.value = response.data.data || []
    scheduleExpiredOrderRefresh()
  } finally {
    loading.value = false
  }
}

async function switchStatus(status) {
  if (!['all', 'pending', 'paid'].includes(status)) return

  activeStatus.value = status
  await loadOrders(status)
}

watch(() => route.query.status, async status => {
  if (['all', 'pending', 'paid'].includes(status)) {
    await switchStatus(status)
  }
})

onMounted(async () => {
  await loadOrders()
  countdownTimer = window.setInterval(() => {
    now.value = Date.now()
    scheduleExpiredOrderRefresh()
  }, 1000)
})

onBeforeUnmount(() => {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
  }
  if (refreshAfterExpiredTimer) {
    window.clearTimeout(refreshAfterExpiredTimer)
  }
})
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 12% 8%, rgba(26, 115, 232, 0.12), transparent 26%),
    linear-gradient(180deg, #f8fbff 0%, #eef3f8 100%);
}

.orders-content {
  max-width: 980px;
  margin: 0 auto;
  padding: 34px 24px 48px;
}

.orders-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.orders-kicker {
  color: #1a73e8;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.orders-hero h1 {
  font-size: 34px;
  color: #172033;
  margin-bottom: 8px;
}

.orders-hero p {
  color: #667085;
}

.shop-link,
.pay-btn,
.detail-btn,
.empty-orders button {
  border: none;
  border-radius: 999px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.shop-link {
  background: #172033;
  color: #fff;
  padding: 11px 18px;
}

.shop-link:hover,
.pay-btn:hover,
.detail-btn:hover,
.empty-orders button:hover {
  transform: translateY(-1px);
}

.status-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.status-tab {
  border: 1px solid #dce7f6;
  background: #fff;
  color: #475569;
  border-radius: 16px;
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.status-tab span {
  min-width: 26px;
  height: 26px;
  border-radius: 999px;
  background: #edf4ff;
  color: #1a73e8;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.status-tab.active {
  background: #1a73e8;
  color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 10px 24px rgba(26, 115, 232, 0.22);
}

.status-tab.active span {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order-card {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.order-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.order-cover {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: #fff7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
}

.order-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.order-title-row h2 {
  margin: 0;
  color: #172033;
  font-size: 18px;
}

.order-status {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.order-status.pending {
  background: #fff7ed;
  color: #c2410c;
}

.order-status.paid {
  background: #ecfdf5;
  color: #047857;
}

.order-meta {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.order-countdown {
  display: inline-flex;
  align-items: center;
  margin-top: 6px;
  padding: 5px 10px;
  border-radius: 999px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 13px;
  font-weight: 800;
}

.order-side {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.order-price {
  color: #b45309;
  font-size: 24px;
  font-weight: 900;
}

.pay-btn {
  background: #b45309;
  color: #fff;
  padding: 10px 16px;
}

.pay-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.detail-btn {
  background: #edf4ff;
  color: #1a73e8;
  padding: 10px 16px;
}

.empty-orders {
  text-align: center;
  background: #fff;
  border-radius: 20px;
  padding: 54px 20px;
  color: #64748b;
  border: 1px dashed #cbd5e1;
}

.empty-icon {
  font-size: 52px;
  margin-bottom: 12px;
}

.empty-orders button {
  margin-top: 18px;
  background: #172033;
  color: #fff;
  padding: 10px 18px;
}

@media (max-width: 768px) {
  .orders-content {
    padding: 26px 16px 36px;
  }

  .orders-hero,
  .order-card,
  .order-side {
    flex-direction: column;
    align-items: flex-start;
  }

  .status-tabs {
    grid-template-columns: 1fr;
  }

  .shop-link,
  .pay-btn,
  .detail-btn {
    width: 100%;
  }
}
</style>

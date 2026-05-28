<template>
  <main class="shell">
    <section class="hero">
      <div>
        <p class="eyebrow">Mall Studio</p>
        <h1>会购物，也会服务的智能电商平台</h1>
        <p class="hero-text">浏览商品、加入购物车、提交订单、模拟支付、追踪物流，并随时向小智咨询商品与售后问题。</p>
        <div class="hero-actions">
          <button @click="refreshAll">刷新数据</button>
          <span>演示订单：MO202605290001</span>
        </div>
      </div>
      <div class="hero-panel">
        <strong>今日精选</strong>
        <p>无线蓝牙耳机 Pro</p>
        <span>主动降噪 · 蓝牙5.3 · 299元</span>
      </div>
    </section>

    <section class="toolbar">
      <input v-model="keyword" placeholder="搜索商品" @keyup.enter="loadProducts" />
      <select v-model="categoryId" @change="loadProducts">
        <option value="">全部分类</option>
        <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option>
      </select>
      <button @click="loadProducts">查询</button>
    </section>

    <section class="grid products-grid">
      <article v-for="product in products" :key="product.id" class="product-card">
        <img :src="product.imageUrl" :alt="product.name" />
        <div>
          <span class="tag">库存 {{ product.stock }}</span>
          <h3>{{ product.name }}</h3>
          <p>{{ product.specs }}</p>
          <footer>
            <strong>¥{{ product.price }}</strong>
            <button @click="addToCart(product.id)">加入购物车</button>
          </footer>
        </div>
      </article>
    </section>

    <section class="workspace">
      <div class="panel">
        <div class="panel-title">
          <h2>购物车</h2>
          <button @click="loadCart">刷新</button>
        </div>
        <div v-if="cart.length === 0" class="empty">购物车还是空的，先添加商品吧。</div>
        <div v-for="item in cart" :key="item.id" class="line-item">
          <div>
            <strong>{{ item.product.name }}</strong>
            <span>¥{{ item.product.price }} × {{ item.quantity }}</span>
          </div>
          <div class="line-actions">
            <button @click="changeQuantity(item, item.quantity - 1)" :disabled="item.quantity <= 1">-</button>
            <button @click="changeQuantity(item, item.quantity + 1)">+</button>
            <button @click="removeCart(item.id)">删除</button>
          </div>
        </div>
        <div class="checkout">
          <span>合计：¥{{ cartTotal }}</span>
          <button @click="createOrder" :disabled="cart.length === 0">提交订单</button>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">
          <h2>我的订单</h2>
          <button @click="loadOrders">刷新</button>
        </div>
        <div v-if="orders.length === 0" class="empty">暂无订单。</div>
        <div v-for="order in orders" :key="order.id" class="order-card">
          <div>
            <strong>{{ order.orderNo }}</strong>
            <span>{{ statusLabel(order.status) }} · ¥{{ order.totalAmount }}</span>
          </div>
          <div class="line-actions">
            <button v-if="order.status === 'PENDING_PAYMENT'" @click="pay(order.id)">模拟支付</button>
            <button v-if="order.status === 'PENDING_PAYMENT' || order.status === 'PENDING_SHIPMENT'" @click="cancel(order.id)">取消</button>
            <button @click="showTracking(order.id)">物流</button>
          </div>
        </div>
        <div v-if="tracking.length" class="tracking">
          <h3>物流轨迹</h3>
          <p v-for="track in tracking" :key="track.id">{{ track.description }} · {{ track.occurTime }}</p>
        </div>
      </div>
    </section>

    <ChatWidget />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { mallApi } from './api/mall'
import ChatWidget from './components/ChatWidget.vue'

const products = ref([])
const categories = ref([])
const cart = ref([])
const orders = ref([])
const tracking = ref([])
const keyword = ref('')
const categoryId = ref('')

const cartTotal = computed(() => cart.value.reduce((sum, item) => sum + Number(item.product.price) * item.quantity, 0).toFixed(2))

async function loadProducts() {
  products.value = await mallApi.products({ keyword: keyword.value, categoryId: categoryId.value })
}

async function loadCategories() {
  categories.value = await mallApi.categories()
}

async function loadCart() {
  cart.value = await mallApi.cart()
}

async function loadOrders() {
  orders.value = await mallApi.orders()
}

async function addToCart(productId) {
  await mallApi.addCart({ productId, quantity: 1 })
  await loadCart()
}

async function changeQuantity(item, quantity) {
  await mallApi.updateCart(item.id, { quantity, selected: true })
  await loadCart()
}

async function removeCart(id) {
  await mallApi.deleteCart(id)
  await loadCart()
}

async function createOrder() {
  await mallApi.createOrder({ receiverName: '测试用户', receiverPhone: '13800000000', receiverAddress: '上海市浦东新区演示路100号' })
  await Promise.all([loadCart(), loadOrders()])
}

async function pay(id) {
  await mallApi.payOrder(id)
  await loadOrders()
}

async function cancel(id) {
  await mallApi.cancelOrder(id)
  await loadOrders()
}

async function showTracking(id) {
  tracking.value = await mallApi.tracking(id)
}

async function refreshAll() {
  await Promise.all([loadProducts(), loadCategories(), loadCart(), loadOrders()])
}

function statusLabel(status) {
  return {
    PENDING_PAYMENT: '待付款',
    PENDING_SHIPMENT: '待发货',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }[status] || status
}

onMounted(refreshAll)
</script>

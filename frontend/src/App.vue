<template>
  <main class="app-shell">
    <section :class="['phone-page', { 'detail-mode': currentView === 'detail' }]">
      <header v-if="currentView !== 'message'" class="app-topbar">
        <button v-if="currentView === 'detail'" class="icon-only" type="button" aria-label="返回" @click="goBack">
          <ChevronLeft :size="22" />
        </button>
        <button v-else class="location-button" type="button" @click="toast('当前定位：揭阳')">
          <MapPin :size="16" />
          <span>揭阳</span>
        </button>

        <label v-if="currentView !== 'detail'" class="search-pill">
          <Search :size="17" />
          <input v-model="keyword" placeholder="搜索商品、品牌、店铺" @keyup.enter="searchProducts" />
        </label>
        <div v-else class="detail-title">商品详情</div>

        <button class="icon-only" type="button" aria-label="消息" @click="selectTab('message')">
          <MessageCircle :size="20" />
          <em v-if="unreadCount">{{ unreadCount }}</em>
        </button>
        <button class="icon-only" type="button" aria-label="购物车" @click="selectTab('cart')">
          <ShoppingCart :size="20" />
          <em v-if="cartCount">{{ cartCount }}</em>
        </button>
      </header>

      <template v-if="currentView === 'home'">
        <section class="quick-grid" aria-label="功能入口">
          <button v-for="item in shortcuts" :key="item.label" type="button" @click="toast(`${item.label}功能开发中`)">
            <span :style="{ background: item.color }">
              <component :is="item.icon" :size="20" />
            </span>
            <strong>{{ item.label }}</strong>
          </button>
        </section>

        <section class="promo-grid" aria-label="精选活动">
          <article v-for="item in promoCards" :key="item.title" class="promo-card" @click="toast(`${item.title}已打开`)">
            <div class="promo-title">
              <strong>{{ item.title }}</strong>
              <span>{{ item.badge }}</span>
            </div>
            <div class="promo-products">
              <div v-for="goods in item.goods" :key="goods.name">
                <img :src="goods.image" :alt="goods.name" />
                <b>{{ goods.price }}</b>
                <small>{{ goods.caption }}</small>
              </div>
            </div>
          </article>
        </section>

        <section class="channel-panel" aria-label="精选频道">
          <div class="channel-head">
            <strong>今日必逛</strong>
            <span>低价、国补、同城服务</span>
          </div>
          <div class="channel-grid">
            <button v-for="card in channelCards" :key="card.title" type="button" @click="toast(`${card.title}已打开`)">
              <h2>{{ card.title }}</h2>
              <p>{{ card.subtitle }}</p>
              <span>{{ card.tag }}</span>
            </button>
          </div>
        </section>

        <section class="section-head">
          <h2>为你推荐</h2>
          <button type="button" @click="loadMore">加载更多</button>
        </section>

        <section class="product-waterfall" aria-label="商品列表">
          <article
            v-for="product in visibleProducts"
            :key="product.id"
            class="product-card"
            @click="openProduct(product)"
          >
            <img :src="product.imageUrl" :alt="product.name" />
            <div class="product-content">
              <h3>{{ product.name }}</h3>
              <p>{{ product.sellingPoint }}</p>
              <small>{{ product.specs }}</small>
              <div class="service-line">{{ product.service }}</div>
              <div class="product-tags">
                <span v-for="tag in product.tags" :key="tag">{{ tag }}</span>
              </div>
              <footer>
                <strong>￥{{ product.price }}</strong>
                <button type="button" aria-label="加入购物车" @click.stop="addToCart(product)">
                  <ShoppingCart :size="15" />
                </button>
              </footer>
            </div>
          </article>
        </section>
      </template>

      <section v-else-if="currentView === 'detail' && selectedProduct" class="detail-page">
        <img class="detail-hero" :src="selectedProduct.imageUrl" :alt="selectedProduct.name" />
        <article class="detail-main">
          <div class="detail-price-row">
            <strong>￥{{ selectedProduct.price }}</strong>
            <span>已售 {{ selectedProduct.sales }} 件</span>
          </div>
          <h1>{{ selectedProduct.name }}</h1>
          <p>{{ selectedProduct.sellingPoint }}</p>
          <div class="product-tags detail-tags">
            <span v-for="tag in selectedProduct.tags" :key="tag">{{ tag }}</span>
          </div>
        </article>

        <article class="detail-panel">
          <h2>规格服务</h2>
          <dl>
            <div>
              <dt>规格</dt>
              <dd>{{ selectedProduct.specs }}</dd>
            </div>
            <div>
              <dt>服务</dt>
              <dd>{{ selectedProduct.service }}</dd>
            </div>
            <div>
              <dt>配送</dt>
              <dd>揭阳仓发货，预计明日达，支持送货上门</dd>
            </div>
          </dl>
        </article>

        <article class="detail-panel">
          <h2>商品亮点</h2>
          <ul class="detail-bullets">
            <li v-for="point in selectedProduct.highlights" :key="point">{{ point }}</li>
          </ul>
        </article>

        <div class="detail-action-bar">
          <button type="button" class="ghost" @click="selectTab('message')">
            <MessageCircle :size="16" />
            客服
          </button>
          <button type="button" @click="addToCart(selectedProduct)">加入购物车</button>
          <button type="button" class="buy" @click="buyNow(selectedProduct)">立即购买</button>
        </div>
      </section>

      <section v-else-if="currentView === 'cart'" class="tab-page">
        <div class="page-heading">
          <div>
            <h1>购物车</h1>
            <p>{{ currentUser ? `${currentUser.username} 的购物车` : '游客购物车，登录后可保存用户数据' }}</p>
          </div>
          <button type="button" :disabled="cartItems.length === 0" @click="clearCart">
            <Trash2 :size="15" />
            清空
          </button>
        </div>

        <div v-if="cartItems.length === 0" class="empty-state">
          <ShoppingBag :size="42" />
          <h2>购物车还是空的</h2>
          <p>去首页挑选商品，加入购物车后可在这里统一结算。</p>
          <button type="button" @click="selectTab('home')">去逛逛</button>
        </div>

        <section v-else class="cart-list">
          <article v-for="item in cartItems" :key="item.product.id" class="cart-row">
            <img :src="item.product.imageUrl" :alt="item.product.name" />
            <div class="cart-row-main">
              <h2>{{ item.product.name }}</h2>
              <small>{{ item.product.specs }}</small>
              <p>￥{{ item.product.price }}</p>
              <div class="cart-row-actions">
                <div class="quantity-stepper">
                  <button type="button" aria-label="减少数量" @click="changeQuantity(item, -1)">
                    <Minus :size="14" />
                  </button>
                  <span>{{ item.quantity }}</span>
                  <button type="button" aria-label="增加数量" @click="changeQuantity(item, 1)">
                    <Plus :size="14" />
                  </button>
                </div>
                <button class="delete-item" type="button" @click="removeCartItem(item)">
                  <Trash2 :size="14" />
                  删除
                </button>
              </div>
            </div>
          </article>
          <footer class="cart-total">
            <span>合计 <b>￥{{ cartTotal }}</b></span>
            <button type="button" @click="checkout">去结算</button>
          </footer>
        </section>
      </section>

      <section v-else-if="currentView === 'mine'" class="tab-page mine-page">
        <section v-if="currentUser" class="profile-card">
          <div class="avatar">{{ userInitial }}</div>
          <div>
            <h1>{{ currentUser.username }}</h1>
            <p>{{ currentUser.phone }}，已同步购物车和客服会话。</p>
          </div>
          <button type="button" @click="logout">
            <LogOut :size="15" />
            退出
          </button>
        </section>

        <section v-else class="auth-card">
          <div class="auth-tabs">
            <button type="button" :class="{ active: authMode === 'login' }" @click="authMode = 'login'">登录</button>
            <button type="button" :class="{ active: authMode === 'register' }" @click="authMode = 'register'">注册</button>
          </div>
          <form class="auth-form" @submit.prevent="submitAuth">
            <label>
              <span>用户名</span>
              <input v-model.trim="authForm.username" autocomplete="username" placeholder="3-20 个字符" />
            </label>
            <label v-if="authMode === 'register'">
              <span>手机号</span>
              <input v-model.trim="authForm.phone" autocomplete="tel" placeholder="11 位手机号" />
            </label>
            <label>
              <span>密码</span>
              <input v-model="authForm.password" type="password" autocomplete="current-password" placeholder="至少 6 位" />
            </label>
            <button type="submit" :disabled="authLoading">
              <UserRound :size="16" />
              {{ authLoading ? '处理中' : authMode === 'login' ? '登录' : '注册并登录' }}
            </button>
          </form>
        </section>

        <section class="mine-stats">
          <button v-for="item in orderStats" :key="item.label" type="button" @click="openOrders(item.key)">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </button>
        </section>

        <section class="wallet-card">
          <h2>我的资产</h2>
          <div>
            <span>优惠券 <b>8</b></span>
            <span>积分 <b>1260</b></span>
            <span>服务 <b>可用</b></span>
          </div>
        </section>

        <section class="mine-menu">
          <button v-for="item in mineMenu" :key="item.label" type="button" @click="toast(`${item.label}功能开发中`)">
            <span>
              <component :is="item.icon" :size="18" />
              {{ item.label }}
            </span>
            <ChevronRight :size="18" />
          </button>
        </section>
      </section>

      <section v-else-if="currentView === 'orders'" class="tab-page orders-page">
        <div class="page-heading">
          <div>
            <h1>{{ orderViewTitle }}</h1>
            <p>{{ orderViewSubtitle }}</p>
          </div>
          <button type="button" @click="loadOrders">
            <PackageCheck :size="15" />
            刷新
          </button>
        </div>

        <div class="order-filter-row" aria-label="订单筛选">
          <button
            v-for="item in orderTabs"
            :key="item.key"
            type="button"
            :class="{ active: orderFilter === item.key }"
            @click="openOrders(item.key)"
          >
            {{ item.label }}
          </button>
        </div>

        <div v-if="ordersLoading" class="empty-state">
          <PackageCheck :size="42" />
          <h2>正在加载订单</h2>
          <p>请稍候，正在同步订单状态。</p>
        </div>

        <div v-else-if="filteredOrders.length === 0" class="empty-state">
          <PackageCheck :size="42" />
          <h2>暂无相关订单</h2>
          <p>{{ emptyOrderText }}</p>
          <button type="button" @click="selectTab('home')">去逛逛</button>
        </div>

        <section v-else class="order-list">
          <article v-for="order in filteredOrders" :key="order.id" class="order-card">
            <header>
              <span>{{ order.orderNo }}</span>
              <strong>{{ orderStatusText(order.status) }}</strong>
            </header>

            <div class="order-items">
              <div v-for="item in order.items" :key="`${order.id}-${item.id || item.productId}`" class="order-item">
                <div>
                  <h2>{{ item.productName }}</h2>
                  <small>数量 x{{ item.quantity }}</small>
                </div>
                <b>￥{{ item.price }}</b>
              </div>
            </div>

            <footer>
              <span>合计 <b>￥{{ order.totalAmount }}</b></span>
              <div>
                <button v-if="order.status === 'PENDING_PAYMENT'" type="button" class="ghost" @click="cancelOrder(order)">
                  取消订单
                </button>
                <button v-if="order.status === 'PENDING_PAYMENT'" type="button" @click="payOrder(order)">
                  去付款
                </button>
                <button v-if="order.status === 'SHIPPED'" type="button" class="ghost" @click="showTracking(order)">
                  查看物流
                </button>
                <button v-if="order.status === 'SHIPPED'" type="button" @click="confirmOrder(order)">
                  确认收货
                </button>
                <button v-if="canApplyAfterSale(order)" type="button" class="ghost" @click="applyAfterSale(order)">
                  申请售后
                </button>
              </div>
            </footer>

            <div v-if="trackingMap[order.id]?.length" class="tracking-panel">
              <p v-for="track in trackingMap[order.id]" :key="`${order.id}-${track.status}-${track.occurTime}`">
                {{ formatTime(track.occurTime) }} {{ track.description }}
              </p>
            </div>
          </article>
        </section>
      </section>

      <section v-show="currentView === 'message'" class="message-page">
        <ChatWidget :user="currentUser" :products="products" />
      </section>

      <p v-if="notice" class="toast-message">{{ notice }}</p>

      <nav v-if="currentView !== 'detail'" class="bottom-tabs" aria-label="底部导航">
        <button
          v-for="item in bottomTabs"
          :key="item.key"
          type="button"
          :class="{ active: currentTab === item.key }"
          @click="selectTab(item.key)"
        >
          <component :is="item.icon" :size="21" />
          <span>{{ item.label }}</span>
          <em v-if="item.key === 'message' && unreadCount">{{ unreadCount }}</em>
        </button>
      </nav>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import {
  BadgePercent,
  Bell,
  ChevronLeft,
  ChevronRight,
  Gift,
  Heart,
  Home,
  List,
  LogOut,
  MapPin,
  MessageCircle,
  Minus,
  PackageCheck,
  Pill,
  Plus,
  Search,
  Settings,
  Shirt,
  ShoppingBag,
  ShoppingCart,
  Sparkles,
  Store,
  TicketPercent,
  Trash2,
  Truck,
  UserRound,
  WalletCards,
  Zap
} from 'lucide-vue-next'
import { mallApi } from './api/mall'
import ChatWidget from './components/ChatWidget.vue'

const keyword = ref('')
const currentTab = ref('home')
const currentView = ref('home')
const previousView = ref('home')
const notice = ref('')
const products = ref([])
const visibleCount = ref(8)
const selectedProduct = ref(null)
const cartItems = ref([])
const orders = ref([])
const ordersLoading = ref(false)
const orderFilter = ref('all')
const trackingMap = ref({})
const currentUser = ref(null)
const authMode = ref('login')
const authLoading = ref(false)
const authForm = ref({
  username: '',
  phone: '',
  password: ''
})
const messages = ref([
  {
    id: 1,
    title: '物流提醒',
    content: '你的无线蓝牙耳机已从揭阳仓发出，预计明日送达。',
    unread: true
  },
  {
    id: 2,
    title: '智能客服',
    content: '售后、物流、退换货问题都可以直接咨询我。',
    unread: false
  }
])

const shortcuts = [
  { label: '秒送', icon: Zap, color: '#ef2d2d' },
  { label: '超市', icon: Store, color: '#ff7a1a' },
  { label: '新品', icon: Sparkles, color: '#f0447b' },
  { label: '关注', icon: Heart, color: '#8b5cf6' },
  { label: '推荐', icon: Gift, color: '#2f80ed' },
  { label: '国家补贴', icon: BadgePercent, color: '#e1251b' },
  { label: '医药健康', icon: Pill, color: '#10a870' },
  { label: '内衣', icon: Shirt, color: '#ec4899' },
  { label: '个护', icon: Sparkles, color: '#14b8a6' },
  { label: '分类', icon: List, color: '#64748b' }
]

const promoCards = [
  {
    title: '国家补贴',
    badge: '国补',
    goods: [
      {
        name: '智能手机',
        price: '￥2720',
        caption: '补贴价',
        image: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=360&q=80'
      },
      {
        name: '智能手表',
        price: '￥529',
        caption: '券后价',
        image: 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=360&q=80'
      }
    ]
  },
  {
    title: '大额券限时领',
    badge: '领券',
    goods: [
      {
        name: '无线耳机',
        price: '￥19.9',
        caption: '秒杀价',
        image: 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?auto=format&fit=crop&w=360&q=80'
      },
      {
        name: '运动相机',
        price: '￥529',
        caption: '券后价',
        image: 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=360&q=80'
      }
    ]
  }
]

const channelCards = [
  { title: '9.9 包邮', subtitle: '日用小物低价包邮', tag: '今日低价' },
  { title: '品质生活', subtitle: '超市秒送、买药秒送', tag: '本地服务' },
  { title: '以旧换新', subtitle: '家电数码补贴专区', tag: '送装一体' },
  { title: '学生专区', subtitle: '开学好物与试用福利', tag: '专属价' }
]

const bottomTabs = [
  { key: 'home', label: '首页', icon: Home },
  { key: 'message', label: '消息', icon: MessageCircle },
  { key: 'cart', label: '购物车', icon: ShoppingCart },
  { key: 'mine', label: '我的', icon: UserRound }
]

const orderTabs = [
  { key: 'pendingPayment', label: '待付款', statuses: ['PENDING_PAYMENT'] },
  { key: 'pendingReceive', label: '待收货', statuses: ['SHIPPED'] },
  { key: 'afterSale', label: '退换售后', statuses: ['SHIPPED', 'COMPLETED'] },
  { key: 'all', label: '全部订单', statuses: [] }
]

const mineMenu = [
  { label: '收货地址', icon: MapPin },
  { label: '我的关注', icon: Heart },
  { label: '优惠券', icon: TicketPercent },
  { label: '客户服务', icon: MessageCircle },
  { label: '账户设置', icon: Settings }
]

const fallbackProducts = [
  {
    id: 'p1',
    name: '晶核 Xtacking 3.0 固态硬盘',
    sellingPoint: '纵享 GEN4 疾速',
    specs: '长江存储原厂颗粒 1TB',
    service: '下单赠送安装工具',
    price: '299',
    sales: 2180,
    imageUrl: 'https://images.unsplash.com/photo-1591488320449-011701bb6704?auto=format&fit=crop&w=520&q=80',
    tags: ['官方', '七天试用', '国补贴'],
    highlights: ['PCIe 4.0 高速传输', '五年质保', '适合游戏和剪辑素材盘']
  },
  {
    id: 'p2',
    name: '无线蓝牙耳机 Pro 降噪版',
    sellingPoint: '主动降噪 40 小时续航',
    specs: '蓝牙5.3 低延迟游戏模式',
    service: '2年只换不修',
    price: '199',
    sales: 9840,
    imageUrl: 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?auto=format&fit=crop&w=520&q=80',
    tags: ['官方', '满减', '爆款'],
    highlights: ['通勤降噪更安静', '开盖即连', '支持快充和游戏低延迟']
  },
  {
    id: 'p3',
    name: '智能手表 NFC 血氧监测',
    sellingPoint: '全天候健康管理',
    specs: '高清屏 运动模式 100+',
    service: '免费表带调节',
    price: '529',
    sales: 3610,
    imageUrl: 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?auto=format&fit=crop&w=520&q=80',
    tags: ['国补', '健康', '新品'],
    highlights: ['NFC 门禁交通卡', '血氧心率监测', '长续航轻量机身']
  },
  {
    id: 'p4',
    name: '4K 144Hz 电竞显示器 27英寸',
    sellingPoint: '低蓝光 高刷新',
    specs: 'IPS 面板 HDR400',
    service: '送货上门 包安装',
    price: '1399',
    sales: 1540,
    imageUrl: 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=520&q=80',
    tags: ['官方', '晒单返券', '国补贴'],
    highlights: ['4K 细腻画质', '144Hz 高刷', '支持升降旋转支架']
  },
  {
    id: 'p5',
    name: '65W 氮化镓快充套装',
    sellingPoint: '小体积 三口输出',
    specs: '手机平板笔记本通用',
    service: '一年质保',
    price: '79',
    sales: 12600,
    imageUrl: 'https://images.unsplash.com/photo-1619953942547-233eab5a70d6?auto=format&fit=crop&w=520&q=80',
    tags: ['9.9包邮', '券后价', '热卖'],
    highlights: ['三设备同时充电', '折叠插脚便携', '智能温控保护']
  },
  {
    id: 'p6',
    name: '家用空气炸锅 5L 大容量',
    sellingPoint: '少油烹饪 一键清洗',
    specs: '智能触控 可视窗口',
    service: '七天无理由退换',
    price: '189',
    sales: 7230,
    imageUrl: 'https://images.unsplash.com/photo-1625944230945-1b7dd3b949ab?auto=format&fit=crop&w=520&q=80',
    tags: ['厨房', '放心买', '包邮'],
    highlights: ['5L 家庭容量', '可视窗口', '薯条鸡翅一键菜单']
  },
  {
    id: 'p7',
    name: '超薄移动电源 20000mAh',
    sellingPoint: '双向快充 轻巧便携',
    specs: '适配苹果安卓',
    service: '航空标准电芯',
    price: '119',
    sales: 6800,
    imageUrl: 'https://images.unsplash.com/photo-1618410320928-25228d811631?auto=format&fit=crop&w=520&q=80',
    tags: ['官方', '学生价', '低价'],
    highlights: ['20000mAh 大容量', '双口输出', '多重安全保护']
  },
  {
    id: 'p8',
    name: '人体工学办公椅 可躺午休',
    sellingPoint: '腰托可调 久坐舒适',
    specs: '高弹网布 静音轮',
    service: '送装一体',
    price: '399',
    sales: 940,
    imageUrl: 'https://images.unsplash.com/photo-1505843490701-5be5d0b31a93?auto=format&fit=crop&w=520&q=80',
    tags: ['家装', '大额券', '放心买'],
    highlights: ['腰托和头枕可调', '后仰午休模式', '静音万向轮']
  }
]

const storageUserKey = 'mall-current-user'
const activeUserKey = computed(() => currentUser.value?.username || 'guest')
const cartStorageKey = computed(() => `mall-cart-${activeUserKey.value}`)
const visibleProducts = computed(() => products.value.slice(0, visibleCount.value))
const cartCount = computed(() => cartItems.value.reduce((sum, item) => sum + item.quantity, 0))
const cartTotal = computed(() => {
  return cartItems.value
    .reduce((sum, item) => sum + Number(item.product.price) * item.quantity, 0)
    .toFixed(2)
})
const unreadCount = computed(() => messages.value.filter(item => item.unread).length)
const userInitial = computed(() => currentUser.value?.username?.slice(0, 1).toUpperCase() || 'U')
const orderStats = computed(() => orderTabs.map(item => ({
  key: item.key,
  label: item.label,
  value: countOrdersByFilter(item.key)
})))
const filteredOrders = computed(() => filterOrders(orderFilter.value))
const orderViewTitle = computed(() => orderTabs.find(item => item.key === orderFilter.value)?.label || '全部订单')
const orderViewSubtitle = computed(() => {
  const count = filteredOrders.value.length
  return count > 0 ? `共 ${count} 个订单，可查看状态、物流和售后` : '订单状态会在这里集中展示'
})
const emptyOrderText = computed(() => {
  if (orderFilter.value === 'pendingPayment') return '当前没有待付款订单。'
  if (orderFilter.value === 'pendingReceive') return '当前没有待收货订单。'
  if (orderFilter.value === 'afterSale') return '当前没有可申请售后的订单。'
  return '当前还没有订单，先去首页选择商品下单。'
})

watch(cartItems, saveCart, { deep: true })

async function loadProducts() {
  try {
    const remoteProducts = await mallApi.products({ keyword: keyword.value })
    products.value = normalizeProducts(remoteProducts)
  } catch {
    products.value = filterFallbackProducts()
  }
}

function normalizeProducts(remoteProducts) {
  if (!Array.isArray(remoteProducts) || remoteProducts.length === 0) {
    return filterFallbackProducts()
  }

  return remoteProducts.map((item, index) => ({
    id: item.id ?? `remote-${index}`,
    name: item.name,
    sellingPoint: item.sellingPoint || '官方自营 品质保障',
    specs: item.specs || '热销规格',
    service: item.service || '支持七天无理由退换',
    price: item.price,
    sales: 1000 + index * 437,
    imageUrl: item.imageUrl || fallbackProducts[index % fallbackProducts.length].imageUrl,
    tags: ['官方', '国补贴', '放心买'],
    highlights: ['官方自营渠道', '支持售后保障', '移动端快捷下单']
  }))
}

function filterFallbackProducts() {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return fallbackProducts
  return fallbackProducts.filter(item => {
    return [item.name, item.sellingPoint, item.specs, item.service, ...item.tags]
      .join(' ')
      .toLowerCase()
      .includes(value)
  })
}

function searchProducts() {
  visibleCount.value = 8
  currentView.value = 'home'
  currentTab.value = 'home'
  loadProducts()
}

function loadMore() {
  if (visibleCount.value >= products.value.length) {
    toast('已经加载全部商品')
    return
  }
  visibleCount.value += 4
}

function openProduct(product) {
  selectedProduct.value = product
  previousView.value = currentView.value
  currentView.value = 'detail'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goBack() {
  currentView.value = previousView.value === 'detail' ? 'home' : previousView.value
  if (['home', 'message', 'cart', 'mine'].includes(currentView.value)) {
    currentTab.value = currentView.value
  }
  selectedProduct.value = null
}

function addToCart(product) {
  const existing = cartItems.value.find(item => item.product.id === product.id)
  if (existing) {
    existing.quantity += 1
  } else {
    cartItems.value.push({ product, quantity: 1 })
  }
  toast(`${product.name} 已加入购物车`)
}

function buyNow(product) {
  addToCart(product)
  selectTab('cart')
}

function changeQuantity(item, delta) {
  item.quantity += delta
  if (item.quantity <= 0) {
    removeCartItem(item)
  }
}

function removeCartItem(item) {
  cartItems.value = cartItems.value.filter(cartItem => cartItem.product.id !== item.product.id)
}

function clearCart() {
  cartItems.value = []
  toast('购物车已清空')
}

async function checkout() {
  if (!currentUser.value) {
    selectTab('mine')
    toast('请先登录再结算')
    return
  }
  if (cartItems.value.length === 0) {
    toast('请先选择购物车商品')
    return
  }
  try {
    const submittedCount = cartCount.value
    await mallApi.clearCart()
    for (const item of cartItems.value) {
      if (Number.isFinite(Number(item.product.id))) {
        await mallApi.addCart({
          productId: Number(item.product.id),
          quantity: item.quantity
        })
      }
    }
    await mallApi.createOrder({
      receiverName: currentUser.value.username,
      receiverPhone: currentUser.value.phone || '13800000000',
      receiverAddress: '广东省揭阳市演示地址'
    })
    cartItems.value = []
    await loadOrders()
    await openOrders('pendingPayment')
    toast(`已提交 ${submittedCount} 件商品，待付款`)
  } catch (error) {
    toast(error.message || '提交订单失败')
  }
}

function selectTab(tab) {
  currentTab.value = tab
  currentView.value = tab
  selectedProduct.value = null
  if (tab === 'message') {
    messages.value.forEach(item => {
      item.unread = false
    })
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function submitAuth() {
  const payload = {
    username: authForm.value.username,
    phone: authForm.value.phone,
    password: authForm.value.password
  }
  if (!payload.username || !payload.password || (authMode.value === 'register' && !payload.phone)) {
    toast('请填写完整信息')
    return
  }
  authLoading.value = true
  try {
    const user = authMode.value === 'login'
      ? await mallApi.login({ username: payload.username, password: payload.password })
      : await mallApi.register(payload)
    setCurrentUser(user)
    toast(authMode.value === 'login' ? '登录成功' : '注册成功')
  } catch (error) {
    if (authMode.value === 'register') {
      const localUser = saveLocalUser(payload)
      setCurrentUser(localUser)
      toast('后端暂不可用，已保存到本地')
    } else {
      const localUser = findLocalUser(payload)
      if (localUser) {
        setCurrentUser(localUser)
        toast('已使用本地账号登录')
      } else {
        toast(error.message || '登录失败')
      }
    }
  } finally {
    authLoading.value = false
  }
}

async function openOrders(filter = 'all') {
  orderFilter.value = filter
  previousView.value = currentView.value
  currentView.value = 'orders'
  currentTab.value = 'mine'
  selectedProduct.value = null
  await loadOrders()
}

async function loadOrders() {
  ordersLoading.value = true
  try {
    orders.value = await mallApi.orders()
  } catch (error) {
    orders.value = []
    toast(error.message || '订单加载失败')
  } finally {
    ordersLoading.value = false
  }
}

function filterOrders(filter) {
  const tab = orderTabs.find(item => item.key === filter)
  if (!tab || tab.statuses.length === 0) {
    return orders.value
  }
  return orders.value.filter(order => tab.statuses.includes(order.status))
}

function countOrdersByFilter(filter) {
  return filterOrders(filter).length
}

async function payOrder(order) {
  try {
    await mallApi.payOrder(order.id)
    toast('付款成功，订单已进入待发货')
    await loadOrders()
  } catch (error) {
    toast(error.message || '付款失败')
  }
}

async function cancelOrder(order) {
  try {
    await mallApi.cancelOrder(order.id)
    toast('订单已取消')
    await loadOrders()
  } catch (error) {
    toast(error.message || '取消失败')
  }
}

async function confirmOrder(order) {
  try {
    await mallApi.confirmOrder(order.id)
    toast('已确认收货')
    await loadOrders()
  } catch (error) {
    toast(error.message || '确认收货失败')
  }
}

async function showTracking(order) {
  try {
    trackingMap.value = {
      ...trackingMap.value,
      [order.id]: await mallApi.tracking(order.id)
    }
    toast('物流信息已更新')
  } catch (error) {
    toast(error.message || '物流查询失败')
  }
}

function applyAfterSale(order) {
  selectTab('message')
  toast(`请在客服中发送订单号 ${order.orderNo} 和售后原因`)
}

function canApplyAfterSale(order) {
  return ['SHIPPED', 'COMPLETED'].includes(order.status)
}

function orderStatusText(status) {
  const labels = {
    PENDING_PAYMENT: '待付款',
    PENDING_SHIPMENT: '待发货',
    SHIPPED: '待收货',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }
  return labels[status] || status
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function setCurrentUser(user) {
  currentUser.value = user
  localStorage.setItem(storageUserKey, JSON.stringify(user))
  authForm.value = { username: '', phone: '', password: '' }
  loadCart()
}

function logout() {
  localStorage.removeItem(storageUserKey)
  currentUser.value = null
  cartItems.value = []
  loadCart()
  toast('已退出登录')
}

function saveLocalUser(payload) {
  const users = readJson('mall-local-users', [])
  const user = {
    id: `local-${Date.now()}`,
    username: payload.username,
    phone: payload.phone,
    password: payload.password,
    createdAt: new Date().toISOString()
  }
  const nextUsers = users.filter(item => item.username !== user.username)
  nextUsers.push(user)
  localStorage.setItem('mall-local-users', JSON.stringify(nextUsers))
  return { id: user.id, username: user.username, phone: user.phone, createdAt: user.createdAt }
}

function findLocalUser(payload) {
  const users = readJson('mall-local-users', [])
  const user = users.find(item => item.username === payload.username && item.password === payload.password)
  if (!user) return null
  return { id: user.id, username: user.username, phone: user.phone, createdAt: user.createdAt }
}

function loadPersistedUser() {
  currentUser.value = readJson(storageUserKey, null)
}

function saveCart() {
  localStorage.setItem(cartStorageKey.value, JSON.stringify(cartItems.value))
}

function loadCart() {
  cartItems.value = readJson(cartStorageKey.value, [])
}

function readJson(key, fallback) {
  try {
    const value = localStorage.getItem(key)
    return value ? JSON.parse(value) : fallback
  } catch {
    return fallback
  }
}

let noticeTimer = 0
function toast(message) {
  notice.value = message
  clearTimeout(noticeTimer)
  noticeTimer = window.setTimeout(() => {
    notice.value = ''
  }, 1800)
}

onMounted(() => {
  loadPersistedUser()
  loadCart()
  loadProducts()
  loadOrders()
})
</script>

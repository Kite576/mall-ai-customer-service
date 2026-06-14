<template>
  <section class="chat-page" aria-label="智能客服">
    <header class="chat-page-header">
      <div>
        <h1>消息</h1>
        <p>{{ user ? `${user.username}，智能客服在线` : '智能客服在线，登录后可保存用户数据' }}</p>
      </div>
      <button type="button" :disabled="messages.length <= 1" @click="clearConversation">
        <Trash2 :size="15" />
        清空
      </button>
    </header>

    <div ref="messagesEl" class="chat-body" role="log" aria-live="polite">
      <div v-for="message in visibleMessages" :key="message.id" :class="['message-line', message.role]">
        <div class="message-bubble">
          {{ message.content }}
          <span v-if="message.streaming" class="cursor-dot"></span>
        </div>
      </div>
    </div>

    <div class="quick-row" aria-label="快捷问题">
      <button v-for="item in quickQuestions" :key="item" type="button" @click="ask(item)">
        {{ item }}
      </button>
    </div>

    <form class="chat-inputbar" @submit.prevent="sendMessage">
      <input v-model="input" :disabled="streaming" placeholder="输入物流、退换货、商品咨询等问题" />
      <button type="submit" :disabled="streaming || !input.trim()">
        <SendHorizontal :size="16" />
      </button>
    </form>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { SendHorizontal, Trash2 } from 'lucide-vue-next'
import { mallApi } from '../api/mall'

const props = defineProps({
  user: {
    type: Object,
    default: null
  },
  products: {
    type: Array,
    default: () => []
  }
})

const welcomeMessage = '您好，我是智能客服。可以帮您查询商品、物流、退换货和售后政策。'
const quickQuestions = ['物流到哪了', '怎么退换货', '无线耳机有货吗', '有国家补贴吗']
const chatSchemaVersion = 2

const input = ref('')
const streaming = ref(false)
const messagesEl = ref(null)
const sessionId = ref('')
const messages = ref([])
const userKey = computed(() => props.user?.username || 'guest')
const chatStorageKey = computed(() => `mall-chat-${userKey.value}`)
const visibleMessages = computed(() => {
  return messages.value.filter(message => message.streaming || message.content?.trim())
})

watch(messages, saveConversation, { deep: true })
watch(userKey, loadConversation)

function scrollToBottom() {
  nextTick(() => {
    if (!messagesEl.value) return
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  })
}

async function ask(question) {
  input.value = question
  await sendMessage()
}

async function sendMessage() {
  const content = input.value.trim()
  if (!content || streaming.value) return

  input.value = ''
  if (!sessionId.value && crypto?.randomUUID) {
    sessionId.value = crypto.randomUUID()
  }

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content
  })

  const assistant = {
    id: Date.now() + 1,
    role: 'assistant',
    content: '',
    streaming: true
  }
  messages.value.push(assistant)
  streaming.value = true
  scrollToBottom()

  try {
    await mallApi.chatStream(
      {
        message: content,
        sessionId: sessionId.value,
        context: buildContext()
      },
      chunk => {
        assistant.content += chunk
        scrollToBottom()
      }
    )
  } catch {
    await simulateStream(content, chunk => {
      assistant.content += chunk
      scrollToBottom()
    })
  } finally {
    assistant.streaming = false
    streaming.value = false
    if (!assistant.content.trim()) {
      assistant.content = buildLocalReply(content)
    }
    scrollToBottom()
  }
}

function buildContext() {
  return messages.value
    .filter(item => item.content)
    .slice(-8)
    .map(item => ({
      role: item.role,
      content: item.content
    }))
}

function buildLocalReply(content) {
  const text = content.toLowerCase()
  if (text.includes('物流') || text.includes('订单') || text.includes('快递')) {
    return '请提供订单号，我会帮您查看发货状态、最新物流节点和预计送达时间。'
  }
  if (text.includes('退') || text.includes('换') || text.includes('售后')) {
    return '平台支持 7 天无理由退换货。请保持商品、配件和包装完整，在订单详情中提交售后申请即可。'
  }
  if (text.includes('补贴') || text.includes('国补')) {
    return '带有“国补贴”标签的商品可参与补贴活动，实际补贴金额以结算页展示为准。'
  }
  const matchedProduct = props.products.find(item => text.includes(String(item.name).slice(0, 2).toLowerCase()))
  if (matchedProduct) {
    return `${matchedProduct.name} 当前价格 ￥${matchedProduct.price}，${matchedProduct.sellingPoint || '支持官方售后'}。`
  }
  return '我已收到您的问题。请补充商品名称、订单号或具体售后场景，我会继续帮您处理。'
}

async function simulateStream(content, onChunk) {
  const reply = buildLocalReply(content)
  for (const char of reply) {
    await new Promise(resolve => window.setTimeout(resolve, 18))
    onChunk(char)
  }
}

async function clearConversation() {
  if (sessionId.value) {
    try {
      await mallApi.clearChat(sessionId.value)
    } catch {
      // 本地会话不需要服务端清理。
    }
  }
  sessionId.value = ''
  messages.value = [createWelcomeMessage()]
  saveConversation()
  scrollToBottom()
}

function createWelcomeMessage() {
  return {
    id: Date.now(),
    role: 'assistant',
    content: welcomeMessage
  }
}

function saveConversation() {
  const payload = {
    version: chatSchemaVersion,
    sessionId: sessionId.value,
    messages: messages.value
      .filter(message => message.content?.trim())
      .map(({ streaming, ...message }) => message)
  }
  localStorage.setItem(chatStorageKey.value, JSON.stringify(payload))
}

function loadConversation() {
  try {
    const raw = localStorage.getItem(chatStorageKey.value)
    if (!raw) {
      messages.value = [createWelcomeMessage()]
      sessionId.value = ''
      return
    }
    const payload = JSON.parse(raw)
    if (payload.version !== chatSchemaVersion) {
      localStorage.removeItem(chatStorageKey.value)
      messages.value = [createWelcomeMessage()]
      sessionId.value = ''
      return
    }
    sessionId.value = payload.sessionId || ''
    const savedMessages = Array.isArray(payload.messages)
      ? payload.messages.filter(message => message.content?.trim())
      : []
    messages.value = savedMessages.length
      ? savedMessages
      : [createWelcomeMessage()]
  } catch {
    messages.value = [createWelcomeMessage()]
    sessionId.value = ''
  }
  scrollToBottom()
}

onMounted(loadConversation)
</script>

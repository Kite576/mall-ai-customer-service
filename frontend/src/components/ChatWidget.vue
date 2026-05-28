<template>
  <section class="chat-card">
    <div class="chat-header">
      <div>
        <p class="eyebrow">智能客服</p>
        <h2>小智在线</h2>
      </div>
      <span>购物咨询 · 售后 · 物流</span>
    </div>

    <div class="chat-messages">
      <div v-for="item in messages" :key="item.id" :class="['bubble', item.role]">
        {{ item.content }}
      </div>
    </div>

    <form class="chat-form" @submit.prevent="send">
      <input v-model="input" placeholder="问问小智：耳机多少钱？订单 MO202605290001 到哪了？" />
      <button :disabled="loading">{{ loading ? '发送中' : '发送' }}</button>
    </form>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { mallApi } from '../api/mall'

const input = ref('')
const loading = ref(false)
const sessionId = ref('')
const messages = ref([
  { id: 1, role: 'assistant', content: '亲，你好呀，我是小智。商品、订单、物流和售后问题都可以问我。' }
])

async function send() {
  const content = input.value.trim()
  if (!content || loading.value) return
  input.value = ''
  messages.value.push({ id: Date.now(), role: 'user', content })
  loading.value = true
  try {
    const result = await mallApi.chat({ message: content, sessionId: sessionId.value })
    sessionId.value = result.sessionId
    messages.value.push({ id: Date.now() + 1, role: 'assistant', content: result.reply })
  } catch (error) {
    messages.value.push({ id: Date.now() + 1, role: 'assistant', content: `亲，${error.message}，请稍后再试或联系人工客服。` })
  } finally {
    loading.value = false
  }
}
</script>

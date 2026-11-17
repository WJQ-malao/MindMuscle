<template>
  <div class="chat-page">
    <div class="messages" ref="msgRef">
      <div v-for="(m, i) in messages" :key="i" class="row" :class="m.role">
        <div class="bubble" :class="{ thinking: m.thinking }">{{ m.content }}</div>
      </div>
    </div>
    <div class="input-bar">
      <el-input v-model="input" size="large" placeholder="问问 AI 教练…" @keyup.enter="send" />
      <el-button type="primary" size="large" :loading="streaming" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatApi } from '../../api'

interface Message { role: 'user' | 'assistant'; content: string; thinking?: boolean }

const messages = ref<Message[]>([
  { role: 'assistant', content: '我是你的 AI 教练，可以帮你制定计划、解答训练问题、记录打卡。' },
])
const input = ref('')
const streaming = ref(false)
const msgRef = ref<HTMLElement>()
const conversationId = 'mobile-' + Date.now()

async function scrollBottom() {
  await nextTick()
  msgRef.value!.scrollTop = msgRef.value!.scrollHeight
}

async function send() {
  const text = input.value.trim()
  if (!text || streaming.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  const reply: Message = { role: 'assistant', content: '', thinking: true }
  messages.value.push(reply)
  streaming.value = true
  await scrollBottom()
  try {
    await chatApi.stream(text, conversationId, (chunk) => {
      reply.thinking = false
      reply.content += chunk
      scrollBottom()
    })
  } catch (e: any) {
    reply.thinking = false
    reply.content = '请求失败：' + (e.message || e)
    ElMessage.error('对话请求失败')
  } finally {
    streaming.value = false
  }
}
</script>

<style scoped>
.chat-page { display: flex; flex-direction: column; height: calc(100vh - 130px); }
.messages { flex: 1; overflow-y: auto; padding: 4px; }
.row { display: flex; margin-bottom: 12px; }
.row.user { justify-content: flex-end; }
.bubble { max-width: 80%; padding: 10px 13px; border-radius: 12px; line-height: 1.7;
  white-space: pre-wrap; word-break: break-word; font-size: 15px; }
.row.user .bubble { background: #409eff; color: #fff; }
.row.assistant .bubble { background: #fff; border: 1px solid #eee; }
.thinking { color: #999; }
.thinking::after { content: '▍'; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0; } }
.input-bar { display: flex; gap: 8px; padding: 8px 0; }
</style>

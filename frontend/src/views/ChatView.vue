<template>
  <div class="chat-page">
    <div class="messages" ref="msgRef">
      <div v-for="(m, i) in messages" :key="i" class="row" :class="m.role">
        <div class="bubble" :class="{ thinking: m.thinking }">{{ m.content }}</div>
      </div>
    </div>
    <div class="input-bar">
      <el-input v-model="input" size="large" placeholder="问问你的 AI 教练，例如：帮我制定一个增肌计划"
                @keyup.enter="send" :disabled="streaming" />
      <el-button type="primary" size="large" :loading="streaming" @click="send">发送</el-button>
    </div>
    <div class="tips">
      试试：
      <el-tag v-for="t in tips" :key="t" size="small" class="tip" @click="input = t; send()">{{ t }}</el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatApi } from '../api'

interface Message {
  role: 'user' | 'assistant'
  content: string
  thinking?: boolean
}

const messages = ref<Message[]>([
  { role: 'assistant', content: '你好，我是你的 AI 健身教练。我可以基于健身知识库回答专业问题，也能帮你制定训练计划、记录打卡。' },
])
const input = ref('')
const streaming = ref(false)
const msgRef = ref<HTMLElement>()
// 会话ID固定为当前标签页会话，保证多轮上下文
const conversationId = 'web-' + Date.now()
const tips = [
  '帮我制定一个每周四练的增肌计划',
  '减脂应该怎么安排训练和饮食？',
  '深蹲时膝盖能不能超过脚尖？',
]

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
.chat-page { display: flex; flex-direction: column; height: calc(100vh - 120px); }
.messages { flex: 1; overflow-y: auto; padding: 8px; }
.row { display: flex; margin-bottom: 14px; }
.row.user { justify-content: flex-end; }
.bubble { max-width: 70%; padding: 10px 14px; border-radius: 10px; line-height: 1.7;
  white-space: pre-wrap; word-break: break-word; }
.row.user .bubble { background: #409eff; color: #fff; }
.row.assistant .bubble { background: #fff; border: 1px solid #eee; }
.thinking { color: #999; }
.thinking::after { content: '▍'; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0; } }
.input-bar { display: flex; gap: 10px; padding: 10px 0; }
.tips { color: #999; font-size: 12px; }
.tip { margin-left: 8px; cursor: pointer; }
</style>

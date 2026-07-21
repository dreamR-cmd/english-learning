<template>
  <div v-if="visible" class="rag-float">
    <button
      v-if="!open"
      type="button"
      class="rag-toggle"
      aria-label="打开 RAG 对话"
      @click="open = true"
    >
      AI
    </button>

    <section v-else class="rag-window" aria-label="RAG 对话">
      <header class="rag-head">
        <strong>RAG 对话</strong>
        <button type="button" class="icon-btn" aria-label="关闭 RAG 对话" @click="open = false">×</button>
      </header>

      <div ref="bodyRef" class="rag-body">
        <article
          v-for="message in messages"
          :key="message.id"
          class="rag-message"
          :class="message.role"
        >
          <p>{{ message.content }}</p>
        </article>
        <p v-if="!messages.length" class="rag-empty">可以问单词、语法或资料里的内容</p>
      </div>

      <form class="rag-input" @submit.prevent="submitQuestion">
        <textarea
          v-model.trim="question"
          rows="2"
          placeholder="输入问题..."
          :disabled="busy"
          required
          @keydown.enter.exact.prevent="submitQuestion"
        ></textarea>
        <button type="submit" :disabled="busy || !question">
          {{ busy ? '生成中' : '发送' }}
        </button>
      </form>
    </section>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { streamRagQuestion } from '../utils/api'

defineProps({
  visible: {
    type: Boolean,
    default: true
  }
})

const open = ref(false)
const busy = ref(false)
const question = ref('')
const messages = ref([])
const bodyRef = ref(null)
const MAX_VISIBLE_MESSAGES = 20

function createMessageId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function keepLatestMessages() {
  if (messages.value.length > MAX_VISIBLE_MESSAGES) {
    messages.value = messages.value.slice(-MAX_VISIBLE_MESSAGES)
  }
}

function updateMessage(messageId, updater) {
  const message = messages.value.find(item => item.id === messageId)
  if (message) updater(message)
}

async function scrollToBottom() {
  await nextTick()
  if (bodyRef.value) {
    bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  }
}

watch(open, isOpen => {
  if (isOpen) {
    scrollToBottom()
  }
})

async function submitQuestion() {
  const currentQuestion = question.value
  if (!currentQuestion || busy.value) return

  messages.value.push({
    id: createMessageId(),
    role: 'user',
    content: currentQuestion
  })
  keepLatestMessages()
  await scrollToBottom()

  question.value = ''
  busy.value = true

  const assistantMessage = {
    id: createMessageId(),
    role: 'assistant',
    content: ''
  }
  messages.value.push(assistantMessage)
  keepLatestMessages()
  await scrollToBottom()

  try {
    await streamRagQuestion(currentQuestion, 5, {
      onToken(token) {
        updateMessage(assistantMessage.id, message => {
          message.content += token
        })
        scrollToBottom()
      },
      onDone() {
        updateMessage(assistantMessage.id, message => {
          if (!message.content) {
            message.content = '回答已结束。'
          }
        })
        scrollToBottom()
      }
    })
  } catch (error) {
    updateMessage(assistantMessage.id, message => {
      message.content = error?.message || 'RAG 对话失败'
    })
    await scrollToBottom()
  } finally {
    busy.value = false
    await scrollToBottom()
  }
}
</script>

<style scoped>
.rag-float {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1000;
}

.rag-toggle {
  width: 58px;
  height: 58px;
  border: 0;
  border-radius: 50%;
  background: #2563eb;
  color: #fff;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.28);
}

.rag-window {
  width: min(360px, calc(100vw - 32px));
  height: min(500px, calc(100vh - 48px));
  display: grid;
  grid-template-rows: auto 1fr auto;
  overflow: hidden;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.22);
}

.rag-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #e2e8f0;
  color: #0f172a;
}

.icon-btn {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 6px;
  background: #f1f5f9;
  color: #334155;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.rag-body {
  display: grid;
  align-content: start;
  gap: 10px;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 12px;
  background: #f8fafc;
  scrollbar-width: none;
}

.rag-body::-webkit-scrollbar {
  display: none;
}

.rag-message {
  max-width: 86%;
  border-radius: 8px;
  padding: 9px 10px;
  background: #fff;
  color: #334155;
}

.rag-message.user {
  justify-self: end;
  background: #dbeafe;
}

.rag-message p {
  margin: 0;
  white-space: pre-line;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.rag-empty {
  margin: 0;
  color: #64748b;
  text-align: center;
  font-size: 13px;
}

.rag-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid #e2e8f0;
}

.rag-input textarea {
  width: 100%;
  min-height: 46px;
  max-height: 70px;
  resize: none;
  overflow: hidden;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 8px;
  font: inherit;
}

.rag-input button {
  border: 0;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  padding: 0 14px;
  cursor: pointer;
}

.rag-input button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 640px) {
  .rag-float {
    right: 14px;
    bottom: 14px;
  }

  .rag-window {
    width: calc(100vw - 28px);
    height: min(520px, calc(100vh - 28px));
  }
}
</style>

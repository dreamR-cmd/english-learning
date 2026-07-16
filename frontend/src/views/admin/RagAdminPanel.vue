<template>
  <div class="rag-admin">
    <section v-if="notice || errorMessage" class="rag-alert" :class="{ error: errorMessage }">
      {{ errorMessage || notice }}
    </section>

    <section class="rag-panel ingest-panel">
      <header class="panel-head">
        <div>
          <h2>文档入库</h2>
          <p>PDF、Word 或文本资料会被切分并写入向量库。</p>
        </div>
        <button type="button" class="secondary-btn" @click="loadDocuments">刷新列表</button>
      </header>

      <div class="ingest-grid">
        <form class="rag-form" @submit.prevent="submitTextDocument">
          <label>
            <span>标题</span>
            <input v-model.trim="textForm.title" placeholder="例如：定语从句讲义" required />
          </label>
          <label>
            <span>来源</span>
            <input v-model.trim="textForm.source" placeholder="manual / lesson / book" />
          </label>
          <label class="full-row">
            <span>正文</span>
            <textarea v-model.trim="textForm.content" rows="8" placeholder="粘贴要入库的资料正文" required></textarea>
          </label>
          <button type="submit" :disabled="busy.text">
            {{ busy.text ? '入库中...' : '文本入库' }}
          </button>
        </form>

        <form class="rag-form upload-form" @submit.prevent="submitFileDocument">
          <label>
            <span>文件标题</span>
            <input v-model.trim="fileForm.title" placeholder="默认使用文件名" />
          </label>
          <label>
            <span>来源</span>
            <input v-model.trim="fileForm.source" placeholder="默认使用原文件名" />
          </label>
          <label class="full-row">
            <span>文件</span>
            <input ref="fileInput" type="file" accept=".pdf,.doc,.docx" @change="handleFileChange" />
          </label>
          <button type="submit" :disabled="busy.file || !selectedFile">
            {{ busy.file ? '解析中...' : '文件入库' }}
          </button>
        </form>
      </div>
    </section>

    <section class="rag-panel docs-panel">
      <header class="panel-head compact">
        <div>
          <h2>资料列表</h2>
          <p>{{ documents.length }} 份资料</p>
        </div>
      </header>
      <table>
        <thead>
          <tr>
            <th>标题</th>
            <th>来源</th>
            <th>创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="document in documents" :key="document.id">
            <td>{{ document.title }}</td>
            <td>{{ document.source || '-' }}</td>
            <td>{{ formatDate(document.createdAt) }}</td>
          </tr>
          <tr v-if="!documents.length">
            <td colspan="3" class="empty-cell">暂无入库资料</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="rag-panel chat-panel">
      <header class="panel-head">
        <div>
          <h2>RAG 对话</h2>
          <p>基于知识库检索结果生成回答。</p>
        </div>
        <label class="topk-control">
          <span>TopK</span>
          <input v-model.number="topK" type="number" min="1" max="20" />
        </label>
      </header>

      <div class="chat-history">
        <article v-for="message in chatMessages" :key="message.id" class="chat-message" :class="message.role">
          <strong>{{ message.role === 'user' ? '我' : 'RAG' }}</strong>
          <p>{{ message.content }}</p>
        </article>
        <p v-if="!chatMessages.length" class="empty-chat">还没有对话记录</p>
      </div>

      <form class="chat-form" @submit.prevent="submitQuestion">
        <textarea v-model.trim="question" rows="3" placeholder="输入问题，例如：现在完成时怎么用？" required></textarea>
        <button type="submit" :disabled="busy.chat">{{ busy.chat ? '生成中...' : '发送' }}</button>
      </form>
    </section>

    <section class="rag-panel search-panel">
      <header class="panel-head">
        <div>
          <h2>资料检索</h2>
          <p>查看问题命中的原始片段。</p>
        </div>
      </header>
      <form class="search-form" @submit.prevent="submitSearch">
        <input v-model.trim="searchQuestion" placeholder="输入检索问题" required />
        <button type="submit" :disabled="busy.search">{{ busy.search ? '检索中...' : '检索' }}</button>
      </form>
      <div class="search-results">
        <article v-for="item in searchResults" :key="`${item.documentId}-${item.score}-${item.snippet}`">
          <header>
            <strong>{{ item.title }}</strong>
            <span>{{ item.score }}</span>
          </header>
          <p>{{ item.snippet }}</p>
        </article>
        <p v-if="searched && !searchResults.length" class="empty-cell">没有命中资料</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import {
  createRagDocument,
  getRagDocuments,
  searchRagDocuments,
  streamRagQuestion,
  uploadRagDocument
} from '../../utils/api'

const props = defineProps({
  refreshKey: {
    type: Number,
    default: 0
  }
})

const documents = ref([])
const question = ref('')
const searchQuestion = ref('')
const searchResults = ref([])
const chatMessages = ref([])
const selectedFile = ref(null)
const fileInput = ref(null)
const topK = ref(5)
const searched = ref(false)
const notice = ref('')
const errorMessage = ref('')

const busy = reactive({
  text: false,
  file: false,
  chat: false,
  search: false
})

const textForm = reactive({
  title: '',
  source: '',
  content: ''
})

const fileForm = reactive({
  title: '',
  source: ''
})

watch(() => props.refreshKey, () => {
  loadDocuments()
})

function formatDate(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : ''
}

function normalizeTopK() {
  const value = Number(topK.value) || 5
  return Math.max(1, Math.min(value, 20))
}

function showNotice(text) {
  notice.value = text
  errorMessage.value = ''
  window.setTimeout(() => {
    if (notice.value === text) notice.value = ''
  }, 2400)
}

function showError(error) {
  notice.value = ''
  errorMessage.value = error?.response?.data?.message || error?.message || '操作失败'
}

function createMessageId() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function resetTextForm() {
  Object.assign(textForm, { title: '', source: '', content: '' })
}

function resetFileForm() {
  Object.assign(fileForm, { title: '', source: '' })
  selectedFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function handleFileChange(event) {
  selectedFile.value = event.target.files?.[0] || null
}

async function loadDocuments() {
  try {
    const response = await getRagDocuments()
    documents.value = response.data.data || []
  } catch (error) {
    showError(error)
  }
}

async function submitTextDocument() {
  busy.text = true
  try {
    await createRagDocument({
      title: textForm.title,
      source: textForm.source,
      content: textForm.content
    })
    resetTextForm()
    await loadDocuments()
    showNotice('文本资料已入库')
  } catch (error) {
    showError(error)
  } finally {
    busy.text = false
  }
}

async function submitFileDocument() {
  if (!selectedFile.value) return
  busy.file = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    if (fileForm.title) formData.append('title', fileForm.title)
    if (fileForm.source) formData.append('source', fileForm.source)
    await uploadRagDocument(formData)
    resetFileForm()
    await loadDocuments()
    showNotice('文件资料已入库')
  } catch (error) {
    showError(error)
  } finally {
    busy.file = false
  }
}

async function submitQuestion() {
  const currentQuestion = question.value
  if (!currentQuestion) return

  const userMessage = { id: createMessageId(), role: 'user', content: currentQuestion }
  chatMessages.value.push(userMessage)
  question.value = ''
  busy.chat = true

  const assistantMessage = {
    id: createMessageId(),
    role: 'assistant',
    content: '',
    references: []
  }
  chatMessages.value.push(assistantMessage)
  const assistantIndex = chatMessages.value.length - 1

  const updateAssistantMessage = updater => {
    const message = chatMessages.value[assistantIndex]
    if (message) updater(message)
  }

  try {
    await streamRagQuestion(currentQuestion, normalizeTopK(), {
      onReferences(references) {
        updateAssistantMessage(message => {
          message.references = references || []
        })
      },
      onToken(token) {
        updateAssistantMessage(message => {
          message.content += token
        })
      },
      onDone() {
        updateAssistantMessage(message => {
          if (!message.content) {
            message.content = '回答已结束。'
          }
        })
      }
    })
  } catch (error) {
    if (!chatMessages.value[assistantIndex]?.content) {
      chatMessages.value = chatMessages.value.filter(message => message.id !== assistantMessage.id)
    }
    showError(error)
  } finally {
    busy.chat = false
  }
}

async function submitSearch() {
  busy.search = true
  searched.value = true
  try {
    const response = await searchRagDocuments(searchQuestion.value, normalizeTopK())
    searchResults.value = response.data.data || []
  } catch (error) {
    showError(error)
  } finally {
    busy.search = false
  }
}

onMounted(loadDocuments)
</script>

<style scoped>
.rag-admin {
  display: grid;
  gap: 16px;
}

.rag-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.rag-alert {
  border-radius: 8px;
  background: #ecfdf5;
  color: #047857;
  padding: 12px 14px;
}

.rag-alert.error {
  background: #fef2f2;
  color: #b91c1c;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.panel-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.panel-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.ingest-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.8fr);
  gap: 14px;
}

.rag-form,
.chat-form,
.search-form {
  display: grid;
  gap: 12px;
}

.rag-form {
  align-content: start;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.upload-form {
  grid-template-columns: 1fr;
}

.full-row {
  grid-column: 1 / -1;
}

label {
  display: grid;
  gap: 6px;
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

input,
textarea {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 9px 10px;
  font: inherit;
  font-weight: 400;
}

textarea {
  resize: vertical;
  min-height: 88px;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.secondary-btn {
  background: #64748b;
}

.docs-panel table {
  width: 100%;
  border-collapse: collapse;
}

.docs-panel th,
.docs-panel td {
  border-bottom: 1px solid #e2e8f0;
  padding: 10px;
  text-align: left;
}

.empty-cell,
.empty-chat {
  color: #64748b;
  text-align: center;
}

.topk-control {
  width: 100px;
}

.chat-history {
  display: grid;
  gap: 10px;
  min-height: 180px;
  max-height: 420px;
  overflow: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  background: #f8fafc;
}

.chat-message {
  width: min(720px, 100%);
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
}

.chat-message.user {
  justify-self: end;
  background: #dbeafe;
}

.chat-message strong {
  display: block;
  margin-bottom: 4px;
  color: #0f172a;
}

.chat-message p {
  margin: 0;
  white-space: pre-wrap;
  color: #334155;
}

.chat-form {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  margin-top: 12px;
}

.search-form {
  grid-template-columns: minmax(0, 1fr) auto;
}

.search-results {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.search-results article {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
}

.search-results header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.search-results p {
  margin: 0;
  color: #475569;
}

@media (max-width: 900px) {
  .ingest-grid,
  .rag-form,
  .chat-form,
  .search-form {
    grid-template-columns: 1fr;
  }
}
</style>

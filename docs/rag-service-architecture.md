# RAG 服务架构文档

> 模块：`backend-services/rag-service`，AI 能力核心（LangChain4j 1.17.2 + Ollama）。
> 本文覆盖：问答端到端流程、意图路由、多 Agent 与工具调用、文档入库（含图片/视频/扫描 PDF）、对话记忆与上下文压缩、配置项与已知限制。

---

## 1. 端到端问答流程

```
用户输入 (Vue 3, api.js streamRagQuestion)
   ↓ fetch POST /api/rag/ask/stream
网关 GatewayAuthFilter 鉴权 → 注入 X-User-Id
   ↓
RagController.askStream() → SseEmitter（CompletableFuture 异步推送）
   ↓
InMemoryRagService.askStream()
   ├─ rag.agent.enabled=false → 检索直答（naive RAG 兜底路径）
   ↓ (agent.enabled=true)
① RagIntentRouter.route()        意图路由（纯规则，无 LLM）
② RagAgentService.selectAgent()  switch 映射到专科 Agent
③ SpecialistAgentRunner          LangChain4j AiServices 构建
   + Agent 中文 System Message
   + ChatMemory（按 sessionId，带溢出压缩）
   + 工具循环 maxToolCallingRoundTrips(4)
④ LLM（默认 Ollama qwen3.5:9b）自主决定工具调用
   ├─ KnowledgeTools.searchKnowledgeBase → RAG 检索（向量+关键词合并）
   ├─ LearningTools / WrongRecordTools   → 内部微服务数据
   └─ UserActionTools                    → 待确认写操作 (PendingUserActionStore)
⑤ 模型基于工具结果二次推理 → 生成回答
   ↓ SSE: references → token(逐个) → done/error
前端 ReadableStream 逐块解析渲染
```

**兜底**：Agent 流式失败且未输出过 token 时，自动降级为"检索上下文直答"（`askWithRetrievedContext`）。

## 2. 意图路由（规则版）

`RagIntentRouter.route()` 按优先级顺序做关键词/正则匹配：

| 优先级 | 规则 | 意图 | 目标 Agent |
|---|---|---|---|
| 1 | 消息本身是"确认/好的/取消/算了"等 | USER_ACTION | SafetyAgent |
| 2 | 含 删除/修改/购买/收藏/记录到错题 等操作动词 | USER_ACTION | SafetyAgent |
| 3 | 含 错题/做错/错因/错题本 | WRONG_RECORD | WrongRecordAgent |
| 4 | 含 今天/复习/学习计划/进度 或 cet4/ielts 等考试词 | LEARNING_PLAN | LearningAgent |
| 5 | 含 作文/翻译/批改/润色 | WRITING_CORRECTION | KnowledgeAgent |
| 6 | 含 出题/练习题/测试 | PRACTICE_GENERATE | KnowledgeAgent |
| 7 | 含 什么意思/语法/例句 或任意英文单词 `[a-zA-Z]{2,}` | KNOWLEDGE_QA | KnowledgeAgent |
| 兜底 | 都不匹配 | KNOWLEDGE_QA | KnowledgeAgent |

已知缺陷：顺序敏感的误判（如"我不想**删除**错题"命中"删除"进 USER_ACTION）；路由一次性、不可纠正。

## 3. 文档入库

### 3.1 入口（两个，汇合后管道相同）

| 入口 | 接口 | 说明 |
|---|---|---|
| JSON 直传文本 | `POST /api/rag/documents` | 调用方保证 content 为纯文本，无解析环节 |
| 文件上传 | `POST /api/rag/documents/upload` | multipart，经 `RagDocumentFileParser` 解析 |

统一管道：`MySQL 存原文 → SimpleRagChunker 分块(800字/重叠120) → 逐块: MySQL 存 chunk + bge-m3 向量化(1024维) + Qdrant upsert → 失败补偿删除已写向量`。

### 3.2 支持的格式与解析方式

| 格式 | 解析方式 | 备注 |
|---|---|---|
| PDF（文本层） | PDFBox `PDFTextStripper`，**逐页**抽取 | 页间 `\n\n` 分隔 |
| PDF（图片/扫描页） | 文本层为空的页 → `PDFRenderer` 渲染 150DPI JPEG → Ollama 视觉模型 OCR | 页数上限 `rag.ocr.max-pdf-pages`(50)，超出跳过并告警 |
| DOCX | POI XWPFWordExtractor | 表格文本会带出，图片/文本框丢失 |
| DOC | POI HWPF WordExtractor | 老格式，复杂排版可能漏段 |
| 图片 png/jpg/jpeg/webp/bmp/gif | Ollama 视觉模型 OCR | 无文字返回空 |
| 视频 mp4/mov/mkv/avi/webm | ffmpeg 按 `rag.ocr.video-frame-interval-seconds`(10s) 抽帧 → 逐帧 OCR → 相邻去重拼接 | 只抓画面文字（字幕/板书/PPT），不含语音内容 |

OCR 实现：`RagOcrService` → `POST {ollama}/api/generate`（model=`rag.ocr.model`，默认 `qwen2.5vl:7b`，图片 base64，stream=false）。识别失败降级为空串并 warn，不阻断整份文档。

**部署前提**：
```bash
ollama pull qwen2.5vl:7b   # 视觉模型
ffmpeg                      # 视频抽帧必需（Windows: winget install ffmpeg）
```

### 3.3 向量库写入（Qdrant）

- point id = MySQL `rag_document_chunk` 主键；vectorId 字符串 `doc-{文档ID}-chunk-{序号}`
- payload 携带 `document_id / title / source / content(原文)`，检索命中无需回查 MySQL
- 可选向量归一化（cosine）；检索 `ef` 参数经 HNSW params 配置

### 3.4 入库已知问题

| 问题 | 位置 | 影响 |
|---|---|---|
| Qdrant upsert 异常被吞（只 log 不抛） | QdrantRagVectorStore:122 | 半截入库无感知，补偿逻辑失效 |
| MySQL 不回滚 | InMemoryRagService:138 | 失败后留孤儿 chunk 行 |
| 同步逐块 embed / 同步 OCR | addDocument | 大文件长时间占住 HTTP 请求 |
| 内容无去重 | — | 同文件重复上传产生重复 chunk |
| 无魔数/病毒校验 | RagDocumentFileParser | 仅按扩展名与大小限制 |

## 4. 对话记忆与上下文压缩

- key：sessionId（流式来自前端；同步路径取 userId）
- 实现：`SummarizingChatMemory`（自定义 `ChatMemory`，替代原 MessageWindowChatMemory）
- 压缩逻辑：非 system 消息数 > `rag.chat.memory.max-messages`(20) 时，摘走最旧的 `(max - keep)` 条交给 LLM 摘要；摘要以 `SystemMessage("【此前对话摘要】…")` 固定在上下文最前；最近 `rag.chat.memory.keep-messages`(10) 条原文永远保留
- **摘要有界**：新摘要超过 `rag.chat.memory.max-summary-chars`(2000) 字时对摘要本身再压缩一轮，仍超长则硬截断——上下文总规模封顶
- 压缩失败降级为普通滑动窗口（丢最旧），不阻断请求
- 时序：摘要在**检索前**随上下文被模型读取（影响查询词改写）；压缩动作发生在**每轮问答完成后**写入记忆时
- 记忆存进程内存：重启失忆、不能多实例；持久化（RedisChatMemoryStore）是后续项

## 5. 配置项速查

| 配置 | 默认 | 说明 |
|---|---|---|
| `rag.chat.provider` | ollama | ollama / openai 兼容 |
| `rag.chat.model-name` | qwen3.5:9b | 对话模型 |
| `rag.chat.ollama.base-url` | host.docker.internal:11434 | Ollama 地址 |
| `rag.agent.enabled` | true | Agent 模式开关（false 走检索直答） |
| `rag.max-results` | 5 | 默认 topK |
| `rag.chunk.max-chars` | 800 | 分块大小 |
| `rag.chunk.overlap-chars` | 120 | 分块重叠 |
| `rag.document.max-upload-bytes` | 20971520 | 上传大小上限 |
| `rag.ocr.model` | qwen2.5vl:7b | 视觉 OCR 模型 |
| `rag.ocr.max-pdf-pages` | 50 | 扫描 PDF OCR 页数上限 |
| `rag.ocr.video-frame-interval-seconds` | 10 | 视频抽帧间隔 |
| `rag.ocr.timeout-seconds` | 120 | OCR HTTP 超时 |
| `rag.chat.memory.max-messages` | 20 | 触发压缩的消息数阈值 |
| `rag.chat.memory.keep-messages` | 10 | 压缩后保留的最近消息数 |
| `rag.chat.memory.max-summary-chars` | 2000 | 摘要长度上限 |
| `rag.vector.store` | - | qdrant / redis 栈选择 |

## 6. 待改进清单

1. **路由升级**（见 §7 讨论）：小模型意图分类替代纯正则
2. **入库异步化**：上传返回 taskId，后台任务解析+入库，前端轮询进度（OCR/视频场景尤其必要）
3. **修复 upsert 吞异常**：让其抛出走补偿逻辑
4. **记忆持久化**：ChatMemoryStore 写 Redis，支持重启与多实例
5. **thinking 透传**：`OllamaStreamingChatModel.returnThinking(true)` + `TokenStream.onPartialThinking` + SSE `thinking` 事件，让模型推理过程前端可见
6. **答案自检**：引用归因校验（答案论断是否被检索资料支撑），失败重生成一次
7. **分块升级**：`DocumentSplitters.recursive` 替代字符硬切；`ParsedDocument` 结构化（页码/标题进 chunk metadata）
8. **上传安全**：魔数校验、内容 hash 去重
9. **txt/md 格式支持**：解析器补两个 case 即可

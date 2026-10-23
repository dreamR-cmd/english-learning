# English Learning（英语学习平台）

面向 CET-4、CET-6、考研英语、托福、雅思、GRE 等场景的英语学习平台。项目包含学习练习、用户中心、商城订单、后台管理、RAG 知识库问答等模块，当前主线架构是 **Vue 3 + Vite 前端**、**Spring Cloud Gateway** 和多个 **Spring Boot 3 微服务**。

> 当前文档基于 2026-10-23 的仓库状态整理。原单体后端已删除，主线为 `backend-services/` 下的 7 个微服务。RAG 向量库已从 Redis Stack 迁移至 Qdrant，Embedding 和 LLM 全部本地化（Ollama）。

## 当前能力

- 多考试模块：CET-4、CET-6、托福、雅思、考研英语、GRE。
- 学习练习：单词、每日单词、复习单词、阅读、听力。
- 用户中心：注册、登录、资料设置、错题本、阅读收藏、精选读物收藏。
- 商城交易：商品列表、商品搜索、同步下单、秒杀排队下单、模拟支付、订单查询、超时取消。
- 并发治理：Gateway 令牌桶限流、后端一次性下单 token、Redis 幂等 key、Redisson 分布式锁、数据库唯一约束、数据库原子扣库存。
- 缓存防护：商品库存 Redis 缓存、非法 ID 拦截、Bitmap 布隆过滤器、空值缓存、双 Buffer 定时重建。
- 搜索能力：商城商品使用 Elasticsearch 建索引，Redis 缓存搜索结果，失败时回退 MySQL 模糊查询。
- 后台管理：订单、模块、用户、角色、权限、审计日志、权限变更日志。
- 风险控制：管理员敏感操作需要 `CONFIRM` 二次确认，系统内置角色和模块受后端保护。
- RAG 知识库：文本/PDF/Word 文档入库、切片、向量检索、知识库问答、SSE 流式回答、前台悬浮 AI 对话。
- 数据治理：通过 `migration-service` 运行 Flyway 迁移脚本，基础数据由版本化 SQL 初始化。
- 网关边界：Gateway 校验登录态后注入 `X-User-Id`，并向下游注入 `X-Internal-Gateway-Secret`，下游服务拒绝绕过网关的普通业务请求。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3、Vue Router 4、Pinia、Axios、Vite 5 |
| 网关 | Spring Cloud Gateway、RedisRateLimiter、Actuator |
| 后端 | Spring Boot 3.2.5、Spring Web、Spring Data JPA、Bean Validation |
| 数据库迁移 | Flyway、MySQL 8 |
| 缓存 | Redis 7（缓存、锁、布隆过滤器，不再承担向量存储） |
| 向量检索 | **Qdrant**（HNSW + COSINE） |
| Embedding | **Ollama + bge-m3**（本地，1024 维，零成本） |
| LLM | **Ollama + qwen3.5:9b**（本地，中英双语 Agent 问答） |
| 消息队列 | RabbitMQ、TTL 队列、死信队列 |
| 商品搜索 | Elasticsearch 8、Redis 搜索结果缓存 |
| RAG | LangChain4j（Agent、Function Calling、AiServices）、PDFBox、Apache POI |
| 文档 | springdoc-openapi / Swagger UI |
| 安全 | Argon2 密码哈希、自定义 HMAC Token、Gateway 统一认证、RBAC、内部网关密钥 |
| 构建 | Maven、npm、Docker Compose |
| 运行时 | Java 17、Node.js 18+ |

## 项目结构

```text
english-learning/
├── README.md
├── PROJECT_ANALYSIS.md
├── docker-compose.yml
├── Dockerfile.backend
├── services.ps1
├── docs/
│   └── sql/
│       └── cleanup-test-data.sql
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── components/
│       │   └── FloatingRagChat.vue
│       ├── router/
│       ├── utils/
│       └── views/
└── backend-services/
    ├── gateway/              # 统一 API 入口，默认 8081
    ├── migration-service/    # Flyway 数据库迁移，一次性任务
    ├── auth-service/         # 登录、注册、Token 签发，默认 8087
    ├── user-service/         # 用户资料、错题、收藏、单词进度，默认 8088
    ├── learning-service/     # 模块、单词、阅读、听力、精选读物，默认 8089
    ├── shop-service/         # 商城、订单、库存、搜索、秒杀，默认 8090
    ├── admin-service/        # 后台管理、RBAC、审计，默认 8091
    └── rag-service/          # RAG 文档、Agent、向量检索、问答，默认 8092
```

## 服务划分

| 服务 | 端口 | 职责 |
|---|---:|---|
| frontend | 3000 | Vue 前端页面，开发环境代理到 Gateway |
| gateway | 8081 | 统一入口、路由、认证、用户上下文透传、限流、Swagger 转发 |
| migration-service | 无 Web 端口 | 启动后执行 Flyway 迁移并退出 |
| auth-service | 8087 | 登录、注册、密码哈希、Token 签发 |
| user-service | 8088 | 个人资料、错题本、收藏夹、单词掌握进度 |
| learning-service | 8089 | 考试模块、单词/阅读/听力练习、精选读物 |
| shop-service | 8090 | 商品、搜索、订单、库存、秒杀排队、超时取消 |
| admin-service | 8091 | 后台管理、权限校验、审计日志、风险确认 |
| rag-service | 8092 | RAG 文档入库、向量检索、问答、流式输出 |

## 请求链路

推荐访问路径：

```text
浏览器 -> frontend:3000 -> gateway:8081 -> auth/user/learning/shop/admin/rag 服务
```

Gateway 会做三件关键事情：

1. 校验 `/api/**` 的 `Authorization: Bearer <token>`，登录和注册接口除外。
2. 校验通过后注入 `X-User-Id` 和 `X-Token-Expires-At`。
3. 向所有下游请求注入 `X-Internal-Gateway-Secret`，下游服务用它拦截直接访问。

Docker Compose 中大部分业务服务使用 `expose` 只在内部网络暴露，统一入口是 `gateway:8081`。`rag-service` 当前仍映射了 `8092` 和 `5006`，便于本地调试；生产环境建议改为仅内部暴露。

## Gateway 路由

| 路径 | 目标服务 |
|---|---|
| `/api/auth/**` | auth-service |
| `/api/user/**` | user-service |
| `/api/modules/**` | learning-service |
| `/api/practice/**` | learning-service |
| `/api/selected-readings/**` | learning-service |
| `/api/shop/**` | shop-service |
| `/api/admin/**` | admin-service |
| `/api/rag/**` | rag-service |
| `/swagger-ui/**`、`/swagger-ui.html` | learning-service |
| `/v3/api-docs/auth` | auth-service OpenAPI |
| `/v3/api-docs/user` | user-service OpenAPI |
| `/v3/api-docs/learning` | learning-service OpenAPI |
| `/v3/api-docs/shop` | shop-service OpenAPI |
| `/v3/api-docs/admin` | admin-service OpenAPI |
| `/v3/api-docs/rag` | rag-service OpenAPI |

Gateway 当前配置的重点限流：

| 路径 | 限流对象 | replenishRate | burstCapacity |
|---|---|---:|---:|
| `/api/auth/**` | IP | 5 | 20 |
| `/api/shop/orders` | 用户，拿不到用户时按 IP | 3 | 10 |
| `/api/shop/**` | 用户，拿不到用户时按 IP | 10 | 30 |

## 数据库迁移

数据库结构和基础数据由 `backend-services/migration-service` 统一维护：

```text
backend-services/migration-service/src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__seed_base_data.sql
├── V3__admin_audit_risk_control.sql
└── V4__rag_documents_chunks.sql
```

后端业务服务默认使用：

```yaml
spring.jpa.hibernate.ddl-auto: ${JPA_DDL_AUTO:validate}
spring.sql.init.mode: never
```

开发和生产都应通过新增 Flyway 脚本演进数据库，不建议再依赖 JPA 自动改表。新增表、字段、索引或种子数据时，按 `V{版本号}__说明.sql` 命名。

## 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+
- Docker / Docker Compose
- MySQL 8
- Redis 7
- RabbitMQ 3
- Elasticsearch 8
- **Qdrant**（向量库，Docker Compose 自动部署）
- **Ollama**（本地模型推理，宿主机运行，需要预先拉取 `bge-m3` 和 `qwen3.5:9b`）

### Ollama 宿主机初始化

Ollama **不在** Docker Compose 里（避免大模型重复下载），需在宿主机单独运行：

```powershell
# 1. 安装 Ollama（Windows: https://ollama.com/download）

# 2. 带 0.0.0.0 重启（让 Docker 容器也能访问）
$env:OLLAMA_HOST = "0.0.0.0:11434"
ollama serve

# 3. 另开终端，拉两个模型
ollama pull bge-m3        # Embedding（1024 维）
ollama pull qwen3.5:9b    # Chat Model
```

Docker Compose 会通过 `host.docker.internal:11434` 访问宿主机 Ollama。

## 快速启动

推荐使用 Docker Compose 一次性启动全部依赖和服务：

```bash
docker compose up --build
```

后台启动：

```bash
docker compose up -d --build
```

常用访问地址：

| 服务 | 地址 |
|---|---|
| 前端 | http://localhost:3000 |
| Gateway / API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui/index.html |
| RabbitMQ 管理台 | http://localhost:15672 |
| Elasticsearch | http://localhost:9200 |

Docker Compose 会等待 MySQL 健康检查通过，先运行 `migration-service`，迁移成功后再启动业务服务。

停止服务：

```bash
docker compose down
```

如果要同时清除容器卷数据：

```bash
docker compose down -v
```

## 本地开发启动

先启动基础设施，可以只用 Compose 启动依赖：

```bash
docker compose up -d mysql redis rabbitmq elasticsearch qdrant
```

首次启动或数据库结构变化后，先执行迁移：

```powershell
cd backend-services/migration-service
mvn spring-boot:run
```

然后在项目根目录启动各服务：

```powershell
.\services.ps1 start
.\services.ps1 status
.\services.ps1 stop
```

也可以单独启动某个服务：

```powershell
cd backend-services/gateway
mvn spring-boot:run

cd frontend
npm install
npm run dev
```

前端开发代理默认指向 `http://localhost:8081`。如需覆盖：

```powershell
$env:VITE_API_TARGET = 'http://localhost:8081'
npm run dev
```

## 核心环境变量

| 变量 | 说明 |
|---|---|
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | MySQL 连接 |
| `AUTH_DB_URL`、`USER_DB_URL`、`LEARNING_DB_URL`、`SHOP_DB_URL`、`ADMIN_DB_URL`、`RAG_DB_URL` | 各服务独立数据库连接覆盖 |
| `JPA_DDL_AUTO` | JPA DDL 策略，默认 `validate` |
| `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DATABASE` | Redis 7（缓存、锁） |
| `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USERNAME`、`RABBITMQ_PASSWORD` | RabbitMQ |
| `ELASTICSEARCH_URL` | 商品搜索 Elasticsearch 地址 |
| `SHOP_SEARCH_ENABLED` | 是否启用商城 ES 搜索 |
| `ADMIN_TOKEN_SECRET` | 登录 token HMAC 密钥，auth、gateway、admin 必须一致 |
| `INTERNAL_GATEWAY_SECRET` | Gateway 与下游服务之间的内部访问密钥 |
| `ORDER_TIMEOUT_MINUTES` | 待支付订单超时时间，默认 10 分钟 |
| `RAG_VECTOR_STORE` | 向量库实现：`qdrant`（默认）或 `redis`（备用） |
| `RAG_VECTOR_QDRANT_HOST` / `RAG_VECTOR_QDRANT_HTTP_PORT` | Qdrant 连接 |
| `RAG_VECTOR_DIMENSION` | 向量维度，需与 embedding 模型一致（当前 bge-m3 为 1024） |
| `RAG_EMBEDDING_PROVIDER` | Embedding 提供方：`ollama`（默认）或 `openai` |
| `RAG_EMBEDDING_MODEL_NAME` | Embedding 模型名（默认 `bge-m3`） |
| `RAG_EMBEDDING_OLLAMA_BASE_URL` | Ollama embedding 地址（默认 `http://host.docker.internal:11434`） |
| `RAG_CHAT_PROVIDER` | Chat 提供方：`ollama`（默认）或 `openai` |
| `RAG_CHAT_MODEL_NAME` | Chat 模型名（默认 `qwen3.5:9b`） |
| `RAG_CHAT_OLLAMA_BASE_URL` | Ollama chat 地址（默认 `http://host.docker.internal:11434`） |
| `RAG_CHAT_API_KEY` / `RAG_CHAT_BASE_URL` | OpenAI 兼容端点（provider=openai 时生效） |
| `RAG_AGENT_ENABLED` | 是否启用 Agent 意图路由，默认 `true` |

生产环境必须显式设置 `ADMIN_TOKEN_SECRET`、`INTERNAL_GATEWAY_SECRET` 和所有模型 API Key，不要使用代码或 Compose 中的开发兜底值。

## 商城链路

### 商品搜索

`GET /api/shop/products/search?keyword=...`

搜索流程：

```text
前端关键词
  -> Gateway
  -> shop-service
  -> Redis 搜索缓存
  -> Elasticsearch 商品索引
  -> 失败或无结果时回退 MySQL 查询
  -> MySQL 商品详情 + Redis 库存同步
```

`shop-service` 启动时会重建 `shop_products` 索引，并在商品展示前同步 Redis 中的库存状态。

### 同步下单

```text
POST /api/shop/order-tokens
POST /api/shop/orders
POST /api/shop/orders/{orderId}/pay
GET  /api/shop/orders
```

下单前必须先申请一次性 token。业务身份来自 Gateway 注入的 `X-User-Id`，前端不再需要传可信 `userId`。

### 秒杀下单

```text
POST /api/shop/order-tokens
POST /api/shop/seckill-orders
GET  /api/shop/orders/result?requestId=...
```

秒杀链路：

```text
Gateway 限流
  -> 后端校验一次性 token
  -> Redis 预扣库存
  -> RabbitMQ 排队
  -> 消费者创建订单并扣减数据库库存
  -> 前端轮询结果
  -> TTL / 死信队列取消超时未支付订单
```

## RAG 知识库

`rag-service` 提供以下接口：

| 接口 | 说明 |
|---|---|
| `POST /api/rag/documents` | 文本资料入库 |
| `POST /api/rag/documents/upload` | PDF、DOCX、DOC 文件入库 |
| `GET /api/rag/documents` | 查询已入库资料 |
| `POST /api/rag/search` | 检索知识库片段 |
| `POST /api/rag/ask` | 普通知识库问答 |
| `POST /api/rag/ask/stream` | SSE 流式知识库问答 |

### 架构

```
用户问题 → rag-service
                │
                ├─ RagIntentRouter（LLM 意图分类）
                │   ├─ LEARNING_PLAN → LearningAgent
                │   ├─ WRONG_RECORD  → WrongRecordAgent
                │   ├─ USER_ACTION   → SafetyAgent
                │   └─ KNOWLEDGE_QA / GENERAL_CHAT → KnowledgeAgent
                │
                └─ KnowledgeAgent（Function Calling）
                    └─ knowledgeBaseTool(query)
                        ├─ Ollama bge-m3 embed(query) → float[1024]
                        ├─ Qdrant COSINE 检索 → Top-K chunks
                        └─ Ollama qwen3.5:9b 生成回答 + references
```

### 数据落点

| 存储 | 内容 |
|------|------|
| MySQL `rag_documents` / `rag_document_chunks` | 文档元数据 + chunk 原文 |
| **Qdrant**（collection: `rag_chunks`） | chunk 向量（1024 维，HNSW + COSINE）+ payload |
| Redis | 仅用于其他模块的缓存、幂等、分布式锁 |

### 关键特性

- **Hybrid Search**：向量检索 + MySQL 关键词检索合并返回
- **Agent 模式**：意图路由 + Function Calling + 多 Agent 分工
- **Conversational Memory**：同一 session 内支持上下文复用（短问句自动承接上一轮）
- **Embedding/LLM 全本地化**：Ollama 部署，零外部 API 成本

### 向量库切换

通过环境变量 `RAG_VECTOR_STORE` 切换：

```yaml
RAG_VECTOR_STORE: qdrant    # Qdrant（默认，推荐）
RAG_VECTOR_STORE: redis    # Redis Stack（旧方案，需 Redis Stack 镜像）
```

切换到不同模型或维度后，需要删除 Qdrant collection 让它按新维度重建：

```powershell
Invoke-RestMethod -Method Delete http://localhost:6333/collections/rag_chunks
```

## 后台管理和审计

后台入口：`/admin`。

后台接口统一走 `/api/admin/**`，由 Gateway 校验登录态，再由 `admin-service` 校验角色权限。管理员敏感操作会：

- 要求前端传入 `confirmText: "CONFIRM"`。
- 使用 `@AdminAudit` 记录操作日志。
- 对权限变更记录前后权限集合。
- 禁止删除或破坏系统内置角色、系统内置模块。
- 用户删除当前实现为禁用用户，而不是物理删除。

审计接口：

```text
GET /api/admin/audit/operations
GET /api/admin/audit/permission-changes
```

## 测试数据清理

仓库提供测试数据清理脚本：

```text
docs/sql/cleanup-test-data.sql
```

该脚本会删除名称中包含 `TEST`、`STRESS`、`IDEMPOTENCY`、`REDISSON`、`SERVER_TOKEN` 等标记的测试用户、商品和订单。执行前请确认当前数据库环境，生产库禁止直接运行。

## 当前企业级完成度

已具备：

- 微服务拆分、统一 Gateway、统一认证、内部网关密钥。
- Flyway 数据库迁移和基础数据 seed。
- Redis、RabbitMQ、Elasticsearch、**Qdrant** 等中间件集成。
- **全本地化 RAG**：Qdrant 向量库 + Ollama (bge-m3 embedding + qwen3.5:9b chat) + LangChain4j Agent/Function Calling。
- 商城并发控制、幂等、防超卖、订单超时取消。
- 后台 RBAC、敏感操作确认、审计日志。
- RAG 文档入库、Hybrid Search、Agent 意图路由、Function Calling、流式问答。
- Docker Compose 本地编排。

仍建议补齐：

- 自动化测试：后端单元测试、集成测试、并发下单测试、前端组件测试。
- CI/CD：构建、测试、镜像扫描、部署流水线。
- 生产配置：独立 profile、密钥管理、关闭默认密钥、收紧 CORS。
- 可观测性：traceId、结构化日志、Prometheus 指标、告警、慢 SQL 和 MQ 堆积监控。
- 接口规范：统一异常处理、统一错误码、DTO 参数校验全覆盖。
- 文档治理：OpenAPI 聚合页、部署手册、故障排查手册、容量压测报告。

## 常见问题

### Swagger 打不开

确认访问的是 Gateway：

```text
http://localhost:8081/swagger-ui/index.html
```

并确认 `learning-service` 和 Gateway 都已启动。

### 业务服务返回“请通过网关访问”

这是下游服务的内部网关保护生效。请通过 `http://localhost:8081/api/...` 访问，不要直接访问业务服务端口。

### RAG 搜索无结果

检查：

- Qdrant 容器已启动：`Invoke-RestMethod http://localhost:6333/collections`
- Ollama 已启动且 `bge-m3` / `qwen3.5:9b` 已拉取：`ollama list`
- `RAG_VECTOR_DIMENSION` 与 embedding 模型输出维度一致（当前 bge-m3 为 1024）
- `host.docker.internal:11434` 可从 Docker 容器访问宿主机 Ollama
- 已通过 `/api/rag/documents` 或 `/api/rag/documents/upload` 完成资料入库
- rag-service 日志里没有 Ollama 连接失败或 Qdrant 连接失败

### Qdrant 查看数据

```powershell
# 所有 collection
Invoke-RestMethod http://localhost:6333/collections

# collection 详情（点数、维度）
Invoke-RestMethod http://localhost:6333/collections/rag_chunks

# 点查询（payload + 向量）
$body = '{"limit": 10, "with_payload": true, "with_vector": true}'
Invoke-RestMethod -Uri "http://localhost:6333/collections/rag_chunks/points/scroll" -Method Post -ContentType "application/json" -Body $body
```

### 商城搜索无结果

检查：

- Elasticsearch 已启动并通过 `http://localhost:9200` 健康检查。
- `shop-service` 启动日志中没有索引重建失败。
- `SHOP_SEARCH_ENABLED` 未被设置为 `false`。
- 关键词无 ES 命中时，会尝试回退 MySQL 模糊查询。


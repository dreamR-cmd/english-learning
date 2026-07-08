# English Learning（英语学习平台）

一个面向 CET-4 / CET-6、考研英语、托福、雅思、GRE 等考试的英语学习平台，提供单词、阅读、听力练习、考试倒计时、错题本、收藏夹、精选读物、学习商城和后台管理功能。

当前项目采用 **Vue 3 + Vite 前端** 与 **Spring Boot 3 微服务后端** 的前后端分离架构，同时保留了一个单体后端用于兼容或简化本地调试。

> 本文档主要描述 `backend-services` 下的微服务架构；`backend-services/backend` 是保留的单体后端。

## 功能特性

- **多考试模块**：CET-4、CET-6、托福、雅思、考研英语、GRE 等模块。
- **学习练习**：单词、阅读、听力练习，按模块归类。
- **每日单词**：根据用户每日目标生成每日单词任务，并记录熟练度。
- **复习机制**：单词多次标记“认识”后进入复习列表。
- **考试倒计时**：后端根据考试规则自动计算下一场考试时间。
- **用户体系**：注册 / 登录、资料修改、每日单词目标设置。
- **错题本与收藏夹**：记录错题，收藏阅读篇目和精选读物。
- **精选读物**：独立于阅读理解题库的分级读物模块。
- **学习商城**：商品列表、下单、模拟支付、订单查询。
- **订单超时取消**：使用 Redis + RabbitMQ 实现库存扣减和订单超时取消。
- **独立后台管理服务**：后台订单、模块、用户、角色、权限统一由 `admin-service` 承接。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3、Vue Router 4、Pinia、Axios、Vite 5 |
| 网关 | Spring Cloud Gateway、Spring Boot Actuator |
| 后端 | Spring Boot 3.2.5、Spring Web、Spring Data JPA、Bean Validation |
| 数据库 | MySQL 8 |
| 中间件 | Redis、RabbitMQ |
| 文档 | springdoc-openapi |
| 安全 | Argon2 密码哈希、自定义 HMAC 后台 Token、RBAC 权限 |
| 构建 | Maven（后端）、npm + Vite（前端） |
| JDK | Java 17 |

## 项目结构

```text
english-learning/
├── README.md
├── PROJECT_ANALYSIS.md
├── frontend/                         # Vue 3 + Vite 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── components/
│       ├── router/
│       ├── utils/
│       └── views/
└── backend-services/
    ├── gateway/                      # Spring Cloud Gateway，默认 8081
    ├── auth-service/                 # 登录注册与 Token 创建，默认 8087
    ├── user-service/                 # 用户资料、错题、收藏、单词进度，默认 8088
    ├── learning-service/             # 模块、单词/阅读/听力、精选读物，默认 8089
    ├── shop-service/                 # 商城、订单、库存、超时取消，默认 8090
    ├── admin-service/                # 后台管理聚合服务，默认 8091
    └── backend/                      # 保留的单体 Spring Boot 后端，默认 8081
```

## 后端服务划分

| 服务 | 默认端口 | 职责 |
|---|---:|---|
| gateway | 8081 | 统一 API 入口、CORS、路由转发、Swagger 入口转发 |
| auth-service | 8087 | 登录、注册、密码加密、后台 Token 创建 |
| user-service | 8088 | 用户资料、错题本、收藏夹、单词进度 |
| learning-service | 8089 | 考试模块、单词/阅读/听力练习、精选读物、Swagger UI 多文档入口 |
| shop-service | 8090 | 商品、订单、库存扣减、模拟支付、订单超时取消 |
| admin-service | 8091 | 后台订单、模块、用户、角色、权限管理与后台权限校验 |
| backend | 8081 | 单体兼容后端，包含主要业务模块 |

## Gateway 路由

`backend-services/gateway` 默认将请求转发到对应服务：

| 路径 | 目标服务 |
|---|---|
| `/api/auth/**` | auth-service |
| `/api/admin/**` | admin-service |
| `/api/user/**` | user-service |
| `/api/modules/**` | learning-service |
| `/api/practice/**` | learning-service |
| `/api/selected-readings/**` | learning-service |
| `/api/shop/**` | shop-service |
| `/swagger-ui/**`、`/swagger-ui.html` | learning-service |
| `/v3/api-docs/auth` | auth-service OpenAPI |
| `/v3/api-docs/admin` | admin-service OpenAPI |
| `/v3/api-docs/user` | user-service OpenAPI |
| `/v3/api-docs/learning` | learning-service OpenAPI |
| `/v3/api-docs/shop` | shop-service OpenAPI |

## 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8.x
- Redis 6+
- RabbitMQ 3.x

## 数据库与中间件

### MySQL

默认数据库名：

```sql
CREATE DATABASE english_learning DEFAULT CHARACTER SET utf8mb4;
```

默认连接配置使用环境变量兜底：

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/english_learning?...}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:123456}
```

各服务支持独立数据库变量：

```text
AUTH_DB_URL / AUTH_DB_USERNAME / AUTH_DB_PASSWORD
USER_DB_URL / USER_DB_USERNAME / USER_DB_PASSWORD
LEARNING_DB_URL / LEARNING_DB_USERNAME / LEARNING_DB_PASSWORD
SHOP_DB_URL / SHOP_DB_USERNAME / SHOP_DB_PASSWORD
ADMIN_DB_URL / ADMIN_DB_USERNAME / ADMIN_DB_PASSWORD
```

当前 JPA 使用：

```yaml
spring.jpa.hibernate.ddl-auto: update
spring.sql.init.mode: never
```

### Redis

商城库存缓存和原子扣减依赖 Redis。

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
REDIS_DATABASE
```

默认：`localhost:6379`。

### RabbitMQ

商城订单超时取消依赖 RabbitMQ。

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
```

默认：`localhost:5672`，`guest / guest`。

订单超时时间：

```text
ORDER_TIMEOUT_MINUTES，默认 10 分钟
```

### 后台 Token

后台 Token 由 `auth-service` 登录成功后返回，`admin-service` 使用相同密钥校验：

```text
ADMIN_TOKEN_SECRET
```

## 快速开始：微服务模式（推荐）

### 1. 准备基础设施

确保本地已启动：

- MySQL
- Redis
- RabbitMQ

创建数据库：

```sql
CREATE DATABASE english_learning DEFAULT CHARACTER SET utf8mb4;
```

### 2. 启动后端服务

分别启动网关和各微服务。

```bash
cd backend-services/auth-service
mvn spring-boot:run
```

```bash
cd backend-services/user-service
mvn spring-boot:run
```

```bash
cd backend-services/learning-service
mvn spring-boot:run
```

```bash
cd backend-services/shop-service
mvn spring-boot:run
```

```bash
cd backend-services/admin-service
mvn spring-boot:run
```

```bash
cd backend-services/gateway
mvn spring-boot:run
```

默认 API 入口：

```text
http://localhost:8081
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在：

```text
http://localhost:3000
```

前端默认将 `/api` 代理到：

```text
http://localhost:8080
```

如需连接当前微服务网关端口 `8081`，可以通过环境变量覆盖：

```bash
VITE_API_TARGET=http://localhost:8081 npm run dev
```

Windows PowerShell：

```powershell
$env:VITE_API_TARGET = 'http://localhost:8081'
npm run dev
```

## 快速开始：单体模式

如果只想启动一个后端服务，可以使用保留的单体后端：

```bash
cd backend-services/backend
mvn spring-boot:run
```

单体后端默认端口：

```text
http://localhost:8081
```

前端需要将代理目标指向单体后端：

```bash
cd frontend
VITE_API_TARGET=http://localhost:8081 npm run dev
```

## 构建

### 后端构建

```bash
mvn -q -DskipTests package -f backend-services/gateway/pom.xml
mvn -q -DskipTests package -f backend-services/auth-service/pom.xml
mvn -q -DskipTests package -f backend-services/user-service/pom.xml
mvn -q -DskipTests package -f backend-services/learning-service/pom.xml
mvn -q -DskipTests package -f backend-services/shop-service/pom.xml
mvn -q -DskipTests package -f backend-services/admin-service/pom.xml
```

单体后端：

```bash
mvn -q -DskipTests package -f backend-services/backend/pom.xml
```

### 前端构建

```bash
cd frontend
npm run build
```

构建产物输出到：

```text
frontend/dist
```

## Swagger / OpenAPI

启动 gateway、learning-service 和需要查看的业务服务后访问：

```text
http://localhost:8081/swagger-ui/index.html
```

Swagger UI 可切换：

- 认证服务：`/v3/api-docs/auth`
- 后台管理服务：`/v3/api-docs/admin`
- 用户服务：`/v3/api-docs/user`
- 学习服务：`/v3/api-docs/learning`
- 商城服务：`/v3/api-docs/shop`

登录接口位于“认证服务”：

```text
POST /api/auth/login
POST /api/auth/register
```

后台管理接口位于“后台管理服务”。

## API 概览

所有接口统一返回 `ApiResult` 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

### 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 登录 |
| POST | `/register` | 注册 |

### 模块 `/api/modules`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/` | 获取全部模块，含考试倒计时 |
| GET | `/{code}` | 按 code 获取模块 |

### 练习 `/api/practice`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/words/{moduleCode}` | 按模块获取单词 |
| GET | `/words/daily?userId=` | 获取每日单词 |
| GET | `/readings/{moduleCode}` | 按模块获取阅读 |
| GET | `/listenings/{moduleCode}` | 按模块获取听力 |

### 用户 `/api/user`

| 方法 | 路径 | 说明 |
|---|---|---|
| PUT | `/profile` | 修改资料和每日单词目标 |
| POST | `/wrong-records` | 提交错题记录 |
| GET | `/wrong-records?userId=` | 获取错题列表 |
| DELETE | `/wrong-records/{wrongRecordId}?userId=` | 删除错题 |
| POST | `/favorites` | 添加阅读收藏 |
| DELETE | `/favorites/{readingId}?userId=` | 取消阅读收藏 |
| GET | `/favorites?userId=` | 获取收藏列表 |
| GET | `/favorites/check?userId=&readingId=` | 查询是否已收藏 |
| POST | `/word-progress/known` | 标记单词认识 |
| POST | `/word-progress/reset` | 重置单词进度 |
| GET | `/word-progress/review?userId=` | 获取复习单词 |

### 精选读物 `/api/selected-readings`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/` | 获取精选读物 |
| POST | `/favorites` | 收藏精选读物 |
| DELETE | `/favorites/{selectedReadingId}?userId=` | 取消收藏 |
| GET | `/favorites?userId=` | 获取精选读物收藏 |

### 商城 `/api/shop`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/products` | 获取商品列表 |
| POST | `/orders` | 创建订单 |
| GET | `/orders?userId=&status=` | 查询用户订单 |
| POST | `/orders/{orderId}/pay` | 模拟支付订单 |

### 后台管理 `/api/admin`

> 后台接口由 `admin-service` 提供，需要 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/orders` | 查询全部订单 |
| PUT | `/orders/{orderId}/status` | 更新订单状态 |
| GET | `/modules` | 查询全部模块 |
| POST | `/modules` | 新建模块 |
| PUT | `/modules/{moduleId}` | 更新模块 |
| DELETE | `/modules/{moduleId}` | 删除模块 |
| GET | `/users` | 查询用户 |
| PUT | `/users/{userId}/role` | 分配用户角色 |
| DELETE | `/users/{userId}` | 删除用户 |
| GET | `/roles` | 查询角色 |
| POST | `/roles` | 创建角色 |
| PUT | `/roles/{roleId}` | 更新角色 |
| DELETE | `/roles/{roleId}` | 删除角色 |
| GET | `/permissions` | 查询权限 |
| GET | `/roles/{roleId}/permissions` | 查询角色权限 |
| PUT | `/roles/{roleId}/permissions` | 分配角色权限 |

## 后台权限说明

当前后台采用 RBAC 权限模型：

- `ADMIN_DASHBOARD`：后台入口权限
- `ORDER_MANAGE`：订单管理
- `MODULE_MANAGE`：模块管理
- `USER_MANAGE`：用户管理
- `ROLE_MANAGE`：角色管理
- `PERMISSION_MANAGE`：权限管理

## 安全与配置建议

- 不要在生产环境使用默认数据库密码、RabbitMQ 密码或后台 Token 密钥。
- 生产环境应通过环境变量设置：
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `ADMIN_TOKEN_SECRET`
  - `REDIS_*`
  - `RABBITMQ_*`
- 如果使用 AI 相关配置，API Key 必须通过环境变量注入，不应提交到仓库。
- 普通用户接口当前仍大量使用请求中的 `userId`，后续建议统一从 Token 中解析当前用户身份。

## 项目分析文档

更详细的架构分析、风险点和优化建议见：

```text
PROJECT_ANALYSIS.md
```

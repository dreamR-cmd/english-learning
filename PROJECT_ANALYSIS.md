# English Learning 项目分析文档

> 分析时间：2026-07-09  
> 项目路径：`F:\idea_project\english_learning\english-learning`

## 1. 项目概述

`english-learning` 是一个面向 CET-4、CET-6、考研英语、托福、雅思、GRE 等考试场景的英语学习平台。

当前项目包含以下核心能力：

- 用户注册 / 登录
- 考试模块导航
- 单词练习、每日单词任务与复习机制
- 阅读练习与听力练习
- 精选读物与精选读物收藏
- 错题本与阅读收藏夹
- 学习商城、订单、模拟支付与超时取消
- 后台管理：订单、模块、用户、角色、权限管理

当前代码已经从单体后端演进为：

```text
Vue 3 前端 + Spring Cloud Gateway + 多个 Spring Boot 微服务 + 保留单体后端
```

也就是说，项目目前处于 **微服务为主、单体兼容保留** 的状态。`backend-services/backend` 仍然存在，主要用于兼容或简化本地调试；主线架构位于 `backend-services` 下的各微服务。

---

## 2. 项目目录结构

当前主要目录如下：

```text
english-learning/
├── README.md
├── PROJECT_ANALYSIS.md
├── services.ps1                         # Windows 本地一键启动/停止服务脚本
├── frontend/                            # Vue 3 + Vite 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── components/
│       ├── router/
│       ├── utils/
│       └── views/
└── backend-services/
    ├── gateway/                         # Spring Cloud Gateway，默认 8081
    ├── auth-service/                    # 登录、注册、Token 创建，默认 8087
    ├── user-service/                    # 用户资料、错题、收藏、单词进度，默认 8088
    ├── learning-service/                # 模块、单词/阅读/听力、精选读物，默认 8089
    ├── shop-service/                    # 商城、订单、库存、超时取消，默认 8090
    ├── admin-service/                   # 后台管理聚合服务，默认 8091
    └── backend/                         # 保留的单体 Spring Boot 后端，默认 8081
```

其中：

- `gateway` 负责统一入口、CORS、路由转发、登录态认证、用户上下文透传和 Swagger 文档转发。
- `auth-service` 负责登录注册、密码校验、Token 生成。
- `user-service` 负责用户中心相关业务。
- `learning-service` 负责学习内容和精选读物。
- `shop-service` 负责商城订单与库存。
- `admin-service` 是后台管理聚合服务，承担后台 RBAC 鉴权和后台 CRUD。
- `backend-services/backend` 是保留的单体后端，包含主要业务模块，与微服务代码有较多重复。

---

## 3. 前端分析

### 3.1 技术栈

前端位于 `frontend/`，使用：

- Vue 3
- Vue Router 4
- Pinia
- Axios
- Vite 5

依赖配置位于：

```text
frontend/package.json
```

核心脚本：

```json
{
  "dev": "vite",
  "build": "vite build",
  "preview": "vite preview"
}
```

### 3.2 启动与代理

前端 Vite 配置位于：

```text
frontend/vite.config.js
```

当前开发服务端口：

```text
http://localhost:3000
```

前端 API 默认代理到：

```text
http://localhost:8081
```

也就是默认请求会先进入 Spring Cloud Gateway。

如果要覆盖代理目标，可以通过环境变量：

```bash
VITE_API_TARGET=http://localhost:8081 npm run dev
```

Windows PowerShell：

```powershell
$env:VITE_API_TARGET = 'http://localhost:8081'
npm run dev
```

### 3.3 前端页面

主要路由包括：

```text
/login                         登录 / 注册
/modules                       考试模块首页
/module/:code                  模块详情
/practice/words/:moduleCode    单词练习
/practice/readings/:moduleCode 阅读练习
/practice/listenings/:moduleCode 听力练习
/profile                       个人中心
/settings                      设置
/shop                          商城
/orders                        我的订单
/selected-readings             精选读物
/wrong-records                 错题本
/review-words                  复习单词
/favorites                     收藏夹
/admin                         后台管理
```

路由配置位于：

```text
frontend/src/router/index.js
```

路由守卫逻辑：

- 未登录用户访问非 `/login` 页面时跳转到登录页。
- 非管理员访问 `/admin` 时跳转到 `/modules`。
- 已登录用户访问 `/login` 时，根据角色跳转到 `/admin` 或 `/modules`。

### 3.4 登录状态管理

登录状态由以下文件维护：

```text
frontend/src/utils/currentUser.js
```

特点：

- 使用 Vue `ref` 维护当前用户。
- 用户信息存储在 `sessionStorage.currentUser`。
- 前端设置 2 小时登录态过期时间。
- Axios 请求拦截器会自动添加：

```http
Authorization: Bearer <token>
```

Axios API 封装位于：

```text
frontend/src/utils/api.js
```

### 3.5 前端权限控制

前端有两层权限控制：

1. 路由层控制是否能进入 `/admin`。
2. 后台菜单根据登录返回的 `permissions` 显示。

但前端控制只能作为体验优化，真正的安全边界仍应由后端鉴权保证。

---

## 4. 后端架构分析

## 4.1 Gateway

目录：

```text
backend-services/gateway
```

技术栈：

- Spring Boot 3.2.5
- Spring Cloud Gateway
- Spring Boot Actuator

默认端口：

```text
8081
```

当前路由关系：

| 路径 | 目标服务 | 默认地址 |
|---|---|---|
| `/api/auth/**` | auth-service | `http://localhost:8087` |
| `/api/admin/**` | admin-service | `http://localhost:8091` |
| `/api/user/**` | user-service | `http://localhost:8088` |
| `/api/modules/**` | learning-service | `http://localhost:8089` |
| `/api/practice/**` | learning-service | `http://localhost:8089` |
| `/api/selected-readings/**` | learning-service | `http://localhost:8089` |
| `/api/shop/**` | shop-service | `http://localhost:8090` |
| `/v3/api-docs/auth` | auth-service OpenAPI | `http://localhost:8087/v3/api-docs` |
| `/v3/api-docs/user` | user-service OpenAPI | `http://localhost:8088/v3/api-docs` |
| `/v3/api-docs/admin` | admin-service OpenAPI | `http://localhost:8091/v3/api-docs` |
| `/v3/api-docs/learning` | learning-service OpenAPI | `http://localhost:8089/v3/api-docs` |
| `/v3/api-docs/shop` | shop-service OpenAPI | `http://localhost:8090/v3/api-docs` |
| `/swagger-ui/**`、`/swagger-ui.html`、`/v3/**` | learning-service | `http://localhost:8089` |

推荐访问链路：

```text
frontend:3000 -> gateway:8081 -> auth/user/learning/shop/admin services
```

当前 Gateway 已增加统一认证过滤器：除登录、注册、健康检查和 Swagger/OpenAPI 外，所有 `/api/**` 请求都需要携带 `Authorization: Bearer <token>`；校验通过后会向下游服务透传 `X-User-Id` 和 `X-Token-Expires-At`。

---

## 4.2 auth-service

目录：

```text
backend-services/auth-service
```

默认端口：

```text
8087
```

职责：

- 用户登录
- 用户注册
- 密码加密
- 兼容旧明文密码并自动升级到 Argon2
- 创建自定义 HMAC token
- 返回用户角色和权限信息

主要接口：

```text
POST /api/auth/login
POST /api/auth/register
```

认证特点：

- 新注册用户密码使用 Argon2 哈希。
- 登录时兼容历史明文密码。
- 如果发现旧密码是明文，登录成功后自动升级为 Argon2 哈希。
- 登录成功返回 `LoginUserInfo`，包含用户信息、角色信息、权限码列表和 token。
- token 是自定义 HMAC-SHA256 token，不是标准 JWT。

Token 格式：

```text
base64Url("userId:expiresAt") + "." + HMAC_SHA256签名
```

后端 token 有效期当前是 24 小时。

---

## 4.3 user-service

目录：

```text
backend-services/user-service
```

默认端口：

```text
8088
```

职责：

- 用户资料修改
- 每日单词目标设置
- 错题记录
- 阅读收藏
- 单词熟练度进度
- 复习单词列表

主要接口：

```text
PUT    /api/user/profile
POST   /api/user/wrong-records
GET    /api/user/wrong-records
DELETE /api/user/wrong-records/{wrongRecordId}

POST   /api/user/favorites
DELETE /api/user/favorites/{readingId}
GET    /api/user/favorites
GET    /api/user/favorites/check

POST   /api/user/word-progress/known
POST   /api/user/word-progress/reset
GET    /api/user/word-progress/review
```

业务特点：

- 每日单词目标限制在 `1 ~ 100` 之间。
- 单词认识次数 `knownCount` 最高为 4。
- 当 `knownCount >= 4` 时，单词进入复习状态，即 `reviewReady = true`。
- 复习单词列表读取 `reviewReady = true` 的单词进度。

当前风险：

- 多数接口依赖前端传入 `userId`。
- 当前没有看到用户服务统一校验 token 中的用户 ID 是否等于请求中的 `userId`。
- 这会导致普通用户侧接口存在越权风险。

---

## 4.4 learning-service

目录：

```text
backend-services/learning-service
```

默认端口：

```text
8089
```

职责：

- 考试模块
- 考试倒计时
- 单词练习
- 每日单词
- 阅读练习
- 听力练习
- 精选读物
- 精选读物收藏
- Swagger UI 多服务文档入口

主要接口：

```text
GET /api/modules
GET /api/modules/{code}

GET /api/practice/words/{moduleCode}
GET /api/practice/words/daily
GET /api/practice/readings/{moduleCode}
GET /api/practice/listenings/{moduleCode}

GET    /api/selected-readings
POST   /api/selected-readings/favorites
DELETE /api/selected-readings/favorites/{selectedReadingId}
GET    /api/selected-readings/favorites
```

每日单词逻辑：

- 每天为用户生成一批 `UserDailyWordAssignment`。
- 如果当天没有任务，则随机从可用单词中抽取。
- 已经进入复习状态的单词不会进入每日新词任务。
- 如果用户修改每日目标，系统会自动增减当天任务。

阅读题库初始化：

- 每个考试模块自动生成 20 篇阅读文章。
- 每篇文章 5 道题。
- 每个模块合计约 100 道阅读理解题。
- 初始化过程是幂等的，不会重复插入相同标题。
- 使用原创模板文本，避免直接使用真题导致版权问题。

当前风险：

- 精选读物收藏接口同样依赖前端传入 `userId`。
- 服务中存在 `AdminAuthFilter` 类，但没有注册为 Spring Bean；结合当前 Gateway 路由看，更像是拆分过程中的预留或残留代码。

---

## 4.5 shop-service

目录：

```text
backend-services/shop-service
```

默认端口：

```text
8090
```

职责：

- 商品列表
- 创建订单
- 查询订单
- 支付订单
- Redis 库存扣减
- MySQL 库存扣减
- RabbitMQ 订单超时取消

主要接口：

```text
GET  /api/shop/products
POST /api/shop/orders
GET  /api/shop/orders
POST /api/shop/orders/{orderId}/pay
```

技术点：

- MySQL 存储商品与订单。
- Redis 缓存商品库存并做原子扣减。
- RabbitMQ 使用 TTL + 死信队列实现订单延迟超时取消。

下单流程：

1. 校验用户和商品。
2. Redis 初始化或读取库存缓存。
3. Redis 原子扣减库存。
4. MySQL 条件扣减库存。
5. 创建 `pending` 订单。
6. 发送 RabbitMQ 延迟消息。
7. 到期后检查订单是否仍为 `pending`。
8. 未支付则取消订单并回补库存。

RabbitMQ 延迟方案：

```text
普通队列 TTL + 死信队列
```

优点：

- 不依赖 `rabbitmq_delayed_message_exchange` 插件。
- 普通 RabbitMQ 即可运行。

默认订单超时时间：

```text
10 分钟
```

当前风险：

- 创建订单、查询订单、支付订单依赖请求中的 `userId`。
- RabbitMQ 延迟消息发送失败时仅记录 warn，订单仍会创建；后续需要补偿扫描机制，避免订单长期 pending。
- 服务中存在未注册的 `AdminAuthFilter`，更像拆分残留。

---

## 4.6 admin-service

目录：

```text
backend-services/admin-service
```

默认端口：

```text
8091
```

职责：

- 后台订单管理
- 后台模块管理
- 后台用户管理
- 后台角色管理
- 后台权限管理
- 后台接口 token 校验与 RBAC 权限校验

主要接口：

```text
GET    /api/admin/orders
PUT    /api/admin/orders/{orderId}/status

GET    /api/admin/modules
POST   /api/admin/modules
PUT    /api/admin/modules/{moduleId}
DELETE /api/admin/modules/{moduleId}

GET    /api/admin/users
PUT    /api/admin/users/{userId}/role
DELETE /api/admin/users/{userId}

GET    /api/admin/roles
POST   /api/admin/roles
PUT    /api/admin/roles/{roleId}
DELETE /api/admin/roles/{roleId}

GET    /api/admin/permissions
GET    /api/admin/roles/{roleId}/permissions
PUT    /api/admin/roles/{roleId}/permissions
```

后台鉴权逻辑：

- `AdminAuthFilter` 拦截 `/api/admin/**`。
- 从 `Authorization: Bearer <token>` 中读取 token。
- 使用与 `auth-service` 相同的 `ADMIN_TOKEN_SECRET` 校验 HMAC 签名和过期时间。
- 根据请求路径映射权限码。
- 要求用户角色 code 为 `ADMIN`，并且角色拥有对应权限码。

当前权限映射：

```text
/api/admin/orders       -> ORDER_MANAGE
/api/admin/modules      -> MODULE_MANAGE
/api/admin/users        -> USER_MANAGE
/api/admin/roles        -> ROLE_MANAGE
/api/admin/permissions  -> PERMISSION_MANAGE
其他 /api/admin/**      -> ADMIN_DASHBOARD
```

架构特点：

- `admin-service` 是后台聚合服务。
- 它直接依赖订单、模块、用户、角色、权限等多张表对应的 Mapper。
- 这说明当前拆分仍然是共享数据库模式，不是严格的“每个服务独占数据库”。

---

## 5. 认证与权限链路

### 5.1 登录认证链路

```text
frontend Login.vue
  -> POST /api/auth/login
  -> gateway /api/auth/**
  -> auth-service
  -> 校验用户名和密码
  -> 查询角色和权限
  -> 生成 HMAC token
  -> 返回 LoginUserInfo
  -> 前端存 sessionStorage
```

### 5.2 普通请求链路

```text
frontend axios
  -> request interceptor 添加 Authorization: Bearer <token>
  -> gateway
  -> user/learning/shop service
```

当前 Gateway 已统一校验 `/api/**` 登录态，并向下游透传 `X-User-Id`；但多数业务服务还没有基于该 header 做资源级授权，历史接口仍主要使用请求中的 `userId`。

### 5.3 后台请求链路

```text
frontend /admin
  -> axios 添加 Authorization: Bearer <token>
  -> gateway /api/admin/**
  -> admin-service
  -> AdminAuthFilter 校验 token
  -> AdminAuthFilter 校验权限码
  -> AdminController 执行业务
```

---

## 6. 数据库与中间件

### 6.1 MySQL

所有服务默认连接同一个数据库：

```text
english_learning
```

默认连接：

```yaml
jdbc:mysql://localhost:3306/english_learning
username: root
password: 123456
```

支持通过环境变量覆盖：

```text
DB_URL
DB_USERNAME
DB_PASSWORD

AUTH_DB_URL / AUTH_DB_USERNAME / AUTH_DB_PASSWORD
USER_DB_URL / USER_DB_USERNAME / USER_DB_PASSWORD
LEARNING_DB_URL / LEARNING_DB_USERNAME / LEARNING_DB_PASSWORD
SHOP_DB_URL / SHOP_DB_USERNAME / SHOP_DB_PASSWORD
ADMIN_DB_URL / ADMIN_DB_USERNAME / ADMIN_DB_PASSWORD
```

JPA 配置：

```yaml
spring.jpa.hibernate.ddl-auto: update
spring.sql.init.mode: never
```

说明：当前表结构主要依赖 JPA 自动更新。

当前架构更准确地说是：

```text
多 Spring Boot 服务 + 共享 MySQL 数据库
```

而不是严格的服务独占数据库模式。

### 6.2 Redis

主要用于商城库存缓存和扣减。

默认配置：

```text
localhost:6379
```

支持环境变量：

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
REDIS_DATABASE
```

### 6.3 RabbitMQ

主要用于订单超时取消。

默认配置：

```text
localhost:5672
guest / guest
```

支持环境变量：

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
```

### 6.4 后台 Token 密钥

`auth-service` 创建 token，`admin-service` 校验 token，所以必须保持同一个密钥：

```text
ADMIN_TOKEN_SECRET
```

当前多个服务里都有默认值：

```text
english-learning-admin-secret
```

生产环境应强制覆盖。

---

## 7. 推荐运行方式

## 7.1 一键启动微服务模式

项目根目录提供：

```text
services.ps1
```

启动：

```powershell
.\services.ps1 start
```

停止：

```powershell
.\services.ps1 stop
```

重启：

```powershell
.\services.ps1 restart
```

查看状态：

```powershell
.\services.ps1 status
```

脚本默认启动：

```text
auth-service
user-service
learning-service
shop-service
admin-service
gateway
frontend
```

注意：该脚本会打开多个 PowerShell 窗口，适合 Windows 本地开发演示，不适合生产部署。

## 7.2 手动启动微服务模式

环境要求：

- JDK 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8+
- Redis
- RabbitMQ

分别启动：

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

启动前端：

```bash
cd frontend
npm install
npm run dev
```

访问：

```text
http://localhost:3000
```

API 入口：

```text
http://localhost:8081
```

## 7.3 单体模式

也可以只启动保留的单体后端：

```bash
cd backend-services/backend
mvn spring-boot:run
```

单体后端默认端口：

```text
8081
```

前端默认也代理到 `8081`，因此本地单体模式下通常不需要额外改代理目标。

---

## 8. Swagger / OpenAPI

启动 gateway、learning-service 和需要查看的业务服务后访问：

```text
http://localhost:8081/swagger-ui/index.html
```

Swagger UI 可切换：

```text
认证服务：/v3/api-docs/auth
后台管理服务：/v3/api-docs/admin
用户服务：/v3/api-docs/user
学习服务：/v3/api-docs/learning
商城服务：/v3/api-docs/shop
```

当前由 Gateway 转发各服务 OpenAPI 文档，Swagger UI 配置主要放在 `learning-service`。

---

## 9. 项目优点

### 9.1 功能较完整

项目不仅有基础学习功能，还包含：

- 商城
- 订单
- 后台管理
- RBAC 权限
- 精选读物
- 错题本
- 收藏夹
- 单词复习机制

适合作为课程设计、毕业设计或学习型平台项目。

### 9.2 有微服务拆分雏形

当前拆分为：

```text
gateway
auth-service
user-service
learning-service
shop-service
admin-service
```

服务职责边界比较直观。

### 9.3 商城模块具有工程亮点

商城订单逻辑包含：

- Redis 原子扣库存
- MySQL 条件扣库存
- RabbitMQ TTL + 死信队列
- 订单超时取消
- 幂等取消逻辑
- 支付时再次校验过期时间

这比普通 CRUD 项目更有技术深度。

### 9.4 密码迁移设计较实用

登录时兼容历史明文密码，并在登录成功后自动升级为 Argon2 哈希，有实际工程意义。

### 9.5 后台权限具备 RBAC 雏形

项目已有：

- 角色表
- 权限表
- 角色权限关联表
- 前端菜单按权限展示
- 后端过滤器按权限校验

比简单的 `ADMIN` 判断更完整。

### 9.6 文档和运行脚本已有基础

项目已有：

- README
- PROJECT_ANALYSIS
- services.ps1
- Swagger / OpenAPI 聚合入口

后续完善成本相对较低。

---

## 10. 当前问题与风险

### 10.1 单体与微服务代码重复较多

大量实体、DTO、Mapper 在单体和多个服务中重复存在，例如：

```text
ApiResult
User
LoginRequest
LoginUserInfo
Word
Reading
WrongRecord
ShopOrder
ShopProduct
AdminPermission
AdminRolePermission
```

风险：

- 字段变更需要修改多处。
- 服务之间模型容易不一致。
- 维护成本较高。
- 单体后端和微服务版本容易分叉。

### 10.2 微服务共享同一个数据库

虽然代码拆成多个服务，但配置上都默认连接同一个 `english_learning` 数据库。

这更接近：

```text
模块化单体拆成多个进程
```

而不是严格意义上的微服务。

这不一定错误，但需要在文档中明确这是当前阶段的设计选择。

### 10.3 用户接口鉴权不足

当前后台接口通过 token 保护，但很多普通用户接口依赖前端传入：

```text
userId
```

如果后端不从 token 中校验真实用户身份，理论上用户可以伪造其他人的 `userId` 调用接口。

涉及范围：

- `user-service`：资料、错题、收藏、单词进度。
- `learning-service`：每日单词、精选读物收藏。
- `shop-service`：订单创建、订单查询、订单支付。

当前已完成第一步：Gateway 统一校验 `/api/**` 登录态，并向下游服务透传 `X-User-Id`。建议后续继续：

- 后端业务接口从 `X-User-Id` 解析当前用户 ID。
- 对保留 `userId` 参数的旧接口，校验其必须等于 `X-User-Id`。
- 逐步不再信任请求体或 query 参数中的 `userId`。

### 10.4 Gateway 统一认证已落地

当前 Gateway 已承担统一登录态认证：

- 登录、注册、健康检查和 Swagger/OpenAPI 放行。
- 其余 `/api/**` 请求必须携带 `Authorization: Bearer <token>`。
- Gateway 本地校验 HMAC token 签名和过期时间。
- Gateway 转发前移除客户端传入的身份 header，并重新写入 `X-User-Id` 和 `X-Token-Expires-At`。
- 后台 RBAC 仍由 `admin-service` 的 `AdminAuthFilter` 负责。

### 10.5 token 过期策略不一致

当前：

```text
后端 token：24 小时
前端 sessionStorage 登录态：2 小时
```

这会导致策略不一致：前端 2 小时后退出，但后端 token 实际仍可用。

建议：

- 后端返回 `expiresAt`。
- 前端按后端过期时间判断。
- 或设计 accessToken + refreshToken。

### 10.6 敏感配置存在泄露风险

发现以下风险配置：

- 数据库默认密码：`123456`
- 后台 token 默认密钥：`english-learning-admin-secret`
- RabbitMQ 默认密码：`guest`
- 单体后端 `application.yml` 中存在默认 OpenAI API Key

尤其需要注意：

```yaml
OPENAI_API_KEY: sk-...
```

建议：

1. 立即移除代码中的默认 API Key。
2. 作废已经提交过的 key。
3. 生产环境密钥必须通过环境变量传入。
4. 增加 `.env.example`，不要提交真实 `.env`。

### 10.7 admin-service 强依赖共享数据库

`admin-service` 直接操作订单、模块、用户、角色、权限等表。

优点：

- 后台管理开发简单。
- 查询和聚合方便。

缺点：

- 绕过了 shop-service、learning-service、user-service 的领域边界。
- 后续如果业务服务有额外规则，admin-service 直接改表可能漏掉。
- 不符合严格微服务自治。

短期可接受，但需要明确它是后台聚合服务。

### 10.8 拆分残留代码需要清理

`learning-service` 和 `shop-service` 中存在类似 `AdminAuthFilter` 的代码，但当前没有注册为 Spring Bean，Gateway 也没有把 `/api/admin/learning/**` 或 `/api/admin/shop/**` 路由到它们。

这类代码容易让后续维护者误解权限链路。

建议：

- 如果后台全部由 `admin-service` 承接，删除这些残留 filter。
- 如果后台按业务服务拆分，则补齐 Gateway 路由和 filter 注册。

### 10.9 缺少 Docker Compose

项目依赖：

- MySQL
- Redis
- RabbitMQ
- Gateway
- 多个微服务
- 前端

但当前没有 `docker-compose.yml`，新环境搭建成本较高。

建议至少提供中间件版 compose：

- mysql
- redis
- rabbitmq-management

### 10.10 测试较少

当前基本没有覆盖核心业务的自动化测试。

建议重点补充：

- 登录 / 注册 / 密码升级测试
- 后台权限过滤测试
- 商城下单 / 支付 / 超时取消测试
- 单词进度测试
- 每日单词分配测试

---

## 11. 优化建议优先级

### P0：安全与配置

1. 移除硬编码 API Key。
2. 作废已经泄露或提交过的密钥。
3. 生产环境强制配置 `ADMIN_TOKEN_SECRET`。
4. 用户接口不要信任前端传入的 `userId`。
5. 普通用户接口基于 Gateway 透传的 `X-User-Id` 做资源级授权。
6. 对保留 `userId` 参数的旧接口增加 `userId == X-User-Id` 校验。
7. 统一前后端 token 过期策略。

### P1：文档与运行体验

1. 持续保持 README 与实际微服务端口、路由一致。
2. 写清楚微服务模式和单体模式两种运行方式。
3. 增加 `.env.example`。
4. 增加 Docker Compose，至少覆盖 MySQL、Redis、RabbitMQ。
5. 明确 `services.ps1` 是 Windows 本地开发脚本，不是部署方案。

### P2：架构整理

1. 明确是否继续保留 `backend-services/backend` 单体版。
2. 如果以后以微服务为主，可以冻结或删除单体版。
3. 抽取公共模块，降低实体、DTO、工具类重复。
4. 清理 learning-service 和 shop-service 中未生效的后台过滤器。
5. 明确 admin-service 是共享数据库后台聚合，还是改为调用各业务服务 API。

### P3：认证授权规范化

1. 将 token 解析和校验抽成公共能力。
2. 普通用户接口从 token 获取当前用户 ID。
3. 后台权限映射从硬编码逐步迁移到配置或数据库。
4. 考虑使用标准 JWT 或 Spring Security Resource Server。

### P4：测试补齐

1. 先补核心 Service 单元测试。
2. 再补 Controller 集成测试。
3. 商城库存扣减和超时取消逻辑应优先测试。
4. 补后台权限过滤器测试。

### P5：工程质量

1. 增加统一异常处理 `@ControllerAdvice`。
2. 增加统一错误码。
3. 增加日志 traceId。
4. 完善 OpenAPI 文档。
5. 增加 CI 构建检查。

---

## 12. 建议的后续演进路线

### 阶段一：安全补强

```text
移除敏感默认值
统一 token 过期
普通用户接口接入 token 鉴权
```

这是最优先的阶段，因为它直接影响数据安全。

### 阶段二：运行体验完善

```text
.env.example
Docker Compose
README 运行说明
服务健康检查
```

让新环境能快速启动项目。

### 阶段三：微服务边界整理

```text
清理残留代码
抽 common 模块
明确 admin-service 边界
冻结或移除单体 backend
```

降低维护成本。

### 阶段四：质量保障

```text
单元测试
集成测试
CI
统一异常与错误码
```

提升项目稳定性。

---

## 13. 总结

当前项目已经超出简单 CRUD 范畴，具备比较完整的业务功能和一定工程复杂度。

核心亮点：

- 前后端分离
- Spring Cloud Gateway 网关
- 多服务拆分
- 独立 admin-service 后台聚合服务
- RBAC 后台权限
- 单词、阅读、听力学习业务
- 精选读物、错题本、收藏夹
- 商城订单系统
- Redis 库存扣减
- RabbitMQ 延迟取消订单
- Swagger / OpenAPI 聚合文档

主要短板：

- 单体和微服务并存，重复代码较多
- 多服务共享数据库，服务边界还不严格
- 普通用户接口仍需继续收紧资源级授权
- Gateway 已增加统一认证，但业务服务还需逐步使用 `X-User-Id`
- token 过期策略前后端不一致
- 存在敏感配置泄露风险
- 缺少 Docker Compose 和自动化测试

整体判断：

```text
这是一个功能完整、工程元素较丰富的学习平台项目；
当前微服务拆分已经完成第一阶段，但还需要继续补安全、清理边界、完善运行环境和测试体系。
```

建议后续优先处理安全配置和普通用户鉴权问题，然后完善文档与运行环境，最后再进行架构整理和测试补齐。

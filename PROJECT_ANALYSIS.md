# English Learning 项目分析文档

> 分析时间：2026-07-08  
> 项目路径：`F:\idea_project\english_learning\english-learning`

## 1. 项目概述

`english-learning` 是一个面向 CET-4、CET-6、考研英语、托福、雅思、GRE 等考试场景的英语学习平台。

当前项目包含以下核心能力：

- 用户注册 / 登录
- 考试模块导航
- 单词练习与每日单词任务
- 阅读练习与听力练习
- 精选读物
- 错题本
- 收藏夹
- 学习商城与订单
- 后台管理，包括模块、用户、角色、权限、订单管理

README 中描述的是较早期的单体结构，但当前代码已经演进为：

```text
前端 Vue 应用 + Spring Cloud Gateway + 多个 Spring Boot 微服务 + 保留单体后端
```

也就是说，项目目前处于 **单体与微服务并存的过渡状态**。

---

## 2. 项目目录结构

当前主要目录如下：

```text
english-learning/
├── README.md
├── .gitignore
├── PROJECT_ANALYSIS.md
├── frontend/                     # Vue 3 + Vite 前端
└── backend-services/
    ├── gateway/                  # Spring Cloud Gateway，统一 API 入口
    ├── auth-service/             # 登录注册、后台权限、角色权限
    ├── user-service/             # 用户资料、错题、收藏、单词进度
    ├── learning-service/         # 考试模块、单词/阅读/听力、精选读物
    ├── shop-service/             # 商城、订单、库存、超时取消
    └── backend/                  # 旧的/兼容的单体 Spring Boot 应用
```

其中：

- `backend-services/backend` 是完整单体版后端，包含 auth、user、learning、shop 等所有业务。
- `auth-service`、`user-service`、`learning-service`、`shop-service` 是拆分后的微服务版本。
- `gateway` 负责将前端请求转发到对应服务。

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
http://localhost:8080
```

即默认请求会先进入 Spring Cloud Gateway。

如果要代理到其他后端地址，可以通过环境变量覆盖：

```bash
VITE_API_TARGET=http://localhost:8081 npm run dev
```

### 3.3 前端页面

主要路由包括：

```text
/login                    登录 / 注册
/modules                  考试模块首页
/module/:code             模块详情
/practice/words/:code     单词练习
/practice/readings/:code  阅读练习
/practice/listenings/:code 听力练习
/profile                  个人中心
/settings                 设置
/shop                     商城
/orders                   我的订单
/selected-readings        精选读物
/wrong-records            错题本
/review-words             复习单词
/favorites                收藏夹
/admin                    后台管理
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
- 前端设置 2 小时过期时间。
- Axios 请求拦截器会自动添加：

```http
Authorization: Bearer <token>
```

Axios API 封装位于：

```text
frontend/src/utils/api.js
```

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
8080
```

路由关系：

| 路径 | 目标服务 | 默认地址 |
|---|---|---|
| `/api/auth/**` | auth-service | `http://localhost:8082` |
| `/api/admin/**` | auth-service | `http://localhost:8082` |
| `/api/user/**` | user-service | `http://localhost:8083` |
| `/api/modules/**` | learning-service | `http://localhost:8084` |
| `/api/practice/**` | learning-service | `http://localhost:8084` |
| `/api/selected-readings/**` | learning-service | `http://localhost:8084` |
| `/api/shop/**` | shop-service | `http://localhost:8085` |
| `/swagger-ui/**`、`/v3/**` | learning-service | `http://localhost:8084` |

推荐访问链路：

```text
frontend:3000 -> gateway:8080 -> auth/user/learning/shop services
```

---

## 4.2 auth-service

目录：

```text
backend-services/auth-service
```

职责：

- 用户登录
- 用户注册
- 密码加密与旧密码升级
- 后台 token 创建与校验
- 后台权限过滤
- 后台订单、模块、用户、角色、权限管理接口

主要接口：

```text
POST /api/auth/login
POST /api/auth/register

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

认证特点：

- 新注册用户密码使用 `PasswordEncoder` 加密。
- 登录时兼容历史明文密码。
- 如果发现旧密码是明文，登录成功后自动升级为 Argon2 哈希。
- 后台 token 是自定义 HMAC-SHA256 token，不是标准 JWT。
- 后台权限通过 `AdminAuthFilter` 过滤 `/api/admin/` 请求。

---

## 4.3 user-service

目录：

```text
backend-services/user-service
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

---

## 4.4 learning-service

目录：

```text
backend-services/learning-service
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

导航模块初始化：

- 自动确保存在“学习商城”。
- 自动确保存在“精选读物”。

---

## 4.5 shop-service

目录：

```text
backend-services/shop-service
```

职责：

- 商品列表
- 创建订单
- 查询订单
- 支付订单
- 库存扣减
- 订单超时取消

主要接口：

```text
GET  /api/shop/products
POST /api/shop/orders
GET  /api/shop/orders
POST /api/shop/orders/{orderId}/pay
```

技术点：

- MySQL 存储商品与订单。
- Redis 缓存和原子扣减库存。
- RabbitMQ 实现订单延迟超时取消。

下单流程：

1. 校验用户和商品。
2. Redis 原子扣减库存。
3. MySQL 条件扣减库存。
4. 创建 `pending` 订单。
5. 发送 RabbitMQ 延迟消息。
6. 到期后自动检查并取消未支付订单。
7. 取消订单时回补库存。

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

---

## 5. 数据库与中间件

### 5.1 MySQL

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
```

JPA 配置：

```yaml
spring.jpa.hibernate.ddl-auto: update
spring.sql.init.mode: never
```

说明：当前表结构主要依赖 JPA 自动更新。

### 5.2 Redis

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

### 5.3 RabbitMQ

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

---

## 6. 推荐运行方式

## 6.1 微服务模式

环境要求：

- JDK 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8+
- Redis
- RabbitMQ

启动网关：

```bash
cd backend-services/gateway
mvn spring-boot:run
```

启动认证服务：

```bash
cd backend-services/auth-service
mvn spring-boot:run
```

启动用户服务：

```bash
cd backend-services/user-service
mvn spring-boot:run
```

启动学习服务：

```bash
cd backend-services/learning-service
mvn spring-boot:run
```

启动商城服务：

```bash
cd backend-services/shop-service
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

## 6.2 单体模式

也可以只启动单体后端：

```bash
cd backend-services/backend
mvn spring-boot:run
```

单体后端默认端口：

```text
8081
```

此时前端需要改为代理到单体后端：

```bash
cd frontend
VITE_API_TARGET=http://localhost:8081 npm run dev
```

Windows PowerShell 可用：

```powershell
$env:VITE_API_TARGET = 'http://localhost:8081'
npm run dev
```

---

## 7. 项目优点

### 7.1 功能较完整

项目不仅有基础学习功能，还包含：

- 商城
- 订单
- 后台管理
- RBAC 权限
- 精选读物
- 错题本
- 单词复习机制

适合作为课程设计、毕业设计或学习型平台项目。

### 7.2 有微服务拆分雏形

当前拆分为：

```text
gateway
auth-service
user-service
learning-service
shop-service
```

服务职责边界相对清晰。

### 7.3 商城模块具有工程亮点

商城订单逻辑包含：

- Redis 原子扣库存
- MySQL 条件扣库存
- RabbitMQ TTL + 死信队列
- 订单超时取消
- 幂等取消逻辑

这比普通 CRUD 项目更有技术深度。

### 7.4 密码迁移设计较实用

登录时兼容历史明文密码，并在登录成功后自动升级为 Argon2 哈希，有实际工程意义。

### 7.5 后台权限具备 RBAC 雏形

项目已有：

- 角色表
- 权限表
- 角色权限关联表
- 前端菜单按权限展示
- 后端过滤器按权限校验

比简单的 `ADMIN` 判断更完整。

---

## 8. 当前问题与风险

### 8.1 README 与实际代码不一致

README 仍描述早期结构：

```text
backend/
frontend/
```

但当前实际主结构是：

```text
backend-services/
frontend/
```

README 中端口也已过时：

- README 写前端默认 5173，但当前 Vite 配置是 3000。
- README 写后端默认 8081，但当前微服务推荐入口是 gateway 8080。

建议尽快更新 README。

### 8.2 单体与微服务代码重复较多

大量实体、DTO、Mapper 在多个服务中重复存在，例如：

```text
auth-service/entity/User.java
user-service/entity/User.java
learning-service/entity/User.java
shop-service/entity/User.java
backend/entity/User.java
```

风险：

- 字段变更需要修改多处。
- 服务之间模型容易不一致。
- 维护成本较高。
- 业务边界不够清晰。

### 8.3 微服务共享同一个数据库

虽然代码拆成多个服务，但配置上都连接同一个 `english_learning` 数据库。

这更接近：

```text
模块化单体拆成多个进程
```

而不是严格意义上的微服务。

这不一定错误，但需要在文档中明确这是当前阶段的设计选择。

### 8.4 敏感配置存在泄露风险

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

### 8.5 用户接口鉴权不足

当前后台接口通过 token 保护，但很多普通用户接口依赖前端传入：

```text
userId
```

如果后端不从 token 中校验真实用户身份，理论上用户可以伪造其他人的 `userId` 调用接口。

建议后续：

- 普通用户接口也统一接入 token 鉴权。
- 后端从 token 中解析当前用户 ID。
- 不信任请求体或 query 参数中的 `userId`。

### 8.6 auth-service 承担了过多后台业务

当前 auth-service 不只负责认证授权，还包含：

- 后台订单管理
- 后台模块管理
- 后台用户管理
- 后台角色权限管理

短期可以接受，但从领域边界看不够清晰。

后续可以考虑：

- 保留 auth-service 只做认证授权。
- 新增 admin-service 聚合后台能力。
- 或由各业务服务分别提供自己的 admin API。

### 8.7 缺少 Docker Compose

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

### 8.8 测试较少

当前基本没有覆盖核心业务的自动化测试。

建议重点补充：

- 登录 / 注册 / 密码升级测试
- 后台权限过滤测试
- 商城下单 / 支付 / 超时取消测试
- 单词进度测试
- 每日单词分配测试

---

## 9. 优化建议优先级

### P0：安全与配置

1. 移除硬编码 API Key。
2. 作废已经泄露或提交过的密钥。
3. 生产环境强制配置 `ADMIN_TOKEN_SECRET`。
4. 用户接口不要信任前端传入的 `userId`。
5. 普通用户接口也接入 token 鉴权。

### P1：文档与运行体验

1. 更新 README，使其匹配当前微服务结构。
2. 写清楚微服务模式和单体模式两种运行方式。
3. 增加 `.env.example`。
4. 增加 Docker Compose，至少覆盖 MySQL、Redis、RabbitMQ。

### P2：架构整理

1. 明确是否继续保留 `backend-services/backend` 单体版。
2. 如果以后以微服务为主，可以冻结或删除单体版。
3. 抽取公共模块，降低实体、DTO、Mapper 重复。
4. 梳理 auth-service 与后台管理业务的边界。

### P3：测试补齐

1. 先补核心 Service 单元测试。
2. 再补 Controller 集成测试。
3. 商城库存扣减和超时取消逻辑应优先测试。

### P4：工程质量

1. 增加统一异常处理 `@ControllerAdvice`。
2. 增加统一错误码。
3. 增加日志 traceId。
4. 完善 OpenAPI 文档。
5. 增加 CI 构建检查。

---

## 10. 总结

当前项目已经超出简单 CRUD 范畴，具备比较完整的业务功能和一定工程复杂度。

核心亮点：

- 前后端分离
- Spring Cloud Gateway 网关
- 多服务拆分
- RBAC 后台权限
- 单词、阅读、听力学习业务
- 商城订单系统
- Redis 库存扣减
- RabbitMQ 延迟取消订单

主要短板：

- README 与实际结构不一致
- 单体和微服务并存，重复代码较多
- 用户接口鉴权不足
- 存在敏感配置泄露风险
- 缺少 Docker Compose 和测试

建议后续优先处理安全配置和鉴权问题，然后完善文档与运行环境，最后再进行架构整理和测试补齐。

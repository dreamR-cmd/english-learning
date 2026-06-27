# English Learning（英语学习平台）

一个面向 CET-4 / CET-6、考研、托福、雅思、GRE 等考试的英语学习平台，提供单词、阅读、听力练习，以及考试倒计时、错题本和收藏功能。

技术栈：**Vue 3 + Vite** 前端 + **Spring Boot 3 + Spring Data JPA + MySQL** 后端。

## 功能特性

- **多考试模块**：内置 CET-4、CET-6、托福、雅思、考研英语、GRE 六大模块。
- **三种练习**：单词练习、阅读练习、听力练习，按模块归类。
- **考试倒计时**：后端根据各考试固定规律（如 CET 为每年 6 月、12 月的第三个星期六）自动推算下一场考试日期并计算倒计时，无需依赖外部数据源。
- **用户体系**：注册 / 登录，修改昵称。
- **错题本**：记录练习中的错题，便于复习。
- **收藏夹**：收藏感兴趣的阅读篇目。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3、Vue Router 4、Pinia、Axios、Vite 5 |
| 后端 | Spring Boot 3.2.5、Spring Web、Spring Data JPA、Bean Validation |
| 数据库 | MySQL 8 |
| 构建 | Maven（后端）、npm + Vite（前端） |
| JDK | Java 17 |

## 项目结构

```
english-learning/
├── backend/                       # Spring Boot 后端
│   └── src/main/
│       ├── java/com/english/
│       │   ├── config/            # CORS 等配置
│       │   ├── controller/        # REST 控制器（auth/modules/practice/user）
│       │   ├── dto/               # 请求/响应对象（ApiResult、LoginRequest 等）
│       │   ├── entity/            # JPA 实体
│       │   ├── mapper/            # Spring Data JPA Repository
│       │   └── service/(impl)     # 业务逻辑
│       └── resources/
│           ├── application.yml    # 数据库与 JPA 配置
│           └── data.sql           # 模块及示例数据初始化脚本
└── frontend/                      # Vue 3 前端
    └── src/
        ├── components/            # 公共组件（NavBar）
        ├── router/                # 路由配置
        ├── utils/api.js           # Axios 封装
        └── views/                 # 页面（Login、Modules、ModuleDetail、各练习页、Profile）
```

## 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+
- MySQL 8.x

## 快速开始

### 1. 准备数据库

创建数据库（表结构由 JPA `ddl-auto: update` 自动建立，初始数据由 `data.sql` 注入）：

```sql
CREATE DATABASE english_learning DEFAULT CHARACTER SET utf8mb4;
```

默认连接配置见 `backend/src/main/resources/application.yml`，如有不同请按需修改：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/english_learning?...
    username: root
    password: 123456
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 **http://localhost:8081**。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器由 Vite 启动（默认 http://localhost:5173）。生产构建：

```bash
npm run build      # 产物输出到 frontend/dist
npm run preview    # 本地预览构建产物
```

## API 概览

所有接口统一返回 `ApiResult` 结构：`{ code, message, data }`。

### 认证 `/api/auth`
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/login` | 登录，body：`{ username, password }` |
| POST | `/register` | 注册，body：`{ username, password }` |

### 模块 `/api/modules`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 获取所有考试模块（含考试倒计时） |
| GET | `/{code}` | 按 code 获取单个模块 |

### 练习 `/api/practice`
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/words/{moduleCode}` | 按模块获取单词 |
| GET | `/readings/{moduleCode}` | 按模块获取阅读 |
| GET | `/listenings/{moduleCode}` | 按模块获取听力 |

### 用户 `/api/user`
| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/profile` | 修改资料，body：`{ userId, nickname }` |
| POST | `/wrong-records` | 提交错题记录 |
| GET | `/wrong-records?userId=` | 获取错题列表 |
| POST | `/favorites` | 添加收藏，body：`{ userId, readingId }` |
| DELETE | `/favorites/{readingId}?userId=` | 取消收藏 |
| GET | `/favorites?userId=` | 获取收藏列表 |
| GET | `/favorites/check?userId=&readingId=` | 查询是否已收藏 |

## 说明

- 跨域已在后端 `CorsConfig` 中开放，便于前后端分离开发。
- `application.yml` 中的数据库账号密码为示例值，正式部署请改用环境变量或独立配置，避免提交敏感信息。

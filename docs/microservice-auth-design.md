# 微服务统一认证设计与实现

## 1. 背景

项目已经拆分为多个后端服务：

```text
frontend -> gateway -> auth-service / user-service / learning-service / shop-service / admin-service
```

改造前，`gateway` 主要负责 CORS、路由转发和 Swagger/OpenAPI 聚合，普通业务接口没有统一登录校验。只有 `admin-service` 内部的 `AdminAuthFilter` 会对 `/api/admin/**` 做后台权限校验。

改造后采用企业项目中更常见的认证分层：

```text
auth-service：负责登录、注册、签发 token
gateway：统一校验登录态，并向下游服务透传用户上下文
业务服务：处理业务逻辑，后续逐步基于用户上下文做资源级授权
admin-service：保留后台 RBAC 细粒度权限校验
```

## 2. 为什么不每次请求都调用 auth-service

不建议设计成“每次请求都远程调用 auth-service 校验 token”，原因是：

1. 每个业务请求都会多一次远程调用，性能更差。
2. 所有流量都会压到 auth-service，容易形成瓶颈。
3. auth-service 一旦异常，整个系统的业务接口都会被放大影响。
4. 服务之间强耦合，链路更长，排查问题更困难。

当前项目使用的是 HMAC 签名 token，Gateway 可以在本地用同一个密钥完成签名和过期时间校验，不需要每次请求都访问 auth-service。

## 3. 当前请求链路

### 3.1 登录/注册

```text
frontend
  -> gateway
  -> auth-service
  -> 校验用户名密码或创建用户
  -> 签发 token
  -> 返回 LoginUserInfo
```

放行接口：

```text
POST /api/auth/login
POST /api/auth/register
```

### 3.2 普通业务接口

```text
frontend
  -> Authorization: Bearer <token>
  -> gateway 校验 token
  -> gateway 透传 X-User-Id / X-Token-Expires-At
  -> user-service / learning-service / shop-service
```

### 3.3 后台管理接口

```text
frontend
  -> Authorization: Bearer <token>
  -> gateway 校验是否登录
  -> admin-service
  -> AdminAuthFilter 校验后台角色和权限码
  -> AdminController
```

职责边界：

- Gateway 判断“是否登录”。
- `admin-service` 判断“是否有后台管理权限”。

## 4. Token 格式

当前 token 由 `auth-service` 生成，格式为：

```text
base64Url("userId:expiresAt") + "." + HMAC_SHA256(payload)
```

其中：

- `userId`：登录用户 ID。
- `expiresAt`：秒级 Unix 时间戳。
- 签名算法：`HmacSHA256`。
- 密钥配置：`ADMIN_TOKEN_SECRET`。

相关实现：

- 签发：`backend-services/auth-service/src/main/java/com/english/service/AdminTokenService.java`
- Gateway 校验：`backend-services/gateway/src/main/java/com/english/gateway/auth/GatewayTokenService.java`
- 后台 RBAC 校验：`backend-services/admin-service/src/main/java/com/english/service/AdminTokenService.java`

生产环境必须保证以下服务使用同一个密钥：

```text
auth-service
gateway
admin-service
```

配置示例：

```text
ADMIN_TOKEN_SECRET=替换为生产环境强随机密钥
```

## 5. Gateway 放行路径

Gateway 认证配置位于：

```text
backend-services/gateway/src/main/resources/application.yml
```

当前放行路径：

```yaml
gateway:
  auth:
    enabled: true
    exclude-paths:
      - /api/auth/login
      - /api/auth/register
      - /actuator/health
      - /swagger-ui/**
      - /swagger-ui.html
      - /v3/api-docs/**
      - /v3/**
```

除放行路径外，所有 `/api/**` 请求都需要携带：

```http
Authorization: Bearer <token>
```

非 `/api/**` 请求不进入认证逻辑。

## 6. Gateway 透传给下游服务的 Header

Gateway 校验通过后，会向下游服务追加：

```http
X-User-Id: <当前登录用户ID>
X-Token-Expires-At: <token过期时间戳>
```

Gateway 会先移除客户端传入的同名 header，再写入自己解析出的值，避免客户端伪造用户身份。

## 7. 与 admin-service RBAC 的关系

`admin-service` 继续保留自己的权限过滤器：

```text
backend-services/admin-service/src/main/java/com/english/config/AdminAuthFilter.java
```

权限映射仍由 `admin-service` 控制：

```text
/api/admin/orders       -> ORDER_MANAGE
/api/admin/modules      -> MODULE_MANAGE
/api/admin/users        -> USER_MANAGE
/api/admin/roles        -> ROLE_MANAGE
/api/admin/permissions  -> PERMISSION_MANAGE
其他 /api/admin/**      -> ADMIN_DASHBOARD
```

这样可以避免 Gateway 承担过多业务权限细节。

## 8. 后续业务服务授权演进

本次改造完成入口层统一登录校验，但部分业务接口仍保留历史参数，例如：

```text
userId
```

后续建议逐步演进为：

1. 新接口不要再从请求体或 query 参数接收当前用户 ID。
2. 业务服务优先读取 Gateway 透传的 `X-User-Id`。
3. 对暂时保留 `userId` 参数的旧接口，增加校验：请求中的 `userId` 必须等于 `X-User-Id`。
4. 用户私有资源接口，例如错题、收藏、订单、个人资料，应全部以 token 中的用户身份为准。

目标是最终形成：

```text
前端不再决定“我是谁”
后端只信任 token / Gateway 解析出的用户身份
```

## 9. 验证方式

### 9.1 构建验证

```powershell
mvn -q -DskipTests package -f backend-services/gateway/pom.xml
mvn -q -DskipTests package -f backend-services/auth-service/pom.xml
mvn -q -DskipTests package -f backend-services/admin-service/pom.xml
```

### 9.2 接口验证

未带 token 请求受保护接口：

```text
GET http://localhost:8081/api/user/favorites?userId=1
```

预期：Gateway 返回 `401`。

登录获取 token：

```text
POST http://localhost:8081/api/auth/login
```

预期：返回 `data.token`。

带 token 请求普通业务接口：

```text
GET http://localhost:8081/api/user/favorites?userId=<当前用户ID>
Authorization: Bearer <token>
```

预期：Gateway 放行并转发到业务服务。

带 token 请求后台接口：

```text
GET http://localhost:8081/api/admin/orders
Authorization: Bearer <token>
```

预期：

- 普通用户 token：通过 Gateway，但被 `admin-service` 拒绝。
- 管理员 token：通过 Gateway 和 `admin-service` RBAC。

Swagger/OpenAPI：

```text
http://localhost:8081/swagger-ui/index.html
/v3/api-docs/auth
/v3/api-docs/user
/v3/api-docs/learning
/v3/api-docs/shop
/v3/api-docs/admin
```

预期：文档路径不被 Gateway 认证拦截。

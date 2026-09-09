# Nacos Config

## 本机 Docker 全栈（推荐）

`docker-compose.yml` 中已内置 `nacos` 服务（standalone 模式，端口 `8848` / `9848`）。
所有后端服务通过容器内地址 `nacos:8848` 注册和拉取配置。

首次启动顺序：

```powershell
# 1. 启动全部依赖 + 应用（nacos 健康通过后 migration 先跑，再启动业务服务）
docker compose up -d --build

# 2. 等 Nacos 就绪后，一次性导入配置（Data ID = 文件名，Group = DEFAULT_GROUP）
.\scripts\import-nacos-config.ps1

# 3. 重启业务服务，让它们从 Nacos 取到配置
docker compose restart auth-service user-service learning-service shop-service admin-service rag-service migration-service gateway
```

如果中途清空了 Nacos 数据（`docker compose down -v`），只需重新执行第 2、3 步即可恢复，
无需重新搭建 MySQL/Redis。

## 手动导入

也可以按 Data ID 逐个导入：

- Group: `DEFAULT_GROUP`
- Data ID: 文件名，例如 `english-learning-common.yaml`
- Format: `YAML`

`english-learning-common.yaml` 是所有后端模块第一个导入的共享配置，每个服务再导入自己的
Data ID，如 `auth-service.yaml`。

## Nacos 地址约定

- 纯 Docker：服务用 `NACOS_SERVER_ADDR=nacos:8848`（已写入 docker-compose.yml）。
- Windows 主机上单独跑 Nacos：服务用 `NACOS_SERVER_ADDR=localhost:8848`。

### 单机 Nacos（可选）

不需要为 Docker 单独准备 Nacos 实例；如果你只想在主机跑一个 Nacos，用：

```bash
docker run -d --name nacos \
  -p 8848:8848 -p 9848:9848 -p 9849:9849 \
  -e MODE=standalone -e NACOS_AUTH_ENABLE=false \
  nacos/nacos-server:v2.3.2
```
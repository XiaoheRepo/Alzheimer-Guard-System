# Deployment Guide — Alzheimer Guard Server (Java)

> 阿尔兹海默症患者协同寻回系统 · 后端服务部署指南
> 适用版本：v2.0 · 2025
> 运行时：**JDK 21** · Spring Boot 3.4.5

---

## 1. 组件拓扑

```
                ┌──────────────┐
  Browser / App ─┤  NGINX (TLS) ├───┐
                └──────────────┘   │
                                    ▼
             ┌──────────────────────────────┐
             │   guard-server-java (jar)    │
             │   8080 / actuator / ws       │
             └──────┬───────────────────────┘
                    │
    ┌───────────────┼─────────────────┬──────────────┐
    ▼               ▼                 ▼              ▼
PostgreSQL 16   Redis 7         Kafka 3.6        DashScope
 (business)    (cache/lock)   (outbox / events)  (Qwen LLM)
```

## 2. 环境要求

| 组件 | 最低版本 | 说明 |
|------|----------|------|
| JDK | 21 | Temurin / GraalVM 均可 |
| Maven | 3.9+ | Wrapper `./mvnw` 可直接使用 |
| PostgreSQL | 16 | 建议启用 `pgcrypto`（可选 PostGIS 以后续启用 GIS） |
| Redis | 7+ | 幂等 / JWT 黑名单 / WS ticket |
| Kafka | 3.6+ | Outbox 事件下发（单 broker 即可用于毕设） |
| OS | Linux (推荐) / macOS / Windows (WSL2) | |

## 3. 快速开始（本地）

### 3.1 使用 docker compose 启动依赖

创建 `deploy/docker-compose.yml`：

```yaml
version: "3.9"
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: guard
      POSTGRES_USER: guard
      POSTGRES_PASSWORD: guard
    ports: ["5432:5432"]
    volumes: [pg:/var/lib/postgresql/data]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  kafka:
    image: bitnami/kafka:3.6
    environment:
      KAFKA_ENABLE_KRAFT: "yes"
      KAFKA_CFG_NODE_ID: "1"
      KAFKA_CFG_PROCESS_ROLES: controller,broker
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: "1@kafka:9093"
      KAFKA_CFG_LISTENERS: "PLAINTEXT://:9092,CONTROLLER://:9093"
      KAFKA_CFG_ADVERTISED_LISTENERS: "PLAINTEXT://localhost:9092"
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: CONTROLLER
      ALLOW_PLAINTEXT_LISTENER: "yes"
    ports: ["9092:9092"]

volumes:
  pg: {}
```

```bash
docker compose -f deploy/docker-compose.yml up -d
```

### 3.2 构建与启动

```bash
./mvnw -DskipTests clean package
java -jar target/guard-server-java-0.0.1-SNAPSHOT.jar
```

启动后：

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>

## 4. 环境变量

> 所有配置支持通过环境变量覆盖 `application.yml`。示例见下表（仅核心项）。

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/guard` | PG 连接字符串 |
| `SPRING_DATASOURCE_USERNAME` | `guard` | 数据库用户 |
| `SPRING_DATASOURCE_PASSWORD` | `guard` | 数据库密码 |
| `SPRING_REDIS_HOST` | `localhost` | Redis 主机 |
| `SPRING_REDIS_PORT` | `6379` | Redis 端口 |
| `SPRING_REDIS_PASSWORD` | 空 | Redis 密码（如启用 ACL） |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka 入口 |
| `GUARD_JWT_SECRET` | `change-me-in-production-...` | **务必线上替换为 ≥ 32 字节随机值** |
| `GUARD_JWT_EXPIRATION_MS` | `86400000` | Access Token 24h |
| `GUARD_JWT_REFRESH_EXPIRATION_MS` | `604800000` | Refresh Token 7d |
| `GUARD_AI_API_KEY` | 空 | DashScope（通义千问）Key |
| `GUARD_AI_MODEL_NAME` | `qwen-max-latest` | 模型名 |
| `GUARD_AI_USER_DAILY_QUOTA` | `20000` | 每用户日 token 额度 |
| `GUARD_AI_PATIENT_DAILY_QUOTA` | `50000` | 每患者日 token 额度 |
| `GUARD_OUTBOX_POLLING_INTERVAL_MS` | `1000` | Outbox 调度间隔 |
| `GUARD_OUTBOX_BATCH_SIZE` | `50` | Outbox 每批数量 |
| `GUARD_OUTBOX_MAX_RETRY` | `5` | 失败重试上限 |

## 5. 数据库初始化

Flyway 已托管所有迁移脚本。`src/main/resources/db/migration/V1__init_schema.sql` 覆盖全部 22 张表。

- 启动时会自动执行 `flyway migrate`；
- 生产首次部署建议执行 `./mvnw flyway:info` 预检；
- 如需清空重建（**仅测试环境**）：`./mvnw flyway:clean -Dflyway.cleanDisabled=false`。

## 6. 镜像化部署

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-alpine
ARG JAR_FILE=target/guard-server-java-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENV JAVA_OPTS="-XX:+UseZGC -Xms512m -Xmx1024m"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app.jar"]
```

```bash
docker build -t xiaohelab/guard-server:2.0 .
docker run -d --name guard-server --network host \
  -e GUARD_JWT_SECRET=... -e GUARD_AI_API_KEY=... \
  xiaohelab/guard-server:2.0
```

## 7. 运维要点

### 7.1 健康与可观测

- `/actuator/health` —— LB 探活。
- `/actuator/metrics` —— Micrometer 指标（可对接 Prometheus）。
- 日志 MDC 携带 `trace_id / request_id / user_id`，便于串联。

### 7.2 Outbox 维护

- 表 `outbox` 与 `outbox_dead` 由后台调度器自动分发；
- 管理接口：`POST /api/v1/admin/outbox/replay`（可重放 DEAD 消息）。

### 7.3 安全基线

| 项目 | 要求 |
|------|------|
| JWT Secret | 生产环境 ≥ 32 字节、定期轮换 |
| CORS | 通过 NGINX 或 `WebMvcConfigurer` 限定来源 |
| 密码 | BCryptPasswordEncoder，不可逆 |
| 授权 | `@PreAuthorize` + 业务层 `assertGuardian` 双层 |
| 幂等 | `X-Request-Id` + Redis SETNX，TTL 15 分钟 |
| 限流 | 建议在 NGINX / API 网关层做 QPS 限流（未内置） |

### 7.4 备份与恢复

- PostgreSQL：`pg_dump -Fc guard > guard.dump`；恢复 `pg_restore -d guard guard.dump`。
- Redis：AOF 持久化；WS ticket / 幂等 key 丢失不致命。
- Kafka：Outbox 侧保留 `status=SENT` 记录 30 天后归档。

## 8. 故障排查

| 现象 | 可能原因 | 处置 |
|------|----------|------|
| 启动报 `Flyway migration failed` | schema drift | `flyway:info` 对齐；必要时回滚 |
| 401 Token 无效 | Secret 不一致 / 黑名单 | 检查 `GUARD_JWT_SECRET`、`auth:blacklist:*` |
| WS 握手 403 | ticket 过期 / 已消费 | 重新调 `POST /api/v1/auth/ws-ticket` |
| 请求 409 E_*_4091 | 乐观锁冲突 | 前端重新拉取最新版本再更新 |
| Outbox 积压 | Kafka 不可达 | 观察 `outbox.status` 与 `retry_count`，排查 broker |

---

**附录 A：默认端口**

| 端口 | 用途 |
|------|------|
| 8080 | HTTP API + WebSocket |
| 5432 | PostgreSQL |
| 6379 | Redis |
| 9092 | Kafka |

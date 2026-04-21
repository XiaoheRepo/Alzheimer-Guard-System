# 测试方案与用例 · Alzheimer Guard Server

> 全量后端模块测试计划（中文版） · v2.0 · 2026-04
> 与 [TESTING.md](TESTING.md) 配套：该文件侧重**执行方式 / 基础设施**，本文件侧重**逐模块的测试点与用例矩阵**。

---

## 0. 测试层级与准入条件

| 层级 | 框架 | 范围 | 准入 | 目标覆盖率 |
|------|------|------|------|-----------|
| L1 单元测试 | JUnit 5 + Mockito + AssertJ | Service 内业务逻辑、Util 工具 | mvn test 本地通过 | 行覆盖 ≥ 80% |
| L2 切片测试 | `@WebMvcTest` / `@DataJpaTest` | Controller 参数校验 / Mapper SQL | 单元全绿 | 核心 Controller ≥ 70% |
| L3 集成测试 | `@SpringBootTest` + Testcontainers (PG/Redis/Kafka) | 跨层契约、事务、事件 | L1/L2 全绿 | 关键流程 100% |
| L4 E2E | Postman / cURL / Swagger UI | 核心用户旅程（监护人 + 公众 + Admin） | 集成测试通过 | 冒烟必过 |
| L5 非功能 | k6 / JMeter / JaCoCo / OWASP DC | 压测、覆盖率、依赖漏洞 | 可选 | — |

---

## 1. 通用规约

### 1.1 测试数据约定

- 测试数据命名前缀：`t_`（如 `t_alice`, `t_patient_001`）。
- 每个 IT 用例使用独立事务回滚（`@Transactional` + 默认 rollback=true）或显式 `DROP schema` 清理。
- 密码约定：`Aa123456!`；JWT Secret 在 `application-test.yml` 中固定。

### 1.2 通用断言

每个接口均应验证：

1. **HTTP 状态码**正确（200 / 401 / 403 / 404 / 409 / 422）。
2. **响应体 code** 与 `ErrorCode` 一致。
3. **Outbox 副作用**：发生状态流转的接口，事件必须落 `outbox` 表且 `partition_key` 正确。
4. **审计日志**：高危操作应产生 `sys_log` 记录（可后续接入）。
5. **PII 脱敏**：响应中手机号 / 身份证等字段符合 `DesensitizeUtil` 模式。

### 1.3 幂等与并发

| 场景 | 构造 | 期望 |
|------|------|------|
| 相同 `X-Request-Id` 双发 | 并发两次调用 | 第二次返回首次结果，不产生副作用 |
| 乐观锁冲突 | 两个请求同时更新同一记录 | 一个成功，一个抛 `E_*_4091` |
| 分布式锁冲突 | Redis SETNX 冲突 | 返回 `E_GOV_4092`（冲突） |

---

## 2. 模块测试矩阵

### 2.1 Auth 认证模块

**覆盖文件**：`auth/service/AuthService`, `auth/controller/AuthController`, `common/security/JwtTokenProvider`, `common/security/JwtAuthFilter`

| # | 场景 | 用例描述 | 期望结果 |
|---|------|---------|---------|
| A1 | 正常注册 | `POST /auth/register` 传合法 username + password | 200 · `code=OK` · 返回用户信息 |
| A2 | 用户名重复 | 用已存在 username 注册 | 409 · `E_GOV_4091` |
| A3 | 邮箱重复 | 邮箱已被占用 | 409 · `E_GOV_4092` |
| A4 | 密码弱口令 | 长度 < 6 | 422 · `E_REQ_4220` |
| A5 | 正常登录 | 正确密码 | 200 · 返回 access + refresh token |
| A6 | 错误密码 | 错误密码 5 次以内 | 401 · `E_AUTH_4011` |
| A7 | 冻结账号 | status=FROZEN 的用户登录 | 403 · `E_GOV_4031` |
| A8 | Token 刷新 | 合法 refresh token | 200 · 新 access token |
| A9 | Token 非 refresh 类型 | 用 access token 当 refresh | 401 · `E_GOV_4011` |
| A10 | Logout 黑名单 | logout 后使用旧 token | 401 · 命中 Redis 黑名单 |
| A11 | 修改密码 · 旧密错误 | | 401 · `E_USR_4011` |
| A12 | 修改密码 · 新旧相同 | | 422 · `E_USR_4001` |
| A13 | ws-ticket 签发 | 登录后请求 | 返回 ticket；Redis TTL ≤ 30s；DB 有记录 |
| A14 | ws-ticket 被消费 | 同一 ticket 握手 2 次 | 第一次成功，第二次 403 |
| A15 | 未携带 Token 访问 /auth/me | | 401 |
| A16 | 非法 JWT 签名 | | 401 · `E_GOV_4011` |

### 2.2 Patient 患者档案模块

**覆盖文件**：`patient/service/PatientService`, `patient/service/GuardianAuthorizationService`, `patient/controller/PatientController`

| # | 场景 | 用例 | 期望 |
|---|------|------|------|
| P1 | 创建患者 | 合法请求 | 200 · 自动生成 profile_no / short_code · 创建者为 PRIMARY_GUARDIAN · outbox 有 profile.created |
| P2 | 短码唯一性冲突 | Mock `existsByShortCode` 一直返回 true | 500 · `E_SYS_5000` |
| P3 | 非监护人查询 | 用户 B 查用户 A 的患者 | 403 · `E_PRO_4030` |
| P4 | 患者已删除查询 | `deleted_at != null` | 404 · `E_PRO_4041` |
| P5 | 更新患者 · 乐观锁 | 并发两次更新 | 一个成功（version+1），一个 409 |
| P6 | 更新头像 · 传空字符串 | `avatar_url=""` | 422 · `E_PRO_4014` |
| P7 | 围栏启用但无坐标 | `fence_enabled=true` 缺 lat/lng | 422 · `E_PRO_4221` |
| P8 | 围栏正常更新 | | 200 · 版本 +1 |
| P9 | confirmSafe 状态非法 | `lost_status=NORMAL` 调用 | 409 · `E_PRO_4092` |
| P10 | 逻辑删除 | 主监护人删除 | 关联关系全部 REVOKED · outbox 有 profile.deleted.logical |
| P11 | 非主监护人删除 | 普通监护人删除 | 403 · `E_PRO_4032` |
| P12 | listMyPatients 排除已删除 | | 已软删记录不出现 |

### 2.3 Guardian 监护关系模块

**覆盖文件**：`patient/service/GuardianService`, `patient/controller/GuardianController`

| # | 场景 | 期望 |
|---|------|------|
| G1 | 主监护人邀请 | 创建 PENDING 邀请 · outbox guardian.invited |
| G2 | 邀请自己 | 409 · `E_PRO_4094` |
| G3 | 重复邀请（PENDING 存在） | 409 · `E_PRO_4094` |
| G4 | 被邀请人接受 | 关系 ACTIVE · outbox guardian.joined |
| G5 | 其他人响应别人的邀请 | 403 · `E_PRO_4033` |
| G6 | 邀请过期后响应 | 状态变更为 EXPIRED · 409 · `E_PRO_4096` |
| G7 | 主监护转移发起 | PENDING_CONFIRM 记录 · outbox guardian.transfer.req |
| G8 | 同一患者转移已挂起 | 409 · `E_PRO_4095` |
| G9 | 转移确认 | 旧 primary 降级，目标升级 · outbox guardian.transfer.done |
| G10 | 转移撤销（发起方） | REVOKED |
| G11 | 移除主监护人本身 | 403 · `E_PRO_4035` |
| G12 | 成员列表 | 返回全部 ACTIVE 成员 |

### 2.4 Rescue 寻回任务模块

**覆盖文件**：`rescue/service/RescueTaskService`, `rescue/controller/RescueTaskController`

| # | 场景 | 期望 |
|---|------|------|
| R1 | 创建任务 | 生成 task_no · 患者 lost_status 切换到 MISSING · outbox rescue.task.created |
| R2 | 同一患者并发创建（唯一索引） | 第二个 409 · `E_TASK_4091` |
| R3 | 非监护人创建 | 403 · `E_PRO_4030` |
| R4 | 关闭任务 | status=CLOSED · outbox rescue.task.closed |
| R5 | 关闭非 OPEN 任务 | 409 · `E_TASK_4091` |
| R6 | 续期任务 | `expire_at` 延长 |
| R7 | listMine 分页 | Pageable 正确工作 · desc 顺序 |

### 2.5 Clue 线索模块

**覆盖文件**：`clue/service/ClueService`, `clue/controller/ClueController`, `clue/controller/PublicEntryController`

| # | 场景 | 期望 |
|---|------|------|
| C1 | 家属上报线索 | 坐标统一转 WGS84 · outbox clue.reported |
| C2 | 坐标系非法 | 422 · `E_REQ_4220` |
| C3 | 匿名公众上报 | tag 合法 → 成功 · `reporter_type=ANONYMOUS` |
| C4 | tag 不存在 | 404 · `E_CLUE_4044` |
| C5 | tag 过期 | 409 · `E_CLUE_4104` |
| C6 | entry_token 重复使用 | 409 · `E_CLUE_4105` |
| C7 | 管理员 override validated | 状态 VALIDATED · outbox clue.validated |
| C8 | 管理员 reject | 状态 REJECTED · outbox clue.suspected 不发 |
| C9 | 非 Admin 复核 | 403 · `E_GOV_4030` |
| C10 | 分页查询 | 按 created_at desc |

### 2.6 Track & Fence 轨迹与围栏模块

**覆盖文件**：`clue/service/PatientTrajectoryService`, `clue/controller/PatientTrajectoryController`

| # | 场景 | 期望 |
|---|------|------|
| T1 | 正常上报单点 | 落库 · outbox track.updated |
| T2 | 越界（GCJ-02 坐标） | 自动转 WGS84 后落库 |
| T3 | 超出围栏半径 | 额外 outbox fence.breached |
| T4 | 围栏未启用 | 仅发 track.updated，不发 fence.breached |
| T5 | 非监护人上报 | 403 · `E_PRO_4030` |
| T6 | 按任务查询 | 顺序 ascending |
| T7 | 按时间范围查询 | 闭区间，无记录返回空列表 |

### 2.7 Material 物资工单模块

**覆盖文件**：`material/service/MaterialOrderService`, `material/service/TagService`

| # | 场景 | 期望 |
|---|------|------|
| M1 | 创建工单 | PENDING · outbox material.order.created |
| M2 | 自审自己工单 | 403 · `E_MAT_4031` |
| M3 | 审批通过 · 库存不足 | 409 · `E_MAT_4092` |
| M4 | 审批通过 · 成功 | 标签 ALLOCATED · outbox tag.allocated + material.order.approved |
| M5 | 并发审批同一工单（FOR UPDATE） | 仅一个成功 |
| M6 | 发货 | status=SHIPPED · outbox material.order.shipped |
| M7 | 未审批直接发货 | 409 · `E_MAT_4091` |
| M8 | 签收 | status=RECEIVED |
| M9 | 取消工单（非 PENDING） | 409 · `E_MAT_4091` |
| M10 | 绑定标签 · tag 非 ALLOCATED | 409 · `E_MAT_4091` |
| M11 | 正常绑定 | BOUND · 生成 `resource_token` · outbox tag.bound |
| M12 | 疑似丢失 | status=SUSPECTED_LOST · outbox tag.suspected_lost |
| M13 | 确认丢失 | status=LOST · outbox tag.lost |
| M14 | 非 BOUND 直接 confirmLost | 409 · `E_MAT_4098` |

### 2.8 AI 模块

#### 2.8.1 AI Session / Quota

| # | 场景 | 期望 |
|---|------|------|
| AI1 | 创建会话 | session_id 返回 · quota 不扣 |
| AI2 | 发送消息 | quota reserve → commit |
| AI3 | quota 不足 | 429 · `E_AI_4290`（根据 ErrorCode 实际值） |
| AI4 | LLM 异常回滚 quota | 预扣释放，账本平账 |
| AI5 | 非会话属主查询 | 403 · `E_AI_4033` |
| AI6 | 会话不存在 | 404 · `E_AI_4041` |

#### 2.8.2 AI Intent 双重确认

| # | 场景 | 期望 |
|---|------|------|
| I1 | 提出意图 | status=PENDING · expire_at 10min |
| I2 | 正确属主 APPROVE | status=APPROVED · processed_at 有值 |
| I3 | 非属主确认 | 403 · `E_AI_4033` |
| I4 | 过期意图 APPROVE | status=EXPIRED · 409 · `E_AI_4091` |
| I5 | decision 非法 | 422 · `E_REQ_4220` |
| I6 | 重复 APPROVE | 409 · `E_AI_4091`（非 PENDING） |

#### 2.8.3 AI Memory Note

| # | 场景 | 期望 |
|---|------|------|
| N1 | 创建记忆笔记 | note_id 生成 · outbox memory.appended |
| N2 | 非监护人创建 | 403 · `E_PRO_4030` |
| N3 | kind 非法值 | 422 · `E_REQ_4220` |
| N4 | 分页列表 | desc 顺序 |
| N5 | 删除他人患者笔记 | 403 |

#### 2.8.4 AI Poster

| # | 场景 | 期望 |
|---|------|------|
| Po1 | 生成海报 | 返回 URL · RescueTaskEntity.posterUrl 更新 · outbox ai.poster.generated |
| Po2 | 任务不存在 | 404 · `E_TASK_4041` |
| Po3 | 非监护人 | 403 · `E_PRO_4030` |

### 2.9 Notification 通知模块

| # | 场景 | 期望 |
|---|------|------|
| N1 | 分页查询未读 | 仅 unread=true 条目 |
| N2 | 未读数 | count = 未读数量 |
| N3 | 标记已读 | unread=false · read_at 有值 |
| N4 | 标记他人消息为已读 | 403 |
| N5 | 全部标记已读 | 批量更新 |

### 2.10 Governance / Admin

#### 2.10.1 SysConfig

| # | 场景 | 期望 |
|---|------|------|
| S1 | Admin CRUD | 成功 |
| S2 | 非 Admin 访问 | 403 · `E_GOV_4030` |
| S3 | key 重复 | 409 · `E_GOV_4093` |

#### 2.10.2 Outbox Admin

| # | 场景 | 期望 |
|---|------|------|
| O1 | 列 DEAD 消息 | 分页有数据 |
| O2 | 重放 DEAD | status=PENDING · retry_count 重置 |
| O3 | 非 Admin | 403 |

#### 2.10.3 SysLog

| # | 场景 | 期望 |
|---|------|------|
| L1 | cursor 翻页 | `id < cursor` |
| L2 | 按 module 过滤 | |
| L3 | 非 Admin 调用 | 403 |

### 2.11 WebSocket

| # | 场景 | 期望 |
|---|------|------|
| W1 | ws?ticket=xxx 握手 | 101 Switching Protocols |
| W2 | 无 ticket 握手 | 403 |
| W3 | ticket 已用 | 403 |
| W4 | ticket 过期 | 403 |
| W5 | 连接成功后收到 hello | 消息 `{"type":"hello","user_id":123}` |
| W6 | 定向推送 | `sendToUser(userId)` 仅发送给该用户的所有 session |
| W7 | 广播 | `broadcast()` 发给全部在线 |
| W8 | 会话关闭后从 map 清理 | `onlineUserCount()` 减 1 |

### 2.12 Outbox 发布器

| # | 场景 | 期望 |
|---|------|------|
| OB1 | publish 必须在事务内 | 无事务调用 → `IllegalTransactionStateException` |
| OB2 | JSON 序列化 | payload 正确落 `payload` 列 |
| OB3 | 调度器捞取 PENDING | `FOR UPDATE SKIP LOCKED` 正确 |
| OB4 | 发送成功 | status=SENT · sent_at 有值 |
| OB5 | 连续失败 | retry_count + 1 · 指数退避 `next_attempt_at` |
| OB6 | 达到 max-retry | status=DEAD |

### 2.13 通用组件 Common

| 组件 | 核心用例 |
|------|---------|
| `Idempotent` AOP | 相同 `X-Request-Id` 在 TTL 内再次请求 → 返回首次结果；TTL 过后 → 正常处理 |
| `CoordUtil.toWgs84` | GCJ-02 → WGS84 有偏差但在 <500m；BD-09 → WGS84 两步转换 |
| `CoordUtil.haversineMeter` | 已知参考距离误差 < 1% |
| `DesensitizeUtil.phone` | `13812345678` → `138****5678` |
| `BusinessNoUtil` | 返回值含前缀（T/C/O/...）且长度稳定 |
| `JwtTokenProvider` | access/refresh typ 正确；超 exp 抛异常 |
| `TraceIdUtil` | MDC 中 trace_id 写入/清除 |
| `JacksonConfig` | LocalDateTime / OffsetDateTime 序列化为 ISO-8601 |

---

## 3. 集成测试冒烟（E2E 脚本）

以下为一次端到端的**核心用户旅程**，用于演示与冒烟：

```
 1. 注册监护人 A，登录 → TOKEN_A
 2. 创建患者 P → patient_id
 3. 配置围栏
 4. 注册 B，登录 → TOKEN_B
 5. A 邀请 B → invite_id
 6. B 接受邀请 → 关系 ACTIVE
 7. A 创建寻回任务 → task_id
 8. A 生成海报 → poster_url
 9. 上报轨迹点（超围栏） → fence.breached
10. 物资：A 申领标签 → Admin 审批 → A 绑定到 P
11. 公开扫码：GET /r/{resource_token} → entry_token
12. 匿名上报线索 POST /api/v1/public/clues
13. Admin 复核线索 → VALIDATED
14. A 请求 /auth/ws-ticket → ticket
15. 建立 WS 连接 ws://localhost:8080/ws?ticket=...
16. B 关闭任务 → CLOSED
17. A confirmSafe → lost_status=NORMAL
```

该脚本对应的 Postman Collection 可从 Swagger UI 导出生成。

---

## 4. 性能与非功能测试（可选）

| 指标 | 工具 | 目标 |
|------|------|------|
| 单节点 QPS | k6 | 创建任务 ≥ 100 RPS，P95 < 300ms |
| WebSocket 并发 | Autocannon-ws | 同时连接 1000 用户 |
| 数据库连接 | HikariCP metrics | active ≤ 20 / 30 pool |
| Outbox 吞吐 | 观察 outbox.created vs sent 差 | 差值稳定不累积 |
| 依赖漏洞 | OWASP DC | 无 High/Critical |
| 覆盖率 | JaCoCo | Service ≥ 80%，整体 ≥ 70% |

---

## 5. 追踪矩阵（RTM · 需求 → 测试）

| SRS 需求 ID | 用例集 |
|-------------|--------|
| FR-AUTH-001 登录 | A1-A10 |
| FR-PROFILE-001 建档 | P1-P4 |
| FR-PROFILE-002 围栏 | P7-P8 + T3-T4 |
| FR-GUARD-001 邀请/转移 | G1-G12 |
| FR-TASK-001 寻回任务 | R1-R7 |
| FR-CLUE-001 家属上报 | C1-C2 |
| FR-CLUE-002 匿名上报 | C3-C6 |
| FR-MAT-001 物资 | M1-M14 |
| FR-AI-001 会话 | AI1-AI6 |
| FR-AI-002 意图 | I1-I6 |
| FR-AI-003 记忆 | N1-N5 |
| FR-AI-004 海报 | Po1-Po3 |
| FR-GOV-001 配置 | S1-S3 |
| FR-GOV-002 Outbox | O1-O3, OB1-OB6 |
| FR-GOV-003 审计 | L1-L3 |
| FR-PUSH-001 WebSocket | W1-W8 |

## 6. 持续集成建议 (`.github/workflows/ci.yml` 示意)

```yaml
name: ci
on: [push, pull_request]
jobs:
  build-test:
    runs-on: ubuntu-latest
    services:
      postgres: { image: postgres:16, env: { POSTGRES_PASSWORD: guard, POSTGRES_DB: guard, POSTGRES_USER: guard }, ports: [5432:5432] }
      redis:    { image: redis:7,   ports: [6379:6379] }
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - run: ./mvnw -q -DskipITs test
      - run: ./mvnw -q verify
      - run: ./mvnw -q jacoco:report
      - uses: actions/upload-artifact@v4
        with: { name: coverage, path: target/site/jacoco }
```

完。

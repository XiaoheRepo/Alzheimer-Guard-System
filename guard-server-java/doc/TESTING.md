# Testing Guide — Alzheimer Guard Server

> 测试文档 v2.0 · 覆盖 **单元测试 / 集成测试 / 接口回归 / 手工验证** 四类

---

## 1. 测试金字塔

```
           ┌────────────────────┐
           │  E2E (手工 / Post- │   少量、覆盖关键用户旅程
           │   man / Swagger)   │
           └────────────────────┘
          ┌────────────────────────┐
          │  Integration (Spring  │  Testcontainers 起 PG/Redis/Kafka
          │   Boot + TC)          │
          └────────────────────────┘
    ┌─────────────────────────────────┐
    │  Unit (JUnit 5 + Mockito)       │  Service / Util / Mapper 级别
    └─────────────────────────────────┘
```

## 2. 运行全部测试

```bash
./mvnw test                 # 仅单元
./mvnw verify               # 单元 + 集成 (推荐 PR 门禁)
./mvnw -Dtest=AuthServiceTest test   # 指定
```

测试覆盖率建议阈值：**行覆盖率 ≥ 70% / 核心 Service ≥ 85%**（可接入 JaCoCo）。

## 3. 依赖与约定

| 类型 | 框架 | 说明 |
|------|------|------|
| 单元测试 | JUnit 5 · Mockito · AssertJ | 已随 `spring-boot-starter-test` 引入 |
| 集成测试 | Spring Boot Test · Testcontainers | 需 Docker Desktop / Docker Engine 运行 |
| Mock MVC | `MockMvcRequestBuilders` | 断言控制器契约 |
| 安全 | spring-security-test | `@WithMockUser` 等 |

## 4. 单元测试样板

### 4.1 Service 层

```java
@ExtendWith(MockitoExtension.class)
class RescueTaskServiceTest {

    @Mock RescueTaskRepository taskRepository;
    @Mock PatientProfileRepository patientRepository;
    @Mock GuardianAuthorizationService authorizationService;
    @Mock OutboxService outboxService;

    @InjectMocks RescueTaskService taskService;

    @Test
    void should_create_task_and_publish_outbox() {
        // given
        AuthUser user = new AuthUser(1L, "u1", List.of("USER"));
        try (MockedStatic<SecurityUtil> mocked = mockStatic(SecurityUtil.class)) {
            mocked.when(SecurityUtil::current).thenReturn(user);
            when(authorizationService.assertGuardian(user, 7L))
                    .thenReturn(new PatientProfileEntity());
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // when
            var req = new TaskCreateRequest();
            req.setPatientId(7L); req.setTitle("走失");
            RescueTaskEntity task = taskService.create(req);

            // then
            assertThat(task.getTaskNo()).startsWith("T");
            verify(outboxService).publish(eq(OutboxTopics.RESCUE_TASK_CREATED),
                    anyString(), anyString(), anyMap());
        }
    }
}
```

### 4.2 工具类

```java
class CoordUtilTest {
    @Test void haversine_within_1_percent_reference() {
        double d = CoordUtil.haversineMeter(116.397, 39.908, 116.407, 39.918);
        assertThat(d).isBetween(1300.0, 1500.0);
    }
}
```

## 5. 集成测试（Testcontainers）

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("guard").withUsername("guard").withPassword("guard");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.redis.host",          redis::getHost);
        r.add("spring.redis.port",          () -> redis.getMappedPort(6379));
        r.add("spring.kafka.bootstrap-servers", () -> "localhost:9092"); // 或使用 KafkaContainer
    }

    @Autowired MockMvc mvc;

    @Test void register_login_me_flow() throws Exception {
        // 1) 注册
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"alice","password":"Aa123456!","phone":"13900000001"}"""))
           .andExpect(status().isOk());

        // 2) 登录
        String body = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"alice","password":"Aa123456!"}"""))
           .andExpect(status().isOk())
           .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.data.access_token");

        // 3) me
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.data.username").value("alice"));
    }
}
```

## 6. 关键用户旅程（E2E · 手工 / Postman）

### 6.1 流程清单

1. **注册 / 登录 / Me**
2. **创建患者 → 配置围栏**
3. **邀请监护人 → 响应邀请**
4. **创建寻回任务 → 发布 Outbox 事件**
5. **家属上报线索 / 匿名公众上报（`POST /api/v1/public/clues?tag={token}`）**
6. **管理员审核线索**
7. **物资申领 → 审批 → 发货 → 签收 → 标签绑定**
8. **AI 会话 → 意图确认 → 生成海报**
9. **记忆笔记追加**
10. **WebSocket：`POST /api/v1/auth/ws-ticket` 获取 → `ws://host/ws?ticket=...`**

### 6.2 cURL 小样

```bash
# 登录
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"Aa123456!"}'

# 创建寻回任务（需携带 JWT + X-Request-Id 幂等头）
curl -X POST http://localhost:8080/api/v1/rescue/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Request-Id: $(uuidgen)" \
  -H 'Content-Type: application/json' \
  -d '{"patient_id":7,"title":"老人走失","lost_address":"北京市海淀区中关村大街 1 号","contact_phone":"13900000001"}'

# 匿名线索（路径为 tag 的 resource_token）
curl -X POST "http://localhost:8080/r/ABCDEFGH?coord_system=WGS84&lat=39.9&lng=116.4" \
  -F 'message=看到一位老人在路口'
```

### 6.3 Postman Collection

建议根据 Swagger UI 导出的 OpenAPI JSON 自动生成：

```bash
curl -s http://localhost:8080/v3/api-docs -o openapi.json
# Postman: Import → openapi.json
```

## 7. 常见用例矩阵

| 模块 | 测试点 | 预期错误码 |
|------|--------|-----------|
| 认证 | 错误密码 | `E_AUTH_4011` |
| 认证 | Token 黑名单 | `E_AUTH_4013` |
| 患者 | 非监护人访问 | `E_PATIENT_4030` |
| 任务 | 同一患者重复 OPEN | `E_TASK_4091` |
| 线索 | 超出 tag TTL 扫码 | `E_CLUE_4104` |
| 物资 | 发起人重复审批自己 | `E_MAT_4031` |
| 标签 | 非 ALLOCATED 直接绑定 | `E_MAT_4091` |
| AI 意图 | 过期确认 | `E_AI_4091` |
| 意图 | 非会话属主确认 | `E_AI_4033` |
| 幂等 | 同 `X-Request-Id` 重放 | 返回首次结果（无副作用） |

## 8. 性能 / 压测（可选）

> 毕设阶段非硬性要求；若做演示建议用 k6 或 JMeter。

```js
// k6 示例：任务创建 RPS
import http from 'k6/http';
export const options = { vus: 20, duration: '30s' };
export default () => http.post('http://localhost:8080/api/v1/rescue/tasks', JSON.stringify({
    patient_id: 7, title: '压测', lost_address: 'x', contact_phone: '13900000001'
}), { headers: { Authorization: `Bearer ${__ENV.TOKEN}`, 'X-Request-Id': crypto.randomUUID(),
                'Content-Type': 'application/json' }});
```

## 9. CI 建议

| 阶段 | 命令 |
|------|------|
| Lint | `./mvnw -q compile` |
| Unit | `./mvnw -q test` |
| Integration | `./mvnw -q verify -Pintegration` |
| Coverage | `./mvnw -q jacoco:report`（需接入 JaCoCo 插件） |
| Security | `./mvnw -q dependency-check:check`（OWASP） |

---

**输出约定**：所有接口返回统一 `Result<T>`：

```json
{ "code": "OK", "message": "success", "data": { ... }, "trace_id": "..." }
```

错误响应：

```json
{ "code": "E_TASK_4091", "message": "工单状态不允许流转", "data": null, "trace_id": "..." }
```

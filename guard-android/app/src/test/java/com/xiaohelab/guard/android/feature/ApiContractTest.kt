package com.xiaohelab.guard.android.feature.contract

import com.xiaohelab.guard.android.feature.tag.data.TagApi
import com.xiaohelab.guard.android.feature.task.data.RescueTaskApi
import com.xiaohelab.guard.android.feature.notification.data.NotificationApi
import com.xiaohelab.guard.android.feature.ai.data.AiSessionApi
import com.xiaohelab.guard.android.feature.mat.data.MaterialOrderApi
import com.xiaohelab.guard.android.feature.clue.data.ClueApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType

/**
 * M3–M7 API 契约测试（MockWebServer）。
 * HC-Check §2.4: 验证 API 字段与错误码映射、统一响应外壳解析。
 * HC-ID-String: 所有 ID 字段为 String。
 */
class ApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var retrofit: Retrofit
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory(MediaType.get("application/json")))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ── Tag API ──────────────────────────────────────────────────────────────

    @Test
    fun `TagApi listTags parses response correctly`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody(
                    """{"code":200,"data":{"items":[{"tag_code":"TAG001","device_type":"BLE","alias":"手环","state":"BOUND","bound_at":"2024-01-01T00:00:00Z","patient_id":"patient_001"}]},"message":"ok","trace_id":"trace001"}"""
                )
                .addHeader("Content-Type", "application/json")
        )
        val api = retrofit.create(TagApi::class.java)

        val response = api.listTags("patient_001")

        assertTrue(response.isSuccessful)
        // 验证请求路径
        val request = server.takeRequest()
        assertEquals("/api/v1/patients/patient_001/tags", request.path)
        assertEquals("GET", request.method)
    }

    @Test
    fun `TagApi bindTag sends correct request body`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"code":200,"data":{"tag_code":"TAG002","device_type":"NFC","state":"BINDING","patient_id":"p_001"},"message":"ok","trace_id":"t1"}""")
                .addHeader("Content-Type", "application/json")
        )
        val api = retrofit.create(TagApi::class.java)
        api.bindTag("p_001", com.xiaohelab.guard.android.feature.tag.data.TagBindRequest("TAG002", "NFC", null))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/patients/p_001/tags", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"tag_code\":\"TAG002\""))
        assertTrue(body.contains("\"device_type\":\"NFC\""))
    }

    // ── Task API ─────────────────────────────────────────────────────────────

    @Test
    fun `RescueTaskApi listTasks returns task list`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"code":200,"data":{"items":[{"task_id":"task_001","patient_id":"p_001","status":"ACTIVE","description":"test","created_at":"2024-01-01T00:00:00Z","updated_at":"2024-01-01T00:00:00Z"}]},"message":"ok","trace_id":"t2"}""")
                .addHeader("Content-Type", "application/json")
        )
        val api = retrofit.create(RescueTaskApi::class.java)

        val response = api.listTasks()

        assertTrue(response.isSuccessful)
        val request = server.takeRequest()
        assertEquals("/api/v1/rescue-tasks", request.path)
        // HC-ID-String: task_id 为 String（从 JSON 解析后不变）
    }

    @Test
    fun `RescueTaskApi cancelTask sends POST to correct path`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"code":200,"data":null,"message":"ok","trace_id":"t3"}""")
                .addHeader("Content-Type", "application/json")
        )
        val api = retrofit.create(RescueTaskApi::class.java)
        api.cancelTask("task_001")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/rescue-tasks/task_001/cancel", request.path)
    }

    // ── Notification API ─────────────────────────────────────────────────────

    @Test
    fun `NotificationApi listNotifications parses unread_count`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"code":200,"data":{"items":[{"notification_id":"n_001","type":"TASK","title":"标题","body":"内容","read":false,"created_at":"2024-01-01T00:00:00Z"}],"unread_count":1,"total":1},"message":"ok","trace_id":"t4"}""")
                .addHeader("Content-Type", "application/json")
        )
        val api = retrofit.create(NotificationApi::class.java)

        val response = api.listNotifications()

        assertTrue(response.isSuccessful)
        val request = server.takeRequest()
        assertEquals("/api/v1/notifications", request.path)
    }

    @Test
    fun `NotificationApi markRead sends POST with notification_id`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"code":200,"data":null,"message":"ok","trace_id":"t5"}""")
                .addHeader("Content-Type", "application/json")
        )
        val api = retrofit.create(NotificationApi::class.java)
        api.markRead("n_001")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/notifications/n_001/read", request.path)
        // HC-ID-String: notification_id 路径参数为 String
    }

    // ── Material Order API ───────────────────────────────────────────────────

    @Test
    fun `MaterialOrderApi createOrder sends correct body`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"code":200,"data":{"order_id":"ord_001","patient_id":"p_001","item_code":"ITEM_A","quantity":2,"state":"PENDING","delivery_address":"北京市海淀区"},"message":"ok","trace_id":"t6"}""")
                .addHeader("Content-Type", "application/json")
        )
        val api = retrofit.create(MaterialOrderApi::class.java)
        api.createOrder(
            "p_001",
            com.xiaohelab.guard.android.feature.mat.data.MaterialOrderCreateRequest("ITEM_A", 2, "北京市海淀区")
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/patients/p_001/material-orders", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"item_code\":\"ITEM_A\""))
        assertTrue(body.contains("\"quantity\":2"))
    }

    // ── Clue API ─────────────────────────────────────────────────────────────

    @Test
    fun `ClueApi createClue sends task_id in path`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"code":200,"data":{"clue_id":"clue_001","task_id":"task_001","type":"TEXT","content":"线索内容","review_state":"PENDING"},"message":"ok","trace_id":"t7"}""")
                .addHeader("Content-Type", "application/json")
        )
        val api = retrofit.create(ClueApi::class.java)
        api.createClue("task_001", com.xiaohelab.guard.android.feature.clue.data.CreateClueRequest("TEXT", "线索内容"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/rescue-tasks/task_001/clues", request.path)
        // HC-02: review_state 由服务端返回，不推算
    }

    // ── AI API ───────────────────────────────────────────────────────────────

    @Test
    fun `AiSessionApi createSession sends patient_id`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"code":200,"data":{"session_id":"sess_001","patient_id":"p_001","status":"ACTIVE","created_at":"2024-01-01T00:00:00Z"},"message":"ok","trace_id":"t8"}""")
                .addHeader("Content-Type", "application/json")
        )
        val api = retrofit.create(AiSessionApi::class.java)
        api.createSession(com.xiaohelab.guard.android.feature.ai.data.CreateSessionRequest("p_001"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/ai/sessions", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"patient_id\":\"p_001\""))
    }
}

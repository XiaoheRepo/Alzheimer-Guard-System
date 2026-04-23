package com.xiaohelab.guard.android.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RequestIdInterceptorTest {
    private val server = MockWebServer()
    private lateinit var client: OkHttpClient

    @Before fun setUp() {
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(RequestIdInterceptor())
            .build()
    }
    @After fun tearDown() { server.shutdown() }

    @Test fun `POST carries X-Request-Id matching regex`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val req = Request.Builder().url(server.url("/api/v1/any"))
            .post(okhttp3.RequestBody.create(null, ByteArray(0))).build()
        client.newCall(req).execute().close()

        val recorded = server.takeRequest()
        val id = recorded.getHeader("X-Request-Id")
        assertNotNull(id)
        assertTrue(id!!.matches(Regex("^[A-Za-z0-9-]{16,64}$")))
    }

    @Test fun `GET does NOT carry X-Request-Id`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val req = Request.Builder().url(server.url("/api/v1/any")).get().build()
        client.newCall(req).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("X-Request-Id"))
    }

    @Test fun `upstream X-Request-Id is preserved for offline replay`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val given = "req-0123456789abcdef"
        val req = Request.Builder().url(server.url("/api/v1/any"))
            .post(okhttp3.RequestBody.create(null, ByteArray(0)))
            .header("X-Request-Id", given)
            .build()
        client.newCall(req).execute().close()

        val recorded = server.takeRequest()
        assertEquals(given, recorded.getHeader("X-Request-Id"))
    }
}

package com.xiaohelab.guard.android.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class TraceIdInterceptorTest {
    private val server = MockWebServer()
    private lateinit var client: OkHttpClient
    @Before fun setUp() {
        server.start()
        client = OkHttpClient.Builder().addInterceptor(TraceIdInterceptor()).build()
    }
    @After fun tearDown() { server.shutdown() }

    @Test fun `every method carries X-Trace-Id`() {
        for (method in listOf("GET", "POST", "PUT", "DELETE")) {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
            val builder = Request.Builder().url(server.url("/api/v1/x"))
            val req = when (method) {
                "GET" -> builder.get()
                "DELETE" -> builder.delete()
                else -> builder.method(method, okhttp3.RequestBody.create(null, ByteArray(0)))
            }.build()
            client.newCall(req).execute().close()
            assertNotNull(server.takeRequest().getHeader("X-Trace-Id"))
        }
    }
}

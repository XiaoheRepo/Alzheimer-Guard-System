package com.xiaohelab.guard.android.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ReservedHeaderGuardInterceptorTest {
    @Test fun `requests carrying X-User-Id fail fast`() {
        val server = MockWebServer().apply { start() }
        try {
            val client = OkHttpClient.Builder()
                .addInterceptor(ReservedHeaderGuardInterceptor())
                .build()
            val req = Request.Builder()
                .url(server.url("/api/v1/x"))
                .header("X-User-Id", "u1")
                .get()
                .build()
            try {
                client.newCall(req).execute().close()
                fail("Should have thrown")
            } catch (t: Throwable) {
                assertTrue(t.message?.contains("X-User-Id") == true)
            }
        } finally {
            server.shutdown()
        }
    }
}

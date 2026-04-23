package com.xiaohelab.guard.android.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackoffPolicyTest {
    @Test fun `parseRetryAfterSeconds accepts integer`() {
        assertEquals(12, BackoffPolicy.parseRetryAfterSeconds("12"))
    }
    @Test fun `parseRetryAfterSeconds returns null on invalid`() {
        assertNull(BackoffPolicy.parseRetryAfterSeconds(null))
        assertNull(BackoffPolicy.parseRetryAfterSeconds("abc"))
    }
    @Test fun `exponentialWithJitter caps at 30 seconds`() {
        repeat(20) {
            val ms = BackoffPolicy.exponentialWithJitter(attempt = 10)
            assertTrue(ms <= BackoffPolicy.CAP_MS)
        }
    }
}

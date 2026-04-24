package com.xiaohelab.guard.android.feature.task

import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.feature.task.data.RescueTaskDto
import com.xiaohelab.guard.android.feature.task.data.RescueTaskListDto
import com.xiaohelab.guard.android.feature.task.domain.CancelTaskUseCase
import com.xiaohelab.guard.android.feature.task.domain.GetTaskUseCase
import com.xiaohelab.guard.android.feature.task.domain.ListTasksUseCase
import com.xiaohelab.guard.android.feature.task.domain.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Task domain UseCases.
 * HC-02: status 由服务端维护，客户端不推算。
 * HC-ID-String: task_id / patient_id 均为 String。
 */
class TaskUseCaseTest {

    private val repo: TaskRepository = mockk()
    private val listTasks = ListTasksUseCase(repo)
    private val getTask = GetTaskUseCase(repo)
    private val cancelTask = CancelTaskUseCase(repo)

    private val sampleTask = RescueTaskDto(
        taskId = "task_001",          // HC-ID-String: String
        patientId = "patient_001",    // HC-ID-String: String
        status = "ACTIVE",            // HC-02: status 由服务端返回，不推算
        description = "测试任务",
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z",
        lastSeenLocation = null,
    )

    @Test
    fun `listTasks returns tasks from repository`() = runTest {
        coEvery { repo.listTasks(null, null) } returns MhResult.Success(
            RescueTaskListDto(listOf(sampleTask)), null
        )

        val result = listTasks()

        assertTrue(result is MhResult.Success)
        val items = (result as MhResult.Success).data.items
        assertEquals(1, items.size)
        assertEquals("task_001", items[0].taskId)
        // HC-02: status 直接来自服务端
        assertEquals("ACTIVE", items[0].status)
        coVerify { repo.listTasks(null, null) }
    }

    @Test
    fun `listTasks with patientId filter delegates correctly`() = runTest {
        coEvery { repo.listTasks("patient_001", null) } returns MhResult.Success(
            RescueTaskListDto(listOf(sampleTask)), null
        )

        val result = listTasks("patient_001")

        assertTrue(result is MhResult.Success)
        coVerify { repo.listTasks("patient_001", null) }
    }

    @Test
    fun `getTask returns task detail`() = runTest {
        coEvery { repo.getTask("task_001") } returns MhResult.Success(sampleTask, null)

        val result = getTask("task_001")

        assertTrue(result is MhResult.Success)
        assertEquals("task_001", (result as MhResult.Success).data.taskId)
        // HC-02: status 未被客户端修改
        assertEquals("ACTIVE", result.data.status)
    }

    @Test
    fun `cancelTask returns success`() = runTest {
        coEvery { repo.cancelTask("task_001") } returns MhResult.Success(Unit, null)

        val result = cancelTask("task_001")

        assertTrue(result is MhResult.Success)
        coVerify { repo.cancelTask("task_001") }
    }

    @Test
    fun `cancelTask propagates E_TASK_4221 when status disallows`() = runTest {
        val error = DomainException("E_TASK_4221", "Task state does not allow this action")
        coEvery { repo.cancelTask("task_completed") } returns MhResult.Failure(error)

        val result = cancelTask("task_completed")

        assertTrue(result is MhResult.Failure)
        assertEquals("E_TASK_4221", (result as MhResult.Failure).error.code)
    }

    @Test
    fun `listTasks propagates E_TASK_4041 failure`() = runTest {
        val error = DomainException("E_TASK_4041", "Task not found")
        coEvery { repo.listTasks(null, null) } returns MhResult.Failure(error)

        val result = listTasks()

        assertTrue(result is MhResult.Failure)
        assertEquals("E_TASK_4041", (result as MhResult.Failure).error.code)
    }
}

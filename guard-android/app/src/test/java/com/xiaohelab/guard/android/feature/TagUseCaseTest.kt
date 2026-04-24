package com.xiaohelab.guard.android.feature.tag

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.feature.tag.data.TagDto
import com.xiaohelab.guard.android.feature.tag.data.TagListDto
import com.xiaohelab.guard.android.feature.tag.domain.BindTagUseCase
import com.xiaohelab.guard.android.feature.tag.domain.ListTagsUseCase
import com.xiaohelab.guard.android.feature.tag.domain.TagRepository
import com.xiaohelab.guard.android.feature.tag.domain.UnbindTagUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Tag domain UseCases.
 * HC-02: 验证状态由服务端返回，客户端不推算。
 * HC-ID-String: tag_code / patient_id 均为 String。
 */
class TagUseCaseTest {

    private val repo: TagRepository = mockk()
    private val listTagsUseCase = ListTagsUseCase(repo)
    private val bindTagUseCase = BindTagUseCase(repo)
    private val unbindTagUseCase = UnbindTagUseCase(repo)

    @Test
    fun `listTags returns tag list from repository`() = runTest {
        val expected = TagListDto(
            items = listOf(
                TagDto(
                    tagCode = "TAG001",          // HC-ID-String: String
                    deviceType = "BLE",
                    alias = "老爸的手环",
                    state = "BOUND",             // HC-02: state 由服务端返回
                    boundAt = "2024-01-01T00:00:00Z",
                    patientId = "patient_001",  // HC-ID-String: String
                )
            )
        )
        coEvery { repo.listTags("patient_001") } returns MhResult.Success(expected, null)

        val result = listTagsUseCase("patient_001")

        assertTrue(result is MhResult.Success)
        val data = (result as MhResult.Success).data
        assertEquals(1, data.items.size)
        assertEquals("TAG001", data.items[0].tagCode)
        // HC-02: state 直接来自服务端，客户端不推算
        assertEquals("BOUND", data.items[0].state)
        coVerify(exactly = 1) { repo.listTags("patient_001") }
    }

    @Test
    fun `bindTag delegates to repository with correct params`() = runTest {
        val dto = TagDto("TAG002", "NFC", "手表", "BINDING", null, "patient_002")
        coEvery { repo.bindTag("patient_002", "TAG002", "NFC", "手表") } returns MhResult.Success(dto, null)

        val result = bindTagUseCase("patient_002", "TAG002", "NFC", "手表")

        assertTrue(result is MhResult.Success)
        assertEquals("TAG002", (result as MhResult.Success).data.tagCode)
        coVerify { repo.bindTag("patient_002", "TAG002", "NFC", "手表") }
    }

    @Test
    fun `bindTag with null alias delegates correctly`() = runTest {
        val dto = TagDto("TAG003", "BLE", null, "BINDING", null, "patient_003")
        coEvery { repo.bindTag("patient_003", "TAG003", "BLE", null) } returns MhResult.Success(dto, null)

        val result = bindTagUseCase("patient_003", "TAG003", "BLE", null)

        assertTrue(result is MhResult.Success)
        coVerify { repo.bindTag("patient_003", "TAG003", "BLE", null) }
    }

    @Test
    fun `unbindTag returns success on success`() = runTest {
        coEvery { repo.unbindTag("patient_001", "TAG001") } returns MhResult.Success(Unit, null)

        val result = unbindTagUseCase("patient_001", "TAG001")

        assertTrue(result is MhResult.Success)
        coVerify { repo.unbindTag("patient_001", "TAG001") }
    }

    @Test
    fun `listTags propagates failure from repository`() = runTest {
        val error = com.xiaohelab.guard.android.core.common.DomainException("E_TAG_4041", "Tag not found")
        coEvery { repo.listTags("patient_999") } returns MhResult.Failure(error)

        val result = listTagsUseCase("patient_999")

        assertTrue(result is MhResult.Failure)
        assertEquals("E_TAG_4041", (result as MhResult.Failure).error.code)
    }
}

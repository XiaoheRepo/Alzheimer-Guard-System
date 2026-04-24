package com.xiaohelab.guard.android.feature.me.domain

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.network.handleEnvelope
import com.xiaohelab.guard.android.feature.auth.data.UserProfileDto
import com.xiaohelab.guard.android.feature.me.data.ChangePasswordRequest
import com.xiaohelab.guard.android.feature.me.data.UserApi
import javax.inject.Inject

interface UserRepository {
    suspend fun me(): MhResult<UserProfileDto>
    suspend fun changePassword(oldPassword: String, newPassword: String): MhResult<Unit>
}

class UserRepositoryImpl @Inject constructor(private val api: UserApi) : UserRepository {
    override suspend fun me(): MhResult<UserProfileDto> = handleEnvelope { api.me() }

    override suspend fun changePassword(oldPassword: String, newPassword: String): MhResult<Unit> =
        when (val r = handleEnvelope { api.changePassword(ChangePasswordRequest(oldPassword, newPassword)) }) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
}

class GetMeUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(): MhResult<UserProfileDto> = repo.me()
}

/** MH-ME-03 修改密码。仅校验复杂度，不发送任何 SMS（HC-06）。 */
class ChangePasswordUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(oldPassword: String, newPassword: String): MhResult<Unit> =
        repo.changePassword(oldPassword, newPassword)
}

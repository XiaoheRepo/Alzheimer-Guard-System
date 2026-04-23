package com.xiaohelab.guard.android.feature.me.domain

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.network.handleEnvelope
import com.xiaohelab.guard.android.feature.auth.data.UserProfileDto
import com.xiaohelab.guard.android.feature.me.data.UserApi
import javax.inject.Inject

interface UserRepository {
    suspend fun me(): MhResult<UserProfileDto>
}

class UserRepositoryImpl @Inject constructor(private val api: UserApi) : UserRepository {
    override suspend fun me(): MhResult<UserProfileDto> = handleEnvelope { api.me() }
}

class GetMeUseCase @Inject constructor(private val repo: UserRepository) {
    suspend operator fun invoke(): MhResult<UserProfileDto> = repo.me()
}

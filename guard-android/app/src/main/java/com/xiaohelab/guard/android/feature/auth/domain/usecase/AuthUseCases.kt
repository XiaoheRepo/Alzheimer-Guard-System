package com.xiaohelab.guard.android.feature.auth.domain.usecase

import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.feature.auth.data.UserProfileDto
import com.xiaohelab.guard.android.feature.auth.domain.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(username: String, password: String): MhResult<UserProfileDto> =
        repo.login(username.trim(), password)
}

class RegisterUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(username: String, email: String, phone: String, password: String, nickname: String?): MhResult<Unit> =
        repo.register(username.trim(), email.trim(), phone.trim(), password, nickname?.trim())
}

class LogoutUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(): MhResult<Unit> = repo.logout()
}

class RequestPasswordResetUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(email: String, locale: String?): MhResult<Unit> = repo.requestPasswordReset(email.trim(), locale)
}

class ConfirmPasswordResetUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(token: String, newPassword: String): MhResult<Unit> = repo.confirmPasswordReset(token.trim(), newPassword)
}

package com.xiaohelab.guard.android.feature.auth.data

import com.xiaohelab.guard.android.core.auth.AuthSession
import com.xiaohelab.guard.android.core.auth.AuthTokenStore
import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.network.TokenRefresher
import com.xiaohelab.guard.android.core.network.handleEnvelope
import com.xiaohelab.guard.android.core.network.handleEnvelopeUnit
import com.xiaohelab.guard.android.feature.auth.domain.AuthRepository
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: AuthTokenStore,
) : AuthRepository, TokenRefresher {

    override suspend fun login(username: String, password: String): MhResult<UserProfileDto> {
        val result = handleEnvelope { api.login(LoginRequest(username, password)) }
        return when (result) {
            is MhResult.Success -> {
                val body = result.data
                // HC-Auth: 家属端仅接受 FAMILY 角色，否则拒绝登录。
                if (body.user.role != AuthSession.ROLE_FAMILY) {
                    tokenStore.clear()
                    MhResult.Failure(DomainException(
                        code = "E_AUTH_4012",
                        message = "Role ${body.user.role} not allowed on family app",
                        traceId = result.trace,
                        httpStatus = 403,
                    ))
                } else {
                    tokenStore.save(AuthSession(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        userId = body.user.userId,
                        role = body.user.role,
                        expiresAtEpochSeconds = System.currentTimeMillis() / 1000 + body.expiresIn,
                    ))
                    MhResult.Success(body.user, result.trace)
                }
            }
            is MhResult.Failure -> result
        }
    }

    override suspend fun register(username: String, email: String, password: String, nickname: String?): MhResult<Unit> {
        val r = handleEnvelope { api.register(RegisterRequest(username, email, password, nickname)) }
        return when (r) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
    }

    override suspend fun logout(): MhResult<Unit> {
        val refresh = tokenStore.refreshToken()
        val body = LogoutRequest(refreshToken = refresh, requestTime = Clock.System.now().toString())
        val r = handleEnvelope { api.logout(body) }
        tokenStore.clear()
        return when (r) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
    }

    override suspend fun requestPasswordReset(email: String, locale: String?): MhResult<Unit> =
        handleEnvelopeUnitMapped { api.requestPasswordReset(PasswordResetRequest(email, locale)) }

    override suspend fun confirmPasswordReset(token: String, newPassword: String): MhResult<Unit> =
        handleEnvelopeUnitMapped { api.confirmPasswordReset(PasswordResetConfirm(token, newPassword)) }

    // --- TokenRefresher (injected into OkHttp Authenticator) ---
    override suspend fun refreshOnce(): String? {
        val refresh = tokenStore.refreshToken() ?: return null
        val r = handleEnvelope { api.refresh(RefreshRequest(refresh)) }
        return when (r) {
            is MhResult.Success -> {
                tokenStore.updateTokens(
                    accessToken = r.data.accessToken,
                    refreshToken = r.data.refreshToken,
                    expiresInSeconds = r.data.expiresIn,
                )
                r.data.accessToken
            }
            is MhResult.Failure -> null
        }
    }

    private inline fun handleEnvelopeUnitMapped(block: () -> retrofit2.Response<com.xiaohelab.guard.android.core.network.ApiEnvelope<Unit>>): MhResult<Unit> {
        return when (val r = handleEnvelope(block)) {
            is MhResult.Success -> MhResult.Success(Unit, r.trace)
            is MhResult.Failure -> r
        }
    }
}

package com.xiaohelab.guard.android.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val phone: String = "",
    val role: String = "GUARDIAN",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface RegisterUiEffect {
    object NavigateToLogin : RegisterUiEffect
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state

    private val _effect = MutableSharedFlow<RegisterUiEffect>()
    val effect: SharedFlow<RegisterUiEffect> = _effect

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onConfirmPasswordChange(v: String) = _state.update { it.copy(confirmPassword = v, error = null) }
    fun onPhoneChange(v: String) = _state.update { it.copy(phone = v, error = null) }
    fun onRoleChange(v: String) = _state.update { it.copy(role = v) }

    fun register() {
        val s = _state.value
        when {
            s.username.isBlank() -> _state.update { it.copy(error = "用户名不能为空") }
            s.password.length < 6 -> _state.update { it.copy(error = "密码至少6位") }
            s.password != s.confirmPassword -> _state.update { it.copy(error = "两次密码不一致") }
            else -> viewModelScope.launch {
                _state.update { it.copy(loading = true, error = null) }
                when (val result = authRepository.register(
                    s.username.trim(), s.password, s.phone.trim(), s.role
                )) {
                    is ApiResult.Success -> _effect.emit(RegisterUiEffect.NavigateToLogin)
                    is ApiResult.Failure -> _state.update {
                        it.copy(loading = false, error = result.message)
                    }
                }
            }
        }
    }
}

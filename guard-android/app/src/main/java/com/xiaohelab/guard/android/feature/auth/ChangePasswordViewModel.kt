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

data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmNew: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface ChangePasswordUiEffect {
    object Success : ChangePasswordUiEffect
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state

    private val _effect = MutableSharedFlow<ChangePasswordUiEffect>()
    val effect: SharedFlow<ChangePasswordUiEffect> = _effect

    fun onOldPasswordChange(v: String) = _state.update { it.copy(oldPassword = v, error = null) }
    fun onNewPasswordChange(v: String) = _state.update { it.copy(newPassword = v, error = null) }
    fun onConfirmNewChange(v: String) = _state.update { it.copy(confirmNew = v, error = null) }

    fun submit() {
        val s = _state.value
        when {
            s.oldPassword.isBlank() -> _state.update { it.copy(error = "请输入原密码") }
            s.newPassword.length < 6 -> _state.update { it.copy(error = "新密码至少6位") }
            s.newPassword != s.confirmNew -> _state.update { it.copy(error = "两次新密码不一致") }
            else -> viewModelScope.launch {
                _state.update { it.copy(loading = true, error = null) }
                when (val result = authRepository.changePassword(s.oldPassword, s.newPassword)) {
                    is ApiResult.Success -> _effect.emit(ChangePasswordUiEffect.Success)
                    is ApiResult.Failure -> _state.update {
                        it.copy(loading = false, error = result.message)
                    }
                }
            }
        }
    }
}

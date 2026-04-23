package com.xiaohelab.guard.android.feature.auth.ui.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.feature.auth.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }

    fun submit() {
        if (_state.value.loading) return
        val u = _state.value.username.trim()
        val p = _state.value.password
        if (u.isBlank() || p.isBlank()) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = loginUseCase(u, p)) {
                is MhResult.Success -> _state.update { it.copy(loading = false, success = true) }
                is MhResult.Failure -> _state.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    fun consumeError() = _state.update { it.copy(error = null) }
}

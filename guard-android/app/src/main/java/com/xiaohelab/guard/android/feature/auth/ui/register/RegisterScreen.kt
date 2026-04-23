@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.auth.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhPrimaryButton
import com.xiaohelab.guard.android.feature.auth.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val nickname: String = "",
    val loading: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(private val useCase: RegisterUseCase) : ViewModel() {
    private val _s = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _s.asStateFlow()
    fun onUsername(v: String) = _s.update { it.copy(username = v, error = null) }
    fun onEmail(v: String) = _s.update { it.copy(email = v, error = null) }
    fun onPassword(v: String) = _s.update { it.copy(password = v, error = null) }
    fun onNickname(v: String) = _s.update { it.copy(nickname = v, error = null) }
    fun submit() {
        val s = _s.value
        if (s.loading || s.username.isBlank() || s.email.isBlank() || s.password.isBlank()) return
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = useCase(s.username, s.email, s.password, s.nickname.ifBlank { null })) {
                is MhResult.Success -> _s.update { it.copy(loading = false, success = true) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

@Composable
fun RegisterScreen(onDone: () -> Unit, vm: RegisterViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    LaunchedEffect(state.success) { if (state.success) onDone() }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.auth_register_title)) }) }) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(state.username, vm::onUsername, label = { Text(stringResource(R.string.auth_field_username)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.email, vm::onEmail, label = { Text(stringResource(R.string.auth_field_email)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.password, vm::onPassword, label = { Text(stringResource(R.string.auth_field_password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.nickname, vm::onNickname, label = { Text(stringResource(R.string.auth_field_nickname)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            state.error?.let { e -> Text(ErrorMessageMapper.message(ctx, e), color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            MhPrimaryButton(
                text = stringResource(R.string.auth_register_submit),
                contentDesc = stringResource(R.string.auth_register_submit),
                onClick = vm::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.loading,
            )
        }
    }
}

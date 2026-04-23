@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.auth.ui.reset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.xiaohelab.guard.android.feature.auth.domain.usecase.ConfirmPasswordResetUseCase
import com.xiaohelab.guard.android.feature.auth.domain.usecase.RequestPasswordResetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// ---------- MH-AUTH-03 Reset Request (email link; HC-06 no SMS) ----------

data class ResetRequestUiState(
    val email: String = "",
    val loading: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class ResetRequestViewModel @Inject constructor(private val useCase: RequestPasswordResetUseCase) : ViewModel() {
    private val _s = MutableStateFlow(ResetRequestUiState())
    val state: StateFlow<ResetRequestUiState> = _s.asStateFlow()
    fun onEmail(v: String) = _s.update { it.copy(email = v, error = null) }
    fun submit() {
        val s = _s.value
        if (s.loading || s.email.isBlank()) return
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val locale = Locale.getDefault().toLanguageTag()
            when (val r = useCase(s.email, locale)) {
                is MhResult.Success -> _s.update { it.copy(loading = false, success = true) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

@Composable
fun ResetRequestScreen(
    onSubmitted: () -> Unit,
    onBack: () -> Unit,
    vm: ResetRequestViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    LaunchedEffect(state.success) { if (state.success) onSubmitted() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.auth_reset_request_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.auth_reset_request_hint))
            OutlinedTextField(
                state.email, vm::onEmail,
                label = { Text(stringResource(R.string.auth_field_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { e -> Text(ErrorMessageMapper.message(ctx, e), color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            MhPrimaryButton(
                text = stringResource(R.string.auth_reset_request_submit),
                contentDesc = stringResource(R.string.auth_reset_request_submit),
                onClick = vm::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.loading,
            )
        }
    }
}

// ---------- MH-AUTH-04 Reset Confirm ----------

data class ResetConfirmUiState(
    val token: String = "",
    val newPassword: String = "",
    val loading: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class ResetConfirmViewModel @Inject constructor(private val useCase: ConfirmPasswordResetUseCase) : ViewModel() {
    private val _s = MutableStateFlow(ResetConfirmUiState())
    val state: StateFlow<ResetConfirmUiState> = _s.asStateFlow()
    fun onToken(v: String) = _s.update { it.copy(token = v, error = null) }
    fun onPassword(v: String) = _s.update { it.copy(newPassword = v, error = null) }
    fun submit() {
        val s = _s.value
        if (s.loading || s.token.isBlank() || s.newPassword.isBlank()) return
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = useCase(s.token, s.newPassword)) {
                is MhResult.Success -> _s.update { it.copy(loading = false, success = true) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

@Composable
fun ResetConfirmScreen(onDone: () -> Unit, vm: ResetConfirmViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    LaunchedEffect(state.success) { if (state.success) onDone() }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.auth_reset_confirm_title)) }) }) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.auth_reset_confirm_hint))
            OutlinedTextField(state.token, vm::onToken, label = { Text(stringResource(R.string.auth_field_reset_token)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                state.newPassword, vm::onPassword,
                label = { Text(stringResource(R.string.auth_field_new_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { e -> Text(ErrorMessageMapper.message(ctx, e), color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            MhPrimaryButton(
                text = stringResource(R.string.auth_reset_confirm_submit),
                contentDesc = stringResource(R.string.auth_reset_confirm_submit),
                onClick = vm::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.loading,
            )
        }
    }
}

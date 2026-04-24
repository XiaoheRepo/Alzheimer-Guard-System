@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.me.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.xiaohelab.guard.android.feature.me.domain.ChangePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MH-ME-03 修改密码（handbook §12 / API V2.0 §users.me.password）。
 * HC-06: 不涉及 SMS 验证码；HC-07: 页面涉及敏感输入，MainActivity 已全局 FLAG_SECURE。
 * 复杂度规则（handbook §12）：长度 ≥ 10，且至少含有大写、小写、数字各一。
 */
data class ChangePasswordUiState(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val submitting: Boolean = false,
    val error: DomainException? = null,
    val localError: String? = null,
    val done: Boolean = false,
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val changePassword: ChangePasswordUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _s.asStateFlow()

    fun onOld(v: String) = _s.update { it.copy(oldPassword = v, localError = null, error = null) }
    fun onNew(v: String) = _s.update { it.copy(newPassword = v, localError = null, error = null) }
    fun onConfirm(v: String) = _s.update { it.copy(confirmPassword = v, localError = null, error = null) }

    fun submit() {
        val cur = _s.value
        val localError = when {
            cur.oldPassword.isBlank() -> "empty_old"
            !isStrong(cur.newPassword) -> "weak"
            cur.newPassword != cur.confirmPassword -> "mismatch"
            else -> null
        }
        if (localError != null) {
            _s.update { it.copy(localError = localError) }
            return
        }
        _s.update { it.copy(submitting = true, error = null, localError = null) }
        viewModelScope.launch {
            when (val r = changePassword(cur.oldPassword, cur.newPassword)) {
                is MhResult.Success -> _s.update { it.copy(submitting = false, done = true) }
                is MhResult.Failure -> _s.update { it.copy(submitting = false, error = r.error) }
            }
        }
    }

    private fun isStrong(pwd: String): Boolean =
        pwd.length >= 10 &&
            pwd.any { it.isUpperCase() } &&
            pwd.any { it.isLowerCase() } &&
            pwd.any { it.isDigit() }
}

@Composable
fun ChangePasswordScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: ChangePasswordViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(s.done) { if (s.done) onDone() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.change_password_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
            },
        )
    }) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = s.oldPassword,
                onValueChange = vm::onOld,
                label = { Text(stringResource(R.string.change_password_old)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = s.newPassword,
                onValueChange = vm::onNew,
                label = { Text(stringResource(R.string.change_password_new)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(stringResource(R.string.change_password_hint)) },
            )
            OutlinedTextField(
                value = s.confirmPassword,
                onValueChange = vm::onConfirm,
                label = { Text(stringResource(R.string.change_password_confirm)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            s.localError?.let { key ->
                val msg = when (key) {
                    "empty_old" -> stringResource(R.string.change_password_err_empty_old)
                    "weak" -> stringResource(R.string.change_password_err_weak)
                    "mismatch" -> stringResource(R.string.change_password_err_mismatch)
                    else -> key
                }
                Text(msg, color = MaterialTheme.colorScheme.error)
            }
            s.error?.let { e ->
                Text(ErrorMessageMapper.message(ctx, e), color = MaterialTheme.colorScheme.error)
            }
            MhPrimaryButton(
                text = stringResource(R.string.change_password_submit),
                contentDesc = stringResource(R.string.change_password_submit),
                enabled = !s.submitting,
                onClick = vm::submit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

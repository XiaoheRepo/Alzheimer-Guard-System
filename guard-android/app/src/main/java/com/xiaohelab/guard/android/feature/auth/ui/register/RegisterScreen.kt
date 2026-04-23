@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.auth.ui.register

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

/** Registration step: 1 = account identity, 2 = credentials, 3 = success/email-sent */
enum class RegisterStep { IDENTITY, CREDENTIALS, EMAIL_SENT }

data class RegisterUiState(
    val step: RegisterStep = RegisterStep.IDENTITY,
    val username: String = "",
    val nickname: String = "",
    /** API V2.0 §3.6.1: 家属注册必填，中国大陆手机号。 */
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val loading: Boolean = false,
    val error: DomainException? = null,
    /** Client-side validation message (e.g. passwords don't match) */
    val localError: String? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(private val useCase: RegisterUseCase) : ViewModel() {
    private val _s = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _s.asStateFlow()

    fun onUsername(v: String) = _s.update { it.copy(username = v, error = null, localError = null) }
    fun onNickname(v: String) = _s.update { it.copy(nickname = v, error = null, localError = null) }
    fun onPhone(v: String) = _s.update { it.copy(phone = v.filter { ch -> ch.isDigit() }.take(11), error = null, localError = null) }
    fun onEmail(v: String) = _s.update { it.copy(email = v, error = null, localError = null) }
    fun onPassword(v: String) = _s.update { it.copy(password = v, error = null, localError = null) }
    fun onPasswordConfirm(v: String) = _s.update { it.copy(passwordConfirm = v, error = null, localError = null) }

    /** 对齐服务端正则 `^1[3-9]\d{9}$`。 */
    private fun isPhoneValid(p: String): Boolean = p.matches(Regex("^1[3-9]\\d{9}$"))

    /** Advance from step 1 (IDENTITY) to step 2 (CREDENTIALS) with client-side validation. */
    fun nextStep(passwordsMismatchMsg: String, invalidPhoneMsg: String) {
        val s = _s.value
        if (s.username.isBlank()) return
        if (!isPhoneValid(s.phone)) {
            _s.update { it.copy(localError = invalidPhoneMsg) }
            return
        }
        _s.update { it.copy(step = RegisterStep.CREDENTIALS, localError = null) }
    }

    fun back() {
        val s = _s.value
        if (s.step == RegisterStep.CREDENTIALS) _s.update { it.copy(step = RegisterStep.IDENTITY, localError = null, error = null) }
    }

    fun submit(passwordsMismatchMsg: String) {
        val s = _s.value
        if (s.loading || s.email.isBlank() || s.password.isBlank() || !isPhoneValid(s.phone)) return
        if (s.password != s.passwordConfirm) {
            _s.update { it.copy(localError = passwordsMismatchMsg) }
            return
        }
        _s.update { it.copy(loading = true, error = null, localError = null) }
        viewModelScope.launch {
            when (val r = useCase(s.username, s.email, s.phone, s.password, s.nickname.ifBlank { null })) {
                is MhResult.Success -> _s.update { it.copy(loading = false, step = RegisterStep.EMAIL_SENT) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

/** MH-REG 注册页（两步 + 邮件提示）。*/
@Composable
fun RegisterScreen(
    onDone: () -> Unit,
    vm: RegisterViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val passwordsMismatchMsg = stringResource(R.string.auth_register_passwords_mismatch)
    val invalidPhoneMsg = stringResource(R.string.auth_register_phone_invalid)

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    when (state.step) {
                        RegisterStep.IDENTITY -> stringResource(R.string.auth_register_step1_title)
                        RegisterStep.CREDENTIALS -> stringResource(R.string.auth_register_step2_title)
                        RegisterStep.EMAIL_SENT -> stringResource(R.string.auth_register_email_sent_title)
                    }
                )
            },
            navigationIcon = {
                when (state.step) {
                    RegisterStep.CREDENTIALS -> IconButton(onClick = { vm.back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                    else -> {}
                }
            },
        )
    }) { pad ->
        Column(modifier = Modifier.padding(pad)) {
            // Step progress indicator (steps 1 & 2 only)
            if (state.step != RegisterStep.EMAIL_SENT) {
                LinearProgressIndicator(
                    progress = { if (state.step == RegisterStep.IDENTITY) 0.5f else 1.0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal)
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    else
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                },
                label = "register_step",
            ) { step ->
                when (step) {
                    RegisterStep.IDENTITY -> StepIdentity(state, vm, ctx, invalidPhoneMsg)
                    RegisterStep.CREDENTIALS -> StepCredentials(state, vm, ctx, passwordsMismatchMsg)
                    RegisterStep.EMAIL_SENT -> StepEmailSent(state.email, onDone)
                }
            }
        }
    }
}

@Composable
private fun StepIdentity(
    state: RegisterUiState,
    vm: RegisterViewModel,
    ctx: android.content.Context,
    invalidPhoneMsg: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.auth_register_step1_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.username,
            onValueChange = vm::onUsername,
            label = { Text(stringResource(R.string.auth_field_username)) },
            supportingText = { Text(stringResource(R.string.auth_field_username_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.nickname,
            onValueChange = vm::onNickname,
            label = { Text(stringResource(R.string.auth_field_nickname)) },
            supportingText = { Text(stringResource(R.string.auth_field_nickname_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        // 手机号（API V2.0 §3.6.1 必填）
        OutlinedTextField(
            value = state.phone,
            onValueChange = vm::onPhone,
            label = { Text(stringResource(R.string.auth_field_phone)) },
            supportingText = { Text(stringResource(R.string.auth_field_phone_hint)) },
            singleLine = true,
            isError = state.localError == invalidPhoneMsg,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        // 本地校验错误提示（如手机号格式错）
        val errorText = state.localError ?: state.error?.let { ErrorMessageMapper.message(ctx, it) }
        errorText?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(8.dp))
        MhPrimaryButton(
            text = stringResource(R.string.common_next),
            contentDesc = stringResource(R.string.common_next),
            onClick = { vm.nextStep(invalidPhoneMsg, invalidPhoneMsg) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.username.isNotBlank() && state.phone.length == 11,
        )
    }
}

@Composable
private fun StepCredentials(
    state: RegisterUiState,
    vm: RegisterViewModel,
    ctx: android.content.Context,
    passwordsMismatchMsg: String,
) {
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.auth_register_step2_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = vm::onEmail,
            label = { Text(stringResource(R.string.auth_field_email)) },
            supportingText = { Text(stringResource(R.string.auth_field_email_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = vm::onPassword,
            label = { Text(stringResource(R.string.auth_field_password)) },
            supportingText = { Text(stringResource(R.string.auth_field_password_hint)) },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(if (showPassword) R.string.auth_hide_password else R.string.auth_show_password),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.passwordConfirm,
            onValueChange = vm::onPasswordConfirm,
            label = { Text(stringResource(R.string.auth_field_password_confirm)) },
            singleLine = true,
            isError = state.localError != null,
            visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showConfirm = !showConfirm }) {
                    Icon(
                        if (showConfirm) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = stringResource(if (showConfirm) R.string.auth_hide_password else R.string.auth_show_password),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        // Show either local (client-side) or server error
        val errorText = state.localError ?: state.error?.let { ErrorMessageMapper.message(ctx, it) }
        errorText?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(8.dp))
        MhPrimaryButton(
            text = stringResource(R.string.auth_register_submit),
            contentDesc = stringResource(R.string.auth_register_submit),
            onClick = { vm.submit(passwordsMismatchMsg) },
            modifier = Modifier.fillMaxWidth(),
            loading = state.loading,
            enabled = state.email.isNotBlank() && state.password.isNotBlank() && state.passwordConfirm.isNotBlank(),
        )
    }
}

/** AC-15：注册成功后提示用户查收验证邮件（邮件链接激活账号后方可登录）。*/
@Composable
private fun StepEmailSent(email: String, onGoLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✉️", style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.auth_register_email_sent_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.auth_register_email_sent_body, email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.auth_register_email_sent_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        MhPrimaryButton(
            text = stringResource(R.string.auth_go_login),
            contentDesc = stringResource(R.string.auth_go_login),
            onClick = onGoLogin,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}


package com.xiaohelab.guard.android.feature.auth.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhPrimaryButton
import androidx.compose.foundation.text.KeyboardOptions

/**
 * MH-AUTH-01 登录页（handbook §16.1）。
 * 字段：username / password；HC-06 不允许短信登录；跳转注册、找回密码。
 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNavRegister: () -> Unit,
    onNavResetRequest: () -> Unit,
    vm: LoginViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(state.success) { if (state.success) onLoggedIn() }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.auth_login_title)) }) }) { pad ->
        Column(
            modifier = Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.username,
                onValueChange = vm::onUsernameChange,
                label = { Text(stringResource(R.string.auth_field_username)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = vm::onPasswordChange,
                label = { Text(stringResource(R.string.auth_field_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let { e ->
                Text(
                    text = ErrorMessageMapper.message(ctx, e),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(8.dp))
            MhPrimaryButton(
                text = stringResource(R.string.auth_login_submit),
                contentDesc = stringResource(R.string.auth_login_submit),
                onClick = vm::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.loading,
            )
            TextButton(onClick = onNavRegister) { Text(stringResource(R.string.auth_go_register)) }
            TextButton(onClick = onNavResetRequest) { Text(stringResource(R.string.auth_go_reset)) }
        }
    }
}

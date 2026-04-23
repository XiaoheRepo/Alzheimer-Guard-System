package com.xiaohelab.guard.android.feature.me.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhPrimaryButton
import com.xiaohelab.guard.android.feature.auth.data.UserProfileDto
import com.xiaohelab.guard.android.feature.auth.domain.usecase.LogoutUseCase
import com.xiaohelab.guard.android.feature.me.domain.GetMeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeUiState(
    val loading: Boolean = true,
    val user: UserProfileDto? = null,
    val error: DomainException? = null,
    val loggedOut: Boolean = false,
)

@HiltViewModel
class MeViewModel @Inject constructor(
    private val getMe: GetMeUseCase,
    private val logout: LogoutUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(MeUiState())
    val state: StateFlow<MeUiState> = _s.asStateFlow()

    init { refresh() }

    fun refresh() {
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = getMe()) {
                is MhResult.Success -> _s.update { it.copy(loading = false, user = r.data) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    fun doLogout() {
        viewModelScope.launch {
            logout()
            _s.update { it.copy(loggedOut = true) }
        }
    }
}

/** MH-ME-01 我的页（handbook §16 / §17 ME）. */
@Composable
fun MeScreen(
    onSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    onBack: () -> Unit,
    vm: MeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current
    LaunchedEffect(s.loggedOut) { if (s.loggedOut) onLoggedOut() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.me_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            s.user?.let { u ->
                ListItem(
                    headlineContent = { Text(u.nickname ?: u.username) },
                    supportingContent = { Text(u.email ?: u.userId) },
                )
                HorizontalDivider()
            }
            s.error?.let { e ->
                Text(ErrorMessageMapper.message(ctx, e), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.me_settings)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
            MhPrimaryButton(
                text = stringResource(R.string.me_settings),
                contentDesc = stringResource(R.string.me_settings),
                onClick = onSettings,
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
            )
            MhPrimaryButton(
                text = stringResource(R.string.me_logout),
                contentDesc = stringResource(R.string.me_logout),
                onClick = vm::doLogout,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            )
        }
    }
}

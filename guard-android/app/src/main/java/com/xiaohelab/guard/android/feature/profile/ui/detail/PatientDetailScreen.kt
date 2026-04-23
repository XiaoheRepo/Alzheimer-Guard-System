@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.profile.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.eventbus.AppEvent
import com.xiaohelab.guard.android.core.eventbus.AppEventBus
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhLoading
import com.xiaohelab.guard.android.core.ui.components.MhPrimaryButton
import com.xiaohelab.guard.android.feature.profile.data.PatientDto
import com.xiaohelab.guard.android.feature.profile.domain.GetPatientDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientDetailUiState(
    val loading: Boolean = true,
    val patient: PatientDto? = null,
    val error: DomainException? = null,
)

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val useCase: GetPatientDetailUseCase,
    private val eventBus: AppEventBus,
) : ViewModel() {
    private val _s = MutableStateFlow(PatientDetailUiState())
    val state: StateFlow<PatientDetailUiState> = _s.asStateFlow()

    fun refresh(id: String) {
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = useCase(id)) {
                is MhResult.Success -> _s.update { it.copy(loading = false, patient = r.data) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    /** HC-02: WS `state.changed` 到达时强制刷新，不本地推算状态。 */
    fun observeWsChanges(id: String) {
        viewModelScope.launch {
            eventBus.events.collect { ev ->
                if (ev is AppEvent.StateChanged && ev.aggregateId == id) refresh(id)
            }
        }
    }
}

/** MH-PAT-01 详情页。 */
@Composable
fun PatientDetailScreen(
    patientId: String,
    onEdit: () -> Unit,
    onGuardians: () -> Unit,
    onBack: () -> Unit,
    vm: PatientDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(patientId) {
        vm.refresh(patientId)
        vm.observeWsChanges(patientId)
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.patient_detail_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                state.loading -> MhLoading()
                state.error != null -> Text(ErrorMessageMapper.message(ctx, state.error!!), color = MaterialTheme.colorScheme.error)
                state.patient != null -> {
                    val p = state.patient!!
                    ListItem(headlineContent = { Text(p.name) }, supportingContent = { Text(p.patientId) })
                    HorizontalDivider()
                    p.gender?.let { ListItem(headlineContent = { Text(stringResource(R.string.patient_field_gender)) }, supportingContent = { Text(it) }) }
                    p.birthDate?.let { ListItem(headlineContent = { Text(stringResource(R.string.patient_field_birth_date)) }, supportingContent = { Text(it) }) }
                    p.medicalNotes?.let { ListItem(headlineContent = { Text(stringResource(R.string.patient_field_medical_notes)) }, supportingContent = { Text(it) }) }
                    HorizontalDivider()
                    MhPrimaryButton(
                        text = stringResource(R.string.patient_edit),
                        contentDesc = stringResource(R.string.patient_edit),
                        onClick = onEdit,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    MhPrimaryButton(
                        text = stringResource(R.string.guardian_manage),
                        contentDesc = stringResource(R.string.guardian_manage),
                        onClick = onGuardians,
                    )
                }
            }
        }
    }
}

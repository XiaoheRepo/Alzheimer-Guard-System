@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.profile.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.common.DomainException
import com.xiaohelab.guard.android.core.common.MhResult
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhEmpty
import com.xiaohelab.guard.android.core.ui.components.MhLoading
import com.xiaohelab.guard.android.feature.profile.data.PatientDto
import com.xiaohelab.guard.android.feature.profile.domain.ListPatientsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientListUiState(
    val loading: Boolean = true,
    val patients: List<PatientDto> = emptyList(),
    val error: DomainException? = null,
)

@HiltViewModel
class PatientListViewModel @Inject constructor(private val useCase: ListPatientsUseCase) : ViewModel() {
    private val _s = MutableStateFlow(PatientListUiState())
    val state: StateFlow<PatientListUiState> = _s.asStateFlow()
    init { refresh() }
    fun refresh() {
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = useCase()) {
                is MhResult.Success -> _s.update { it.copy(loading = false, patients = r.data.items) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

/** MH-HOME / MH-PAT-00 患者列表页。 */
@Composable
fun PatientListScreen(
    onPatientClick: (String) -> Unit,
    onCreate: () -> Unit,
    onMe: () -> Unit,
    vm: PatientListViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.patient_list_title)) },
                actions = {
                    IconButton(onClick = onMe) { Icon(Icons.Filled.Person, null) }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) { Icon(Icons.Filled.Add, null) }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            when {
                state.loading -> MhLoading()
                state.error != null -> Text(
                    ErrorMessageMapper.message(ctx, state.error!!),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
                state.patients.isEmpty() -> MhEmpty(stringResource(R.string.patient_list_empty))
                else -> LazyColumn {
                    items(state.patients, key = { it.patientId }) { p ->
                        ListItem(
                            headlineContent = { Text(p.name) },
                            supportingContent = { Text(p.patientId) },
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onPatientClick(p.patientId) }
                                .padding(horizontal = 4.dp),
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

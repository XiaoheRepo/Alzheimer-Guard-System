@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.profile.ui.edit

import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.DisposableEffect
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
import com.xiaohelab.guard.android.feature.profile.domain.CreatePatientUseCase
import com.xiaohelab.guard.android.feature.profile.domain.GetPatientDetailUseCase
import com.xiaohelab.guard.android.feature.profile.domain.UpdatePatientProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PatientEditUiState(
    val id: String? = null,
    val name: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val medicalNotes: String = "",
    val version: Long = 0,
    val loading: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class PatientEditViewModel @Inject constructor(
    private val getDetail: GetPatientDetailUseCase,
    private val createUseCase: CreatePatientUseCase,
    private val updateUseCase: UpdatePatientProfileUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(PatientEditUiState())
    val state: StateFlow<PatientEditUiState> = _s.asStateFlow()

    fun loadIfNeeded(id: String?) {
        if (id == null || _s.value.id == id) return
        _s.update { it.copy(id = id, loading = true) }
        viewModelScope.launch {
            when (val r = getDetail(id)) {
                is MhResult.Success -> _s.update {
                    it.copy(
                        loading = false,
                        name = r.data.name,
                        gender = r.data.gender.orEmpty(),
                        birthDate = r.data.birthDate.orEmpty(),
                        medicalNotes = r.data.medicalNotes.orEmpty(),
                        version = r.data.version ?: 0,
                    )
                }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
    fun onName(v: String) = _s.update { it.copy(name = v, error = null) }
    fun onGender(v: String) = _s.update { it.copy(gender = v, error = null) }
    fun onBirth(v: String) = _s.update { it.copy(birthDate = v, error = null) }
    fun onNotes(v: String) = _s.update { it.copy(medicalNotes = v, error = null) }

    fun submit() {
        val s = _s.value
        if (s.loading || s.name.isBlank()) return
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val r = if (s.id == null) {
                createUseCase(s.name, s.gender.ifBlank { null }, s.birthDate.ifBlank { null }, s.medicalNotes.ifBlank { null })
            } else {
                updateUseCase(s.id, s.name, s.gender.ifBlank { null }, s.birthDate.ifBlank { null }, s.medicalNotes.ifBlank { null }, s.version)
            }
            when (r) {
                is MhResult.Success -> _s.update { it.copy(loading = false, success = true) }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }
}

/** MH-PAT-02 创建/编辑页。HC-07 强制 FLAG_SECURE，禁止截屏。 */
@Composable
fun PatientEditScreen(
    patientId: String?,
    onDone: () -> Unit,
    vm: PatientEditViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val activity = (ctx as? android.app.Activity)
    DisposableEffect(Unit) {
        activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    LaunchedEffect(patientId) { vm.loadIfNeeded(patientId) }
    LaunchedEffect(state.success) { if (state.success) onDone() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(if (patientId == null) R.string.patient_create_title else R.string.patient_edit_title)) },
            navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
        )
    }) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(state.name, vm::onName, label = { Text(stringResource(R.string.patient_field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.gender, vm::onGender, label = { Text(stringResource(R.string.patient_field_gender)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.birthDate, vm::onBirth, label = { Text(stringResource(R.string.patient_field_birth_date)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.medicalNotes, vm::onNotes, label = { Text(stringResource(R.string.patient_field_medical_notes)) }, modifier = Modifier.fillMaxWidth())
            state.error?.let { e -> Text(ErrorMessageMapper.message(ctx, e), color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            MhPrimaryButton(
                text = stringResource(R.string.common_save),
                contentDesc = stringResource(R.string.common_save),
                onClick = vm::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = state.loading,
            )
        }
    }
}

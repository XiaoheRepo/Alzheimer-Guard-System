@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.profile.ui.edit

import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
import com.xiaohelab.guard.android.feature.profile.domain.UpdateAppearanceUseCase
import com.xiaohelab.guard.android.feature.profile.domain.UpdatePatientProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PatientEditStep { BASIC, APPEARANCE }

data class PatientEditUiState(
    val step: PatientEditStep = PatientEditStep.BASIC,
    val id: String? = null,
    val name: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val medicalNotes: String = "",
    val version: Long = 0,
    val height: String = "",
    val weight: String = "",
    val features: String = "",
    val loading: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
)

@HiltViewModel
class PatientEditViewModel @Inject constructor(
    private val getDetail: GetPatientDetailUseCase,
    private val createUseCase: CreatePatientUseCase,
    private val updateUseCase: UpdatePatientProfileUseCase,
    private val updateAppearanceUseCase: UpdateAppearanceUseCase,
) : ViewModel() {
    private val _s = MutableStateFlow(PatientEditUiState())
    val state: StateFlow<PatientEditUiState> = _s.asStateFlow()

    fun loadIfNeeded(id: String?) {
        if (id == null || _s.value.id == id) return
        _s.update { it.copy(id = id, loading = true) }
        viewModelScope.launch {
            when (val r = getDetail(id)) {
                is MhResult.Success -> _s.update {
                    val app = r.data.appearance
                    it.copy(
                        loading = false,
                        name = r.data.displayName,
                        gender = r.data.gender.orEmpty(),
                        birthDate = r.data.birthDate.orEmpty(),
                        medicalNotes = r.data.medicalNotes.orEmpty(),
                        version = r.data.version ?: 0,
                        height = app?.height?.toString().orEmpty(),
                        weight = app?.weight?.toString().orEmpty(),
                        features = app?.features.orEmpty(),
                    )
                }
                is MhResult.Failure -> _s.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    fun nextStep() {
        if (_s.value.name.isBlank()) return
        _s.update { it.copy(step = PatientEditStep.APPEARANCE, error = null) }
    }

    fun back() {
        if (_s.value.step == PatientEditStep.APPEARANCE)
            _s.update { it.copy(step = PatientEditStep.BASIC, error = null) }
    }

    fun onName(v: String) = _s.update { it.copy(name = v, error = null) }
    fun onGender(v: String) = _s.update { it.copy(gender = v, error = null) }
    fun onBirth(v: String) = _s.update { it.copy(birthDate = v, error = null) }
    fun onNotes(v: String) = _s.update { it.copy(medicalNotes = v, error = null) }
    fun onHeight(v: String) = _s.update { it.copy(height = v, error = null) }
    fun onWeight(v: String) = _s.update { it.copy(weight = v, error = null) }
    fun onFeatures(v: String) = _s.update { it.copy(features = v, error = null) }

    fun submit() {
        val s = _s.value
        if (s.loading || s.name.isBlank()) return
        _s.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val heightInt = s.height.trim().toIntOrNull()
            val weightInt = s.weight.trim().toIntOrNull()
            val featuresStr = s.features.trim().ifBlank { null }

            if (s.id == null) {
                when (val createResult = createUseCase(
                    s.name, s.gender.ifBlank { null }, s.birthDate.ifBlank { null }, s.medicalNotes.ifBlank { null }
                )) {
                    is MhResult.Failure -> _s.update { it.copy(loading = false, error = createResult.error) }
                    is MhResult.Success -> {
                        val patient = createResult.data
                        val hasAppearance = heightInt != null || weightInt != null || featuresStr != null
                        if (hasAppearance) {
                            when (val appResult = updateAppearanceUseCase(
                                patient.patientId, heightInt, weightInt, featuresStr, patient.version ?: 0
                            )) {
                                is MhResult.Failure -> _s.update { it.copy(loading = false, error = appResult.error) }
                                is MhResult.Success -> _s.update { it.copy(loading = false, success = true) }
                            }
                        } else {
                            _s.update { it.copy(loading = false, success = true) }
                        }
                    }
                }
            } else {
                when (val profileResult = updateUseCase(
                    s.id, s.name, s.gender.ifBlank { null }, s.birthDate.ifBlank { null }, s.medicalNotes.ifBlank { null }, s.version
                )) {
                    is MhResult.Failure -> _s.update { it.copy(loading = false, error = profileResult.error) }
                    is MhResult.Success -> {
                        val updatedVersion = profileResult.data.version ?: (s.version + 1)
                        when (val appResult = updateAppearanceUseCase(
                            s.id, heightInt, weightInt, featuresStr, updatedVersion
                        )) {
                            is MhResult.Failure -> _s.update { it.copy(loading = false, error = appResult.error) }
                            is MhResult.Success -> _s.update { it.copy(loading = false, success = true) }
                        }
                    }
                }
            }
        }
    }
}

/** MH-PAT-02 创建/编辑页（两步）。HC-07 强制 FLAG_SECURE，禁止截屏。 */
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
            title = {
                Text(
                    when (state.step) {
                        PatientEditStep.BASIC -> stringResource(
                            if (patientId == null) R.string.patient_create_title else R.string.patient_edit_step_basic_title
                        )
                        PatientEditStep.APPEARANCE -> stringResource(R.string.patient_edit_step_appearance_title)
                    }
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (state.step == PatientEditStep.APPEARANCE) vm.back() else onDone()
                    },
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                    )
                }
            },
        )
    }) { pad ->
        Column(modifier = Modifier.padding(pad)) {
            LinearProgressIndicator(
                progress = { if (state.step == PatientEditStep.BASIC) 0.5f else 1.0f },
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal)
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    else
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                },
                label = "patient_edit_step",
            ) { step ->
                when (step) {
                    PatientEditStep.BASIC -> StepBasic(state, vm, ctx)
                    PatientEditStep.APPEARANCE -> StepAppearance(state, vm, ctx)
                }
            }
        }
    }
}

@Composable
private fun StepBasic(
    state: PatientEditUiState,
    vm: PatientEditViewModel,
    ctx: android.content.Context,
) {
    val genderOptions = listOf(
        "MALE" to R.string.patient_field_gender_male,
        "FEMALE" to R.string.patient_field_gender_female,
        "UNKNOWN" to R.string.patient_field_gender_unknown,
    )
    var genderExpanded by remember { mutableStateOf(false) }
    val genderDisplay = genderOptions.firstOrNull { it.first == state.gender }
        ?.let { stringResource(it.second) }
        ?: state.gender

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.patient_edit_step_basic_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = vm::onName,
            label = { Text(stringResource(R.string.patient_field_name) + " *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = genderDisplay,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.patient_field_gender)) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.patient_field_gender_select),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false },
            ) {
                genderOptions.forEach { (value, labelRes) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(labelRes)) },
                        onClick = {
                            vm.onGender(value)
                            genderExpanded = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.birthDate,
            onValueChange = vm::onBirth,
            label = { Text(stringResource(R.string.patient_field_birth_date)) },
            supportingText = { Text(stringResource(R.string.patient_field_birth_date_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.medicalNotes,
            onValueChange = vm::onNotes,
            label = { Text(stringResource(R.string.patient_field_medical_notes)) },
            supportingText = { Text(stringResource(R.string.patient_field_medical_notes_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let { e ->
            Text(
                text = ErrorMessageMapper.message(ctx, e),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(8.dp))
        MhPrimaryButton(
            text = stringResource(R.string.common_next),
            contentDesc = stringResource(R.string.common_next),
            onClick = vm::nextStep,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.name.isNotBlank(),
        )
    }
}

@Composable
private fun StepAppearance(
    state: PatientEditUiState,
    vm: PatientEditViewModel,
    ctx: android.content.Context,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.patient_edit_step_appearance_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.height,
            onValueChange = vm::onHeight,
            label = { Text(stringResource(R.string.patient_field_height)) },
            supportingText = { Text(stringResource(R.string.patient_field_height_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.weight,
            onValueChange = vm::onWeight,
            label = { Text(stringResource(R.string.patient_field_weight)) },
            supportingText = { Text(stringResource(R.string.patient_field_weight_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.features,
            onValueChange = vm::onFeatures,
            label = { Text(stringResource(R.string.patient_field_features)) },
            supportingText = { Text(stringResource(R.string.patient_field_features_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let { e ->
            Text(
                text = ErrorMessageMapper.message(ctx, e),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(8.dp))
        MhPrimaryButton(
            text = stringResource(R.string.common_save),
            contentDesc = stringResource(R.string.common_save),
            onClick = vm::submit,
            modifier = Modifier.fillMaxWidth(),
            loading = state.loading,
            enabled = !state.loading,
        )
    }
}

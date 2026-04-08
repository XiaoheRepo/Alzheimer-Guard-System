package com.xiaohelab.guard.android.feature.patient

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaohelab.guard.android.core.common.ApiResult
import com.xiaohelab.guard.android.domain.model.Guardian
import com.xiaohelab.guard.android.domain.model.InviteGuardianRequest
import com.xiaohelab.guard.android.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GuardianUiState(
    val guardians: List<Guardian> = emptyList(),
    val invitePhone: String = "",
    val inviteRelation: String = "",
    val loading: Boolean = false,
    val error: String? = null
)

sealed interface GuardianUiEffect {
    data class ShowToast(val message: String) : GuardianUiEffect
}

@HiltViewModel
class GuardianViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle["patientId"])
    private val _state = MutableStateFlow(GuardianUiState())
    val state: StateFlow<GuardianUiState> = _state

    private val _effect = MutableSharedFlow<GuardianUiEffect>()
    val effect: SharedFlow<GuardianUiEffect> = _effect

    init { loadGuardians() }

    private fun loadGuardians() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = patientRepository.getGuardians(patientId)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, guardians = result.data) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun onInvitePhoneChange(v: String) = _state.update { it.copy(invitePhone = v) }
    fun onInviteRelationChange(v: String) = _state.update { it.copy(inviteRelation = v) }

    fun inviteGuardian() {
        val s = _state.value
        if (s.invitePhone.isBlank()) { _state.update { it.copy(error = "请输入被邀请人手机号") }; return }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val req = InviteGuardianRequest(s.invitePhone.trim(), s.inviteRelation.ifBlank { null })
            when (val result = patientRepository.inviteGuardian(patientId, req)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(loading = false, invitePhone = "", inviteRelation = "") }
                    _effect.emit(GuardianUiEffect.ShowToast("邀请已发送"))
                    loadGuardians()
                }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun removeGuardian(guardianId: String) {
        viewModelScope.launch {
            when (val result = patientRepository.removeGuardian(patientId, guardianId)) {
                is ApiResult.Success -> { _effect.emit(GuardianUiEffect.ShowToast("已移除")); loadGuardians() }
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    fun transferOwner(toUserId: String) {
        viewModelScope.launch {
            when (val result = patientRepository.transferOwner(patientId, toUserId)) {
                is ApiResult.Success -> _effect.emit(GuardianUiEffect.ShowToast("已移交主监护人"))
                is ApiResult.Failure -> _state.update { it.copy(error = result.message) }
            }
        }
    }
}

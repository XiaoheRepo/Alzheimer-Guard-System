@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.feature.profile.ui.fence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
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
import com.xiaohelab.guard.android.core.config.DefaultRemoteConfig
import com.xiaohelab.guard.android.core.config.RemoteConfigRepository
import com.xiaohelab.guard.android.core.ui.ErrorMessageMapper
import com.xiaohelab.guard.android.core.ui.components.MhPrimaryButton
import com.xiaohelab.guard.android.feature.profile.data.PatientFenceUpdateRequest
import com.xiaohelab.guard.android.feature.profile.domain.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── FenceEditViewModel ──────────────────────────────────────────────────────

data class FenceEditUiState(
    val centerLat: String = "",
    val centerLng: String = "",
    val radiusM: String = "",
    /** 围栏开关；后端必填字段（API V2.0 §3.3.4 / 后端 FenceUpdateRequest.fence_enabled）。 */
    val enabled: Boolean = false,
    val submitting: Boolean = false,
    val error: DomainException? = null,
    val success: Boolean = false,
    /** HC-05: 从 RemoteConfig 读取，不硬编码。 */
    val radiusMinM: Int = 100,
    val radiusMaxM: Int = 50000,
)

@HiltViewModel
class FenceEditViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    private val remoteConfig: RemoteConfigRepository,
) : ViewModel() {
    private val _s = MutableStateFlow(FenceEditUiState())
    val state: StateFlow<FenceEditUiState> = _s.asStateFlow()

    fun init(patientId: String) {
        // HC-05: 半径阈值来自 RemoteConfigRepository，不硬编码
        val minM = remoteConfig.getInt(DefaultRemoteConfig.KEY_FENCE_RADIUS_MIN_M)
        val maxM = remoteConfig.getInt(DefaultRemoteConfig.KEY_FENCE_RADIUS_MAX_M)
        _s.update { it.copy(radiusMinM = minM, radiusMaxM = maxM) }
        viewModelScope.launch {
            when (val r = profileRepo.detail(patientId)) {
                is MhResult.Success -> {
                    val d = r.data
                    _s.update {
                        it.copy(
                            centerLat = d.fenceCenterLat?.toString() ?: "",
                            centerLng = d.fenceCenterLng?.toString() ?: "",
                            radiusM = d.fenceRadiusM?.toString() ?: "",
                            enabled = d.fenceEnabled ?: false,
                        )
                    }
                }
                is MhResult.Failure -> _s.update { it.copy(error = r.error) }
            }
        }
    }

    fun onLatChange(v: String) = _s.update { it.copy(centerLat = v, error = null) }
    fun onLngChange(v: String) = _s.update { it.copy(centerLng = v, error = null) }
    fun onRadiusChange(v: String) = _s.update { it.copy(radiusM = v, error = null) }
    fun onEnabledChange(enabled: Boolean) = _s.update { it.copy(enabled = enabled, error = null) }

    fun submit(patientId: String) {
        val s = _s.value
        // 启用围栏时校验中心点 + 半径；关闭围栏只发 fence_enabled=false 即可。
        val body = if (s.enabled) {
            val lat = s.centerLat.toDoubleOrNull() ?: return
            val lng = s.centerLng.toDoubleOrNull() ?: return
            val radius = s.radiusM.toIntOrNull() ?: return
            if (radius < s.radiusMinM || radius > s.radiusMaxM) {
                _s.update {
                    it.copy(
                        error = DomainException(
                            "E_PRO_4221",
                            "Radius out of range [${s.radiusMinM}, ${s.radiusMaxM}]",
                        ),
                    )
                }
                return
            }
            PatientFenceUpdateRequest(
                fenceEnabled = true,
                fenceCenterLat = lat,
                fenceCenterLng = lng,
                fenceRadiusM = radius,
                // HC-Coord: Android 端坐标采集为 GCJ-02（高德），上报时显式声明坐标系。
                fenceCoordSystem = "GCJ-02",
            )
        } else {
            PatientFenceUpdateRequest(fenceEnabled = false)
        }
        _s.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            when (val r = profileRepo.updateFence(patientId, body)) {
                is MhResult.Success -> _s.update { it.copy(submitting = false, success = true) }
                is MhResult.Failure -> _s.update { it.copy(submitting = false, error = r.error) }
            }
        }
    }
}

// ─── MockLocationPicker Composable ───────────────────────────────────────────

/**
 * 地图占位 Composable（高德地图集成占位符）。
 * 真实实现需配置 AMAP_KEY（buildConfig 中注入），集成 AMap SDK。
 * 点击时直接返回当前输入的经纬度（供测试）。
 */
@Composable
fun MockLocationPicker(
    lat: String,
    lng: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(180.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.fence_map_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (lat.isNotBlank() && lng.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "lat=$lat, lng=$lng",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.fence_coord_system),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── FenceEditScreen (MH-FENCE-01) ───────────────────────────────────────────

/**
 * MH-FENCE-01: 编辑患者电子围栏。
 * HC-Coord: 坐标使用 GCJ-02（高德原始），上报时在请求体附 coord_system。
 * HC-05: 半径阈值从 RemoteConfigRepository 获取。
 */
@Composable
fun FenceEditScreen(
    patientId: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: FenceEditViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(patientId) { vm.init(patientId) }
    LaunchedEffect(state.success) { if (state.success) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.fence_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 地图占位（HC-Coord: GCJ-02）
            MockLocationPicker(lat = state.centerLat, lng = state.centerLng)

            OutlinedTextField(
                value = state.centerLat,
                onValueChange = vm::onLatChange,
                label = { Text("Lat (GCJ-02)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.centerLng,
                onValueChange = vm::onLngChange,
                label = { Text("Lng (GCJ-02)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.radiusM,
                onValueChange = vm::onRadiusChange,
                label = {
                    // HC-05: 阈值从 RemoteConfig 读取
                    Text("${stringResource(R.string.fence_field_radius)} (${state.radiusMinM}–${state.radiusMaxM}m)")
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            state.error?.let {
                Text(ErrorMessageMapper.message(ctx, it), color = MaterialTheme.colorScheme.error)
            }
            MhPrimaryButton(
                text = stringResource(R.string.common_save),
                contentDesc = stringResource(R.string.fence_edit_title),
                onClick = { vm.submit(patientId) },
                enabled = !state.submitting,
            )
        }
    }
}

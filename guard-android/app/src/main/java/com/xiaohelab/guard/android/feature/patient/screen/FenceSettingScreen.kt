package com.xiaohelab.guard.android.feature.patient.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.feature.patient.FenceSettingUiEffect
import com.xiaohelab.guard.android.feature.patient.FenceSettingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FenceSettingScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: FenceSettingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FenceSettingUiEffect.Success -> { Toast.makeText(context, "围栏已更新", Toast.LENGTH_SHORT).show(); onSuccess() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("围栏设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("设置患者安全区域围栏中心及半径", style = MaterialTheme.typography.bodyMedium)

            OutlinedTextField(
                value = state.centerLat, onValueChange = viewModel::onLatChange,
                label = { Text("围栏中心纬度") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.centerLng, onValueChange = viewModel::onLngChange,
                label = { Text("围栏中心经度") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.radiusMeters, onValueChange = viewModel::onRadiusChange,
                label = { Text("半径（米，50~5000）") }, modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("启用围栏", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.enabled, onCheckedChange = viewModel::onEnabledChange)
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("保存")
            }
        }
    }
}

package com.xiaohelab.guard.android.feature.patient.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.feature.patient.PatientDetailUiEffect
import com.xiaohelab.guard.android.feature.patient.PatientDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToFence: (String) -> Unit,
    onNavigateToGuardians: (String) -> Unit,
    onNavigateToTag: (String) -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PatientDetailUiEffect.NavigateToEdit -> onNavigateToEdit(effect.patientId)
                is PatientDetailUiEffect.NavigateToFence -> onNavigateToFence(effect.patientId)
                is PatientDetailUiEffect.NavigateToGuardians -> onNavigateToGuardians(effect.patientId)
                is PatientDetailUiEffect.NavigateToTag -> onNavigateToTag(effect.patientId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("患者详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.patient == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "加载失败")
            }
            else -> {
                val patient = state.patient!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(patient.name, style = MaterialTheme.typography.titleLarge)
                            patient.age?.let { Text("年龄：$it 岁") }
                            patient.gender?.let { Text("性别：$it") }
                            patient.height?.let { Text("身高：$it cm") }
                            patient.weight?.let { Text("体重：$it kg") }
                            patient.medicalHistory?.let { Text("病史：$it") }
                            patient.characteristics?.let { Text("特征：$it") }
                            patient.boundTagId?.let { Text("绑定标签：$it") } ?: Text("未绑定标签")
                        }
                    }

                    Button(
                        onClick = { viewModel.editProfile() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("编辑基本信息") }

                    Button(
                        onClick = { viewModel.editFence() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("围栏设置") }

                    Button(
                        onClick = { viewModel.manageGuardians() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("监护人管理") }

                    Button(
                        onClick = { viewModel.manageTag() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("标签管理") }
                }
            }
        }
    }
}

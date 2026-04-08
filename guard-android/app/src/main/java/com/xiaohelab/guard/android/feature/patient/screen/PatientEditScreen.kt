package com.xiaohelab.guard.android.feature.patient.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.feature.patient.PatientEditUiEffect
import com.xiaohelab.guard.android.feature.patient.PatientEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientEditScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: PatientEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PatientEditUiEffect.Success -> { Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show(); onSuccess() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑患者信息") },
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
            OutlinedTextField(
                value = state.name, onValueChange = viewModel::onNameChange,
                label = { Text("姓名*") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.age, onValueChange = viewModel::onAgeChange,
                label = { Text("年龄") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.gender, onValueChange = viewModel::onGenderChange,
                label = { Text("性别（男/女）") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.height, onValueChange = viewModel::onHeightChange,
                label = { Text("身高(cm)") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.weight, onValueChange = viewModel::onWeightChange,
                label = { Text("体重(kg)") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.medicalHistory, onValueChange = viewModel::onMedicalHistoryChange,
                label = { Text("既往病史") }, modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            OutlinedTextField(
                value = state.characteristics, onValueChange = viewModel::onCharacteristicsChange,
                label = { Text("外貌特征") }, modifier = Modifier.fillMaxWidth(), minLines = 2
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = viewModel::submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading
            ) {
                if (state.loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("保存")
            }
        }
    }
}

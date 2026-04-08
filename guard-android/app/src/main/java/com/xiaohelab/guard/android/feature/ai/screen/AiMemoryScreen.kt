package com.xiaohelab.guard.android.feature.ai.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiaohelab.guard.android.domain.model.AiMemoryNote
import com.xiaohelab.guard.android.feature.ai.AiMemoryUiEffect
import com.xiaohelab.guard.android.feature.ai.AiMemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMemoryScreen(
    onBack: () -> Unit,
    viewModel: AiMemoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AiMemoryUiEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 记忆笔记") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.newNoteContent,
                    onValueChange = viewModel::onNewNoteContentChange,
                    placeholder = { Text("添加新笔记…") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                Button(onClick = viewModel::addNote, enabled = state.newNoteContent.isNotBlank()) {
                    Text("添加")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            items(state.notes, key = { it.id }) { note ->
                MemoryNoteCard(note = note, onDelete = { viewModel.deleteNote(note.id) })
            }
        }
    }
}

@Composable
private fun MemoryNoteCard(note: AiMemoryNote, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(note.content, style = MaterialTheme.typography.bodyMedium)
                Text(note.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.xiaohelab.guard.android.core.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.xiaohelab.guard.android.R
import com.xiaohelab.guard.android.core.ui.components.MhPrimaryButton

/**
 * MH-SCAN-RESULT：家属端从「我的」页扫码后到达的轻量结果页。
 *
 * - 仅展示扫描到的 `tag_code`（HC-ID-String），不自动发起任何写接口。
 * - 引导用户进入「档案」Tab，按现有流程处置（绑定 / 上报线索 / 确认丢失），
 *   这些都是 patient 上下文下的能力，不应在 Me 页臆造业务调用（HC-01 六域隔离）。
 * - 对应 handbook §9 标签扫码入口补充场景。
 */
@Composable
fun ScanResultScreen(
    tagCode: String,
    onGoToProfiles: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.scan_result_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
            },
        )
    }) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.scan_result_tag_label)) },
                    supportingContent = {
                        Text(
                            tagCode,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { contentDescription = tagCode },
                        )
                    },
                )
            }
            Text(
                stringResource(R.string.scan_result_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
            MhPrimaryButton(
                text = stringResource(R.string.scan_result_go_profiles),
                contentDesc = stringResource(R.string.scan_result_go_profiles),
                onClick = onGoToProfiles,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "scan_result_back"
                },
            ) {
                Text(stringResource(R.string.common_back))
            }
        }
    }
}

package app.studycapture.ui.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.studycapture.R
import app.studycapture.ui.catalogSubjectLabels
import app.studycapture.ui.components.Overline
import app.studycapture.ui.subjectLabel

/** Subject → Topic → title. The on-device suggestion is pre-filled; the user always decides. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SaveSheet(
    suggestedSubject: String?,
    suggestedTopic: String?,
    keywords: String?,
    initialTitle: String,
    existingSubjects: List<String>,
    loadPlacement: suspend () -> Pair<String?, String?>,
    onDismiss: () -> Unit,
    onSave: (subject: String, topic: String, title: String) -> Unit,
) {
    val suggestedLabel = suggestedSubject?.let { subjectLabel(it) }
    var subject by rememberSaveable { mutableStateOf(suggestedLabel.orEmpty()) }
    var topic by rememberSaveable { mutableStateOf(suggestedTopic ?: initialTitle) }
    var title by rememberSaveable { mutableStateOf(initialTitle) }

    LaunchedEffect(Unit) {
        val (savedSubject, savedTopic) = loadPlacement()
        if (savedSubject != null) subject = savedSubject
        if (savedTopic != null) topic = savedTopic
    }

    val choices = (listOfNotNull(suggestedLabel) + existingSubjects + catalogSubjectLabels()).distinct().take(14)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)
                .navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.save_title), style = MaterialTheme.typography.headlineSmall)
            if (suggestedLabel != null) {
                Text(
                    stringResource(R.string.save_suggested, suggestedLabel, keywords.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(R.string.save_no_suggestion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Overline(stringResource(R.string.save_subject))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { name ->
                    FilterChip(selected = subject.equals(name, ignoreCase = true), onClick = { subject = name }, label = { Text(name) })
                }
            }
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text(stringResource(R.string.save_subject)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text(stringResource(R.string.save_topic)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.save_material_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSave(subject.trim(), topic.trim(), title.trim()) },
                enabled = subject.isNotBlank() && title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.check_save)) }
        }
    }
}

package app.studycapture.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.studycapture.R
import app.studycapture.ui.components.AppIcons
import app.studycapture.ui.components.StudyCard

/** Shown until the first material is saved: one obvious action. */
@Composable
fun FirstRunContent(onCapture: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.first_run_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(R.string.first_run_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onCapture, modifier = Modifier.fillMaxWidth().height(64.dp), shape = MaterialTheme.shapes.large) {
            Icon(AppIcons.Capture, null, Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.nav_capture), style = MaterialTheme.typography.titleLarge)
        }
        StudyCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Step(1, stringResource(R.string.first_run_step1))
                Step(2, stringResource(R.string.first_run_step2))
                Step(3, stringResource(R.string.first_run_step3))
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            Icon(AppIcons.Lock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.first_run_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$number", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

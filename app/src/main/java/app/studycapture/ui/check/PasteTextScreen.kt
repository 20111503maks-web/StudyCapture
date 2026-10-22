package app.studycapture.ui.check

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.studycapture.R
import app.studycapture.ui.appContainer
import app.studycapture.ui.components.ScreenHeader
import kotlinx.coroutines.launch

@Composable
fun PasteTextScreen(onBack: () -> Unit, onCreated: (String) -> Unit) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        ScreenHeader(stringResource(R.string.paste_title), onBack)
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= MAX_CHARS) text = it },
            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            placeholder = { Text(stringResource(R.string.paste_hint)) },
        )
        Button(
            onClick = {
                busy = true
                scope.launch { onCreated(container.capture.importText(text)) }
            },
            enabled = text.isNotBlank() && !busy,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
        ) { Text(stringResource(R.string.paste_continue)) }
    }
}

private const val MAX_CHARS = 200_000

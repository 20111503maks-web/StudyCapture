package app.studycapture.ui.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.studycapture.R
import app.studycapture.data.db.BlockEntity
import app.studycapture.data.db.PageEntity
import app.studycapture.data.repo.toModel
import app.studycapture.ui.components.PagePhoto
import app.studycapture.ui.theme.StudyTheme

/** Opens the exact source of a summary point or card: the photo region and the stored text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSheet(page: PageEntity, pageNumber: Int, block: BlockEntity?, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.page_n, pageNumber), style = MaterialTheme.typography.titleLarge)
            val photo = page.processedPath ?: page.originalPath
            if (photo != null) {
                PagePhoto(photo, block?.toModel()?.box, Modifier.fillMaxWidth().height(300.dp), version = page.updatedAt)
            }
            if (block != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = StudyTheme.colors.uncertainContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.5.dp, StudyTheme.colors.uncertainBorder),
                ) {
                    Text(block.text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                }
                Text(
                    stringResource(R.string.source_exact_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

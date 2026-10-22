package app.studycapture.ui.check

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.studycapture.R
import app.studycapture.data.db.BlockEntity
import app.studycapture.model.BlockType
import app.studycapture.ui.components.AppIcons
import app.studycapture.ui.components.Overline
import app.studycapture.ui.theme.StudyTheme

@Composable
fun blockTypeLabel(type: String): String = stringResource(
    when (runCatching { BlockType.valueOf(type) }.getOrNull()) {
        BlockType.HEADING -> R.string.block_heading
        BlockType.SUBHEADING -> R.string.block_subheading
        BlockType.DEFINITION -> R.string.block_definition
        BlockType.FORMULA -> R.string.block_formula
        BlockType.LIST_ITEM -> R.string.block_list
        BlockType.TABLE_ROW -> R.string.block_table
        BlockType.EXAMPLE -> R.string.block_example
        BlockType.TASK -> R.string.block_task
        BlockType.CAPTION -> R.string.block_caption
        BlockType.HIGHLIGHT -> R.string.block_highlight
        else -> R.string.block_paragraph
    },
)

/** Renders block text by type; the text itself is shown exactly as stored. */
@Composable
fun BlockText(block: BlockEntity) {
    when (block.type) {
        BlockType.HEADING.name, BlockType.SUBHEADING.name ->
            Text(block.text, style = MaterialTheme.typography.titleLarge)
        BlockType.FORMULA.name ->
            Text(block.text, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic)
        BlockType.LIST_ITEM.name -> Text("•  " + block.text, style = MaterialTheme.typography.bodyLarge)
        BlockType.TABLE_ROW.name -> Text(block.text, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
        BlockType.DEFINITION.name -> Text(block.text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        else -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * One recognized block. Uncertain blocks are amber and offer "show on photo",
 * "edit" and "looks right" — the app never guesses the missing text.
 */
@Composable
fun EditableBlock(
    block: BlockEntity,
    highlighted: Boolean,
    onShowOnPhoto: () -> Unit,
    onEdit: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    var editing by remember(block.rowId) { mutableStateOf(false) }
    var draft by remember(block.rowId, block.text) { mutableStateOf(block.text) }
    val colors = StudyTheme.colors
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (block.uncertain) colors.uncertainContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            if (block.uncertain || highlighted) 1.5.dp else 1.dp,
            when {
                block.uncertain -> colors.uncertainBorder
                highlighted -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        ),
        onClick = onShowOnPhoto,
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (block.uncertain) {
                    Icon(AppIcons.Warning, null, Modifier.size(16.dp), tint = colors.onUncertainContainer)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.check_uncertain).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onUncertainContainer,
                    )
                } else {
                    Overline(blockTypeLabel(block.type))
                }
                if (block.editedByUser) {
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.check_edited), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (editing) {
                OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editing = false; draft = block.text }) { Text(stringResource(R.string.action_cancel)) }
                    Button(onClick = { onEdit(draft.trim()); editing = false }, enabled = draft.isNotBlank()) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            } else {
                BlockText(block)
                if (block.uncertain) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onShowOnPhoto) { Text(stringResource(R.string.check_show_on_photo)) }
                        Button(
                            onClick = { editing = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.onUncertainContainer, contentColor = colors.uncertainContainer),
                        ) { Text(stringResource(R.string.action_edit)) }
                    }
                    TextButton(onClick = onConfirm) { Text(stringResource(R.string.check_looks_right)) }
                } else {
                    TextButton(onClick = { editing = true }, contentPadding = ButtonDefaults.TextButtonContentPadding) {
                        Icon(AppIcons.Edit, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_edit))
                    }
                }
            }
        }
    }
}

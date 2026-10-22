package app.studycapture.ui.processing

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.studycapture.R
import app.studycapture.data.db.MaterialEntity
import app.studycapture.data.db.MaterialStatus
import app.studycapture.data.db.PageEntity
import app.studycapture.data.db.PageStage
import app.studycapture.data.repo.CaptureRepository
import app.studycapture.data.repo.LibraryRepository
import app.studycapture.ui.appViewModel
import app.studycapture.ui.components.AppIcons
import app.studycapture.ui.components.ScreenHeader
import app.studycapture.ui.theme.StudyTheme
import coil3.compose.AsyncImage
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.studycapture.ui.components.pageImageRequest
import app.studycapture.ui.pageErrorText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ProcessingState(val material: MaterialEntity? = null, val pages: List<PageEntity> = emptyList())

class ProcessingViewModel(materialId: String, library: LibraryRepository, private val capture: CaptureRepository) : ViewModel() {
    val state: StateFlow<ProcessingState> = combine(library.observeMaterial(materialId), library.observePages(materialId)) { m, p ->
        ProcessingState(m, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProcessingState())

    fun retry(pageId: String) {
        viewModelScope.launch { capture.retryPage(pageId) }
    }
}

private enum class StepState { DONE, ACTIVE, WAITING }

/**
 * Honest progress: every row is derived from page stages stored by the worker.
 * There is no timer and no percentage that the pipeline does not report.
 */
@Composable
fun ProcessingScreen(materialId: String, onBack: () -> Unit, onReady: () -> Unit) {
    val vm = appViewModel { ProcessingViewModel(materialId, it.library, it.capture) }
    val state by vm.state.collectAsStateWithLifecycle()
    val pages = state.pages
    val status = state.material?.status

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(status) {
        if (status == MaterialStatus.NEEDS_REVIEW || status == MaterialStatus.READY) {
            // Wait until the screen is actually shown (not mid-transition or in background).
            lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
            onReady()
        }
    }

    val total = pages.size
    val finished = pages.count { it.stage == PageStage.READY || it.stage == PageStage.FAILED }
    val current = pages.firstOrNull { it.stage !in setOf(PageStage.READY, PageStage.FAILED, PageStage.CAPTURED) }
    val failed = pages.filter { it.stage == PageStage.FAILED }

    fun stepState(stage: PageStage): StepState = when {
        total == 0 -> StepState.WAITING
        finished == total -> StepState.DONE
        current == null -> StepState.WAITING
        stage.ordinal < current.stage.ordinal -> StepState.DONE
        stage == current.stage -> StepState.ACTIVE
        else -> StepState.WAITING
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(stringResource(R.string.processing_title, total), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PagePreview(current ?: pages.lastOrNull { it.processedPath != null } ?: pages.firstOrNull())
            Spacer(Modifier.height(16.dp))
            val pageLabel = if (current != null) stringResource(R.string.processing_page_of, current.pageIndex + 1, total) else null
            Step(stringResource(R.string.stage_captured), stringResource(R.string.stage_captured_body, pages.count { it.originalPath != null }), StepState.DONE.takeIf { total > 0 } ?: StepState.WAITING)
            Step(stringResource(R.string.stage_cleaning), stringResource(R.string.stage_cleaning_body), stepState(PageStage.CLEANING), pageLabel.takeIf { current?.stage == PageStage.CLEANING })
            Step(stringResource(R.string.stage_reading), stringResource(R.string.stage_reading_body), stepState(PageStage.READING), pageLabel.takeIf { current?.stage == PageStage.READING })
            Step(stringResource(R.string.stage_understanding), stringResource(R.string.stage_understanding_body), stepState(PageStage.UNDERSTANDING), pageLabel.takeIf { current?.stage == PageStage.UNDERSTANDING })
            Step(
                stringResource(R.string.stage_organizing),
                stringResource(R.string.stage_organizing_body),
                when {
                    status == MaterialStatus.FAILED -> StepState.WAITING
                    status == MaterialStatus.NEEDS_REVIEW || status == MaterialStatus.READY -> StepState.DONE
                    finished == total && total > 0 -> StepState.ACTIVE
                    else -> StepState.WAITING
                },
            )
            failed.forEach { page ->
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).clip(MaterialTheme.shapes.medium)
                        .background(StudyTheme.colors.uncertainContainer).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(AppIcons.Warning, null, tint = StudyTheme.colors.onUncertainContainer)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.processing_page_failed, page.pageIndex + 1, pageErrorText(page.error)),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = StudyTheme.colors.onUncertainContainer,
                    )
                    if (page.originalPath != null) {
                        OutlinedButton(onClick = { vm.retry(page.id) }) { Text(stringResource(R.string.action_retry)) }
                    }
                }
            }
        }
        Row(
            Modifier.padding(20.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
        ) {
            Icon(AppIcons.Clock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.processing_background), style = MaterialTheme.typography.bodySmall)
        }
        Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, Modifier.weight(1f)) { Text(stringResource(R.string.processing_later)) }
            if (status == MaterialStatus.FAILED) {
                Button(onClick = onBack, Modifier.weight(1f)) { Text(stringResource(R.string.action_close)) }
            }
        }
    }
}

@Composable
private fun PagePreview(page: PageEntity?) {
    val transition = rememberInfiniteTransition(label = "scan")
    val line by transition.animateFloat(0.05f, 0.95f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "line")
    Box(
        Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2A2D33)),
        contentAlignment = Alignment.Center,
    ) {
        val path = page?.processedPath ?: page?.originalPath
        if (path != null) {
            AsyncImage(model = pageImageRequest(path, page?.updatedAt ?: 0L), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(12.dp))
        }
        val active = page != null && page.stage in setOf(PageStage.CLEANING, PageStage.READING, PageStage.UNDERSTANDING)
        if (active) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.align(Alignment.TopStart).offset(y = (236 * line).dp)
                        .fillMaxWidth().padding(horizontal = 12.dp).height(2.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun Step(title: String, body: String, state: StepState, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            when (state) {
                StepState.DONE -> Box(
                    Modifier.size(24.dp).clip(CircleShape).background(StudyTheme.colors.success),
                    contentAlignment = Alignment.Center,
                ) { Icon(AppIcons.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.surface) }
                StepState.ACTIVE -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp)
                StepState.WAITING -> Box(Modifier.size(22.dp).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (state == StepState.WAITING) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

package app.studycapture.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import app.studycapture.R
import app.studycapture.ui.components.AppIcons
import kotlin.reflect.KClass

private data class Tab(val route: Any, val type: KClass<*>, val icon: ImageVector, val label: Int)

private val leftTabs = listOf(
    Tab(HomeRoute, HomeRoute::class, AppIcons.Home, R.string.nav_home),
    Tab(LibraryRoute, LibraryRoute::class, AppIcons.Library, R.string.nav_library),
)
private val rightTabs = listOf(
    Tab(ReviewRoute(), ReviewRoute::class, AppIcons.Cards, R.string.nav_review),
    Tab(ProfileRoute, ProfileRoute::class, AppIcons.Profile, R.string.nav_profile),
)

/** Home · Library · [Capture] · Review · Profile. Capture is raised and always visible. */
@Composable
fun BottomBar(destination: NavDestination?, onNavigate: (Any) -> Unit, onCapture: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leftTabs.forEach { TabItem(it, destination?.hasRoute(it.type) == true, onNavigate) }
                CaptureButton(onCapture)
                rightTabs.forEach { TabItem(it, destination?.hasRoute(it.type) == true, onNavigate) }
            }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, onNavigate: (Any) -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.width(72.dp).clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = { onNavigate(tab.route) })
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.width(56.dp).height(30.dp).clip(RoundedCornerShape(15.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) { Icon(tab.icon, null, tint = color, modifier = Modifier.size(22.dp)) }
        Text(stringResource(tab.label), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun CaptureButton(onClick: () -> Unit) {
    val label = stringResource(R.string.nav_capture)
    Surface(
        onClick = onClick,
        modifier = Modifier.offset(y = (-14).dp).size(60.dp).shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(AppIcons.Capture, label, Modifier.size(28.dp)) }
    }
}

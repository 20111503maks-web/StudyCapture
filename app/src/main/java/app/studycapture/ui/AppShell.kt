package app.studycapture.ui

import androidx.activity.compose.LocalActivity
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.studycapture.R
import app.studycapture.ui.capture.CaptureSheet
import app.studycapture.ui.capture.CaptureViewModel
import app.studycapture.ui.navigation.BottomBar
import app.studycapture.ui.navigation.HomeRoute
import app.studycapture.ui.navigation.LibraryRoute
import app.studycapture.ui.navigation.PasteTextRoute
import app.studycapture.ui.navigation.ProcessingRoute
import app.studycapture.ui.navigation.ProfileRoute
import app.studycapture.ui.navigation.ReviewRoute
import app.studycapture.ui.navigation.StudyNavHost
import kotlinx.coroutines.launch

@Composable
fun AppShell() {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showCapture by rememberSaveable { mutableStateOf(false) }

    val container = appContainer()
    val activity = LocalActivity.current as ComponentActivity
    val captureVm = remember(activity) {
        ViewModelProvider(activity, viewModelFactory { initializer { CaptureViewModel(container.capture) } })[CaptureViewModel::class.java]
    }
    LaunchedEffect(captureVm) {
        captureVm.imported.collect { id -> navController.navigate(ProcessingRoute(id)) }
    }

    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val topLevel = listOf(HomeRoute::class, LibraryRoute::class, ReviewRoute::class, ProfileRoute::class)
        .any { destination?.hasRoute(it) == true }
    val scannerUnavailable = stringResource(R.string.capture_scanner_unavailable)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (topLevel) {
                BottomBar(
                    destination = destination,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(HomeRoute) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCapture = { showCapture = true },
                )
            }
        },
    ) { padding ->
        StudyNavHost(
            navController = navController,
            onCapture = { showCapture = true },
            snackbar = snackbar,
            modifier = Modifier.padding(padding),
        )
    }

    if (showCapture) {
        CaptureSheet(
            onDismiss = { showCapture = false },
            onImages = { uris, fromScanner ->
                showCapture = false
                captureVm.importImages(uris, fromScanner)
            },
            onPasteText = {
                showCapture = false
                navController.navigate(PasteTextRoute)
            },
            onScannerUnavailable = { scope.launch { snackbar.showSnackbar(scannerUnavailable) } },
        )
    }
}

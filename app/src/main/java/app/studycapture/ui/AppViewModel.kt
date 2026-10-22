package app.studycapture.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.studycapture.AppContainer
import app.studycapture.StudyCaptureApp

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as StudyCaptureApp).container

/** ViewModel scoped to the current navigation entry, built from the app container. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = appContainer()
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}

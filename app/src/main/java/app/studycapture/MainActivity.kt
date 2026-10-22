package app.studycapture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.studycapture.data.settings.Settings
import app.studycapture.data.settings.ThemeMode
import app.studycapture.ui.AppShell
import app.studycapture.ui.theme.StudyCaptureTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as StudyCaptureApp).container
        setContent {
            val settings by container.settings.settings
                .collectAsStateWithLifecycle(initialValue = Settings(cyrillicOcr = false, themeMode = ThemeMode.SYSTEM))
            StudyCaptureTheme(mode = settings.themeMode) {
                AppShell()
            }
        }
    }
}

package app.studycapture

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StudyCaptureApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Remove page folders whose material no longer exists (a write that raced a deletion).
        appScope.launch {
            runCatching {
                container.files.deleteOrphans(container.database.libraryDao().allMaterialIds().toSet())
            }
        }
    }
}

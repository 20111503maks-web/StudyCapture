package app.studycapture.ui.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.studycapture.data.db.SourceType
import app.studycapture.data.repo.CaptureRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Activity-scoped so an import keeps running across rotation and sheet dismissal. */
class CaptureViewModel(private val capture: CaptureRepository) : ViewModel() {
    private val _imported = Channel<String>(Channel.BUFFERED)
    /** Emits the id of each newly created material. */
    val imported = _imported.receiveAsFlow()

    fun importImages(uris: List<Uri>, fromScanner: Boolean) {
        viewModelScope.launch {
            val id = capture.importImages(uris, if (fromScanner) SourceType.CAMERA else SourceType.GALLERY)
            _imported.send(id)
        }
    }
}

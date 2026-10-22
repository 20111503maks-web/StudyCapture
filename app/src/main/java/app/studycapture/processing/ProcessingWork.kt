package app.studycapture.processing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.studycapture.StudyCaptureApp
import kotlinx.coroutines.CancellationException

class ProcessMaterialWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val materialId = inputData.getString(KEY_MATERIAL_ID) ?: return Result.failure()
        val container = (applicationContext as StudyCaptureApp).container
        return try {
            container.processor.process(materialId)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Never leave a material "processing" forever: show it as failed with Retry.
            container.processor.markFailed(materialId)
            Result.failure()
        }
    }

    companion object {
        const val KEY_MATERIAL_ID = "materialId"
    }
}

/** Persistent queue: WorkManager restarts the job after the app is closed or the phone reboots. */
class ProcessingScheduler(private val context: Context) {
    fun enqueue(materialId: String) {
        val request = OneTimeWorkRequestBuilder<ProcessMaterialWorker>()
            .setInputData(workDataOf(ProcessMaterialWorker.KEY_MATERIAL_ID to materialId))
            .addTag(TAG)
            .build()
        // APPEND_OR_REPLACE: a retry while a run is active queues a second run after it.
        WorkManager.getInstance(context).enqueueUniqueWork("process-$materialId", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun cancel(materialId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("process-$materialId")
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }

    private companion object {
        const val TAG = "page-processing"
    }
}

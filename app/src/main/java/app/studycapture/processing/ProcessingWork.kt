package app.studycapture.processing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.studycapture.StudyCaptureApp

class ProcessMaterialWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val materialId = inputData.getString(KEY_MATERIAL_ID) ?: return Result.failure()
        val container = (applicationContext as StudyCaptureApp).container
        container.processor.process(materialId)
        return Result.success()
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
        WorkManager.getInstance(context).enqueueUniqueWork("process-$materialId", ExistingWorkPolicy.KEEP, request)
    }

    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }

    private companion object {
        const val TAG = "page-processing"
    }
}

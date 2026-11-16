package com.example.data.update

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val UPDATE_TAG = "SelavuUpdate"

sealed interface InAppUpdateUiState {
    data object Hidden : InAppUpdateUiState
    data class Available(val availableVersionCode: Int) : InAppUpdateUiState
    data class Downloading(
        val availableVersionCode: Int,
        val bytesDownloaded: Long,
        val totalBytesToDownload: Long
    ) : InAppUpdateUiState {
        val progress: Float
            get() = if (totalBytesToDownload > 0L) {
                (bytesDownloaded.toFloat() / totalBytesToDownload.toFloat()).coerceIn(0f, 1f)
            } else 0f
    }

    data class Downloaded(val availableVersionCode: Int) : InAppUpdateUiState
}

internal fun resolveFlexibleUpdateState(
    updateAvailability: Int,
    isFlexibleAllowed: Boolean,
    installStatus: Int,
    availableVersionCode: Int,
    ignoredVersionCode: Int?,
    bytesDownloaded: Long = 0L,
    totalBytesToDownload: Long = 0L
): InAppUpdateUiState = when {
    installStatus == InstallStatus.DOWNLOADED -> {
        if (ignoredVersionCode != null && ignoredVersionCode == availableVersionCode) {
            InAppUpdateUiState.Hidden
        } else {
            InAppUpdateUiState.Downloaded(availableVersionCode)
        }
    }

    installStatus == InstallStatus.DOWNLOADING || installStatus == InstallStatus.PENDING -> {
        InAppUpdateUiState.Downloading(
            availableVersionCode = availableVersionCode,
            bytesDownloaded = bytesDownloaded,
            totalBytesToDownload = totalBytesToDownload
        )
    }

    updateAvailability == UpdateAvailability.UPDATE_AVAILABLE && isFlexibleAllowed -> {
        if (ignoredVersionCode != null && ignoredVersionCode == availableVersionCode) {
            InAppUpdateUiState.Hidden
        } else {
            InAppUpdateUiState.Available(availableVersionCode)
        }
    }

    else -> InAppUpdateUiState.Hidden
}

class InAppUpdateController(context: Context) {
    private val appUpdateManager: AppUpdateManager? = try {
        AppUpdateManagerFactory.create(context.applicationContext)
    } catch (e: Exception) {
        Log.w(UPDATE_TAG, "AppUpdateManager unavailable: ${e.message}")
        null
    }

    private var latestUpdateInfo: AppUpdateInfo? = null
    private var currentIgnoredVersion: Int? = null

    private val _uiState = MutableStateFlow<InAppUpdateUiState>(InAppUpdateUiState.Hidden)
    val uiState: StateFlow<InAppUpdateUiState> = _uiState.asStateFlow()

    private val installStateListener = InstallStateUpdatedListener { state ->
        val versionCode = latestUpdateInfo?.availableVersionCode() ?: 0
        when (state.installStatus()) {
            InstallStatus.PENDING,
            InstallStatus.DOWNLOADING -> {
                _uiState.value = InAppUpdateUiState.Downloading(
                    availableVersionCode = versionCode,
                    bytesDownloaded = state.bytesDownloaded(),
                    totalBytesToDownload = state.totalBytesToDownload()
                )
            }

            InstallStatus.DOWNLOADED -> {
                _uiState.value = InAppUpdateUiState.Downloaded(availableVersionCode = versionCode)
            }

            InstallStatus.CANCELED,
            InstallStatus.FAILED,
            InstallStatus.INSTALLED -> {
                _uiState.value = InAppUpdateUiState.Hidden
            }

            else -> Unit
        }
    }

    fun registerListener() {
        try {
            appUpdateManager?.registerListener(installStateListener)
        } catch (e: Exception) {
            Log.w(UPDATE_TAG, "Unable to register install listener: ${e.message}")
        }
    }

    fun unregisterListener() {
        try {
            appUpdateManager?.unregisterListener(installStateListener)
        } catch (e: Exception) {
            Log.w(UPDATE_TAG, "Unable to unregister install listener: ${e.message}")
        }
    }

    fun updateIgnoredVersion(ignoredVersion: Int?) {
        currentIgnoredVersion = ignoredVersion
        val info = latestUpdateInfo ?: return
        _uiState.value = resolveFlexibleUpdateState(
            updateAvailability = info.updateAvailability(),
            isFlexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
            installStatus = info.installStatus(),
            availableVersionCode = info.availableVersionCode(),
            ignoredVersionCode = currentIgnoredVersion,
            bytesDownloaded = info.bytesDownloaded(),
            totalBytesToDownload = info.totalBytesToDownload()
        )
    }

    fun checkForUpdate(
        ignoredVersionCode: Int? = currentIgnoredVersion,
        onNoUpdateFound: (() -> Unit)? = null
    ) {
        currentIgnoredVersion = ignoredVersionCode
        val manager = appUpdateManager
        if (manager == null) {
            _uiState.value = InAppUpdateUiState.Hidden
            onNoUpdateFound?.invoke()
            return
        }
        try {
            manager.appUpdateInfo
                .addOnSuccessListener { info ->
                    latestUpdateInfo = info
                    val nextState = resolveFlexibleUpdateState(
                        updateAvailability = info.updateAvailability(),
                        isFlexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
                        installStatus = info.installStatus(),
                        availableVersionCode = info.availableVersionCode(),
                        ignoredVersionCode = currentIgnoredVersion,
                        bytesDownloaded = info.bytesDownloaded(),
                        totalBytesToDownload = info.totalBytesToDownload()
                    )
                    _uiState.value = nextState
                    if (nextState is InAppUpdateUiState.Hidden) {
                        onNoUpdateFound?.invoke()
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(UPDATE_TAG, "Play in-app update check skipped: ${e.message}")
                    _uiState.value = InAppUpdateUiState.Hidden
                    onNoUpdateFound?.invoke()
                }
        } catch (e: Exception) {
            Log.w(UPDATE_TAG, "Error querying appUpdateInfo: ${e.message}")
            _uiState.value = InAppUpdateUiState.Hidden
            onNoUpdateFound?.invoke()
        }
    }

    fun startFlexibleUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        val manager = appUpdateManager ?: return false
        val info = latestUpdateInfo ?: return false
        return try {
            val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            manager.startUpdateFlowForResult(info, launcher, options)
        } catch (e: Exception) {
            Log.w(UPDATE_TAG, "Could not start flexible update flow: ${e.message}")
            false
        }
    }

    fun completeUpdate() {
        try {
            appUpdateManager?.completeUpdate()
        } catch (e: Exception) {
            Log.w(UPDATE_TAG, "Could not complete update: ${e.message}")
        }
    }

    fun dismissBanner() {
        _uiState.value = InAppUpdateUiState.Hidden
    }
}

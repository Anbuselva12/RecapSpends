package com.example.data.update

import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppUpdateControllerTest {

    @Test
    fun `available flexible update surfaces Available when not ignored`() {
        val state = resolveFlexibleUpdateState(
            updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
            isFlexibleAllowed = true,
            installStatus = InstallStatus.UNKNOWN,
            availableVersionCode = 12,
            ignoredVersionCode = null
        )
        assertEquals(InAppUpdateUiState.Available(12), state)
    }

    @Test
    fun `available flexible update is hidden when user ignored same version`() {
        val state = resolveFlexibleUpdateState(
            updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
            isFlexibleAllowed = true,
            installStatus = InstallStatus.UNKNOWN,
            availableVersionCode = 12,
            ignoredVersionCode = 12
        )
        assertEquals(InAppUpdateUiState.Hidden, state)
    }

    @Test
    fun `newer version re-prompts after previous version was ignored`() {
        val state = resolveFlexibleUpdateState(
            updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
            isFlexibleAllowed = true,
            installStatus = InstallStatus.UNKNOWN,
            availableVersionCode = 13,
            ignoredVersionCode = 12
        )
        assertEquals(InAppUpdateUiState.Available(13), state)
    }

    @Test
    fun `downloading state calculates normalized progress`() {
        val state = resolveFlexibleUpdateState(
            updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
            isFlexibleAllowed = true,
            installStatus = InstallStatus.DOWNLOADING,
            availableVersionCode = 12,
            ignoredVersionCode = null,
            bytesDownloaded = 500L,
            totalBytesToDownload = 1000L
        )
        assertTrue(state is InAppUpdateUiState.Downloading)
        assertEquals(0.5f, (state as InAppUpdateUiState.Downloading).progress, 0.001f)
    }

    @Test
    fun `downloaded flexible update prompts restart unless ignored`() {
        val ready = resolveFlexibleUpdateState(
            updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
            isFlexibleAllowed = true,
            installStatus = InstallStatus.DOWNLOADED,
            availableVersionCode = 12,
            ignoredVersionCode = null
        )
        assertEquals(InAppUpdateUiState.Downloaded(12), ready)

        val dismissed = resolveFlexibleUpdateState(
            updateAvailability = UpdateAvailability.UPDATE_AVAILABLE,
            isFlexibleAllowed = true,
            installStatus = InstallStatus.DOWNLOADED,
            availableVersionCode = 12,
            ignoredVersionCode = 12
        )
        assertEquals(InAppUpdateUiState.Hidden, dismissed)
    }
}

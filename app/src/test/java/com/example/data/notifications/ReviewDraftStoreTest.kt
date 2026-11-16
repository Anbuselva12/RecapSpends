package com.example.data.notifications

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReviewDraftStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createStore(scope: TestScope, name: String): ReviewDraftStore {
        val ds = PreferenceDataStoreFactory.create(
            scope = scope.backgroundScope,
            produceFile = { File(tempFolder.root, "$name.preferences_pb") }
        )
        return ReviewDraftStore(ds)
    }

    @Test
    fun enqueueForActiveUser_persistsAcrossSignOutClearActiveUser_andIsolatesPerUid() = runTest(UnconfinedTestDispatcher()) {
        val store = createStore(this, "signout_preserve")
        val nowMs = System.currentTimeMillis()
        val todayStart = ReviewDraftStore.startOfTodayEpochMs(nowMs)
        store.setActiveUser("uid_alice")
        store.setSmsEnabled("uid_alice", enabled = true, acceptedDisclosure = true, sinceEpochMs = todayStart)

        val alert = MessageExpenseParser.parse(
            senderTitle = "HDFC Bank",
            body = "Rs 450 debited to SWIGGY via UPI",
            timestampMs = todayStart + 60_000L
        )!!

        assertTrue(store.enqueueForActiveUser(alert, "SMS"))
        // Duplicate fingerprint within 2-minute bucket is skipped
        assertFalse(store.enqueueForActiveUser(alert, "SMS"))

        // Historical alert before today is ignored
        val oldAlert = MessageExpenseParser.parse(
            senderTitle = "HDFC Bank",
            body = "Rs 300 debited to Zomato via UPI",
            timestampMs = todayStart - 86_400_000L
        )!!
        assertFalse(store.enqueueForActiveUser(oldAlert, "SMS"))

        // Sign out clears ONLY active_uid pointer; drafts stay preserved on device
        store.clearActiveUser()
        assertNull(store.getActiveUid())

        // Incoming SMS while signed out are NOT queued
        val secondAlert = MessageExpenseParser.parse(
            senderTitle = "GPay",
            body = "Paid ₹200 to Zepto via UPI",
            timestampMs = todayStart + 300_000L
        )!!
        assertFalse(store.enqueueForActiveUser(secondAlert, "SMS"))

        // Alice still sees her unapproved local draft; Bob has 0 drafts
        val aliceDrafts = store.observeDrafts("uid_alice").first()
        val bobDrafts = store.observeDrafts("uid_bob").first()
        assertEquals(1, aliceDrafts.size)
        assertEquals(450.0, aliceDrafts.first().amount, 0.01)
        assertEquals(0, bobDrafts.size)
    }

    @Test
    fun batchSyncRespectsTodayCutoffAndDoesNotRequeueRemovedDraft() = runTest(UnconfinedTestDispatcher()) {
        val store = createStore(this, "batch_sync")
        val nowMs = System.currentTimeMillis()
        val todayStart = ReviewDraftStore.startOfTodayEpochMs(nowMs)
        store.setActiveUser("uid_alice")
        store.setSmsEnabled("uid_alice", enabled = true, acceptedDisclosure = true, sinceEpochMs = todayStart)

        val yesterday = MessageExpenseParser.parse(
            senderTitle = "ICICI",
            body = "Rs 999 spent at Amazon",
            timestampMs = todayStart - 3_600_000L
        )!!
        val today = MessageExpenseParser.parse(
            senderTitle = "ICICI",
            body = "Rs 520 spent at Shell Fuel",
            timestampMs = todayStart + 120_000L
        )!!

        val added = store.enqueueBatchForUser("uid_alice", listOf(yesterday, today), sourceLabel = "SMS")
        assertEquals(1, added)

        val currentDraft = store.observeDrafts("uid_alice").first().single()
        store.removeDraft("uid_alice", currentDraft.id)

        // Re-scanning today's inbox must NOT resurrect the deleted/approved draft
        val addedAgain = store.enqueueBatchForUser("uid_alice", listOf(today), sourceLabel = "SMS")
        assertEquals(0, addedAgain)
        assertTrue(store.observeDrafts("uid_alice").first().isEmpty())
    }

    @Test
    fun updateRemoveAndClearUserDrafts_workAsExpected() = runTest(UnconfinedTestDispatcher()) {
        val store = createStore(this, "crud_clear")
        val nowMs = System.currentTimeMillis()
        val todayStart = ReviewDraftStore.startOfTodayEpochMs(nowMs)
        store.setActiveUser("uid_alice")
        store.setSmsEnabled("uid_alice", enabled = true, acceptedDisclosure = true, sinceEpochMs = todayStart)

        val alert = MessageExpenseParser.parse(
            senderTitle = "SMS Bank",
            body = "Paid ₹1,200 to HPCL Petrol via UPI",
            timestampMs = todayStart + 180_000L
        )!!
        assertTrue(store.enqueueForActiveUser(alert, "SMS"))

        val initial = store.observeDrafts("uid_alice").first().single()
        val updated = initial.copy(
            title = "HPCL Fuel Station",
            categoryId = "veh",
            subcategory = "Fuel",
            vehicleId = "veh_activa",
            projectId = "proj_trip",
            odometerReading = 12450,
            fuelPricePerLitre = 104.5,
            fuelLitres = 11.48,
            isPrivate = true,
            items = listOf(ReviewDraftItem(name = "Petrol", qty = 10.0, rate = 120.0))
        )
        store.updateDraft("uid_alice", updated)

        val reloaded = store.observeDrafts("uid_alice").first().single()
        assertEquals("HPCL Fuel Station", reloaded.title)
        assertEquals("veh_activa", reloaded.vehicleId)
        assertEquals("proj_trip", reloaded.projectId)
        assertEquals(12450, reloaded.odometerReading)
        assertEquals(104.5, reloaded.fuelPricePerLitre ?: 0.0, 0.01)
        assertEquals(11.48, reloaded.fuelLitres ?: 0.0, 0.01)
        assertTrue(reloaded.isPrivate)
        assertEquals(1, reloaded.items.size)
        assertEquals(1200.0, reloaded.items.first().amount, 0.01)

        // Permanent account deletion wipes local drafts
        store.clearUserDrafts("uid_alice")
        assertTrue(store.observeDrafts("uid_alice").first().isEmpty())
        assertFalse(store.observeSmsEnabled("uid_alice").first())
    }
}

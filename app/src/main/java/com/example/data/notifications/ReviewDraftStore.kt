package com.example.data.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

private val Context.reviewDraftsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "review_drafts_prefs"
)

data class ReviewDraftItem(
    val id: String = UUID.randomUUID().toString().take(8),
    val name: String = "",
    val qty: Double = 1.0,
    val rate: Double = 0.0
) {
    val amount: Double get() = (qty * rate).coerceAtLeast(0.0)
}

data class ReviewDraft(
    val id: String = "rev_${UUID.randomUUID().toString().take(8)}",
    val title: String = "",
    val amount: Double = 0.0,
    val dateEpochMs: Long = System.currentTimeMillis(),
    val paymentMode: String = "UPI",
    val categoryId: String = "groc",
    val subcategory: String = "",
    val snippet: String = "",
    val fingerprint: String = "",
    val sourceLabel: String = "SMS",
    val isPrivate: Boolean = false,
    val items: List<ReviewDraftItem> = emptyList(),
    val vehicleId: String? = null,
    val projectId: String? = null,
    val odometerReading: Int? = null,
    val fuelPricePerLitre: Double? = null,
    val fuelLitres: Double? = null
)

class ReviewDraftStore internal constructor(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.reviewDraftsDataStore)

    private val activeUidKey = stringPreferencesKey("active_uid")
    private fun draftsKey(uid: String) = stringPreferencesKey("drafts_$uid")
    private fun smsEnabledKey(uid: String) = booleanPreferencesKey("sms_enabled_$uid")
    private fun disclosureAcceptedKey(uid: String) = booleanPreferencesKey("sms_disclosure_accepted_$uid")
    private fun smsSinceKey(uid: String) = longPreferencesKey("sms_since_ms_$uid")
    private fun seenFingerprintsKey(uid: String) = stringPreferencesKey("seen_fps_$uid")

    suspend fun setActiveUser(uid: String) {
        dataStore.edit { prefs ->
            prefs[activeUidKey] = uid
            if (prefs[smsSinceKey(uid)] == null) {
                prefs[smsSinceKey(uid)] = startOfTodayEpochMs()
            }
        }
    }

    /**
     * Clears only the currently active user pointer on sign-out so incoming SMS
     * stop queuing while signed out, WITHOUT deleting pending device-local drafts.
     */
    suspend fun clearActiveUser() {
        dataStore.edit { prefs ->
            prefs.remove(activeUidKey)
        }
    }

    suspend fun getActiveUid(): String? = dataStore.data.first()[activeUidKey]

    suspend fun ensureSmsSinceEpochMs(uid: String, nowMs: Long = System.currentTimeMillis()): Long {
        var resolved = startOfTodayEpochMs(nowMs)
        dataStore.edit { prefs ->
            val existing = prefs[smsSinceKey(uid)]
            if (existing != null && existing > 0L) {
                resolved = existing
            } else {
                prefs[smsSinceKey(uid)] = resolved
            }
        }
        return resolved
    }

    fun observeSmsSinceEpochMs(uid: String): Flow<Long> =
        dataStore.data.map { prefs ->
            prefs[smsSinceKey(uid)] ?: startOfTodayEpochMs()
        }

    fun observeDrafts(uid: String): Flow<List<ReviewDraft>> =
        dataStore.data.map { prefs ->
            decodeDrafts(prefs[draftsKey(uid)])
        }

    fun observeSmsEnabled(uid: String): Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[smsEnabledKey(uid)] ?: false
        }

    fun observeDisclosureAccepted(uid: String): Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[disclosureAcceptedKey(uid)] ?: false
        }

    suspend fun setSmsEnabled(
        uid: String,
        enabled: Boolean,
        acceptedDisclosure: Boolean? = null,
        sinceEpochMs: Long? = null
    ) {
        dataStore.edit { prefs ->
            prefs[smsEnabledKey(uid)] = enabled
            if (acceptedDisclosure != null) {
                prefs[disclosureAcceptedKey(uid)] = acceptedDisclosure
            }
            if (prefs[smsSinceKey(uid)] == null) {
                prefs[smsSinceKey(uid)] = sinceEpochMs ?: startOfTodayEpochMs()
            }
        }
    }

    suspend fun enqueueForActiveUser(alert: ParsedExpenseAlert, sourceLabel: String = "SMS"): Boolean {
        var added = false
        dataStore.edit { prefs ->
            val uid = prefs[activeUidKey] ?: return@edit
            val enabled = prefs[smsEnabledKey(uid)] ?: false
            if (!enabled) return@edit
            val sinceMs = prefs[smsSinceKey(uid)] ?: 0L
            if (sinceMs > 0L && alert.dateEpochMs < sinceMs) return@edit

            val seen = decodeStringList(prefs[seenFingerprintsKey(uid)]).toMutableList()
            val existing = decodeDrafts(prefs[draftsKey(uid)]).toMutableList()
            if (alert.fingerprint in seen || existing.any { it.fingerprint == alert.fingerprint }) {
                return@edit
            }
            val newDraft = ReviewDraft(
                title = alert.title,
                amount = alert.amount,
                dateEpochMs = alert.dateEpochMs,
                paymentMode = alert.paymentMode,
                categoryId = alert.suggestedCategoryId,
                subcategory = alert.suggestedSubcategory,
                snippet = alert.snippet,
                fingerprint = alert.fingerprint,
                sourceLabel = sourceLabel.take(40)
            )
            existing.add(0, newDraft)
            seen.add(0, alert.fingerprint)
            prefs[draftsKey(uid)] = encodeDrafts(existing.take(100))
            prefs[seenFingerprintsKey(uid)] = encodeStringList(seen.distinct().take(300))
            added = true
        }
        return added
    }

    suspend fun enqueueBatchForUser(
        uid: String,
        alerts: List<ParsedExpenseAlert>,
        sourceLabel: String = "SMS"
    ): Int {
        if (alerts.isEmpty()) return 0
        var addedCount = 0
        dataStore.edit { prefs ->
            val enabled = prefs[smsEnabledKey(uid)] ?: false
            if (!enabled) return@edit
            val sinceMs = prefs[smsSinceKey(uid)] ?: 0L

            val seen = decodeStringList(prefs[seenFingerprintsKey(uid)]).toMutableList()
            val existing = decodeDrafts(prefs[draftsKey(uid)]).toMutableList()
            val existingFingerprints = existing.mapTo(HashSet()) { it.fingerprint }
            val seenSet = seen.toHashSet()

            alerts.sortedByDescending { it.dateEpochMs }.forEach { alert ->
                if (sinceMs > 0L && alert.dateEpochMs < sinceMs) return@forEach
                if (alert.fingerprint in seenSet || alert.fingerprint in existingFingerprints) return@forEach

                val newDraft = ReviewDraft(
                    title = alert.title,
                    amount = alert.amount,
                    dateEpochMs = alert.dateEpochMs,
                    paymentMode = alert.paymentMode,
                    categoryId = alert.suggestedCategoryId,
                    subcategory = alert.suggestedSubcategory,
                    snippet = alert.snippet,
                    fingerprint = alert.fingerprint,
                    sourceLabel = sourceLabel.take(40)
                )
                existing.add(newDraft)
                existingFingerprints.add(alert.fingerprint)
                seen.add(0, alert.fingerprint)
                seenSet.add(alert.fingerprint)
                addedCount++
            }

            if (addedCount > 0) {
                prefs[draftsKey(uid)] = encodeDrafts(
                    existing.sortedByDescending { it.dateEpochMs }.take(100)
                )
                prefs[seenFingerprintsKey(uid)] = encodeStringList(seen.distinct().take(300))
            }
        }
        return addedCount
    }

    suspend fun addDraft(uid: String, draft: ReviewDraft) {
        dataStore.edit { prefs ->
            val existing = decodeDrafts(prefs[draftsKey(uid)]).toMutableList()
            if (draft.fingerprint.isNotBlank() && existing.any { it.fingerprint == draft.fingerprint }) {
                return@edit
            }
            existing.add(0, draft)
            prefs[draftsKey(uid)] = encodeDrafts(existing.take(100))
        }
    }

    suspend fun updateDraft(uid: String, updated: ReviewDraft) {
        dataStore.edit { prefs ->
            val existing = decodeDrafts(prefs[draftsKey(uid)])
            val next = existing.map { if (it.id == updated.id) updated else it }
            prefs[draftsKey(uid)] = encodeDrafts(next)
        }
    }

    suspend fun removeDraft(uid: String, draftId: String) {
        dataStore.edit { prefs ->
            val existing = decodeDrafts(prefs[draftsKey(uid)])
            val next = existing.filterNot { it.id == draftId }
            prefs[draftsKey(uid)] = encodeDrafts(next)
        }
    }

    suspend fun clearUserDrafts(uid: String) {
        dataStore.edit { prefs ->
            prefs.remove(draftsKey(uid))
            prefs.remove(smsEnabledKey(uid))
            prefs.remove(disclosureAcceptedKey(uid))
            prefs.remove(smsSinceKey(uid))
            prefs.remove(seenFingerprintsKey(uid))
        }
    }

    private fun encodeStringList(values: List<String>): String =
        JSONArray().apply { values.forEach { put(it) } }.toString()

    private fun decodeStringList(jsonStr: String?): List<String> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i)
                    if (v.isNotBlank()) add(v)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeDrafts(drafts: List<ReviewDraft>): String {
        val arr = JSONArray()
        drafts.forEach { d ->
            val obj = JSONObject().apply {
                put("id", d.id)
                put("title", d.title)
                put("amount", d.amount)
                put("dateEpochMs", d.dateEpochMs)
                put("paymentMode", d.paymentMode)
                put("categoryId", d.categoryId)
                put("subcategory", d.subcategory)
                put("snippet", d.snippet)
                put("fingerprint", d.fingerprint)
                put("sourceLabel", d.sourceLabel)
                put("isPrivate", d.isPrivate)
                if (!d.vehicleId.isNullOrBlank()) put("vehicleId", d.vehicleId)
                if (!d.projectId.isNullOrBlank()) put("projectId", d.projectId)
                if (d.odometerReading != null) put("odometerReading", d.odometerReading)
                if (d.fuelPricePerLitre != null) put("fuelPricePerLitre", d.fuelPricePerLitre)
                if (d.fuelLitres != null) put("fuelLitres", d.fuelLitres)
                val itemsArr = JSONArray()
                d.items.forEach { item ->
                    itemsArr.put(
                        JSONObject().apply {
                            put("id", item.id)
                            put("name", item.name)
                            put("qty", item.qty)
                            put("rate", item.rate)
                        }
                    )
                }
                put("items", itemsArr)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeDrafts(jsonStr: String?): List<ReviewDraft> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val itemsArr = obj.optJSONArray("items")
                    val items = buildList {
                        if (itemsArr != null) {
                            for (j in 0 until itemsArr.length()) {
                                val itemObj = itemsArr.optJSONObject(j) ?: continue
                                add(
                                    ReviewDraftItem(
                                        id = itemObj.optString("id", UUID.randomUUID().toString().take(8)),
                                        name = itemObj.optString("name", ""),
                                        qty = itemObj.optDouble("qty", 1.0),
                                        rate = itemObj.optDouble("rate", 0.0)
                                    )
                                )
                            }
                        }
                    }
                    add(
                        ReviewDraft(
                            id = obj.optString("id", "rev_${UUID.randomUUID().toString().take(8)}"),
                            title = obj.optString("title", "Expense"),
                            amount = obj.optDouble("amount", 0.0),
                            dateEpochMs = obj.optLong("dateEpochMs", System.currentTimeMillis()),
                            paymentMode = obj.optString("paymentMode", "UPI"),
                            categoryId = obj.optString("categoryId", "groc"),
                            subcategory = obj.optString("subcategory", ""),
                            snippet = obj.optString("snippet", ""),
                            fingerprint = obj.optString("fingerprint", ""),
                            sourceLabel = obj.optString("sourceLabel", "SMS"),
                            isPrivate = obj.optBoolean("isPrivate", false),
                            items = items,
                            vehicleId = obj.optString("vehicleId", "").takeIf { it.isNotBlank() },
                            projectId = obj.optString("projectId", "").takeIf { it.isNotBlank() },
                            odometerReading = if (obj.has("odometerReading") && !obj.isNull("odometerReading")) obj.optInt("odometerReading") else null,
                            fuelPricePerLitre = if (obj.has("fuelPricePerLitre") && !obj.isNull("fuelPricePerLitre")) {
                                obj.optDouble("fuelPricePerLitre").takeIf { !it.isNaN() && it > 0.0 }
                            } else null,
                            fuelLitres = if (obj.has("fuelLitres") && !obj.isNull("fuelLitres")) {
                                obj.optDouble("fuelLitres").takeIf { !it.isNaN() && it > 0.0 }
                            } else null
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun startOfTodayEpochMs(nowMs: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    }
}

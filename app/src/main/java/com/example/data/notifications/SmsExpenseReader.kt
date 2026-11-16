package com.example.data.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsExpenseReader {

    val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS,
        Manifest.permission.RECEIVE_SMS
    )

    fun hasReadSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context.applicationContext,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

    fun hasAllSmsPermissions(context: Context): Boolean {
        val app = context.applicationContext
        return ContextCompat.checkSelfPermission(app, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun syncInboxSince(
        context: Context,
        uid: String,
        sinceEpochMs: Long,
        draftStore: ReviewDraftStore
    ): Int = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission(context)) return@withContext 0

        val parsedAlerts = mutableListOf<ParsedExpenseAlert>()
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(sinceEpochMs.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val sender = if (addressIdx >= 0) cursor.getString(addressIdx).orEmpty() else ""
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                    val dateMs = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    // OTPs, verification codes, credits, and non-spend alerts return null immediately in memory.
                    val parsed = MessageExpenseParser.parse(
                        senderTitle = sender,
                        body = body,
                        timestampMs = dateMs
                    )
                    if (parsed != null) {
                        parsedAlerts += parsed
                    }
                }
            }
        } catch (_: SecurityException) {
            return@withContext 0
        }

        if (parsedAlerts.isEmpty()) 0
        else draftStore.enqueueBatchForUser(uid, parsedAlerts, sourceLabel = "SMS")
    }
}

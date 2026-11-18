package com.example.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsExpenseBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group multi-part SMS segments by sender
        val groupedBySender = linkedMapOf<String, MutableList<Pair<String, Long>>>()
        messages.forEach { sms ->
            val sender = sms.displayOriginatingAddress?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: sms.originatingAddress?.trim().orEmpty()
            val bodyPart = sms.displayMessageBody ?: sms.messageBody.orEmpty()
            val rawTs = sms.timestampMillis
            val ts = when {
                rawTs in 1_000_000_000L..9_999_999_999L -> rawTs * 1000L
                rawTs > 0L -> rawTs
                else -> System.currentTimeMillis()
            }
            if (bodyPart.isNotBlank()) {
                groupedBySender.getOrPut(sender) { mutableListOf() }.add(bodyPart to ts)
            }
        }
        if (groupedBySender.isEmpty()) return

        val appContext = context.applicationContext
        val draftStore = ReviewDraftStore(appContext)
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                groupedBySender.forEach { (sender, parts) ->
                    val fullBody = parts.joinToString("") { it.first }
                    val ts = parts.maxOfOrNull { it.second } ?: System.currentTimeMillis()
                    // OTPs, verification codes, and non-transaction alerts return null immediately in memory.
                    val parsed = MessageExpenseParser.parse(
                        senderTitle = sender,
                        body = fullBody,
                        timestampMs = ts
                    ) ?: return@forEach
                    draftStore.enqueueForActiveUser(parsed, sourceLabel = "SMS")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

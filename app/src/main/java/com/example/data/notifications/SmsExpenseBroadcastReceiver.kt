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
            val sender = sms.displayOriginatingAddress?.trim().orEmpty()
            val bodyPart = sms.messageBody.orEmpty()
            val ts = sms.timestampMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
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
                    // OTPs, verification codes, credits, and non-spend alerts return null immediately in memory.
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

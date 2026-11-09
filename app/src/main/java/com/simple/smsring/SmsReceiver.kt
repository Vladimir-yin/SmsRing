package com.simple.smsring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val target = context.getSharedPreferences("cfg", Context.MODE_PRIVATE).getString("target", "") ?: ""
        if (target.isEmpty()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = messages.firstOrNull()?.originatingAddress ?: ""

        // 判断发件人是否包含你设置的号码
        if (sender.contains(target)) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, RingService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

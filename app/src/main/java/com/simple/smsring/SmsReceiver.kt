package com.simple.smsring

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val target = context.getSharedPreferences("cfg", Context.MODE_PRIVATE).getString("target", "") ?: ""
        if (target.isEmpty()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = messages.firstOrNull()?.originatingAddress ?: ""

        if (sender.contains(target)) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, RingService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
                sendFallbackNotification(context)
            }
        }
    }

    private fun sendFallbackNotification(context: Context) {
        val channelId = "fallback_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId, "短信兜底响铃", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("收到监控号码短信！")
            .setContentText("系统拦截了后台服务，使用通知响铃")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(999, notification)
    }
}

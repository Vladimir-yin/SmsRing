package com.simple.smsring

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class RingService : Service() {
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("ring_ch", "短信响铃", NotificationManager.IMPORTANCE_HIGH)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }

        val stopPi = PendingIntent.getService(this, 1, Intent(this, RingService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, "ring_ch")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("收到监控号码短信，正在响铃！")
            .setContentText("点击此通知停止响铃")
            .setContentIntent(stopPi)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        
        if (player == null) {
            // 读取用户选择的铃声，如果没选，就用系统默认闹钟
            val customUriString = getSharedPreferences("cfg", MODE_PRIVATE).getString("custom_ringtone", null)
            val uri = if (customUriString != null) {
                Uri.parse(customUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                setDataSource(this@RingService, uri)
                isLooping = true // 循环响铃
                prepare()
                start()
            }
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmsRing::Lock").apply { acquire(10 * 60 * 1000L) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        player?.let { if (it.isPlaying) it.stop(); it.release() }
        player = null
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}

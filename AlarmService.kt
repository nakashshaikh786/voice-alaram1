package com.galabs.voicealarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import java.io.File

class AlarmService : Service() {
    private var mp: MediaPlayer? = null
    private var wl: PowerManager.WakeLock? = null
    private val h = Handler(Looper.getMainLooper())
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
        val id = i?.getLongExtra("id", 0L) ?: 0L
        when (i?.action) {
            "STOP" -> { end(id, false); return START_NOT_STICKY }
            "SNOOZE" -> { end(id, true); return START_NOT_STICKY }
        }
        val a = Store.get(this, id)
        val n = notif(id, a?.title ?: "Alarm")
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(1, n)
        wl = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "voicealarm:ring").also { it.acquire(11 * 60 * 1000L) }
        if (a != null && File(a.path).exists()) {
            try {
                mp?.release()
                mp = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    setDataSource(a.path); isLooping = true; prepare(); start()
                }
            } catch (e: Exception) { }
        }
        @Suppress("DEPRECATION")
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 300), 0))
        h.postDelayed({ end(id, false) }, 10 * 60 * 1000L)
        return START_NOT_STICKY
    }

    private fun notif(id: Long, title: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("alarm", "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null); enableVibration(false); lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val full = PendingIntent.getActivity(this, 1,
            Intent(this, AlarmActivity::class.java).putExtra("id", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags)
        fun act(code: Int, name: String, label: String) = Notification.Action.Builder(
            Icon.createWithResource(this, android.R.drawable.ic_lock_idle_alarm), label,
            PendingIntent.getService(this, code, Intent(this, AlarmService::class.java).setAction(name).putExtra("id", id), flags)).build()
        return Notification.Builder(this, "alarm").setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title).setContentText("Voice reminder").setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true).setFullScreenIntent(full, true).setContentIntent(full)
            .addAction(act(2, "SNOOZE", "Snooze 5 min")).addAction(act(3, "STOP", "Stop")).build()
    }

    private fun end(id: Long, snooze: Boolean) {
        h.removeCallbacksAndMessages(null)
        mp?.let { try { it.stop() } catch (e: Exception) { }; it.release() }; mp = null
        if (wl?.isHeld == true) wl?.release()
        @Suppress("DEPRECATION")
        (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
        Store.get(this, id)?.let { a ->
            if (snooze) {
                val s = a.copy(whenMs = System.currentTimeMillis() + 5 * 60000, done = false)
                Store.update(this, s); Scheduler.set(this, s)
            } else Store.update(this, a.copy(done = true))
        }
        AlarmActivity.current?.finish()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }

    override fun onDestroy() {
        mp?.release(); mp = null
        if (wl?.isHeld == true) wl?.release()
        super.onDestroy()
    }
}

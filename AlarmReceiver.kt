package com.galabs.voicealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val id = i.getLongExtra("id", 0L)
        try {
            c.startForegroundService(Intent(c, AlarmService::class.java).setAction("START").putExtra("id", id))
        } catch (e: Exception) { /* system blocked the service start; nothing else to do */ }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Store.all(c).filter { !it.done && it.whenMs > System.currentTimeMillis() }.forEach { Scheduler.set(c, it) }
    }
}

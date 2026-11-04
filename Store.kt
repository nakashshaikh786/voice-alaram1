package com.galabs.voicealarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Alarm(val id: Long, val title: String, val whenMs: Long, val path: String, val done: Boolean = false)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    fun all(c: Context): MutableList<Alarm> {
        val a = JSONArray(sp(c).getString("list", "[]")); val out = mutableListOf<Alarm>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out.add(Alarm(o.getLong("id"), o.getString("title"), o.getLong("when"), o.getString("path"), o.optBoolean("done")))
        }
        return out
    }
    private fun save(c: Context, l: List<Alarm>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("title", it.title).put("when", it.whenMs).put("path", it.path).put("done", it.done)) }
        sp(c).edit().putString("list", a.toString()).apply()
    }
    fun get(c: Context, id: Long): Alarm? = all(c).firstOrNull { it.id == id }
    fun update(c: Context, a: Alarm) {
        val l = all(c); val i = l.indexOfFirst { it.id == a.id }
        if (i >= 0) l[i] = a else l.add(a)
        save(c, l)
    }
    fun remove(c: Context, id: Long) {
        val l = all(c); l.firstOrNull { it.id == id }?.let { File(it.path).delete() }
        save(c, l.filter { it.id != id })
    }
}

object Scheduler {
    private fun pi(c: Context, id: Long): PendingIntent = PendingIntent.getBroadcast(
        c, (id and 0x7fffffff).toInt(), Intent(c, AlarmReceiver::class.java).putExtra("id", id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun set(c: Context, a: Alarm) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val show = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(a.whenMs, show), pi(c, a.id))
    }
    fun cancel(c: Context, id: Long) {
        (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi(c, id))
    }
}

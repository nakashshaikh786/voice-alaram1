package com.galabs.voicealarm

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : Activity() {
    companion object { var current: AlarmActivity? = null }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        current = this
        setShowWhenLocked(true); setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val id = intent.getLongExtra("id", 0L)
        val a = Store.get(this, id)
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(0xFFC62D3A.toInt()); setPadding(48, 48, 48, 48)
        }
        l.addView(TextView(this).apply {
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 80f; setTypeface(null, Typeface.BOLD); setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
        })
        l.addView(TextView(this).apply {
            text = a?.title ?: "Alarm"; textSize = 28f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER
        })
        fun btn(label: String, action: String) = Button(this).apply {
            text = label; textSize = 20f
            setOnClickListener { startService(Intent(this@AlarmActivity, AlarmService::class.java).setAction(action).putExtra("id", id)); finish() }
        }
        l.addView(btn("Snooze 5 min", "SNOOZE"), LinearLayout.LayoutParams(-1, 180).apply { topMargin = 80 })
        l.addView(btn("Stop", "STOP"), LinearLayout.LayoutParams(-1, 180).apply { topMargin = 30 })
        setContentView(l)
    }

    override fun onDestroy() { if (current === this) current = null; super.onDestroy() }
}

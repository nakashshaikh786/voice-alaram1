package com.galabs.voicealarm

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private var rec: MediaRecorder? = null
    private var recFile: File? = null
    private var t0 = 0L
    private val h = Handler(Looper.getMainLooper())
    private val cal: Calendar = Calendar.getInstance()
    private val fmt = SimpleDateFormat("EEE d MMM yyyy, HH:mm", Locale.getDefault())
    private lateinit var recBtn: Button
    private lateinit var timer: TextView
    private lateinit var after: LinearLayout
    private lateinit var titleIn: EditText
    private lateinit var whenTv: TextView
    private lateinit var listBox: LinearLayout
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private val tick = object : Runnable {
        override fun run() {
            val s = ((System.currentTimeMillis() - t0) / 1000).toInt()
            timer.text = "${s / 60}:${"%02d".format(s % 60)}"
            if (s >= 120) stopRec() else h.postDelayed(this, 250)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(36), dp(18), dp(18)) }
        root.addView(TextView(this).apply { text = "Voice Alarm"; textSize = 26f; setTypeface(null, Typeface.BOLD) })
        recBtn = Button(this).apply { text = "● Record"; textSize = 20f; setOnClickListener { if (rec == null) startRec() else stopRec() } }
        root.addView(recBtn, LinearLayout.LayoutParams(-1, dp(72)).apply { topMargin = dp(16) })
        timer = TextView(this).apply { text = "0:00"; textSize = 20f; gravity = Gravity.CENTER }
        root.addView(timer)
        after = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        titleIn = EditText(this).apply { hint = "Title (optional)" }
        whenTv = TextView(this).apply { textSize = 16f }
        val pick = LinearLayout(this); val acts = LinearLayout(this)
        pick.addView(Button(this).apply { text = "Pick date"; setOnClickListener { pickDate() } })
        pick.addView(Button(this).apply { text = "Pick time"; setOnClickListener { pickTime() } })
        acts.addView(Button(this).apply { text = "Set alarm"; setOnClickListener { save() } })
        acts.addView(Button(this).apply { text = "Discard"; setOnClickListener { discard() } })
        after.addView(titleIn); after.addView(whenTv); after.addView(pick); after.addView(acts)
        root.addView(after)
        root.addView(TextView(this).apply { text = "Your alarms"; textSize = 18f; setTypeface(null, Typeface.BOLD); setPadding(0, dp(20), 0, dp(6)) })
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(listBox)
        root.addView(Button(this).apply { text = "Allow ringing in background (battery)"; setOnClickListener { battery() } })
        setContentView(ScrollView(this).apply { addView(root) })
        val need = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) need.add(Manifest.permission.POST_NOTIFICATIONS)
        val miss = need.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (miss.isNotEmpty()) requestPermissions(miss.toTypedArray(), 1)
    }

    override fun onResume() { super.onResume(); refresh() }

    @Suppress("DEPRECATION")
    private fun newRec(): MediaRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()

    private fun startRec() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1); return
        }
        val f = File(filesDir, "rec_${System.currentTimeMillis()}.m4a")
        try {
            val r = newRec()
            r.setAudioSource(MediaRecorder.AudioSource.MIC); r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); r.setAudioEncodingBitRate(96000); r.setAudioSamplingRate(44100)
            r.setOutputFile(f.absolutePath); r.prepare(); r.start()
            rec = r; recFile = f
        } catch (e: Exception) { Toast.makeText(this, "Cannot record: ${e.message}", Toast.LENGTH_LONG).show(); return }
        after.visibility = View.GONE; recBtn.text = "■ Stop"; t0 = System.currentTimeMillis(); h.post(tick)
    }

    private fun stopRec() {
        h.removeCallbacks(tick)
        try { rec?.stop() } catch (e: RuntimeException) { recFile?.delete(); recFile = null }
        rec?.release(); rec = null; recBtn.text = "● Record"
        if (recFile != null) { cal.timeInMillis = System.currentTimeMillis() + 10 * 60000; showWhen(); after.visibility = View.VISIBLE }
    }

    private fun showWhen() { whenTv.text = "Alarm: " + fmt.format(cal.time) }
    private fun pickDate() = DatePickerDialog(this, { _, y, m, d -> cal.set(y, m, d); showWhen() },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    private fun pickTime() = TimePickerDialog(this, { _, hh, mm -> cal.set(Calendar.HOUR_OF_DAY, hh); cal.set(Calendar.MINUTE, mm); showWhen() },
        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()

    private fun save() {
        val f = recFile ?: return
        cal.set(Calendar.SECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) { Toast.makeText(this, "Pick a time in the future", Toast.LENGTH_SHORT).show(); return }
        val a = Alarm(System.currentTimeMillis(), titleIn.text.toString().trim().ifEmpty { "Reminder" }, cal.timeInMillis, f.absolutePath)
        Store.update(this, a); Scheduler.set(this, a)
        recFile = null; titleIn.setText(""); after.visibility = View.GONE; timer.text = "0:00"; refresh()
        Toast.makeText(this, "Alarm set for " + fmt.format(Date(a.whenMs)), Toast.LENGTH_LONG).show()
    }

    private fun discard() { recFile?.delete(); recFile = null; after.visibility = View.GONE; timer.text = "0:00" }

    private fun refresh() {
        listBox.removeAllViews()
        val l = Store.all(this).sortedWith(compareBy({ it.done }, { it.whenMs }))
        if (l.isEmpty()) listBox.addView(TextView(this).apply { text = "No alarms yet. Tap Record." })
        for (a in l) listBox.addView(TextView(this).apply {
            text = a.title + "\n" + fmt.format(Date(a.whenMs)) + (if (a.done) " - done" else "")
            textSize = 16f; setPadding(0, dp(12), 0, dp(12)); alpha = if (a.done) 0.5f else 1f
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity).setTitle(a.title).setItems(arrayOf("Play", "Delete")) { _, w ->
                    if (w == 0) play(a) else { Scheduler.cancel(this@MainActivity, a.id); Store.remove(this@MainActivity, a.id); refresh() }
                }.show()
            }
        })
    }

    private fun play(a: Alarm) {
        try { MediaPlayer().apply { setDataSource(a.path); prepare(); start(); setOnCompletionListener { it.release() } } }
        catch (e: Exception) { Toast.makeText(this, "Cannot play: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun battery() {
        try { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))) }
        catch (e: Exception) { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }
}

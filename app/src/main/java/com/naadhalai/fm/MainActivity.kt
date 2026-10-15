package com.naadhalai.fm

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private const val MOOD = "💜 உன் உணர்வின் இசை"
private const val FM = "📻 FM Stations"
private const val DIRECTORS = "🎼 Music Directors"
private const val ACTORS = "🎬 Actors"
private const val OWN = "🎙️ My Own FM"
private const val DEVOTIONAL = "🙏 Devotional Songs"
private const val YEARS = "📅 Year Wise Songs"

data class SongTrack(val name: String, val uri: String, val category: String)
data class RadioStation(val name: String, val category: String, val url: String)

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private lateinit var root: LinearLayout
    private lateinit var currentName: TextView
    private lateinit var status: TextView
    private lateinit var seek: SeekBar
    private lateinit var elapsed: TextView
    private lateinit var duration: TextView
    private lateinit var playPause: Button
    private val handler = Handler(Looper.getMainLooper())
    private var seeking = false
    private var playerPage = false
    private var selectedCategory = MOOD
    private var selectedStation: RadioStation? = null
    private var playlist: List<SongTrack> = emptyList()
    private val songs = mutableListOf<SongTrack>()
    private val stations = mutableListOf<RadioStation>()

    private val bg = Color.rgb(248, 247, 252)
    private val card = Color.WHITE
    private val lavender = Color.rgb(185, 167, 255)
    private val purple = Color.rgb(91, 68, 145)
    private val mint = Color.rgb(90, 183, 161)
    private val dark = Color.rgb(38, 35, 54)
    private val muted = Color.rgb(115, 111, 130)
    private val categories = listOf(MOOD, FM, DIRECTORS, ACTORS, OWN, DEVOTIONAL, YEARS)
    private val radioCategories = setOf(FM, DIRECTORS, ACTORS, OWN)

    private val ticker = object : Runnable {
        override fun run() {
            val p = player
            if (p != null && playerPage && ::seek.isInitialized && !seeking) {
                val d = p.duration
                if (d > 0 && d < Long.MAX_VALUE) {
                    seek.max = d.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    seek.progress = p.currentPosition.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    elapsed.text = time(p.currentPosition)
                    duration.text = time(d)
                } else {
                    elapsed.text = time(p.currentPosition)
                    duration.text = if (selectedStation != null) "LIVE" else "00:00"
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
                if (songs.none { it.uri == uri.toString() && it.category == OWN }) {
                    songs.add(SongTrack(audioName(uri), uri.toString(), OWN))
                }
            }
            saveSongs()
            selectedCategory = OWN
            playPlaylist(songs.filter { it.category == OWN }, songs.filter { it.category == OWN }.size - uris.size.coerceAtLeast(1))
            toast("Audio கோப்புகள் My Own FM-ல் சேர்க்கப்பட்டன.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        loadSongs()
        loadStations()
        showHome()
        handler.post(ticker)
    }

    private fun loadSongs() {
        val s = getSharedPreferences("naadhalai_music", MODE_PRIVATE).getString("songs_json", null) ?: return
        try {
            val a = JSONArray(s)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val old = o.optString("category")
                val cat = when (old) { "My Own FM" -> OWN; "❤️ Love Songs", "💜 Love Songs" -> MOOD; else -> old }
                songs.add(SongTrack(o.optString("name"), o.optString("uri"), cat))
            }
        } catch (_: Exception) { songs.clear() }
    }

    private fun saveSongs() {
        val a = JSONArray()
        songs.forEach { s -> a.put(JSONObject().apply { put("name", s.name); put("uri", s.uri); put("category", s.category) }) }
        getSharedPreferences("naadhalai_music", MODE_PRIVATE).edit().putString("songs_json", a.toString()).apply()
    }

    private fun loadStations() {
        val saved = getSharedPreferences("naadhalai_radio", MODE_PRIVATE).getString("stations_json", null)
        if (saved == null) {
            stations.addAll(listOf(RadioStation("இளையராஜா FM", DIRECTORS, ""), RadioStation("A. R. ரஹ்மான் FM", DIRECTORS, ""), RadioStation("அனிருத் FM", DIRECTORS, ""), RadioStation("விஜய் FM", ACTORS, ""), RadioStation("அஜித் FM", ACTORS, "")))
            saveStations()
        } else try {
            val a = JSONArray(saved)
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val cat = when (o.optString("category")) { "Music Directors FM" -> DIRECTORS; "Actors FM" -> ACTORS; "My Own FM" -> OWN; else -> o.optString("category") }
                stations.add(RadioStation(o.optString("name"), cat, o.optString("url")))
            }
        } catch (_: Exception) { stations.clear() }
    }

    private fun saveStations() {
        val a = JSONArray()
        stations.forEach { s -> a.put(JSONObject().apply { put("name", s.name); put("category", s.category); put("url", s.url) }) }
        getSharedPreferences("naadhalai_radio", MODE_PRIVATE).edit().putString("stations_json", a.toString()).apply()
    }

    private fun showHome() {
        playerPage = false
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)); setBackgroundColor(bg) }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(label("♫  நாதலை FM", 30, purple, true, Gravity.CENTER))
        body.addView(label("உன் உணர்வின் இசை", 20, purple, true, Gravity.CENTER))
        body.addView(label("உன் மனதின் ஓசை…", 15, mint, false, Gravity.CENTER).apply { setPadding(0, 0, 0, dp(18)) })
        body.addView(button("🎧  Player திறக்க", lavender, dark) { showPlayer() }, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(12) })
        body.addView(label("உங்கள் இசை உலகம்", 18, purple, true, Gravity.START).apply { setPadding(0, dp(10), 0, dp(8)) })
        categories.forEach { cat ->
            body.addView(button(cat, if (cat == selectedCategory) lavender else card, if (cat == selectedCategory) dark else purple) {
                selectedCategory = cat
                showHome()
            }, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(7) })
        }
        when {
            selectedCategory == OWN -> {
                body.addView(message("My Own FM: உங்கள் audio கோப்புகளைத் தேர்ந்தெடுத்து playlist உருவாக்கலாம்."))
                body.addView(button("＋ Audio கோப்புகளைச் சேர்", lavender, dark) { audioPicker.launch(arrayOf("audio/*")) }, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(8) })
                body.addView(button("＋ Internet Radio Stream சேர்க்க", card, purple) { addRadioDialog(OWN) }, LinearLayout.LayoutParams(-1, dp(50)))
                val own = songs.filter { it.category == OWN }
                own.forEachIndexed { i, s -> body.addView(row("♫ ${s.name}") { playPlaylist(own, i) }) }
                stations.filter { it.category == OWN }.forEach { s -> body.addView(row("📻 ${s.name}") { playStation(s) }) }
            }
            selectedCategory in radioCategories -> {
                stations.filter { it.category == selectedCategory }.forEach { s -> body.addView(row("📻 ${s.name}") { playStation(s) }) }
                body.addView(button("＋ Radio Stream சேர்க்க", lavender, dark) { addRadioDialog(selectedCategory) }, LinearLayout.LayoutParams(-1, dp(50)))
            }
            else -> {
                val matching = songs.filter { it.category == selectedCategory }
                if (matching.isEmpty()) body.addView(message("இந்தப் பிரிவில் playlist இன்னும் இல்லை. உரிமம் உள்ள பாடல்கள்/stream கிடைத்ததும் சேர்க்கலாம்."))
                matching.forEachIndexed { i, s -> body.addView(row("♫ ${s.name}") { playPlaylist(matching, i) }) }
            }
        }
        body.addView(label("TAVRIXLABS • நாதலை FM", 12, muted, false, Gravity.CENTER).apply { setPadding(0, dp(18), 0, dp(12)) })
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        window.statusBarColor = bg
        window.navigationBarColor = bg
    }

    private fun showPlayer() {
        playerPage = true
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(14)); setBackgroundColor(bg) }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        body.addView(button("‹  HOME", card, purple) { showHome() }, LinearLayout.LayoutParams(-1, dp(44)).apply { bottomMargin = dp(14) })
        body.addView(label("NOW PLAYING", 13, muted, true, Gravity.CENTER))
        body.addView(label("♫", 72, purple, true, Gravity.CENTER).apply {
            background = rounded(card, 26)
            layoutParams = LinearLayout.LayoutParams(-1, dp(220)).apply { topMargin = dp(14); bottomMargin = dp(18) }
        })
        currentName = label(currentTrackName(), 22, dark, true, Gravity.CENTER)
        body.addView(currentName)
        body.addView(label("உன் உணர்வின் இசை • நாதலை FM", 14, mint, false, Gravity.CENTER).apply { setPadding(0, dp(7), 0, dp(8)) })
        status = label(if (player?.isPlaying == true) "இசை இயங்குகிறது…" else "பாடலைத் தேர்ந்தெடுக்கவும்", 14, muted, false, Gravity.CENTER)
        body.addView(status)
        seek = SeekBar(this).apply {
            progressTintList = android.content.res.ColorStateList.valueOf(purple)
            thumbTintList = android.content.res.ColorStateList.valueOf(purple)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(b: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) elapsed.text = time(progress.toLong()) }
                override fun onStartTrackingTouch(b: SeekBar?) { seeking = true }
                override fun onStopTrackingTouch(b: SeekBar?) { if ((player?.duration ?: 0L) > 0L) player?.seekTo(b?.progress?.toLong() ?: 0L); seeking = false }
            })
        }
        body.addView(seek, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(8) })
        val times = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        elapsed = label("00:00", 13, muted, false, Gravity.START)
        duration = label("00:00", 13, muted, false, Gravity.END)
        times.addView(elapsed, LinearLayout.LayoutParams(0, -2, 1f))
        times.addView(duration, LinearLayout.LayoutParams(0, -2, 1f))
        body.addView(times)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(22), 0, dp(18)) }
        controls.addView(button("⏮", card, purple) { previous() })
        playPause = button("▶", lavender, dark) { toggle() }
        controls.addView(playPause, LinearLayout.LayoutParams(dp(88), dp(64)).apply { leftMargin = dp(12); rightMargin = dp(12) })
        controls.addView(button("⏭", card, purple) { next() })
        body.addView(controls)
        body.addView(label("UP NEXT", 14, purple, true, Gravity.START).apply { setPadding(0, dp(10), 0, dp(8)) })
        body.addView(button("☷  Playlist / Queue", card, purple) { queueDialog() }, LinearLayout.LayoutParams(-1, dp(48)))
        body.addView(button("⌂  முகப்புக்குச் செல்", card, purple) { showHome() }, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(10) })
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        updatePlayer()
    }

    private fun playPlaylist(list: List<SongTrack>, index: Int) {
        if (list.isEmpty()) { toast("முதலில் பாடல்களைச் சேர்க்கவும்."); return }
        playlist = list.toList()
        selectedStation = null
        val start = index.coerceIn(0, playlist.lastIndex)
        val p = getPlayer()
        p.setMediaItems(playlist.map { MediaItem.fromUri(it.uri) }, start, 0L)
        p.prepare()
        p.play()
        showPlayer()
    }

    private fun playStation(station: RadioStation) {
        selectedStation = station
        playlist = emptyList()
        if (station.url.isBlank()) { addUrlDialog(station); return }
        try {
            val p = getPlayer()
            p.setMediaItem(MediaItem.fromUri(station.url))
            p.prepare()
            p.play()
            showPlayer()
            currentName.text = station.name
            duration.text = "LIVE"
        } catch (_: Exception) { toast("Radio தொடங்க முடியவில்லை.") }
    }

    private fun toggle() {
        val p = player ?: run { toast("முதலில் பாடலைத் தேர்ந்தெடுக்கவும்."); return }
        if (p.isPlaying) p.pause() else if (p.mediaItemCount > 0) p.play() else toast("முதலில் பாடல் தேர்ந்தெடுக்கவும்.")
        playPause.text = if (p.isPlaying) "Ⅱ" else "▶"
    }

    private fun previous() {
        val p = player ?: return
        if (p.hasPreviousMediaItem()) p.seekToPreviousMediaItem()
        else if (playlist.isNotEmpty()) p.seekTo(playlist.lastIndex, 0L)
        else toast("Previous பாடல் இல்லை.")
    }

    private fun next() {
        val p = player ?: return
        if (p.hasNextMediaItem()) p.seekToNextMediaItem()
        else if (playlist.isNotEmpty()) p.seekTo(0, 0L)
        else toast("Next பாடல் இல்லை. Playlist சேர்க்கவும்.")
    }

    private fun getPlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(this).build().also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (::status.isInitialized) status.text = when (state) {
                            Player.STATE_BUFFERING -> "இசை ஏற்றப்படுகிறது…"
                            Player.STATE_READY -> if (selectedStation != null) "Radio இணைக்கிறது…" else "இசை இயங்குகிறது…"
                            Player.STATE_ENDED -> "Playlist முடிந்தது."
                            else -> status.text
                        }
                        updatePlayer()
                    }
                    override fun onMediaItemTransition(item: MediaItem?, reason: Int) { updatePlayer() }
                    override fun onIsPlayingChanged(isPlaying: Boolean) { if (::playPause.isInitialized) playPause.text = if (isPlaying) "Ⅱ" else "▶" }
                    override fun onPlayerError(error: PlaybackException) {
                        if (::status.isInitialized) status.text = "Playback பிழை: ${error.errorCodeName}"
                    }
                })
            }
        }
        return player!!
    }

    private fun updatePlayer() {
        if (!::currentName.isInitialized) return
        currentName.text = if (selectedStation != null) selectedStation!!.name
        else playlist.getOrNull(player?.currentMediaItemIndex ?: -1)?.name ?: "உங்கள் இசை உலகம்"
        if (::playPause.isInitialized) playPause.text = if (player?.isPlaying == true) "Ⅱ" else "▶"
        if (::duration.isInitialized && selectedStation != null) duration.text = "LIVE"
    }

    private fun queueDialog() {
        if (playlist.isEmpty()) { toast("இப்போது playlist இல்லை."); return }
        val current = player?.currentMediaItemIndex ?: 0
        val labels = playlist.mapIndexed { i, s -> "${if (i == current) "▶ " else ""}${i + 1}. ${s.name}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Playlist / Queue").setItems(labels) { _, which ->
            player?.seekTo(which, 0L); player?.play(); updatePlayer()
        }.setNegativeButton("← பின்செல்", null).show()
    }

    private fun addRadioDialog(category: String) {
        val name = EditText(this).apply { hint = "Radio பெயர்"; setSingleLine(true) }
        val url = EditText(this).apply { hint = "https://stream-url"; setSingleLine(true) }
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0); addView(name); addView(url) }
        AlertDialog.Builder(this).setTitle("Radio Stream சேர்க்க").setView(form).setNegativeButton("ரத்து", null)
            .setPositiveButton("சேமி") { _, _ ->
                val n = name.text.toString().trim(); val u = url.text.toString().trim()
                if (n.isBlank() || !validUrl(u)) { toast("Radio பெயர் மற்றும் சரியான URL கொடுக்கவும்."); return@setPositiveButton }
                stations.add(RadioStation(n, category, u)); saveStations(); showHome(); toast("Radio சேமிக்கப்பட்டது.")
            }.show()
    }

    private fun addUrlDialog(station: RadioStation) {
        val input = EditText(this).apply { hint = "https://stream-url"; setSingleLine(true) }
        AlertDialog.Builder(this).setTitle("${station.name} Stream URL").setView(input).setNegativeButton("ரத்து", null)
            .setPositiveButton("சேமி") { _, _ ->
                val u = input.text.toString().trim()
                if (!validUrl(u)) { toast("சரியான URL கொடுக்கவும்."); return@setPositiveButton }
                val i = stations.indexOf(station); val updated = station.copy(url = u)
                if (i >= 0) stations[i] = updated
                saveStations(); playStation(updated)
            }.show()
    }

    private fun row(title: String, action: () -> Unit): LinearLayout {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(8), dp(8), dp(8)); background = rounded(card, 15) }
        r.addView(label(title, 15, dark, true, Gravity.CENTER_VERTICAL), LinearLayout.LayoutParams(0, -2, 1f))
        r.addView(button("▶", mint, Color.WHITE, action))
        r.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
        return r
    }

    private fun message(s: String) = label(s, 14, muted, false, Gravity.CENTER).apply {
        setPadding(dp(12), dp(16), dp(12), dp(16)); background = rounded(card, 16)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) }
    }

    private fun label(s: String, size: Int, color: Int, bold: Boolean, gravityValue: Int) = TextView(this).apply {
        text = s; textSize = size.toFloat(); setTextColor(color); gravity = gravityValue
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun button(s: String, color: Int, textColor: Int, action: () -> Unit) = Button(this).apply {
        text = s; textSize = 15f; setTextColor(textColor); background = rounded(color, 16); setOnClickListener { action() }
    }

    private fun audioName(uri: Uri): String {
        var name = "Audio Track"
        try {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) { val i = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (i >= 0) name = it.getString(i) ?: name }
            }
        } catch (_: Exception) { }
        return name
    }

    private fun currentTrackName(): String = playlist.getOrNull(player?.currentMediaItemIndex ?: -1)?.name ?: selectedStation?.name ?: "உங்கள் இசை உலகம்"
    private fun time(ms: Long): String {
        if (ms < 0 || ms == Long.MAX_VALUE) return "00:00"
        val sec = ms / 1000
        return String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60)
    }
    private fun validUrl(s: String) = (s.startsWith("https://") || s.startsWith("http://")) && android.util.Patterns.WEB_URL.matcher(s).matches()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        player?.release()
        player = null
        super.onDestroy()
    }
}

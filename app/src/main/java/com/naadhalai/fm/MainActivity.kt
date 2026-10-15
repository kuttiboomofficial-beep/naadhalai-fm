package com.naadhalai.fm

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private lateinit var content: LinearLayout
    private lateinit var currentName: TextView
    private lateinit var status: TextView
    private lateinit var playButton: Button

    private val bg = Color.rgb(16, 20, 38)
    private val panel = Color.rgb(41, 37, 65)
    private val lavender = Color.rgb(185, 167, 255)
    private val mint = Color.rgb(145, 230, 208)
    private val white = Color.rgb(245, 243, 250)
    private val categories = listOf(MOOD, FM, DIRECTORS, ACTORS, OWN, DEVOTIONAL, YEARS)
    private val radioCategories = setOf(FM, DIRECTORS, ACTORS, OWN)

    private var selectedCategory = MOOD
    private var selectedStation: RadioStation? = null
    private val songs = mutableListOf<SongTrack>()
    private val stations = mutableListOf<RadioStation>()

    private val audioPicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            var added = 0
            uris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) { }
                if (songs.none { it.uri == uri.toString() && it.category == OWN }) {
                    songs.add(SongTrack(getAudioName(uri), uri.toString(), OWN))
                    added++
                }
            }
            saveSongs()
            selectedCategory = OWN
            buildUi()
            playOwnPlaylist(0)
            toast("$added audio கோப்பு(கள்) My Own FM-ல் சேர்க்கப்பட்டது.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        loadSongs()
        loadStations()
        buildUi()
    }

    private fun loadSongs() {
        val saved = getSharedPreferences("naadhalai_music", MODE_PRIVATE)
            .getString("songs_json", null) ?: return
        try {
            val arr = JSONArray(saved)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val old = item.optString("category")
                val category = if (old == "My Own FM") OWN else old
                songs.add(SongTrack(item.optString("name"), item.optString("uri"), category))
            }
        } catch (_: Exception) { songs.clear() }
    }

    private fun saveSongs() {
        val arr = JSONArray()
        songs.forEach { song ->
            arr.put(JSONObject().apply {
                put("name", song.name); put("uri", song.uri); put("category", song.category)
            })
        }
        getSharedPreferences("naadhalai_music", MODE_PRIVATE).edit()
            .putString("songs_json", arr.toString()).apply()
    }

    private fun loadStations() {
        val prefs = getSharedPreferences("naadhalai_radio", MODE_PRIVATE)
        val saved = prefs.getString("stations_json", null)
        if (saved == null) {
            stations.addAll(listOf(
                RadioStation("இளையராஜா FM", DIRECTORS, ""),
                RadioStation("A. R. ரஹ்மான் FM", DIRECTORS, ""),
                RadioStation("அனிருத் FM", DIRECTORS, ""),
                RadioStation("விஜய் FM", ACTORS, ""),
                RadioStation("அஜித் FM", ACTORS, "")
            ))
            saveStations()
            return
        }
        try {
            val arr = JSONArray(saved)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val old = item.optString("category")
                val category = when (old) {
                    "Music Directors FM" -> DIRECTORS
                    "Actors FM" -> ACTORS
                    "My Own FM" -> OWN
                    else -> old
                }
                stations.add(RadioStation(item.optString("name"), category, item.optString("url")))
            }
        } catch (_: Exception) { stations.clear() }
    }

    private fun saveStations() {
        val arr = JSONArray()
        stations.forEach { station ->
            arr.put(JSONObject().apply {
                put("name", station.name); put("category", station.category); put("url", station.url)
            })
        }
        getSharedPreferences("naadhalai_radio", MODE_PRIVATE).edit()
            .putString("stations_json", arr.toString()).apply()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(12))
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        content.addView(TextView(this).apply {
            text = "♫  நாதலை FM"; textSize = 30f; typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setTextColor(white); setPadding(0, dp(12), 0, dp(2))
        })
        content.addView(TextView(this).apply {
            text = "உன் உணர்வின் இசை"; textSize = 19f; typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setTextColor(lavender)
        })
        content.addView(TextView(this).apply {
            text = "உன் மனதின் ஓசை…"; textSize = 14f
            gravity = Gravity.CENTER; setTextColor(mint); setPadding(0, 0, 0, dp(18))
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(dp(16), dp(20), dp(16), dp(18)); background = rounded(panel, 24)
        }
        currentName = TextView(this).apply {
            text = selectedStation?.name ?: "உங்கள் இசை உலகம்"
            textSize = 22f; typeface = Typeface.DEFAULT_BOLD; setTextColor(white); gravity = Gravity.CENTER
        }
        card.addView(currentName)
        status = TextView(this).apply {
            text = "உனக்குப் பிடித்த இசையைத் தேர்ந்தெடு"; textSize = 14f
            setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(14))
        }
        card.addView(status)
        playButton = Button(this).apply {
            text = if (player?.isPlaying == true) "Ⅱ  PAUSE" else "▶  PLAY"
            setTextColor(bg); background = rounded(mint, 28)
            setOnClickListener { togglePlayback() }
        }
        card.addView(playButton, LinearLayout.LayoutParams(-1, dp(52)))
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        addSectionTitle("உங்கள் இசை உலகம்")
        categories.forEach { category ->
            content.addView(Button(this).apply {
                text = category; textSize = 16f
                setTextColor(if (selectedCategory == category) bg else white)
                background = rounded(if (selectedCategory == category) lavender else panel, 16)
                setOnClickListener { selectedCategory = category; buildUi() }
            }, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(7) })
        }

        when {
            selectedCategory == OWN -> buildOwnSection()
            selectedCategory in radioCategories -> buildRadioSection()
            else -> buildPlaylistSection(selectedCategory)
        }

        content.addView(TextView(this).apply {
            text = "TAVRIXLABS  •  நாதலை FM"; textSize = 12f
            setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, dp(20), 0, dp(12))
        })
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun buildOwnSection() {
        addSectionTitle("🎙️ My Own FM")
        addMessage("Audio கோப்புகளைத் தேர்ந்தெடுத்து playlist உருவாக்கலாம்; அல்லது அனுமதிக்கப்பட்ட Internet Radio stream சேர்க்கலாம்.")
        content.addView(Button(this).apply {
            text = "＋  Audio கோப்புகளைச் சேர்"; setTextColor(bg)
            background = rounded(lavender, 16)
            setOnClickListener { audioPicker.launch(arrayOf("audio/*")) }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(8) })
        content.addView(Button(this).apply {
            text = "＋  Internet Radio Stream சேர்க்க"; setTextColor(white)
            background = rounded(panel, 16)
            setOnClickListener { showAddRadioDialog(OWN) }
        }, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(12) })

        val ownSongs = songs.filter { it.category == OWN }
        if (ownSongs.isEmpty()) addMessage("இன்னும் audio கோப்புகள் சேர்க்கப்படவில்லை.")
        ownSongs.forEachIndexed { index, song ->
            content.addView(listRow("♫  ${song.name}") { playOwnPlaylist(index) })
        }
        stations.filter { it.category == OWN }.forEach { station ->
            content.addView(listRow("📻  ${station.name}") { selectStation(station) })
        }
    }

    private fun buildRadioSection() {
        addSectionTitle(selectedCategory)
        val matching = stations.filter { it.category == selectedCategory }
        if (matching.isEmpty()) addMessage("இந்தப் பிரிவில் station இல்லை. அனுமதிக்கப்பட்ட stream URL சேர்க்கலாம்.")
        matching.forEach { station ->
            content.addView(listRow("📻  ${station.name}") { selectStation(station) })
        }
        content.addView(Button(this).apply {
            text = "＋  புதிய Radio Stream சேர்க்க"; setTextColor(bg)
            background = rounded(lavender, 16)
            setOnClickListener { showAddRadioDialog(selectedCategory) }
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(8) })
    }

    private fun buildPlaylistSection(category: String) {
        addSectionTitle(category)
        val matching = songs.filter { it.category == category }
        if (matching.isEmpty()) {
            addMessage("இந்தப் பிரிவில் பாடல்கள் இன்னும் சேர்க்கப்படவில்லை.")
            addMessage("பாடல்களைச் சேர்ப்பதற்கான playlist வசதியை அடுத்த கட்டத்தில் அமைக்கலாம். Audio-க்கு தேவையான உரிமம் இருக்க வேண்டும்.")
        } else matching.forEach { song ->
            content.addView(listRow("♫  ${song.name}") { playSingleSong(song) })
        }
    }

    private fun listRow(title: String, onClick: () -> Unit): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(8), dp(8)); background = rounded(panel, 14)
        }
        row.addView(TextView(this).apply {
            text = title; textSize = 15f; setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD; setPadding(dp(4), dp(8), dp(6), dp(8))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply {
            text = "▶"; setTextColor(bg); background = rounded(mint, 12)
            setOnClickListener { onClick() }
        })
        row.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
        return row
    }

    private fun addSectionTitle(title: String) {
        content.addView(TextView(this).apply {
            text = title; textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(mint); setPadding(0, dp(14), 0, dp(10))
        })
    }

    private fun addMessage(message: String) {
        content.addView(TextView(this).apply {
            text = message; textSize = 14f; setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER; setPadding(dp(12), dp(16), dp(12), dp(16))
            background = rounded(panel, 14)
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
    }

    private fun playSingleSong(song: SongTrack) {
        selectedStation = null
        val p = getPlayer()
        p.setMediaItem(MediaItem.fromUri(song.uri))
        p.prepare(); p.play()
        currentName.text = song.name
        status.text = "இசை ஏற்றப்படுகிறது…"
        playButton.text = "Ⅱ  PAUSE"
    }

    private fun playOwnPlaylist(index: Int) {
        val playlist = songs.filter { it.category == OWN }
        if (playlist.isEmpty()) { toast("முதலில் My Own FM-ல் audio கோப்புகளைச் சேர்க்கவும்."); return }
        val start = index.coerceIn(0, playlist.lastIndex)
        selectedStation = null
        val p = getPlayer()
        p.setMediaItems(playlist.map { MediaItem.fromUri(it.uri) }, start, 0L)
        p.prepare(); p.play()
        currentName.text = playlist[start].name
        status.text = "My Own FM இயங்குகிறது…"
        playButton.text = "Ⅱ  PAUSE"
    }

    private fun selectStation(station: RadioStation) {
        selectedStation = station
        if (station.url.isBlank()) { showAddUrlDialog(station); return }
        try {
            val p = getPlayer()
            p.setMediaItem(MediaItem.fromUri(station.url))
            p.prepare(); p.play()
            currentName.text = station.name
            status.text = "Radio இணைக்கிறது…"
            playButton.text = "Ⅱ  PAUSE RADIO"
        } catch (_: Exception) {
            status.text = "Radio தொடங்க முடியவில்லை."
            toast("இந்த stream-ஐ இயக்க முடியவில்லை.")
        }
    }

    private fun togglePlayback() {
        val p = getPlayer()
        if (p.isPlaying) {
            p.pause(); playButton.text = "▶  PLAY"; status.text = "இசை இடைநிறுத்தப்பட்டது."
        } else if (p.mediaItemCount > 0) {
            p.play(); playButton.text = "Ⅱ  PAUSE"; status.text = "இசை இயங்குகிறது…"
        } else toast("முதலில் audio அல்லது radio stream தேர்ந்தெடு.")
    }

    private fun getPlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(this).build().also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> status.text = "இசை ஏற்றப்படுகிறது…"
                            Player.STATE_READY -> if (exo.isPlaying) status.text = "இசை இயங்குகிறது…"
                            Player.STATE_ENDED -> status.text = "Playlist முடிந்தது."
                        }
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        if (selectedStation == null) {
                            mediaItem?.localConfiguration?.uri?.let { currentName.text = getAudioName(it) }
                        }
                    }
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        playButton.text = if (isPlaying) "Ⅱ  PAUSE" else "▶  PLAY"
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        status.text = "Playback பிழை: ${error.errorCodeName}"
                        playButton.text = "▶  PLAY"
                    }
                })
            }
        }
        return player!!
    }

    private fun showAddRadioDialog(category: String) {
        val name = EditText(this).apply { hint = "Radio பெயர்"; setSingleLine(true) }
        val url = EditText(this).apply { hint = "https://stream-url"; setSingleLine(true) }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), 0)
            addView(name); addView(url)
        }
        AlertDialog.Builder(this).setTitle("Radio Stream சேர்க்க").setView(form)
            .setNegativeButton("ரத்து", null)
            .setPositiveButton("சேமி") { _, _ ->
                val n = name.text.toString().trim()
                val u = url.text.toString().trim()
                if (n.isBlank() || !isValidUrl(u)) {
                    toast("Radio பெயர் மற்றும் சரியான http/https URL கொடுக்கவும்.")
                    return@setPositiveButton
                }
                stations.add(RadioStation(n, category, u))
                saveStations(); selectedCategory = category; buildUi()
                toast("Radio சேமிக்கப்பட்டது.")
            }.show()
    }

    private fun showAddUrlDialog(station: RadioStation) {
        val input = EditText(this).apply { hint = "https://stream-url"; setSingleLine(true) }
        AlertDialog.Builder(this).setTitle("${station.name} Stream URL").setView(input)
            .setNegativeButton("ரத்து", null)
            .setPositiveButton("சேமி") { _, _ ->
                val url = input.text.toString().trim()
                if (!isValidUrl(url)) { toast("சரியான http/https URL கொடுக்கவும்."); return@setPositiveButton }
                val index = stations.indexOf(station)
                val updated = station.copy(url = url)
                if (index >= 0) stations[index] = updated
                saveStations(); selectStation(updated)
            }.show()
    }

    private fun getAudioName(uri: Uri): String {
        var name = "Audio Track"
        try {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) name = cursor.getString(index) ?: name
                    }
                }
        } catch (_: Exception) { }
        return name
    }

    private fun isValidUrl(url: String): Boolean =
        (url.startsWith("https://") || url.startsWith("http://")) &&
            android.util.Patterns.WEB_URL.matcher(url).matches()

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}

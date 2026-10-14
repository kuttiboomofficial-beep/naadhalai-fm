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

data class SongTrack(
    val name: String,
    val uri: String,
    val category: String
)

data class RadioStation(
    val name: String,
    val category: String,
    val url: String
)

class MainActivity : ComponentActivity() {

    private var player: ExoPlayer? = null

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var currentName: TextView
    private lateinit var status: TextView
    private lateinit var playButton: Button

    private val bg = Color.rgb(9, 12, 28)
    private val panel = Color.rgb(24, 29, 52)
    private val cyan = Color.rgb(63, 220, 242)
    private val violet = Color.rgb(163, 104, 255)

    private val musicCategories = listOf(
        "❤️ Love Songs",
        "💔 Sad Songs",
        "🔥 Kuthu Songs",
        "🙏 Devotional Songs",
        "🕰️ Old Songs"
    )

    private val radioCategories = listOf(
        "Music Directors FM",
        "Actors FM",
        "My Own FM"
    )

    private var selectedCategory = musicCategories[0]
    private var selectedStation: RadioStation? = null
    private var currentSongUri: Uri? = null

    private val songs = mutableListOf<SongTrack>()
    private val stations = mutableListOf<RadioStation>()

    private val songPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    val name = getSongName(uri)

                    val track = SongTrack(
                        name = name,
                        uri = uri.toString(),
                        category = selectedCategory
                    )

                    val existing = songs.any {
                        it.uri == track.uri &&
                            it.category == track.category
                    }

                    if (!existing) {
                        songs.add(track)
                        saveSongs()
                    }

                    selectedStation = null
                    currentSongUri = uri

                    val p = getPlayer()
                    p.setMediaItem(MediaItem.fromUri(uri))
                    p.prepare()
                    p.play()

                    buildUi()
                    currentName.text = name
                    status.text = "பாடல் இயங்குகிறது."
                    playButton.text = "Ⅱ  PAUSE SONG"

                    toast("பாடல் playlist-ல் சேர்க்கப்பட்டது!")

                } catch (e: Exception) {
                    status.text = "பாடலைத் திறக்க முடியவில்லை."
                    toast("மீண்டும் பாடலைத் தேர்ந்தெடுக்கவும்.")
                }
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
        val prefs = getSharedPreferences("naadhalai_music", MODE_PRIVATE)
        val saved = prefs.getString("songs_json", null) ?: return

        try {
            val array = JSONArray(saved)

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                songs.add(
                    SongTrack(
                        item.optString("name"),
                        item.optString("uri"),
                        item.optString("category")
                    )
                )
            }
        } catch (_: Exception) {
            songs.clear()
        }
    }

    private fun saveSongs() {
        val array = JSONArray()

        songs.forEach { song ->
            val item = JSONObject()
            item.put("name", song.name)
            item.put("uri", song.uri)
            item.put("category", song.category)
            array.put(item)
        }

        getSharedPreferences("naadhalai_music", MODE_PRIVATE)
            .edit()
            .putString("songs_json", array.toString())
            .apply()
    }

    private fun loadStations() {
        val prefs = getSharedPreferences("naadhalai_radio", MODE_PRIVATE)
        val saved = prefs.getString("stations_json", null)

        if (saved == null) {
            stations.addAll(
                listOf(
                    RadioStation("இளையராஜா FM", radioCategories[0], ""),
                    RadioStation("A.R. ரஹ்மான் FM", radioCategories[0], ""),
                    RadioStation("அனிருத் FM", radioCategories[0], ""),
                    RadioStation("விஜய் FM", radioCategories[1], ""),
                    RadioStation("அஜித் FM", radioCategories[1], "")
                )
            )
            saveStations()
            return
        }

        try {
            val array = JSONArray(saved)

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                stations.add(
                    RadioStation(
                        item.optString("name"),
                        item.optString("category"),
                        item.optString("url")
                    )
                )
            }
        } catch (_: Exception) {
            stations.clear()
        }
    }

    private fun saveStations() {
        val array = JSONArray()

        stations.forEach { station ->
            val item = JSONObject()
            item.put("name", station.name)
            item.put("category", station.category)
            item.put("url", station.url)
            array.put(item)
        }

        getSharedPreferences("naadhalai_radio", MODE_PRIVATE)
            .edit()
            .putString("stations_json", array.toString())
            .apply()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val brand = TextView(this).apply {
            text = "♫  நாதலை FM"
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, dp(12), 0, dp(4))
        }

        content.addView(brand)

        val subtitle = TextView(this).apply {
            text = "இசை அலைகள் உங்களுக்காக"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(cyan)
            setPadding(0, 0, 0, dp(22))
        }

        content.addView(subtitle)

        val playerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(22), dp(16), dp(20))
            background = rounded(panel, 24)
        }

        currentName = TextView(this).apply {
            text = currentSongUri?.let { getSongName(it) }
                ?: selectedStation?.name
                ?: "உங்கள் இசை உலகம்"

            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        playerCard.addView(currentName)

        status = TextView(this).apply {
            text = "உனக்குப் பிடித்த பாடலைத் தேர்ந்தெடு!"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(16))
        }

        playerCard.addView(status)

        playButton = Button(this).apply {
            text = if (player?.isPlaying == true) {
                "Ⅱ  PAUSE"
            } else {
                "▶  PLAY"
            }

            setTextColor(bg)
            background = rounded(cyan, 28)

            setOnClickListener {
                togglePlayback()
            }
        }

        playerCard.addView(
            playButton,
            LinearLayout.LayoutParams(-1, dp(54))
        )

        content.addView(
            playerCard,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(18)
            }
        )

        addSectionTitle("🎵  உனக்குப் பிடித்த இசை")

        musicCategories.forEach { category ->
            val button = Button(this).apply {
                text = category
                textSize = 16f
                setTextColor(Color.WHITE)

                background = rounded(
                    if (selectedCategory == category) violet else panel,
                    15
                )

                setOnClickListener {
                    selectedCategory = category
                    selectedStation = null
                    buildUi()
                }
            }

            content.addView(
                button,
                LinearLayout.LayoutParams(-1, dp(52)).apply {
                    bottomMargin = dp(8)
                }
            )
        }

        val addSongsButton = Button(this).apply {
            text = "＋  இந்தப் பிரிவில் பாடல்களைச் சேர்"
            setTextColor(Color.WHITE)
            background = rounded(violet, 15)

            setOnClickListener {
                songPicker.launch(arrayOf("audio/*"))
            }
        }

        content.addView(
            addSongsButton,
            LinearLayout.LayoutParams(-1, dp(54)).apply {
                topMargin = dp(8)
                bottomMargin = dp(16)
            }
        )

        addSectionTitle("📂  ${selectedCategory} Playlist")

        val filteredSongs = songs.filter {
            it.category == selectedCategory
        }

        if (filteredSongs.isEmpty()) {
            addMessage("இந்தப் பிரிவில் இன்னும் பாடல்கள் சேர்க்கப்படவில்லை.")
        }

        filteredSongs.forEach { song ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(10), dp(10))
                background = rounded(panel, 14)
            }

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val name = TextView(this).apply {
                text = "♫  ${song.name}"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }

            info.addView(name)

            row.addView(
                info,
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            val play = Button(this).apply {
                text = "▶"
                setTextColor(bg)
                background = rounded(cyan, 12)

                setOnClickListener {
                    playSong(song)
                }
            }

            row.addView(play)

            content.addView(
                row,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(8)
                }
            )
        }

        addSectionTitle("📻  Internet Radio")

        radioCategories.forEach { category ->
            val button = Button(this).apply {
                text = when (category) {
                    "Music Directors FM" -> "🎼  MUSIC DIRECTORS FM"
                    "Actors FM" -> "🎬  ACTORS FM"
                    else -> "🎧  MY OWN FM"
                }

                setTextColor(Color.WHITE)
                background = rounded(panel, 14)

                setOnClickListener {
                    selectedCategory = category
                    selectedStation = null
                    buildUi()
                }
            }

            content.addView(
                button,
                LinearLayout.LayoutParams(-1, dp(50)).apply {
                    bottomMargin = dp(7)
                }
            )
        }

        val filteredStations = stations.filter {
            it.category == selectedCategory
        }

        if (selectedCategory in radioCategories) {
            if (filteredStations.isEmpty()) {
                addMessage("இந்தப் பிரிவில் FM இல்லை.")
            }

            filteredStations.forEach { station ->
                val button = Button(this).apply {
                    text = "▶  ${station.name}"
                    setTextColor(Color.WHITE)
                    background = rounded(panel, 14)

                    setOnClickListener {
                        selectStation(station)
                    }
                }

                content.addView(
                    button,
                    LinearLayout.LayoutParams(-1, dp(50)).apply {
                        bottomMargin = dp(8)
                    }
                )
            }

            val addRadioButton = Button(this).apply {
                text = "＋  புதிய FM சேர்க்க"
                setTextColor(Color.WHITE)
                background = rounded(violet, 14)

                setOnClickListener {
                    showAddRadioDialog()
                }
            }

            content.addView(
                addRadioButton,
                LinearLayout.LayoutParams(-1, dp(52)).apply {
                    bottomMargin = dp(12)
                }
            )
        }

        val footer = TextView(this).apply {
            text = "நாதலை FM • இசை அலைகள் உங்களுக்காக"
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(12))
        }

        content.addView(footer)

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)
    }

    private fun addSectionTitle(title: String) {
        val text = TextView(this).apply {
            this.text = title
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(cyan)
            setPadding(0, dp(16), 0, dp(10))
        }

        content.addView(text)
    }

    private fun addMessage(message: String) {
        val text = TextView(this).apply {
            this.text = message
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(20), dp(12), dp(20))
            background = rounded(panel, 14)
        }

        content.addView(
            text,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(10)
            }
        )
    }

    private fun playSong(song: SongTrack) {
        try {
            val uri = Uri.parse(song.uri)

            currentSongUri = uri
            selectedStation = null

            val p = getPlayer()
            p.setMediaItem(MediaItem.fromUri(uri))
            p.prepare()
            p.play()

            currentName.text = song.name
            status.text = "பாடல் இயங்குகிறது."
            playButton.text = "Ⅱ  PAUSE"

        } catch (_: Exception) {
            toast("பாடலை இயக்க முடியவில்லை.")
        }
    }

    private fun selectStation(station: RadioStation) {
        selectedStation = station
        currentSongUri = null

        if (station.url.isBlank()) {
            showEditRadioDialog(station)
            return
        }

        try {
            val p = getPlayer()
            p.setMediaItem(MediaItem.fromUri(station.url))
            p.prepare()
            p.play()

            currentName.text = station.name
            status.text = "Radio இணைக்கிறது…"
            playButton.text = "Ⅱ  PAUSE RADIO"

        } catch (_: Exception) {
            status.text = "Radio தொடங்க முடியவில்லை."
        }
    }

    private fun togglePlayback() {
        val p = getPlayer()

        if (p.isPlaying) {
            p.pause()
            playButton.text = "▶  PLAY"
            status.text = "இசை Pause செய்யப்பட்டது."
            return
        }

        if (p.mediaItemCount > 0) {
            p.play()
            playButton.text = "Ⅱ  PAUSE"
            status.text = "இசை இயங்குகிறது."
        } else {
            toast("முதலில் பாடல் அல்லது FM தேர்ந்தெடு.")
        }
    }

    private fun getPlayer(): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(this).build().also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING ->
                                status.text = "இசை ஏற்றப்படுகிறது…"

                            Player.STATE_READY ->
                                status.text =
                                    if (exo.isPlaying) "இசை இயங்குகிறது."
                                    else "Play செய்யத் தயாராக உள்ளது."

                            Player.STATE_ENDED ->
                                status.text = "பாடல் முடிந்தது."
                        }
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

    private fun showAddRadioDialog() {
        val nameInput = EditText(this).apply {
            hint = "FM பெயர்"
            setSingleLine(true)
        }

        val urlInput = EditText(this).apply {
            hint = "https://உங்கள்-stream-url"
            setSingleLine(true)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(nameInput)
            addView(urlInput)
        }

        AlertDialog.Builder(this)
            .setTitle("புதிய FM சேர்க்க")
            .setView(layout)
            .setNegativeButton("ரத்து", null)
            .setPositiveButton("சேமி") { _, _ ->
                val name = nameInput.text.toString().trim()
                val url = urlInput.text.toString().trim()

                if (name.isBlank() || !isValidUrl(url)) {
                    toast("FM பெயர் மற்றும் சரியான URL சேர்க்கவும்.")
                    return@setPositiveButton
                }

                val station = RadioStation(name, selectedCategory, url)
                stations.add(station)
                saveStations()

                buildUi()
                toast("FM சேமிக்கப்பட்டது!")
            }
            .show()
    }

    private fun showEditRadioDialog(station: RadioStation) {
        val input = EditText(this).apply {
            hint = "https://உங்கள்-stream-url"
        }

        AlertDialog.Builder(this)
            .setTitle("${station.name} Stream URL")
            .setView(input)
            .setNegativeButton("ரத்து", null)
            .setPositiveButton("சேமி") { _, _ ->
                val url = input.text.toString().trim()

                if (!isValidUrl(url)) {
                    toast("சரியான URL சேர்க்கவும்.")
                    return@setPositiveButton
                }

                val index = stations.indexOf(station)

                if (index >= 0) {
                    stations[index] = station.copy(url = url)
                }

                selectedStation = station.copy(url = url)
                saveStations()
                selectStation(selectedStation!!)
            }
            .show()
    }

    private fun getSongName(uri: Uri): String {
        var name = "Phone Song"

        contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    android.provider.OpenableColumns.DISPLAY_NAME
                )

                if (index >= 0) {
                    name = cursor.getString(index) ?: name
                }
            }
        }

        return name
    }

    private fun isValidUrl(url: String): Boolean {
        return (url.startsWith("https://") || url.startsWith("http://")) &&
            android.util.Patterns.WEB_URL.matcher(url).matches()
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}

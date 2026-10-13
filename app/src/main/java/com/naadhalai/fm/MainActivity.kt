package com.naadhalai.fm

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONArray
import org.json.JSONObject

data class RadioStation(
    val name: String,
    val category: String,
    val url: String
)

class MainActivity : Activity() {

    private var player: ExoPlayer? = null

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var currentName: TextView
    private lateinit var status: TextView
    private lateinit var playButton: Button
    private val songPickerCode = 501
    private var selectedSongUri: Uri? = null

    private val bg = Color.rgb(9, 12, 28)
    private val panel = Color.rgb(22, 27, 49)
    private val cyan = Color.rgb(63, 220, 242)
    private val violet = Color.rgb(163, 104, 255)
    private val white = Color.WHITE
    private val gray = Color.LTGRAY

    private val categories = listOf(
        "Music Directors FM",
        "Actors FM",
        "My Own FM"
    )

    private var selectedCategory = "Music Directors FM"
    private var selectedStation: RadioStation? = null

    private val stations = mutableListOf<RadioStation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        loadStations()
        buildUi()
    }

    private fun defaultStations(): List<RadioStation> {
        return listOf(
            RadioStation("இளையராஜா FM", categories[0], ""),
            RadioStation("A.R. ரஹ்மான் FM", categories[0], ""),
            RadioStation("அனிருத் FM", categories[0], ""),
            RadioStation("தேவா FM", categories[0], ""),
            RadioStation("விஜய் FM", categories[1], ""),
            RadioStation("அஜித் FM", categories[1], ""),
            RadioStation("ரஜினி FM", categories[1], ""),
            RadioStation("கமல் FM", categories[1], "")
        )
    }

    private fun loadStations() {
        val prefs = getSharedPreferences(
            "naadhalai_radio",
            MODE_PRIVATE
        )

        val saved = prefs.getString("stations_json", null)

        if (saved == null) {
            stations.addAll(defaultStations())
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
            stations.addAll(defaultStations())
            saveStations()
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

        getSharedPreferences(
            "naadhalai_radio",
            MODE_PRIVATE
        ).edit()
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
            textSize = 29f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(4))
        }

        content.addView(brand)

        val subtitle = TextView(this).apply {
            text = "இசை அலைகள் உங்களுக்காக"
            textSize = 16f
            setTextColor(cyan)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
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
            text = selectedStation?.name ?: "உங்கள் Radio"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            gravity = Gravity.CENTER
        }

        playerCard.addView(currentName)

        status = TextView(this).apply {
            text = "உங்களுக்குப் பிடித்த FM-ஐத் தேர்ந்தெடுக்கவும்"
            textSize = 14f
            setTextColor(gray)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(16))
        }

        playerCard.addView(status)

        playButton = Button(this).apply {
            text = "▶  PLAY RADIO"
            textSize = 15f
            setTextColor(bg)
            background = rounded(cyan, 32)

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
                bottomMargin = dp(20)
            }
        )

        val categoryTitle = TextView(this).apply {
            text = "📻  உங்களுடைய FM உலகம்"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(12))
        }

        content.addView(categoryTitle)

        categories.forEach { category ->
            val button = Button(this).apply {
                text = when (category) {
                    "Music Directors FM" -> "🎼  Music Directors FM"
                    "Actors FM" -> "🎬  Actors FM"
                    else -> "🎧  My Own FM"
                }

                textSize = 15f
                setTextColor(white)

                background = rounded(
                    if (selectedCategory == category) violet
                    else panel,
                    14
                )

                setOnClickListener {
                    selectedCategory = category
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

        val listTitle = TextView(this).apply {
            text = "Stations"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(cyan)
            setPadding(0, dp(18), 0, dp(8))
        }

        content.addView(listTitle)

        val filtered = stations.filter {
            it.category == selectedCategory
        }

        if (filtered.isEmpty()) {
            val empty = TextView(this).apply {
                text = "இங்கே இன்னும் FM சேர்க்கப்படவில்லை.\nகீழே உள்ள பொத்தானை அழுத்தவும்."
                textSize = 15f
                setTextColor(gray)
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(22), dp(16), dp(22))
                background = rounded(panel, 16)
            }

            content.addView(empty)
        }

        filtered.forEach { station ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(12), dp(10), dp(12))
                background = rounded(panel, 14)
            }

            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val name = TextView(this).apply {
                text = "♫  ${station.name}"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(white)
            }

            val detail = TextView(this).apply {
                text = if (station.url.isBlank()) {
                    "Stream URL சேர்க்கவும்"
                } else {
                    "Radio stream"
                }

                textSize = 12f
                setTextColor(gray)
                setPadding(0, dp(5), 0, 0)
            }

            info.addView(name)
            info.addView(detail)

            row.addView(
                info,
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            val play = Button(this).apply {
                text = "▶"
                textSize = 17f
                setTextColor(bg)
                background = rounded(cyan, 12)

                setOnClickListener {
                    selectStation(station)
                }
            }

            row.addView(
                play,
                LinearLayout.LayoutParams(dp(58), dp(46))
            )

            row.setOnClickListener {
                selectStation(station)
            }

            content.addView(
                row,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(9)
                }
            )
        }

        val addButton = Button(this).apply {
            text = "＋  இந்தப் பிரிவில் புதிய FM சேர்க்க"
            textSize = 14f
            setTextColor(white)
            background = rounded(violet, 14)

            setOnClickListener {
                showAddDialog()
            }
        }

        content.addView(
            addButton,
            LinearLayout.LayoutParams(-1, dp(54)).apply {
                topMargin = dp(14)
                bottomMargin = dp(14)
            }
        )

        val footer = TextView(this).apply {
            text = "நாதலை FM • இசை அலைகள் உங்களுக்காக"
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(16))
        }

        content.addView(footer)

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)
    }

    private fun selectStation(station: RadioStation) {
        selectedStation = station

        currentName.text = station.name

        if (station.url.isBlank()) {
            status.text = "இந்த FM-க்கான stream URL சேர்க்கவும்."
            showEditDialog(station)
            return
        }

        stopPlayback()
        status.text = "தொடங்கத் தயாராக உள்ளது."
        playButton.text = "▶  PLAY RADIO"
    }

    private fun openPhoneSongPicker() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "audio/*"
    }

    @Suppress("DEPRECATION")
    startActivityForResult(intent, songPickerCode)
}

@Suppress("DEPRECATION")
override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == songPickerCode && resultCode == RESULT_OK) {
        val uri = data?.data ?: return

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // சில file providers நிரந்தர permission வழங்காமல் இருக்கலாம்.
        }

        selectedSongUri = uri

        val station = selectedStation

        if (station != null) {
            currentName.text = station.name
        } else {
            currentName.text = "Phone Songs FM"
        }

        playPhoneSong(uri)
    }
}

private fun playPhoneSong(uri: Uri) {
    try {
        val p = player ?: ExoPlayer.Builder(this)
            .build()
            .also { player = it }

        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()
        p.play()

        currentName.text = "Phone Songs FM"
        status.text = "உங்கள் பாடல் ஒலிக்கிறது…"
        playButton.text = "Ⅱ  PAUSE RADIO"
    } catch (_: Exception) {
        status.text = "பாடலை இயக்க முடியவில்லை."
        playButton.text = "▶  PLAY RADIO"
    }
}

    private fun togglePlayback() {
        val station = selectedStation
        if (selectedCategory == "My Own FM") {
    val songUri = selectedSongUri

    if (songUri != null) {
        val p = player

        if (p?.isPlaying == true) {
            p.pause()
            status.text = "பாடல் Pause செய்யப்பட்டது."
            playButton.text = "▶  PLAY RADIO"
        } else {
            playPhoneSong(songUri)
        }
    } else {
        openPhoneSongPicker()
    }

    return
        }

        if (station == null) {
            Toast.makeText(
                this,
                "முதலில் ஒரு FM station தேர்ந்தெடுக்கவும்.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (station.url.isBlank()) {
            showEditDialog(station)
            return
        }

        try {
            val p = player ?: ExoPlayer.Builder(this)
                .build()
                .also { exo ->

                    exo.addListener(object : Player.Listener {

                        override fun onPlaybackStateChanged(
                            playbackState: Int
                        ) {
                            status.text = when (playbackState) {
                                Player.STATE_BUFFERING ->
                                    "Radio இணைக்கிறது…"

                                Player.STATE_READY ->
                                    if (exo.isPlaying) {
                                        "Live radio playing"
                                    } else {
                                        "Play செய்யத் தயாராக உள்ளது"
                                    }

                                Player.STATE_ENDED ->
                                    "Stream முடிந்தது."

                                else -> status.text
                            }

                            playButton.text =
                                if (exo.isPlaying) {
                                    "Ⅱ  PAUSE RADIO"
                                } else {
                                    "▶  PLAY RADIO"
                                }
                        }

                        override fun onPlayerError(
                            error: PlaybackException
                        ) {
                            status.text =
                                "Stream பிழை: ${error.errorCodeName}"

                            playButton.text = "▶  PLAY RADIO"
                        }
                    })

                    player = exo
                }

            if (p.isPlaying) {
                p.pause()
                status.text = "Radio நிறுத்தப்பட்டுள்ளது."
                playButton.text = "▶  PLAY RADIO"
            } else {
                p.setMediaItem(
                    MediaItem.fromUri(station.url)
                )
                p.prepare()
                p.play()

                currentName.text = station.name
                status.text = "Radio இணைக்கிறது…"
                playButton.text = "Ⅱ  PAUSE RADIO"
            }

        } catch (e: Exception) {
            status.text = "Radio தொடங்க முடியவில்லை."
            playButton.text = "▶  PLAY RADIO"
        }
    }

    private fun stopPlayback() {
        player?.stop()
        player?.clearMediaItems()
        playButton.text = "▶  PLAY RADIO"
    }

    private fun showAddDialog() {
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
            .setMessage(
                "பயன்படுத்த அனுமதிக்கப்பட்ட stream URL மட்டும் சேர்க்கவும்."
            )
            .setView(layout)
            .setNegativeButton("ரத்து", null)
            .setPositiveButton("சேமி") { _, _ ->

                val name = nameInput.text.toString().trim()
                val url = urlInput.text.toString().trim()

                if (name.isBlank()) {
                    toast("FM பெயரை உள்ளிடவும்.")
                    return@setPositiveButton
                }

                if (!isValidUrl(url)) {
                    toast("சரியான http:// அல்லது https:// URL சேர்க்கவும்.")
                    return@setPositiveButton
                }

                val station = RadioStation(
                    name,
                    selectedCategory,
                    url
                )

                stations.add(station)
                saveStations()

                selectedStation = station
                buildUi()

                currentName.text = station.name
                status.text = "FM சேமிக்கப்பட்டது! Play அழுத்தவும்."

                toast("புதிய FM சேமிக்கப்பட்டது!")
            }
            .show()
    }

    private fun showEditDialog(station: RadioStation) {
        val urlInput = EditText(this).apply {
            hint = "https://உங்கள்-stream-url"
            setSingleLine(true)
            setText(station.url)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(urlInput)
        }

        AlertDialog.Builder(this)
            .setTitle("${station.name} அமைப்பு")
            .setMessage(
                "இந்த station-க்கான அனுமதிக்கப்பட்ட audio stream URL சேர்க்கவும்."
            )
            .setView(layout)
            .setNegativeButton("ரத்து", null)
            .setPositiveButton("சேமி") { _, _ ->

                val url = urlInput.text.toString().trim()

                if (!isValidUrl(url)) {
                    toast("சரியான stream URL சேர்க்கவும்.")
                    return@setPositiveButton
                }

                val index = stations.indexOf(station)

                if (index >= 0) {
                    stations[index] = station.copy(url = url)
                }

                selectedStation = station.copy(url = url)

                saveStations()
                buildUi()

                currentName.text = station.name
                status.text = "Stream URL சேமிக்கப்பட்டது."

                toast("சேமிக்கப்பட்டது! Play அழுத்தவும்.")
            }
            .show()
    }

    private fun isValidUrl(url: String): Boolean {
        if (
            !url.startsWith("https://") &&
            !url.startsWith("http://")
        ) {
            return false
        }

        return android.util.Patterns.WEB_URL
            .matcher(url)
            .matches()
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}

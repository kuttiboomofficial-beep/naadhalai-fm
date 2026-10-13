package com.naadhalai.fm

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
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

class MainActivity : Activity() {
    private var player: ExoPlayer? = null
    private lateinit var status: TextView
    private lateinit var playButton: Button
    private lateinit var currentName: TextView
    private var streamUrl: String = ""
    private var stationName: String = "உங்கள் Radio"
    private val bg = Color.rgb(9, 12, 28)
    private val panel = Color.rgb(22, 27, 49)
    private val cyan = Color.rgb(63, 220, 242)
    private val violet = Color.rgb(163, 104, 255)

   override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    window.statusBarColor = bg
    window.navigationBarColor = bg

    // முன்பு சேமித்த station-ஐ மீண்டும் load செய்
    val prefs = getSharedPreferences("naadhalai_radio", MODE_PRIVATE)
    stationName = prefs.getString("station_name", "உங்கள் Radio")
        ?: "உங்கள் Radio"
    streamUrl = prefs.getString("stream_url", "") ?: ""

    buildUi()
}

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val brand = TextView(this).apply {
            text = "♫  நாதலை FM"
            textSize = 29f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(4))
        }
        content.addView(brand)
        content.addView(label("இசை அலைகள் உங்களுக்காக", 14f, cyan, true))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(28), dp(18), dp(24))
            background = rounded(panel, 24)
        }
        currentName = label(stationName, 22f, Color.WHITE, true)
        currentName.gravity = Gravity.CENTER
        card.addView(currentName)
        status = label("உங்கள் stream URL-ஐ சேர்த்து தொடங்குங்கள்", 13f, Color.LTGRAY, false).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(18))
        }
        card.addView(status)
        playButton = Button(this).apply {
            text = "▶  Play Radio"
            setTextColor(bg)
            textSize = 16f
            background = rounded(cyan, 40)
            setOnClickListener { togglePlayback() }
        }
        card.addView(playButton, LinearLayout.LayoutParams(-1, dp(54)))
        val add = Button(this).apply {
            text = "＋  சொந்த Radio Stream சேர்க்க"
            setTextColor(Color.WHITE)
            background = rounded(violet, 14)
            setOnClickListener { showStreamDialog() }
        }
        val addParams = LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(14) }
        card.addView(add, addParams)
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(26) })
        content.addView(label("📻  Stations", 19f, Color.WHITE, true).apply {
            setPadding(0, dp(26), 0, dp(10))
        })
        val info = TextView(this).apply {
            text = "Tamil FM stations\nஅதிகாரப்பூர்வ stream link கிடைத்ததும் இங்கே சேர்க்கலாம்."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(panel, 16)
        }
        content.addView(info)
        content.addView(label("குறிப்பு: இது ஆரம்ப native prototype. முன் நிரப்பப்பட்ட FM stream URLs இல்லை.", 12f, Color.GRAY, false).apply {
            setPadding(dp(2), dp(16), dp(2), dp(10))
        })
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

   
private fun togglePlayback() {
    if (streamUrl.isBlank()) {
        status.text = "முதலில் stream URL சேர்க்கவும்."
        showStreamDialog()
        return
    }

    try {
        val p = player ?: ExoPlayer.Builder(this).build().also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    status.text = when (playbackState) {
                        Player.STATE_BUFFERING -> "இணைக்கிறது…"
                        Player.STATE_READY ->
                            if (exo.isPlaying) "Live radio playing"
                            else "Play செய்ய தயாராக உள்ளது"
                        Player.STATE_ENDED -> "Stream முடிந்தது"
                        else -> status.text
                    }

                    playButton.text =
                        if (exo.isPlaying) "Ⅱ  Pause Radio"
                        else "▶  Play Radio"
                }

                override fun onPlayerError(error: PlaybackException) {
                    status.text =
                        "Stream பிழை: ${error.errorCodeName}"
                    playButton.text = "▶  Play Radio"
package com.naadhalai.fm

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
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

class MainActivity : Activity() {

    private var player: ExoPlayer? = null
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var playButton: Button
    private lateinit var currentName: TextView

    private val prefs by lazy {
        getSharedPreferences("naadhalai_radio", MODE_PRIVATE)
    }

    private val bg = Color.rgb(9, 12, 28)
    private val panel = Color.rgb(22, 27, 49)
    private val cyan = Color.rgb(63, 220, 242)
    private val violet = Color.rgb(163, 104, 255)
    private val white = Color.WHITE
    private val gray = Color.LTGRAY

    private var currentCategory = "LIVE FM"
    private var currentStationName = ""
    private var currentStreamUrl = ""
    private var musicUris = mutableListOf<String>()

    private data class Station(
        val name: String,
        val url: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg

        currentStationName = prefs.getString(
            "station_name", "உங்கள் Radio"
        ) ?: "உங்கள் Radio"

        currentStreamUrl = prefs.getString(
            "stream_url", ""
        ) ?: ""

        loadMusic()
        showHome()
    }

    private fun makeRoot() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)
    }

    private fun showHome() {
        makeRoot()

        content.addView(
            label("♫  நாதலை FM", 30f, white, true)
        )

        content.addView(
            label(
                "இசை அலைகள் உங்களுக்காக",
                15f,
                cyan,
                true
            )
        )

        content.addView(
            label(
                "உனக்குப் பிடித்த இசை உலகம் 🎧",
                16f,
                gray,
                false
            ).apply {
                setPadding(0, dp(20), 0, dp(12))
            }
        )

        addCategory(
            "📻",
            "LIVE FM",
            "இணைய வானொலி சேனல்கள்"
        ) {
            currentCategory = "LIVE FM"
            showStations(currentCategory)
        }

        addCategory(
            "🎼",
            "Music Directors FM",
            "இளையராஜா • ரஹ்மான் • யுவன் • அனிருத்"
        ) {
            currentCategory = "Music Directors FM"
            showStations(currentCategory)
        }

        addCategory(
            "🌟",
            "Actors FM",
            "விஜய் • அஜித் • ரஜினி • சூர்யா"
        ) {
            currentCategory = "Actors FM"
            showStations(currentCategory)
        }

        addCategory(
            "🎧",
            "MY MUSIC",
            "உன் போனில் உள்ள பாடல்களைத் தேர்ந்தெடு"
        ) {
            showMyMusic()
        }

        addCategory(
            "❤️",
            "FAVORITES",
            "உனக்குப் பிடித்த சேனல்கள்"
        ) {
            showFavorites()
        }

        val now = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(18), dp(16), dp(18))
            background = rounded(panel, 20)
        }

        now.addView(
            label("🎵 இப்போது தேர்ந்தெடுத்தது", 15f, cyan, true)
        )

        now.addView(
            label(
                currentStationName,
                20f,
                white,
                true
            ).apply {
                setPadding(0, dp(8), 0, dp(8))
            }
        )

        now.addView(
            label(
                if (player?.isPlaying == true)
                    "இசை ஒலிக்கிறது"
                else
                    "வானொலி கேட்கத் தயாராக உள்ளது",
                13f,
                gray,
                false
            )
        )

        content.addView(
            now,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(16)
            }
        )

        content.addView(
            label(
                "குறிப்பு: Radio ஒலிக்க, பயன்படுத்த அனுமதிக்கப்பட்ட stream URL தேவை.",
                12f,
                gray,
                false
            ).apply {
                setPadding(0, dp(18), 0, dp(10))
            }
        )
    }

    private fun addCategory(
        emoji: String,
        title: String,
        subtitle: String,
        action: () -> Unit
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(panel, 18)
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

        card.addView(
            label("$emoji  $title", 20f, white, true)
        )

        card.addView(
            label(subtitle, 13f, gray, false).apply {
                setPadding(0, dp(8), 0, 0)
            }
        )

        content.addView(
            card,
            LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(10)
            }
        )
    }

    private fun showStations(category: String) {
        makeRoot()

        addBackButton()

        content.addView(
            label(category, 25f, white, true).apply {
                setPadding(0, dp(12), 0, dp(12))
            }
        )

        val names = when (category) {
            "Music Directors FM" -> listOf(
                "இளையராஜா FM",
                "A.R. ரஹ்மான் FM",
                "யுவன் FM",
                "அனிருத் FM"
            )

            "Actors FM" -> listOf(
                "விஜய் FM",
                "அஜித் FM",
                "ரஜினி FM",
                "சூர்யா FM"
            )

            else -> emptyList()
        }

        val stations = loadStations(category).toMutableList()

        names.forEach { name ->
            if (stations.none { it.name == name }) {
                stations.add(Station(name, ""))
            }
        }

        if (stations.isEmpty()) {
            content.addView(
                label(
                    "இன்னும் Radio சேர்க்கப்படவில்லை.",
                    15f,
                    gray,
                    false
                )
            )
        }

        stations.forEach { station ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = rounded(panel, 16)
            }

            card.addView(
                label(station.name, 18f, white, true)
            )

            if (station.url.isBlank()) {
                card.addView(
                    label(
                        "உன் அனுமதியுள்ள stream URL-ஐச் சேர்.",
                        13f,
                        gray,
                        false
                    ).apply {
                        setPadding(0, dp(6), 0, 0)
                    }
                )

                card.addView(
                    makeButton("＋ Stream சேர்க்க", violet) {
                        showAddStationDialog(category, station.name)
                    },
                    LinearLayout.LayoutParams(-1, dp(48)).apply {
                        topMargin = dp(10)
                    }
                )
            } else {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                row.addView(
                    makeButton("▶ PLAY", cyan) {
                        playStation(station)
                    },
                    LinearLayout.LayoutParams(0, dp(48), 1f)
                )

                row.addView(
                    makeButton("❤️", violet) {
                        saveFavorite(station, category)
                    },
                    LinearLayout.LayoutParams(0, dp(48), 1f).apply {
                        leftMargin = dp(8)
                    }
                )

                card.addView(
                    row,
                    LinearLayout.LayoutParams(-1, -2).apply {
                        topMargin = dp(10)
                    }
                )

                card.addView(
                    makeButton("✎ URL மாற்று", panel) {
                        showAddStationDialog(category, station.name)
                    },
                    LinearLayout.LayoutParams(-1, dp(44)).apply {
                        topMargin = dp(8)
                    }
                )
            }

            content.addView(
                card,
                LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = dp(10)
                }
            )
        }

        content.addView(
            makeButton("＋ புதிய Radio சேர்க்க", violet) {
                showAddStationDialog(category, "")
            },
            LinearLayout.LayoutParams(-1, dp(52)).apply {
                topMargin = dp(18)
            }
        )
    }

    private fun showAddStationDialog(
        category: String,
        existingName: String
    ) {
        val old = loadStations(category)
            .find { it.name == existingName }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }

        val name = EditText(this).apply {
            hint = "Radio பெயர்"
            setSingleLine(true)
            setText(existingName)
        }

        val url = EditText(this).apply {
            hint = "https://உங்கள்-stream-url"
            setSingleLine(true)
            setText(old?.url ?: "")
        }

        layout.addView(name)
        layout.addView(url)

        AlertDialog.Builder(this)
            .setTitle("Radio Stream சேர்க்க")
            .setMessage(
                "உரிமையுள்ள அல்லது பயன்படுத்த அனுமதிக்கப்பட்ட stream URL மட்டும் சேர்க்கவும்."
            )
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val stationName = name.text.toString().trim()
                val stream = url.text.toString().trim()

                if (stationName.isBlank()) {
                    toast("Radio பெயரை உள்ளிடு.")
                    return@setPositiveButton
                }

                if (
                    !(stream.startsWith("https://") ||
                      stream.startsWith("http://"))
                ) {
                    toast("சரியான stream URL சேர்க்கவும்.")
                    return@setPositiveButton
                }

                val list = loadStations(category).toMutableList()

                list.removeAll { it.name == stationName }

                list.add(Station(stationName, stream))

                saveStations(category, list)

                currentCategory = category
                toast("Radio சேமிக்கப்பட்டது!")

                showStations(category)
            }
            .show()
    }

    private fun playStation(station: Station) {
        if (station.url.isBlank()) {
            toast("முதலில் stream URL சேர்க்கவும்.")
            return
        }

        try {
            val p = player ?: ExoPlayer.Builder(this).build().also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            toast("Radio தயார்!")
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        toast("Stream பிழை: ${error.errorCodeName}")
                    }
                })

                player = exo
            }

            currentStationName = station.name
            currentStreamUrl = station.url

            prefs.edit()
                .putString("station_name", station.name)
                .putString("stream_url", station.url)
                .apply()

            p.setMediaItem(MediaItem.fromUri(station.url))
            p.prepare()
            p.play()

            showHome()
            toast("${station.name} இணைக்கப்படுகிறது…")

        } catch (e: Exception) {
            toast("Player பிழை: ${e.message}")
        }
    }

    private fun saveFavorite(station: Station, category: String) {
        val key = "favorites"
        val arr = JSONArray(prefs.getString(key, "[]") ?: "[]")

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue

            if (obj.optString("url") == station.url) {
                toast("ஏற்கெனவே Favorites-ல் உள்ளது.")
                return
            }
        }

        arr.put(
            JSONObject().apply {
                put("name", station.name)
                put("url", station.url)
                put("category", category)
            }
        )

        prefs.edit().putString(key, arr.toString()).apply()
        toast("❤️ Favorites-ல் சேமிக்கப்பட்டது!")
    }

    private fun showFavorites() {
        makeRoot()
        addBackButton()

        content.addView(
            label("❤️ FAVORITES", 24f, white, true)
        )

        val arr = JSONArray(prefs.getString("favorites", "[]") ?: "[]")

        if (arr.length() == 0) {
            content.addView(
                label(
                    "இன்னும் Favorites சேர்க்கவில்லை.",
                    15f,
                    gray,
                    false
                )
            )
        }

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue

            val name = obj.optString("name")
            val url = obj.optString("url")

            content.addView(
                makeButton("▶  $name", cyan) {
                    playStation(Station(name, url))
                },
                LinearLayout.LayoutParams(-1, dp(54)).apply {
                    topMargin = dp(10)
                }
            )
        }
    }

    private fun showMyMusic() {
        makeRoot()
        addBackButton()

        content.addView(
            label("🎧 MY MUSIC", 25f, white, true)
        )

        content.addView(
            label(
                "உன் போனிலிருந்து audio files தேர்ந்தெடு.",
                14f,
                gray,
                false
            ).apply {
                setPadding(0, dp(12), 0, dp(12))
            }
        )

        content.addView(
            makeButton("＋ பாடல்களைத் தேர்ந்தெடு", violet) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "audio/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                @Suppress("DEPRECATION")
                startActivityForResult(intent, 1001)
            },
            LinearLayout.LayoutParams(-1, dp(54))
        )

        musicUris.forEachIndexed { index, uriString ->
            val uri = Uri.parse(uriString)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }

            row.addView(
                makeButton("▶  பாடல் ${index + 1}", cyan) {
                    playMusic(uri)
                },
                LinearLayout.LayoutParams(0, dp(48), 1f)
            )

            row.addView(
                makeButton("✕", violet) {
                    musicUris.remove(uriString)
                    saveMusic()
                    showMyMusic()
                },
                LinearLayout.LayoutParams(dp(60), dp(48)).apply {
                    leftMargin = dp(8)
                }
            )

            content.addView(
                row,
                LinearLayout.LayoutParams(-1, -2)
            )
        }
    }

    private fun playMusic(uri: Uri) {
        try {
            val p = player ?: ExoPlayer.Builder(this).build().also {
                player = it
            }

            currentStationName = "MY MUSIC"
            p.setMediaItem(MediaItem.fromUri(uri))
            p.prepare()
            p.play()

            toast("பாடல் இயக்கப்படுகிறது!")
        } catch (e: Exception) {
            toast("பாடலை இயக்க முடியவில்லை.")
        }
    }

    @Deprecated("Using Android document picker result")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != 1001 || resultCode != RESULT_OK || data == null) {
            return
        }

        try {
            val clip = data.clipData

            if (clip != null) {
                for (i in 0 until clip.itemCount) {
                    addMusicUri(clip.getItemAt(i).uri)
                }
            } else {
                data.data?.let { addMusicUri(it) }
            }

            saveMusic()
            showMyMusic()
            toast("பாடல்கள் சேர்க்கப்பட்டன!")

        } catch (e: Exception) {
            toast("பாடல்களைச் சேர்க்க முடியவில்லை.")
        }
    }

    private fun addMusicUri(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        if (!musicUris.contains(uri.toString())) {
            musicUris.add(uri.toString())
        }
    }

    private fun loadMusic() {
        val arr = JSONArray(prefs.getString("music_list", "[]") ?: "[]")

        musicUris.clear()

        for (i in 0 until arr.length()) {
            musicUris.add(arr.optString(i))
        }
    }

    private fun saveMusic() {
        val arr = JSONArray()

        musicUris.forEach { arr.put(it) }

        prefs.edit().putString("music_list", arr.toString()).apply()
    }

    private fun loadStations(category: String): List<Station> {
        val arr = JSONArray(
            prefs.getString("stations_$category", "[]") ?: "[]"
        )

        val list = mutableListOf<Station>()

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue

            list.add(
                Station(
                    obj.optString("name"),
                    obj.optString("url")
                )
            )
        }

        return list
    }

    private fun saveStations(
        category: String,
        stations: List<Station>
    ) {
        val arr = JSONArray()

        stations.forEach { station ->
            arr.put(
                JSONObject().apply {
                    put("name", station.name)
                    put("url", station.url)
                }
            )
        }

        prefs.edit()
            .putString("stations_$category", arr.toString())
            .apply()
    }

    private fun addBackButton() {
        content.addView(
            makeButton("←  முகப்புக்குச் செல்", panel) {
                showHome()
            },
            LinearLayout.LayoutParams(-1, dp(48))
        )
    }

    private fun makeButton(
        title: String,
        color: Int,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = title
            textSize = 14f
            setTextColor(if (color == cyan) bg else white)
            background = rounded(color, 14)
            setOnClickListener { action() }
        }
    }

    private fun label(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }

            gravity = Gravity.CENTER
        }
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
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}

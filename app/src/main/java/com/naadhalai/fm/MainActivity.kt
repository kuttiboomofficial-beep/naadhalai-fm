package com.naadhalai.fm

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

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
                }
            })

            player = exo
        }

        if (p.isPlaying) {
            p.pause()
            playButton.text = "▶  Play Radio"
            status.text = "Paused"
        } else {
            p.setMediaItem(MediaItem.fromUri(streamUrl))
            p.prepare()
            p.play()
            currentName.text = stationName
            status.text = "இணைக்கிறது…"
        }
    } catch (e: Exception) {
        status.text = "Player பிழை: ${e.message ?: "மீண்டும் முயற்சிக்கவும்"}"
        playButton.text = "▶  Play Radio"

        player?.release()
        player = null
    }
}


    private fun showStreamDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val name = EditText(this).apply {
            hint = "Station name"
            setSingleLine(true)
            setText(stationName.takeIf { it != "உங்கள் Radio" } ?: "My Radio")
        }
        val url = EditText(this).apply {
            hint = "https://your-authorized-stream-url"
            setSingleLine(true)
            setText(streamUrl)
        }
        layout.addView(name)
        layout.addView(url)
        AlertDialog.Builder(this)
            .setTitle("Radio stream சேர்க்க")
            .setMessage("உங்களுக்கு உரிமையுள்ள அல்லது பயன்படுத்த அனுமதிக்கப்பட்ட stream URL மட்டும் சேர்க்கவும்.")
            .setView(layout)
            .setNegativeButton("Cancel", null)
           
.setPositiveButton("Save") { _, _ ->
    val enteredName = name.text.toString().trim()
    val enteredUrl = url.text.toString().trim()

    if (
        (enteredUrl.startsWith("https://") ||
         enteredUrl.startsWith("http://")) &&
        android.util.Patterns.WEB_URL.matcher(enteredUrl).matches()
    ) {
        stationName = enteredName.ifBlank { "My Radio" }
        streamUrl = enteredUrl

        // Phone-ல் நிரந்தரமாகச் சேமி
        getSharedPreferences("naadhalai_radio", MODE_PRIVATE)
            .edit()
            .putString("station_name", stationName)
            .putString("stream_url", streamUrl)
            .apply()

        currentName.text = stationName
        status.text = "சேமிக்கப்பட்டது! இப்போது Play அழுத்தவும்."
    } else {
        status.text = "சரியான http:// அல்லது https:// stream URL சேர்க்கவும்."
    }
}
.show()

    }

    private fun label(textValue: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = textValue
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}

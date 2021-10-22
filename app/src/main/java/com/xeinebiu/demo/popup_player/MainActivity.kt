package com.xeinebiu.demo.popup_player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.xeinebiu.floating.video.VideoFloatingService
import com.xeinebiu.floating.video.model.Stream
import com.xeinebiu.floating.video.model.Subtitle
import com.xeinebiu.floating.video.model.VideoItem

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }

    fun playVideo(view: View) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, PERMISSION_DRAW_OVER_OTHER_APP)
            return
        }

        val stream = Stream(
            Uri.parse("https://thepaciellogroup.github.io/AT-browser-tests/video/ElephantsDream.mp4"),
            HashMap()
        )

        val subtitle = Subtitle(
            Uri.parse("https://thepaciellogroup.github.io/AT-browser-tests/video/subtitles-en.vtt"),
            "text/vtt",
            "English",
            HashMap()
        )

        VideoFloatingService.play(
            this,
            VideoItem("demo", listOf(stream), listOf(subtitle))
        )
    }

    companion object {
        private const val PERMISSION_DRAW_OVER_OTHER_APP = 1000
    }
}
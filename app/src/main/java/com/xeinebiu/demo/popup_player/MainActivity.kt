package com.xeinebiu.demo.popup_player

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.xeinebiu.floating.video.VideoFloatingService
import com.xeinebiu.floating.video.model.Stream
import com.xeinebiu.floating.video.model.Subtitle
import com.xeinebiu.floating.video.model.VideoItem

class MainActivity : AppCompatActivity() {
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }

    fun playVideo(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        if (!Settings.canDrawOverlays(this)) {
            askForDrawOverlayPermission()
            return
        }

        val stream = Stream(
            Uri.parse("https://sample-videos.com/video123/mp4/240/big_buck_bunny_240p_30mb.mp4"),
            HashMap(),
        )

        val subtitle = Subtitle(
            Uri.parse("https://thepaciellogroup.github.io/AT-browser-tests/video/subtitles-en.vtt"),
            "text/vtt",
            "English",
            HashMap(),
        )

        VideoFloatingService.play(
            this,
            VideoItem("demo", listOf(stream), listOf(subtitle)),
        )
    }

    private fun askForDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            return
        }

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )

        overlayPermissionLauncher.launch(intent)
    }
}

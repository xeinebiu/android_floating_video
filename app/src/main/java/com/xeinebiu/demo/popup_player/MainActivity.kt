package com.xeinebiu.demo.popup_player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.xeinebiu.floating.video.VideoFloatingService
import com.xeinebiu.floating.video.model.Stream
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
            Uri.parse("https://file-examples-com.github.io/uploads/2017/04/file_example_MP4_480_1_5MG.mp4"),
            HashMap()
        )
        VideoFloatingService.play(
            this,
            VideoItem("demo", listOf(stream))
        )
    }

    companion object {
        private const val PERMISSION_DRAW_OVER_OTHER_APP = 1000
    }
}
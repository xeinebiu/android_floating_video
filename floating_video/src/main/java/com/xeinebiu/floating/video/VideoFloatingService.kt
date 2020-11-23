package com.xeinebiu.floating.video

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.video.VideoListener
import com.xeinebiu.floating.video.databinding.LayoutPlayer1Binding
import com.xeinebiu.floating.video.model.Stream
import com.xeinebiu.floating.video.model.VideoItem
import com.xeinebiu.floating.video.view.XFrameLayout

class VideoFloatingService : Service(), VideoListener {
    private var viewBinding: LayoutPlayer1Binding? = null
    private var floatingRef: FloatingRef? = null
    private var customMediaSourceFactory: CustomMediaSourceFactory? = null
    private var player: SimpleExoPlayer? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(
            SERVICE_CODE,
            createNotification().build()
        )

        LayoutInflater.from(this).also { inflater ->
            viewBinding = LayoutPlayer1Binding.inflate(inflater).also { view ->

                // create custom media source factory
                val cmsf = (CustomMediaSourceFactory(this) { mediaItem ->
                    mediaItem.playbackProperties!!.tag as Stream
                }).also { c -> customMediaSourceFactory = c }

                // create player
                val p = SimpleExoPlayer.Builder(this)
                    .setMediaSourceFactory(cmsf)
                    .build()
                    .also { simpleExoPlayer ->
                        player = simpleExoPlayer
                    }
                view.player.player = p
                p.addVideoListener(this)

                // stop service when stop button is clicked
                view.stopBtn.setOnClickListener { stopService() }

                view.player.setControllerVisibilityListener { visibility ->
                    view.stopBtn.visibility = visibility
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val item = intent?.extras?.get(EXTRA_ITEM) as VideoItem? ?: return START_STICKY

        if (floatingRef == null)
            floatingRef = showPopupWindow(this, viewBinding!!.root)

        play(item.streams)
        return START_STICKY
    }

    override fun onDestroy() {
        player?.release()
        viewBinding?.let {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(it.root)
        }
        super.onDestroy()
    }

    override fun onVideoSizeChanged(
        width: Int,
        height: Int,
        unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float
    ) {
        val fRef = floatingRef ?: return

        // @ref {https://eikhart.com/blog/aspect-ratio-calculator}
        val aspectRatio = width.toFloat() / height
        fRef.params.height = (fRef.params.width / aspectRatio).toInt()
        fRef.updateView()
    }

    private fun stopService() {
        stopForeground(true)
        stopSelf()
    }

    private fun play(streams: List<Stream>) {
        val sources = streams.map {
            MediaItem.Builder().setUri(it.uri).setTag(it).build()
        }
        this.player?.let {
            it.stop(true)
            it.setMediaItems(sources)
            it.prepare()
            it.playWhenReady = true
        }
    }

    private fun createNotification(): NotificationCompat.Builder {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(SERVICE_TITLE)
            .setSmallIcon(R.drawable.exo_notification_small_icon)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setGroup(SERVICE_CHANNEL_ID)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(SERVICE_CHANNEL_ID)
        }
        return builder
    }

    private data class FloatingRef(
        val windowManager: WindowManager,
        val container: XFrameLayout,
        val params: WindowManager.LayoutParams
    ) {
        fun updateView() {
            windowManager.updateViewLayout(container, params)
        }
    }

    companion object {
        private const val SERVICE_CHANNEL_ID = "Floating Video"
        private const val SERVICE_TITLE = "Popup Player"
        private const val SERVICE_CODE = 30004
        private const val EXTRA_ITEM = "extra_item"
        private const val MAX_WIDTH_DP = 350
        private const val MAX_HEIGHT_DP = 196

        fun play(
            context: Context,
            item: VideoItem
        ) {
            val intent = Intent(context, VideoFloatingService::class.java)
            intent.putExtra(EXTRA_ITEM, item)
            ContextCompat.startForegroundService(context, intent)
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun makeDraggable(
            windowManager: WindowManager,
            containerView: XFrameLayout,
            params: WindowManager.LayoutParams
        ) {
            var initialTouchX = 0f
            var initialTouchY = 0f
            var initialX = 0
            var initialY = 0
            containerView.dispatchTouchListener = { event ->
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    MotionEvent.ACTION_UP -> {
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(containerView, params)
                    }
                }
            }
        }

        private fun showPopupWindow(
            context: Context,
            container: XFrameLayout
        ): FloatingRef {
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE

            val params = WindowManager.LayoutParams(
                convertDpToPixel(context, MAX_WIDTH_DP).toInt(),
                convertDpToPixel(context, MAX_HEIGHT_DP).toInt(),
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            params.x = 0
            params.y = 100

            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            makeDraggable(windowManager, container, params)
            windowManager.addView(container, params)

            return FloatingRef(
                windowManager,
                container,
                params
            )
        }

        private fun convertDpToPixel(
            context: Context,
            dp: Int
        ): Float =
            dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
    }
}
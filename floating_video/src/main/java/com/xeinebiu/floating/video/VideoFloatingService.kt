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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.video.VideoSize
import com.xeinebiu.floating.R
import com.xeinebiu.floating.databinding.LayoutPlayer1Binding
import com.xeinebiu.floating.video.model.Stream
import com.xeinebiu.floating.video.model.Subtitle
import com.xeinebiu.floating.video.model.VideoItem
import com.xeinebiu.floating.video.view.XFrameLayout

class VideoFloatingService : Service(), Player.Listener {

    private val viewBinding: LayoutPlayer1Binding by lazy {
        val inflater = LayoutInflater.from(this)
        LayoutPlayer1Binding.inflate(inflater)
    }

    private var floatingRef: FloatingRef? = null

    private val exoMediaSourceHelper by lazy {
        ExoMediaSourceHelper(context = this) { mediaItem ->
            mediaItem.localConfiguration?.tag as Stream
        }
    }

    private val player: ExoPlayer by lazy {
        val maxWidth = resources.getDimension(R.dimen.floating_video_max_window_width).toInt()
        val maxHeight = resources.getDimension(R.dimen.floating_video_max_window_height).toInt()

        val trackSelector = DefaultTrackSelector(
            this,
            DefaultTrackSelector
                .Parameters
                .Builder(this)
                .setMaxVideoSize(maxWidth, maxHeight)
                .build(),
        )

        ExoPlayer
            .Builder(this)
            .setTrackSelector(trackSelector)
            .build()
    }

    private val windowManager: WindowManager
        get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        startForeground(
            SERVICE_CODE,
            createNotification().build(),
        )

        setPlayer(viewBinding)

        initListeners(viewBinding)
    }

    private fun initListeners(view: LayoutPlayer1Binding) {
        view.stopBtn.setOnClickListener { stopService() }

        view.player.setControllerVisibilityListener(
            StyledPlayerView.ControllerVisibilityListener { visibility ->
                view.stopBtn.visibility = visibility
            },
        )
    }

    private fun setPlayer(view: LayoutPlayer1Binding) {
        view.player.player = player

        player.addListener(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val item = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.extras?.getParcelable(EXTRA_ITEM, VideoItem::class.java)
        } else {
            intent?.extras?.getParcelable(EXTRA_ITEM) as VideoItem?
        } ?: return START_STICKY

        if (floatingRef == null) {
            floatingRef = showPopupWindow(
                context = this,
                windowManager = windowManager,
                container = viewBinding.root,
            )
        }

        play(
            streams = item.streams,
            subtitles = item.subtitles,
        )

        return START_STICKY
    }

    override fun onDestroy() {
        player.release()

        windowManager.removeView(viewBinding.root)

        super.onDestroy()
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        val fRef = floatingRef ?: return

        val aspectRatio = videoSize.width.toFloat() / videoSize.height
        fRef.params.height = (fRef.params.width / aspectRatio).toInt()
        fRef.updateView()
    }

    private fun stopService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }

        stopSelf()
    }

    private fun play(
        streams: List<Stream>,
        subtitles: List<Subtitle>,
    ) {
        val mediaItems = streams.map { stream ->
            MediaItem.Builder().setUri(stream.uri).setTag(stream).build()
        }

        val mediaSource = exoMediaSourceHelper.createMergingMediaSource(
            mediaItems,
            subtitles,
        )

        player.let {
            it.stop()
            it.setMediaSource(mediaSource)
            it.prepare()

            it.playWhenReady = true
        }
    }

    private fun createNotification(): NotificationCompat.Builder {
        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE,
        ) as NotificationManager

        val builder = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(SERVICE_TITLE)
            .setSmallIcon(R.drawable.exo_notification_small_icon)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setGroup(SERVICE_CHANNEL_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            channel.setSound(null, null)

            notificationManager.createNotificationChannel(channel)

            builder.setChannelId(SERVICE_CHANNEL_ID)
        }

        return builder
    }

    private class FloatingRef(
        private val windowManager: WindowManager,
        private val container: XFrameLayout,
        val params: WindowManager.LayoutParams,
    ) {
        fun updateView(): Unit = windowManager.updateViewLayout(container, params)
    }

    companion object {
        private const val SERVICE_CHANNEL_ID = "Floating Video"

        private const val SERVICE_TITLE = "Popup Player"

        private const val SERVICE_CODE = 30004

        private const val EXTRA_ITEM = "extra_item"

        fun play(
            context: Context,
            videoItem: VideoItem,
        ) {
            val intent = Intent(context, VideoFloatingService::class.java).also {
                it.putExtra(EXTRA_ITEM, videoItem)
            }

            ContextCompat.startForegroundService(context, intent)
        }

        private fun showPopupWindow(
            context: Context,
            windowManager: WindowManager,
            container: XFrameLayout,
        ): FloatingRef {
            val layoutFlag = getWindowLayoutFlag()

            val params = createPopupWindowLayoutParams(
                context = context,
                layoutFlag = layoutFlag,
            )

            makeDraggable(
                windowManager = windowManager,
                containerView = container,
                params = params,
            )

            windowManager.addView(container, params)

            return FloatingRef(
                windowManager = windowManager,
                container = container,
                params = params,
            )
        }

        private fun getWindowLayoutFlag() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        private fun createPopupWindowLayoutParams(
            context: Context,
            layoutFlag: Int,
        ): WindowManager.LayoutParams {
            return WindowManager.LayoutParams(
                context.resources.getDimension(R.dimen.floating_video_max_window_width).toInt(),
                context.resources.getDimension(R.dimen.floating_video_max_window_height).toInt(),
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                x = 0
                y = 100
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun makeDraggable(
            windowManager: WindowManager,
            containerView: XFrameLayout,
            params: WindowManager.LayoutParams,
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

                    MotionEvent.ACTION_UP -> Unit

                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()

                        windowManager.updateViewLayout(containerView, params)
                    }
                }
            }
        }
    }
}

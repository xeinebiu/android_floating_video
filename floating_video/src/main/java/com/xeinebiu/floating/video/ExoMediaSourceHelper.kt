package com.xeinebiu.floating.video

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import com.xeinebiu.floating.video.model.Stream
import com.xeinebiu.floating.video.model.Subtitle

internal class ExoMediaSourceHelper(
    private val context: Context,
    private val streamRetriever: (MediaItem) -> Stream
) {
    fun createMergingMediaSource(
        mediaItems: List<MediaItem>,
        subtitles: List<Subtitle>
    ): MergingMediaSource {
        val sources = mutableListOf<MediaSource>()

        sources.addAll(createMediaSources(mediaItems))

        sources.addAll(insertSubtitles(subtitles))

        return MergingMediaSource(*sources.toTypedArray())
    }

    private fun createMediaSources(items: List<MediaItem>) = items.map(::createMediaSource)

    private fun insertSubtitles(subtitles: List<Subtitle>) = subtitles.map {
        val dataSource = buildDataSourceFactory(
            context = context,
            headers = it.headers
        )

        val subtitle = MediaItem.SubtitleConfiguration.Builder(it.uri)
            .setMimeType(it.mime)
            .setLanguage(it.language)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        SingleSampleMediaSource.Factory(dataSource)
            .createMediaSource(subtitle, C.TIME_UNSET)
    }

    private fun createMediaSource(mediaItem: MediaItem): MediaSource {
        val stream = streamRetriever(mediaItem)

        val dataSource = buildDataSourceFactory(context, stream.headers)

        val factory = buildMediaSourceFactory(dataSource, stream.uri)

        return factory.createMediaSource(mediaItem)
    }

    private fun buildDataSourceFactory(
        context: Context,
        headers: Map<String, String>?,
    ): () -> DefaultDataSource {
        return {
            val defaultHttpDataSource = DefaultHttpDataSource.Factory()
                .setUserAgent(USER_AGENT)
                .createDataSource()

            headers?.let {
                for (h in it) defaultHttpDataSource.setRequestProperty(h.key, h.value)
            }

            DefaultDataSource(context, defaultHttpDataSource)
        }
    }

    private fun buildMediaSourceFactory(
        factory: DataSource.Factory,
        uri: Uri
    ): MediaSource.Factory = when (val type = Util.inferContentType(uri)) {
        C.TYPE_SS -> SsMediaSource.Factory(factory)

        C.TYPE_DASH -> DashMediaSource.Factory(factory)

        C.TYPE_HLS -> HlsMediaSource.Factory(factory)

        C.TYPE_OTHER -> ProgressiveMediaSource.Factory(factory)

        else -> throw IllegalStateException("Unsupported Content Type: $type");
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36"
    }
}
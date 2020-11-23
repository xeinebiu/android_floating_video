package com.xeinebiu.floating.video

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.*
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
        items: List<MediaItem>,
        subtitles: List<Subtitle>
    ): MergingMediaSource {
        val sources = mutableListOf<MediaSource>()

        items.forEach {
            sources.add(createMediaSource(it))
        }
        subtitles.forEach {
            val dataSource = buildDataSourceFactory(context, it.headers)
            val s = MediaItem.Subtitle(
                it.uri,
                it.mime,  // The mime type. Must be set correctly.
                it.language,  // The subtitle language. May be null.
                C.SELECTION_FLAG_DEFAULT // Selection flags for the track.
            )
            sources.add(
                SingleSampleMediaSource.Factory(dataSource).createMediaSource(s, C.TIME_UNSET)
            )
        }
        return MergingMediaSource(*sources.toTypedArray())
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
            val defaultHttpDataSource = DefaultHttpDataSource(USER_AGENT)
            headers?.let {
                for (h in it)
                    defaultHttpDataSource.setRequestProperty(h.key, h.value)
            }
            DefaultDataSource(context, defaultHttpDataSource)
        }
    }

    private fun buildMediaSourceFactory(
        factory: DataSource.Factory,
        uri: Uri
    ): MediaSourceFactory {
        return when (val type = Util.inferContentType(uri)) {
            C.TYPE_SS -> SsMediaSource.Factory(factory)
            C.TYPE_DASH -> DashMediaSource.Factory(factory)
            C.TYPE_HLS -> HlsMediaSource.Factory(factory)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(factory)
            else -> throw IllegalStateException("Unsupported type: $type");
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36"
    }
}
package com.xeinebiu.floating.video.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Stream(
    val uri: Uri,
    val headers: HashMap<String, String>,
) : Parcelable

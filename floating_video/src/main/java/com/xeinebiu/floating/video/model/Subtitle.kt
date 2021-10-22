package com.xeinebiu.floating.video.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Subtitle(
    val uri: Uri,
    val mime: String,
    val language: String,
    val headers: HashMap<String, String>
) : Parcelable

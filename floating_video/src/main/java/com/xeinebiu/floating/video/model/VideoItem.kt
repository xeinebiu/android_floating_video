package com.xeinebiu.floating.video.model

import android.os.Parcel
import android.os.Parcelable


data class VideoItem(
    val id: String,
    val streams: List<Stream>
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        mutableListOf<Stream>().let {
            parcel.readTypedList(it, Stream.CREATOR)
            it
        }
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(
        dest: Parcel,
        flags: Int
    ) {
        dest.writeString(id)
        dest.writeTypedList(streams)
    }

    companion object CREATOR : Parcelable.Creator<VideoItem> {
        override fun createFromParcel(parcel: Parcel): VideoItem =
            VideoItem(parcel)

        override fun newArray(size: Int): Array<VideoItem?> =
            arrayOfNulls(size)
    }
}
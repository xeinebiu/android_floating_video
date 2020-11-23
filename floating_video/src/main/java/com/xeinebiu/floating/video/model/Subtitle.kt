package com.xeinebiu.floating.video.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class Subtitle(
    val uri: Uri,
    val mime: String,
    val language: String,
    val headers: HashMap<String, String>
) : Parcelable {

    constructor(parcel: Parcel) : this(
        Uri.parse(parcel.readString()),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readSerializable() as HashMap<String, String>
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(
        dest: Parcel,
        flags: Int
    ) {
        dest.writeString(uri.toString())
        dest.writeString(mime)
        dest.writeString(language)
        dest.writeSerializable(headers)
    }

    companion object CREATOR : Parcelable.Creator<Subtitle> {
        override fun createFromParcel(parcel: Parcel): Subtitle =
            Subtitle(parcel)

        override fun newArray(size: Int): Array<Subtitle?> =
            arrayOfNulls(size)
    }
}
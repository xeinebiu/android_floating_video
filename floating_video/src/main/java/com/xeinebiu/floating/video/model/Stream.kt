package com.xeinebiu.floating.video.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable


data class Stream(
    val uri: Uri,
    val headers: HashMap<String, String>
) : Parcelable {

    constructor(parcel: Parcel) : this(
        Uri.parse(parcel.readString()),
        parcel.readSerializable() as HashMap<String, String>
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(
        dest: Parcel,
        flags: Int
    ) {
        dest.writeString(uri.toString())
        dest.writeSerializable(headers)
    }

    companion object CREATOR : Parcelable.Creator<Stream> {
        override fun createFromParcel(parcel: Parcel): Stream =
            Stream(parcel)

        override fun newArray(size: Int): Array<Stream?> =
            arrayOfNulls(size)
    }
}
package com.omarkarimli.mlapp.data.converters

import android.net.Uri
import androidx.room.TypeConverter

class UriConverter {
    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return if (uriString != null) {
            Uri.parse(uriString)
        } else {
            null
        }
    }
}
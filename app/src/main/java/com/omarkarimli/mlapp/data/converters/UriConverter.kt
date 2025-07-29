package com.omarkarimli.mlapp.data.converters

import android.net.Uri
import androidx.room.TypeConverter

class UriConverter {
    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return "$uri"
    }

    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return uriString?.let { Uri.parse(it) }
    }
}
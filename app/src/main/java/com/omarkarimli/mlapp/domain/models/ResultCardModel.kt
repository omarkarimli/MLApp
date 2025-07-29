package com.omarkarimli.mlapp.domain.models

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_results")
data class ResultCardModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "subtitle")
    val subtitle: String,
    @ColumnInfo(name = "image_uri")
    val imageUri: Uri? = null,
    val timestamp: Long = System.currentTimeMillis()
)
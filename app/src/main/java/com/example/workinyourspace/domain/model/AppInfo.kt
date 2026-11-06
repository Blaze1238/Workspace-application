package com.example.workinyourspace.domain.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val name: String,
    val icon: Drawable,
    val packageName: String,
    val iconBitmap: ImageBitmap? = null
)
package com.example.facegrid.domain.repository

import android.graphics.Bitmap
import android.net.Uri

interface GalleryRepository {
    suspend fun save(bitmap: Bitmap): Uri
}

package com.example.facegrid.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.facegrid.domain.model.SavedCollage

interface GalleryRepository {
    suspend fun save(bitmap: Bitmap): Uri
    suspend fun listSaved(): List<SavedCollage> = emptyList()
}

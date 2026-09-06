package com.example.facegrid.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.example.facegrid.domain.repository.GalleryRepository

class SaveCollageUseCase(private val repository: GalleryRepository) {
    suspend operator fun invoke(bitmap: Bitmap): Uri = repository.save(bitmap)
}

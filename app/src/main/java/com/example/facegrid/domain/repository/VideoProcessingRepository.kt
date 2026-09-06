package com.example.facegrid.domain.repository

import android.net.Uri
import com.example.facegrid.domain.model.ProcessingResult
import com.example.facegrid.domain.model.ProgressUpdate

interface VideoProcessingRepository {
    suspend fun process(uri: Uri, onProgress: (ProgressUpdate) -> Unit): ProcessingResult
}

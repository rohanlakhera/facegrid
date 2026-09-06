package com.example.facegrid.domain.usecase

import android.net.Uri
import com.example.facegrid.domain.model.ProcessingResult
import com.example.facegrid.domain.model.ProgressUpdate
import com.example.facegrid.domain.repository.VideoProcessingRepository

class ProcessVideoUseCase(private val repository: VideoProcessingRepository) {
    suspend operator fun invoke(uri: Uri, onProgress: (ProgressUpdate) -> Unit): ProcessingResult =
        repository.process(uri, onProgress)
}

package com.example.facegrid.data.repository

import android.net.Uri
import com.example.facegrid.data.processing.VideoProcessor
import com.example.facegrid.domain.model.ProcessingResult
import com.example.facegrid.domain.model.ProgressUpdate
import com.example.facegrid.domain.repository.VideoProcessingRepository

class VideoProcessingRepositoryImpl(private val processor: VideoProcessor) : VideoProcessingRepository {
    override suspend fun process(uri: Uri, onProgress: (ProgressUpdate) -> Unit): ProcessingResult =
        processor.process(uri, onProgress)
}

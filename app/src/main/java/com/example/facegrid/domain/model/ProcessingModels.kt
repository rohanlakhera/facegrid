package com.example.facegrid.domain.model

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect

data class ProgressUpdate(
    val stage: ProcessingStage,
    val current: Int,
    val total: Int,
    val message: String
)

enum class ProcessingStage {
    EXTRACTING,
    DETECTING,
    EMBEDDING,
    CLUSTERING,
    RENDERING
}

data class FaceObservation(
    val frameIndex: Int,
    val timestampUs: Long,
    val bitmap: Bitmap,
    val boundingBox: Rect,
    val leftEye: PointF? = null,
    val rightEye: PointF? = null,
    val noseBase: PointF? = null,
    val mouthLeft: PointF? = null,
    val mouthRight: PointF? = null,
    val eulerY: Float,
    val eulerZ: Float,
    val smileProbability: Float,
    val leftEyeOpenProbability: Float,
    val rightEyeOpenProbability: Float,
    val sharpness: Double,
    /** All deduplicated face boxes detected in this source frame. */
    val frameFaceBoxes: List<Rect> = emptyList()
)

data class AppearanceSegment(
    val id: Int,
    val bestFrame: FaceObservation,
    val candidateFrames: List<FaceObservation> = listOf(bestFrame),
    val frameStart: Int = bestFrame.frameIndex,
    val frameEnd: Int = bestFrame.frameIndex
)

data class IdentityResult(
    val id: Int,
    val appearanceCount: Int,
    val representative: FaceObservation,
    val segmentIds: List<Int> = emptyList()
)

data class ProcessingResult(
    val collage: Bitmap,
    val identities: List<IdentityResult>,
    val usesFallbackEmbedding: Boolean = false,
    val diagnosticLog: String = ""
)

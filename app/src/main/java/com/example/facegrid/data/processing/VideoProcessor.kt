package com.example.facegrid.data.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.example.facegrid.domain.model.AppearanceSegment
import com.example.facegrid.domain.model.FaceObservation
import com.example.facegrid.domain.model.ProcessingResult
import com.example.facegrid.domain.model.ProcessingStage
import com.example.facegrid.domain.model.ProgressUpdate
import java.util.concurrent.TimeUnit
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import android.media.MediaMetadataRetriever

class VideoProcessor(private val context: Context) {
    suspend fun process(uri: Uri, onProgress: (ProgressUpdate) -> Unit): ProcessingResult = withContext(Dispatchers.Default) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build()
        )
        val embedder = GhostFaceNetEmbedder(context)
        Log.d(TAG, "embedder fallback=${embedder.usesFallbackEmbedding} model=${embedder.modelDescription}")
        try {
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: error("The selected video has no readable duration")
            val intervalUs = 1_000_000L / ProcessingConfig.SAMPLE_FPS
            val totalFrames = maxOf(1, ((durationMs * 1_000L + intervalUs - 1) / intervalUs).toInt())
            val tracks = mutableListOf<OpenTrack>()
            val closedSegments = mutableListOf<AppearanceSegment>()
            var nextTrackId = 1
            var nextSegmentId = 1
            var framesWithDetections = 0
            var totalDetections = 0
            var totalVisibleFaces = 0
            val diagnostic = StringBuilder()
            diagnostic.appendLine("FaceGrid result log v1")
            diagnostic.appendLine("video=$uri")
            diagnostic.appendLine("durationMs=$durationMs sampleFps=${ProcessingConfig.SAMPLE_FPS} totalSampledFrames=$totalFrames")
            diagnostic.appendLine("modelFallback=${embedder.usesFallbackEmbedding} model=${embedder.modelDescription}")
            diagnostic.appendLine("coordinateSystem=bitmap pixels; box=(left,top,right,bottom)")

            for (frameIndex in 0 until totalFrames) {
                ensureActive()
                onProgress(ProgressUpdate(ProcessingStage.EXTRACTING, frameIndex + 1, totalFrames, "Reading frame ${frameIndex + 1}/$totalFrames"))
                val timestampUs = minOf(frameIndex * intervalUs, durationMs * 1_000L)
                val bitmap = retriever.getFrameAtTime(timestampUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bitmap == null) {
                    closeTracks(tracks, closedSegments, nextSegmentId)
                    nextSegmentId += tracks.size
                    tracks.clear()
                    continue
                }
                onProgress(ProgressUpdate(ProcessingStage.DETECTING, frameIndex + 1, totalFrames, "Detecting faces: frame ${frameIndex + 1}/$totalFrames"))
                val rawDetections = detectFaces(detector, bitmap, frameIndex, timestampUs)
                val detected = deduplicateDetections(rawDetections)
                val trackable = detected.filter { isTrackableFace(it, bitmap.width, bitmap.height) }
                val visible = trackable.filter { isClearlyVisible(it, bitmap.width, bitmap.height) }
                if (rawDetections.isNotEmpty()) framesWithDetections++
                totalDetections += rawDetections.size
                totalVisibleFaces += visible.size
                Log.d(TAG, "frame=$frameIndex rawDetections=${rawDetections.size} detections=${detected.size} trackable=${trackable.size} visible=${visible.size} activeTracks=${tracks.size}")
                diagnostic.appendLine(
                    "frame=$frameIndex timeMs=${timestampUs / 1_000} rawDetections=${rawDetections.size} " +
                        "dedupedDetections=${detected.size} trackableFaces=${trackable.size} " +
                        "qualifiedFaces=${visible.size} activeTracksBefore=${tracks.size}"
                )
                visible.forEachIndexed { faceIndex, face ->
                    diagnostic.appendLine(
                            "  qualifiedFace=$faceIndex box=${face.boundingBox.toShortString()} " +
                            "frameFaceCount=${face.frameFaceBoxes.size} " +
                            "sharpness=${"%.2f".format(java.util.Locale.US, face.sharpness)} " +
                            "eulerY=${"%.2f".format(java.util.Locale.US, face.eulerY)} " +
                            "eulerZ=${"%.2f".format(java.util.Locale.US, face.eulerZ)}"
                    )
                }
                // Track only qualified faces. A detector box during a whip-pan
                // can be geometrically valid but visually unreliable; allowing
                // it to bridge two shots fuses two different appearances into
                // one segment (for example frames 32 -> 34 in sample 1).
                val assignments = findAssignments(tracks, visible)
                val matchedFaces = assignments.map { it.second }.toSet()
                val unmatchedTracks = tracks.filter { track -> assignments.none { it.first === track } }
                val stillOpen = mutableListOf<OpenTrack>()
                tracks.forEach { track ->
                    val assignment = assignments.firstOrNull { it.first === track }
                    if (assignment != null) {
                        track.update(assignment.second)
                        stillOpen += track
                    } else if (visible.isEmpty()) {
                        track.markMissed()
                        if (track.missedFrames <= ProcessingConfig.MAX_TRACK_MISSED_FRAMES) {
                            stillOpen += track
                        } else {
                            closedSegments += track.toSegment(nextSegmentId++)
                        }
                    } else {
                        // A different visible face is present. Closing the unmatched
                        // track prevents it from jumping to a new person entering the shot.
                        closedSegments += track.toSegment(nextSegmentId++)
                    }
                }
                tracks.clear()
                tracks += stillOpen
                for (face in visible) {
                    if (face !in matchedFaces) tracks += OpenTrack(nextTrackId++, face)
                }
                diagnostic.appendLine("  activeTracksAfter=${tracks.size} unmatchedTracks=${unmatchedTracks.size}")
            }
            for (track in tracks) closedSegments += track.toSegment(nextSegmentId++)

            val usableSegments = closedSegments.filter { segment ->
                if (segment.candidateFrames.size < ProcessingConfig.MIN_SEGMENT_VISIBLE_FRAMES) {
                    diagnostic.appendLine(
                        "discardedSegment=${segment.id} frameRange=${segment.frameStart}-${segment.frameEnd} " +
                            "reason=tooShort candidateFrames=${segment.candidateFrames.size}"
                    )
                    false
                } else {
                    true
                }
            }
            usableSegments.forEach { segment ->
                val frameIndexes = segment.candidateFrames.map { it.frameIndex }
                Log.d(TAG, "segment=${segment.id} frameRange=${segment.frameStart}-${segment.frameEnd} bestFrame=${segment.bestFrame.frameIndex} candidates=${frameIndexes.joinToString(",")}")
                diagnostic.appendLine(
                    "segment=${segment.id} frameRange=${segment.frameStart}-${segment.frameEnd} " +
                        "bestFrame=${segment.bestFrame.frameIndex} candidates=${frameIndexes.joinToString(",")}"
                )
            }

            onProgress(ProgressUpdate(ProcessingStage.EMBEDDING, 0, usableSegments.size, "Embedding appearance segments"))
            val embedded = usableSegments.mapIndexed { index, segment ->
                ensureActive()
                onProgress(ProgressUpdate(ProcessingStage.EMBEDDING, index + 1, usableSegments.size, "Embedding segment ${index + 1}/${usableSegments.size}"))
                val embeddings = segment.candidateFrames.map { frame ->
                    embedder.embed(frame)
                }
                EmbeddedSegment(
                    segment = segment,
                    embedding = averageEmbeddings(embeddings),
                    candidateEmbeddings = embeddings
                )
            }
            onProgress(ProgressUpdate(ProcessingStage.CLUSTERING, 1, 1, "Clustering identities"))
            val identities = clusterSegments(embedded)
            Log.d(TAG, "summary sampledFrames=$totalFrames framesWithDetections=$framesWithDetections detections=$totalDetections visibleFaces=$totalVisibleFaces segments=${usableSegments.size} identities=${identities.size} fallback=${embedder.usesFallbackEmbedding}")
            diagnostic.appendLine(
                "summary sampledFrames=$totalFrames framesWithDetections=$framesWithDetections " +
                    "detections=$totalDetections qualifiedFaces=$totalVisibleFaces segments=${usableSegments.size} " +
                    "identities=${identities.size} fallback=${embedder.usesFallbackEmbedding}"
            )
            identities.forEach { identity ->
                diagnostic.appendLine(
                    "identity=${identity.id} appearanceCount=${identity.appearanceCount} " +
                        "segmentIds=${identity.segmentIds.joinToString(",")} " +
                        "representativeFrame=${identity.representative.frameIndex}"
                )
            }
            onProgress(ProgressUpdate(ProcessingStage.RENDERING, 1, 1, "Rendering collage"))
            ProcessingResult(
                collage = CollageRenderer.render(identities),
                identities = identities,
                usesFallbackEmbedding = embedder.usesFallbackEmbedding,
                diagnosticLog = diagnostic.toString()
            )
        } finally {
            embedder.close()
            detector.close()
            retriever.release()
        }
    }

    private fun detectFaces(detector: FaceDetector, bitmap: Bitmap, frameIndex: Int, timestampUs: Long): List<FaceObservation> {
        val faces = Tasks.await(detector.process(InputImage.fromBitmap(bitmap, 0)), 30, TimeUnit.SECONDS)
        val observations = faces.map { face ->
            FaceObservation(
                frameIndex = frameIndex,
                timestampUs = timestampUs,
                bitmap = bitmap,
                boundingBox = Rect(face.boundingBox),
                leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position,
                rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position,
                noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position,
                mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position,
                mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position,
                eulerY = face.headEulerAngleY,
                eulerZ = face.headEulerAngleZ,
                smileProbability = face.smilingProbability ?: 0.5f,
                leftEyeOpenProbability = face.leftEyeOpenProbability ?: 0.5f,
                rightEyeOpenProbability = face.rightEyeOpenProbability ?: 0.5f,
                sharpness = varianceOfLaplacian(bitmap, face.boundingBox)
            )
        }
        val frameBoxes = observations.map { Rect(it.boundingBox) }
        return observations.map { it.copy(frameFaceBoxes = frameBoxes.map(::Rect)) }
    }

    private fun deduplicateDetections(faces: List<FaceObservation>): List<FaceObservation> {
        val kept = mutableListOf<FaceObservation>()
        faces.sortedWith(
            compareByDescending<FaceObservation> { it.boundingBox.width() * it.boundingBox.height() }
                .thenByDescending { representativeScore(it) }
        ).forEach { candidate ->
            val duplicate = kept.any { existing ->
                intersectionOverUnion(candidate.boundingBox, existing.boundingBox) >= ProcessingConfig.DUPLICATE_FACE_IOU || (
                    intersectionOverSmallerArea(candidate.boundingBox, existing.boundingBox) >= ProcessingConfig.DUPLICATE_FACE_CONTAINMENT
                    )
            }
            if (!duplicate) kept += candidate
        }
        val frameBoxes = kept.map { Rect(it.boundingBox) }
        return kept.map { it.copy(frameFaceBoxes = frameBoxes.map(::Rect)) }
    }

    private fun findAssignments(tracks: List<OpenTrack>, faces: List<FaceObservation>): List<Pair<OpenTrack, FaceObservation>> {
        val candidates = tracks.flatMap { track ->
            faces.map { face -> Triple(track, face, intersectionOverUnion(track.lastFrame.boundingBox, face.boundingBox)) }
        }.filter { it.third >= ProcessingConfig.TRACK_MIN_IOU }
            .sortedByDescending { it.third }
        val usedTracks = mutableSetOf<OpenTrack>()
        val usedFaces = mutableSetOf<FaceObservation>()
        return candidates.mapNotNull { (track, face, _) ->
            if (usedTracks.add(track) && usedFaces.add(face)) track to face else null
        }
    }

    private fun closeTracks(tracks: List<OpenTrack>, segments: MutableList<AppearanceSegment>, nextSegmentId: Int) {
        tracks.forEachIndexed { index, track -> segments += track.toSegment(nextSegmentId + index) }
    }

    private class OpenTrack(val id: Int, var lastFrame: FaceObservation) {
        private val startFrame = lastFrame.frameIndex
        var missedFrames: Int = 0
            private set
        var bestFrame: FaceObservation = lastFrame
        private val candidateFrames = mutableListOf(lastFrame)

        fun update(frame: FaceObservation) {
            lastFrame = frame
            missedFrames = 0
            candidateFrames += frame
            candidateFrames.sortByDescending { representativeScore(it) }
            while (candidateFrames.size > ProcessingConfig.MAX_SEGMENT_CANDIDATE_FRAMES) candidateFrames.removeLast()
            bestFrame = candidateFrames.first()
        }

        fun markMissed() {
            missedFrames++
        }

        fun toSegment(segmentId: Int): AppearanceSegment = AppearanceSegment(
            id = segmentId,
            bestFrame = bestFrame,
            candidateFrames = candidateFrames.toList(),
            frameStart = startFrame,
            frameEnd = lastFrame.frameIndex
        )
    }

    private companion object {
        const val TAG = "FaceGridPipeline"
    }

    private fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
        require(embeddings.isNotEmpty())
        val result = FloatArray(embeddings.first().size)
        embeddings.forEach { embedding ->
            for (index in result.indices) result[index] += embedding[index]
        }
        for (index in result.indices) result[index] /= embeddings.size
        var magnitude = 0.0
        for (value in result) magnitude += value * value
        val scale = if (magnitude == 0.0) 1f else (1.0 / kotlin.math.sqrt(magnitude)).toFloat()
        return result.apply { for (index in indices) this[index] *= scale }
    }
}

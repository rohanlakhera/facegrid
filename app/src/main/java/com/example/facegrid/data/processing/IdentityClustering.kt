package com.example.facegrid.data.processing

import com.example.facegrid.domain.model.AppearanceSegment
import com.example.facegrid.domain.model.FaceObservation
import com.example.facegrid.domain.model.IdentityResult

data class EmbeddedSegment(
    val segment: AppearanceSegment,
    val embedding: FloatArray,
    val candidateEmbeddings: List<FloatArray> = listOf(embedding)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmbeddedSegment

        if (segment != other.segment) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (candidateEmbeddings != other.candidateEmbeddings) return false

        return true
    }

    override fun hashCode(): Int {
        var result = segment.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + candidateEmbeddings.hashCode()
        return result
    }
}

private data class WorkingCluster(
    val segments: MutableList<EmbeddedSegment>
)

fun clusterSegments(segments: List<EmbeddedSegment>): List<IdentityResult> {
    val clusters = segments.map { WorkingCluster(mutableListOf(it)) }.toMutableList()

    while (true) {
        var bestPair: Pair<Int, Int>? = null
        var bestSimilarity = ProcessingConfig.IDENTITY_SIMILARITY_THRESHOLD
        for (leftIndex in 0 until clusters.lastIndex) {
            for (rightIndex in leftIndex + 1 until clusters.size) {
                val left = clusters[leftIndex]
                val right = clusters[rightIndex]
                if (!clustersAreCompatible(left, right)) continue
                val similarity = averageClusterSimilarity(left, right)
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity
                    bestPair = leftIndex to rightIndex
                }
            }
        }
        val (leftIndex, rightIndex) = bestPair ?: break
        val left = clusters[leftIndex]
        val right = clusters[rightIndex]
        left.segments += right.segments
        clusters.removeAt(rightIndex)
    }

    while (true) {
        var bestSingleton: Triple<Int, Int, Float>? = null
        var bestMargin = 0f
        clusters.forEachIndexed { singletonIndex, singleton ->
            if (singleton.segments.size != 1) return@forEachIndexed
            val candidates = clusters.indices
                .filter { it != singletonIndex && clustersAreCompatible(singleton, clusters[it]) }
                .map { targetIndex ->
                    targetIndex to maxCandidateSimilarity(singleton, clusters[targetIndex])
                }
                .sortedByDescending { it.second }
            val best = candidates.firstOrNull() ?: return@forEachIndexed
            val second = candidates.getOrNull(1)?.second ?: 0f
            val margin = best.second - second
            if (best.second >= ProcessingConfig.SINGLETON_MERGE_SIMILARITY_THRESHOLD &&
                margin >= ProcessingConfig.SINGLETON_MERGE_MARGIN &&
                (bestSingleton == null || margin > bestMargin || (margin == bestMargin && best.second > bestSingleton!!.third))
            ) {
                bestSingleton = Triple(singletonIndex, best.first, best.second)
                bestMargin = margin
            }
        }
        val merge = bestSingleton ?: break
        val (singletonIndex, targetIndex, similarity) = merge
        val singleton = clusters[singletonIndex]
        val target = clusters[targetIndex]
        target.segments += singleton.segments
        clusters.removeAt(singletonIndex)
    }

    val orderedClusters = clusters.sortedBy { cluster -> cluster.segments.minOf { it.segment.id } }
    return orderedClusters.mapIndexed { index, cluster ->
        val representative = cluster.segments
            .flatMap { it.segment.candidateFrames }
            .let { candidates ->
                val singlePerson = candidates.filter { it.frameFaceBoxes.size <= 1 }
                (singlePerson.ifEmpty { candidates }).maxBy { representativeScore(it) }
            }
        IdentityResult(
            id = index + 1,
            appearanceCount = cluster.segments.size,
            representative = representative,
            segmentIds = cluster.segments.map { it.segment.id }.sorted()
        )
    }
}

private fun clustersAreCompatible(left: WorkingCluster, right: WorkingCluster): Boolean {
    val leftFrames = left.segments
        .flatMap { it.segment.candidateFrames }
        .map { it.frameIndex }
        .toSet()
    return right.segments
        .flatMap { it.segment.candidateFrames }
        .none { it.frameIndex in leftFrames }
}

private fun averageClusterSimilarity(left: WorkingCluster, right: WorkingCluster): Float {
    var total = 0.0
    var count = 0
    left.segments.forEach { leftSegment ->
        right.segments.forEach { rightSegment ->
            total += segmentSimilarity(leftSegment, rightSegment)
            count++
        }
    }
    return if (count == 0) 0f else (total / count).toFloat()
}

private fun maxCandidateSimilarity(left: WorkingCluster, right: WorkingCluster): Float {
    return left.segments.flatMap { leftSegment ->
        right.segments.flatMap { rightSegment ->
            leftSegment.candidateEmbeddings.flatMap { leftEmbedding ->
                rightSegment.candidateEmbeddings.map { rightEmbedding ->
                    cosineSimilarity(leftEmbedding, rightEmbedding)
                }
            }
        }
    }.maxOrNull() ?: 0f
}

private fun segmentSimilarity(left: EmbeddedSegment, right: EmbeddedSegment): Float {
    val pairwise = left.candidateEmbeddings.flatMap { leftEmbedding ->
        right.candidateEmbeddings.map { rightEmbedding ->
            cosineSimilarity(
                leftEmbedding,
                rightEmbedding
            )
        }
    }.sortedDescending()
    if (pairwise.isEmpty()) return cosineSimilarity(left.embedding, right.embedding)
    return pairwise.take(ProcessingConfig.SEGMENT_SIMILARITY_TOP_K).average().toFloat()
}

fun representativeScore(face: FaceObservation): Double {
    val frontality =
        (1.0 - (kotlin.math.abs(face.eulerY) / ProcessingConfig.MAX_EULER_Y).coerceIn(0f, 1f)) *
                (1.0 - (kotlin.math.abs(face.eulerZ) / ProcessingConfig.MAX_EULER_Z).coerceIn(
                    0f,
                    1f
                ))
    val sharpness = (face.sharpness / (ProcessingConfig.MIN_SHARPNESS * 8.0)).coerceIn(0.0, 1.0)
    val eyes = ((face.leftEyeOpenProbability + face.rightEyeOpenProbability) / 2f).coerceIn(0f, 1f)
    val smile = face.smileProbability.coerceIn(0f, 1f)
    val base = frontality * 0.40 + sharpness * 0.25 + eyes * 0.20 + smile * 0.15
    return if (eyes < 0.25) base * 0.2 else base
}

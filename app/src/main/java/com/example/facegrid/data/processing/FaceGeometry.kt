package com.example.facegrid.data.processing

import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import com.example.facegrid.domain.model.FaceObservation

fun intersectionOverUnion(first: Rect, second: Rect): Float {
    val left = max(first.left, second.left)
    val top = max(first.top, second.top)
    val right = min(first.right, second.right)
    val bottom = min(first.bottom, second.bottom)
    val intersectionWidth = max(0, right - left)
    val intersectionHeight = max(0, bottom - top)
    val intersection = intersectionWidth.toLong() * intersectionHeight.toLong()
    if (intersection == 0L) return 0f
    val firstArea = first.width().toLong() * first.height().toLong()
    val secondArea = second.width().toLong() * second.height().toLong()
    val union = firstArea + secondArea - intersection
    return if (union == 0L) 0f else intersection.toFloat() / union.toFloat()
}

fun isClearlyVisible(face: FaceObservation, frameWidth: Int, frameHeight: Int): Boolean {
    val areaRatio = face.boundingBox.width().toFloat() * face.boundingBox.height() / (frameWidth * frameHeight).toFloat()
    val widthRatio = face.boundingBox.width().toFloat() / frameWidth.toFloat()
    return areaRatio >= ProcessingConfig.MIN_FACE_AREA_RATIO &&
        widthRatio >= ProcessingConfig.MIN_FACE_WIDTH_RATIO &&
        kotlin.math.abs(face.eulerY) <= ProcessingConfig.MAX_EULER_Y &&
        kotlin.math.abs(face.eulerZ) <= ProcessingConfig.MAX_EULER_Z &&
        face.sharpness >= ProcessingConfig.MIN_SHARPNESS
}

/** A box may be used to keep an existing track alive even when the frame is blurred. */
fun isTrackableFace(face: FaceObservation, frameWidth: Int, frameHeight: Int): Boolean {
    val areaRatio = face.boundingBox.width().toFloat() * face.boundingBox.height() / (frameWidth * frameHeight).toFloat()
    val widthRatio = face.boundingBox.width().toFloat() / frameWidth.toFloat()
    return areaRatio >= ProcessingConfig.MIN_FACE_AREA_RATIO &&
        widthRatio >= ProcessingConfig.MIN_FACE_WIDTH_RATIO &&
        kotlin.math.abs(face.eulerY) <= ProcessingConfig.MAX_EULER_Y &&
        kotlin.math.abs(face.eulerZ) <= ProcessingConfig.MAX_EULER_Z
}

fun intersectionOverSmallerArea(first: Rect, second: Rect): Float {
    val left = max(first.left, second.left)
    val top = max(first.top, second.top)
    val right = min(first.right, second.right)
    val bottom = min(first.bottom, second.bottom)
    val intersectionWidth = max(0, right - left)
    val intersectionHeight = max(0, bottom - top)
    val intersection = intersectionWidth.toLong() * intersectionHeight.toLong()
    val smallerArea = min(
        first.width().toLong() * first.height().toLong(),
        second.width().toLong() * second.height().toLong()
    )
    return if (smallerArea == 0L) 0f else intersection.toFloat() / smallerArea.toFloat()
}

fun centerDistanceRatio(first: Rect, second: Rect): Float {
    val dx = first.exactCenterX() - second.exactCenterX()
    val dy = first.exactCenterY() - second.exactCenterY()
    val scale = max(first.width(), first.height()).coerceAtLeast(max(second.width(), second.height())).toFloat()
    return if (scale == 0f) Float.MAX_VALUE else (sqrt(dx * dx + dy * dy) / scale)
}

fun cosineSimilarity(first: FloatArray, second: FloatArray): Float {
    val size = min(first.size, second.size)
    var dot = 0.0
    var firstMagnitude = 0.0
    var secondMagnitude = 0.0
    for (index in 0 until size) {
        dot += first[index] * second[index]
        firstMagnitude += first[index] * first[index]
        secondMagnitude += second[index] * second[index]
    }
    val denominator = kotlin.math.sqrt(firstMagnitude) * kotlin.math.sqrt(secondMagnitude)
    return if (denominator == 0.0) 0f else (dot / denominator).toFloat()
}

fun normalizedAverage(current: FloatArray, sample: FloatArray, count: Int): FloatArray {
    val result = FloatArray(current.size)
    for (index in current.indices) result[index] = (current[index] * count + sample[index]) / (count + 1)
    var magnitude = 0.0
    for (value in result) magnitude += value * value
    val scale = if (magnitude == 0.0) 1f else (1.0 / kotlin.math.sqrt(magnitude)).toFloat()
    return result.apply { for (index in indices) this[index] *= scale }
}

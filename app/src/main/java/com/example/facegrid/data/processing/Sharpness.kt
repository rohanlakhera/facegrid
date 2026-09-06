package com.example.facegrid.data.processing

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

fun varianceOfLaplacian(bitmap: Bitmap, face: Rect): Double {
    val left = max(0, face.left)
    val top = max(0, face.top)
    val right = min(bitmap.width, face.right)
    val bottom = min(bitmap.height, face.bottom)
    if (right - left < 3 || bottom - top < 3) return 0.0
    val width = right - left
    val height = bottom - top
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, left, top, width, height)
    val gray = DoubleArray(pixels.size)
    for (index in pixels.indices) {
        val color = pixels[index]
        gray[index] = 0.299 * ((color shr 16) and 0xff) +
                0.587 * ((color shr 8) and 0xff) +
                0.114 * (color and 0xff)
    }
    var sum = 0.0
    var sumSquares = 0.0
    var count = 0
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val index = y * width + x
            val laplacian =
                gray[index - width] + gray[index - 1] - 4 * gray[index] + gray[index + 1] + gray[index + width]
            sum += laplacian
            sumSquares += laplacian * laplacian
            count++
        }
    }
    if (count == 0) return 0.0
    val mean = sum / count
    return max(0.0, sumSquares / count - mean * mean)
}

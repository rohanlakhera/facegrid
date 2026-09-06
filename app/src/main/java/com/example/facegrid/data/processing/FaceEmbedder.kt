package com.example.facegrid.data.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import com.example.facegrid.domain.model.FaceObservation
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

interface FaceEmbedder : Closeable {
    suspend fun embed(face: FaceObservation): FloatArray
}

class GhostFaceNetEmbedder(private val context: Context) : FaceEmbedder {
    private val interpreter: Interpreter? = loadInterpreter()
    private val fallback = FallbackFaceEmbedder()
    val usesFallbackEmbedding: Boolean get() = interpreter == null
    val modelDescription: String
        get() = interpreter?.let { model ->
            val input = model.getInputTensor(0)
            val output = model.getOutputTensor(0)
            "inputShape=${input.shape().contentToString()} inputType=${input.dataType()} " +
                    "outputShape=${
                        output.shape().contentToString()
                    } outputType=${output.dataType()}"
        } ?: "fallback"

    override suspend fun embed(face: FaceObservation): FloatArray =
        withContext(Dispatchers.Default) {
            val model = interpreter ?: return@withContext fallback.embed(face)
            val input = prepareInput(face)
            val outputSize =
                model.getOutputTensor(0).shape().fold(1) { product, value -> product * value }
            val output = Array(1) { FloatArray(outputSize) }
            model.run(input, output)
            normalize(output[0])
        }

    override fun close() {
        interpreter?.close()
    }

    private fun loadInterpreter(): Interpreter? = try {
        val modelBytes =
            context.assets.open(ProcessingConfig.MODEL_ASSET_NAME).use { it.readBytes() }
        val buffer = ByteBuffer.allocateDirect(modelBytes.size).order(ByteOrder.nativeOrder())
        buffer.put(modelBytes)
        buffer.rewind()
        Interpreter(buffer, Interpreter.Options().setNumThreads(4))
    } catch (error: Throwable) {
        Log.e(TAG, "Failed to load ${ProcessingConfig.MODEL_ASSET_NAME}", error)
        null
    }

    private fun prepareInput(face: FaceObservation): ByteBuffer {
        val crop = alignedFaceCrop(face)
        val resized =
            crop.scale(ProcessingConfig.MODEL_INPUT_SIZE, ProcessingConfig.MODEL_INPUT_SIZE)
        val input =
            ByteBuffer.allocateDirect(ProcessingConfig.MODEL_INPUT_SIZE * ProcessingConfig.MODEL_INPUT_SIZE * 3 * 4)
                .order(ByteOrder.nativeOrder())
        val pixels = IntArray(resized.width * resized.height)
        resized.getPixels(pixels, 0, resized.width, 0, 0, resized.width, resized.height)
        for (pixel in pixels) {
            input.putFloat((((pixel shr 16) and 0xff) - 127.5f) / 128f)
            input.putFloat((((pixel shr 8) and 0xff) - 127.5f) / 128f)
            input.putFloat(((pixel and 0xff) - 127.5f) / 128f)
        }
        crop.recycle()
        if (resized !== crop) resized.recycle()
        input.rewind()
        return input
    }

    private companion object {
        const val TAG = "FaceGridPipeline"
    }
}

private class FallbackFaceEmbedder : FaceEmbedder {
    override suspend fun embed(face: FaceObservation): FloatArray =
        withContext(Dispatchers.Default) {
            val crop = alignedFaceCrop(face)
            val resized = crop.scale(8, 8)
            val pixels = IntArray(resized.width * resized.height)
            resized.getPixels(pixels, 0, resized.width, 0, 0, resized.width, resized.height)
            val output = FloatArray(ProcessingConfig.FALLBACK_EMBEDDING_SIZE)
            val luminance = FloatArray(pixels.size)
            val chroma = FloatArray(pixels.size)
            var luminanceMean = 0f
            var chromaMean = 0f
            for (index in pixels.indices) {
                val pixel = pixels[index]
                luminance[index] =
                    (0.299f * ((pixel shr 16) and 0xff) + 0.587f * ((pixel shr 8) and 0xff) + 0.114f * (pixel and 0xff)) / 255f
                chroma[index] = (((pixel shr 16) and 0xff) - (pixel and 0xff)) / 255f
                luminanceMean += luminance[index]
                chromaMean += chroma[index]
            }
            luminanceMean /= pixels.size
            chromaMean /= pixels.size
            var luminanceVariance = 0f
            var chromaVariance = 0f
            for (index in pixels.indices) {
                val luminanceDelta = luminance[index] - luminanceMean
                val chromaDelta = chroma[index] - chromaMean
                luminanceVariance += luminanceDelta * luminanceDelta
                chromaVariance += chromaDelta * chromaDelta
            }
            val luminanceScale = 1f / kotlin.math.sqrt(luminanceVariance / pixels.size + 0.0001f)
            val chromaScale = 1f / kotlin.math.sqrt(chromaVariance / pixels.size + 0.0001f)
            for (index in pixels.indices) {
                output[index] = (luminance[index] - luminanceMean) * luminanceScale
                output[index + pixels.size] = (chroma[index] - chromaMean) * chromaScale
            }
            normalize(output).also {
                crop.recycle()
                if (resized !== crop) resized.recycle()
            }
        }

    override fun close() = Unit
}

private fun normalize(values: FloatArray): FloatArray {
    var magnitude = 0.0
    for (value in values) magnitude += value * value
    val scale = if (magnitude == 0.0) 1f else (1.0 / kotlin.math.sqrt(magnitude)).toFloat()
    return values.apply { for (index in indices) this[index] *= scale }
}

fun squareCrop(bitmap: Bitmap, face: Rect, scale: Float): Bitmap {
    val centerX = face.centerX().toFloat()
    val centerY = face.centerY().toFloat()
    val side = maxOf(face.width(), face.height()).toFloat() * scale
    val source = RectF(
        centerX - side / 2,
        centerY - side / 2,
        centerX + side / 2,
        centerY + side / 2
    )
    val clamped = RectF(
        source.left.coerceIn(0f, bitmap.width.toFloat()),
        source.top.coerceIn(0f, bitmap.height.toFloat()),
        source.right.coerceIn(0f, bitmap.width.toFloat()),
        source.bottom.coerceIn(0f, bitmap.height.toFloat())
    )
    val output = createBitmap(maxOf(1, clamped.width().toInt()), maxOf(1, clamped.height().toInt()))
    Canvas(output).drawBitmap(
        bitmap,
        Rect(
            clamped.left.toInt(),
            clamped.top.toInt(),
            clamped.right.toInt(),
            clamped.bottom.toInt()
        ),
        RectF(0f, 0f, output.width.toFloat(), output.height.toFloat()),
        Paint(Paint.FILTER_BITMAP_FLAG)
    )
    return output
}

private fun alignedFaceCrop(face: FaceObservation): Bitmap {
    val leftEye = face.leftEye
    val rightEye = face.rightEye
    val noseBase = face.noseBase
    val mouthLeft = face.mouthLeft
    val mouthRight = face.mouthRight
    if (leftEye == null || rightEye == null || noseBase == null || mouthLeft == null || mouthRight == null) {
        return squareCrop(
            face.bitmap,
            face.boundingBox,
            ProcessingConfig.EMBEDDING_FALLBACK_CROP_SCALE
        )
    }

    val size = ProcessingConfig.MODEL_INPUT_SIZE.toFloat()
    val sourcePoints = listOf(leftEye, rightEye, noseBase, mouthLeft, mouthRight)
    val destinationPoints = listOf(
        floatArrayOf(38.2946f, 51.6963f),
        floatArrayOf(73.5318f, 51.5014f),
        floatArrayOf(56.0252f, 71.7366f),
        floatArrayOf(41.5493f, 92.3655f),
        floatArrayOf(70.7299f, 92.2041f)
    )
    val matrix = Matrix()
    if (!setSimilarityTransform(matrix, sourcePoints, destinationPoints)) {
        return squareCrop(
            face.bitmap,
            face.boundingBox,
            ProcessingConfig.EMBEDDING_FALLBACK_CROP_SCALE
        )
    }
    return createBitmap(size.toInt(), size.toInt())
        .also { aligned ->
            aligned.eraseColor(Color.BLACK)
            Canvas(aligned).drawBitmap(face.bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        }
}

private fun setSimilarityTransform(
    matrix: Matrix,
    source: List<android.graphics.PointF>,
    destination: List<FloatArray>
): Boolean {
    if (source.size != destination.size || source.size < 2) return false
    val sourceCenterX = source.map { it.x }.average().toFloat()
    val sourceCenterY = source.map { it.y }.average().toFloat()
    val destinationCenterX = destination.map { it[0].toDouble() }.average().toFloat()
    val destinationCenterY = destination.map { it[1].toDouble() }.average().toFloat()
    var denominator = 0.0
    var aNumerator = 0.0
    var bNumerator = 0.0
    source.indices.forEach { index ->
        val sourceX = source[index].x - sourceCenterX
        val sourceY = source[index].y - sourceCenterY
        val destinationX = destination[index][0] - destinationCenterX
        val destinationY = destination[index][1] - destinationCenterY
        denominator += sourceX * sourceX + sourceY * sourceY
        aNumerator += sourceX * destinationX + sourceY * destinationY
        bNumerator += sourceX * destinationY - sourceY * destinationX
    }
    if (denominator < 0.0001) return false
    val a = (aNumerator / denominator).toFloat()
    val b = (bNumerator / denominator).toFloat()
    val translationX = destinationCenterX - a * sourceCenterX + b * sourceCenterY
    val translationY = destinationCenterY - b * sourceCenterX - a * sourceCenterY
    matrix.setValues(floatArrayOf(a, -b, translationX, b, a, translationY, 0f, 0f, 1f))
    return true
}

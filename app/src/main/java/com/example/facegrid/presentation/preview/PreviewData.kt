package com.example.facegrid.presentation.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.facegrid.domain.model.FaceObservation
import com.example.facegrid.domain.model.IdentityResult
import com.example.facegrid.domain.model.ProcessingResult
import com.example.facegrid.presentation.FaceGridUiState

object PreviewData {
    fun result(): FaceGridUiState.Result {
        val first = faceFrame(Color.rgb(214, 169, 139), Color.rgb(82, 51, 43))
        val second = faceFrame(Color.rgb(144, 181, 190), Color.rgb(34, 67, 73))
        val identities = listOf(
            IdentityResult(1, 3, observation(first, Rect(105, 72, 215, 210))),
            IdentityResult(2, 1, observation(second, Rect(110, 70, 210, 208)))
        )
        return FaceGridUiState.Result(ProcessingResult(collage(), identities))
    }

    private fun observation(bitmap: Bitmap, bounds: Rect): FaceObservation = FaceObservation(
        frameIndex = 42,
        timestampUs = 8_400_000,
        bitmap = bitmap,
        boundingBox = bounds,
        eulerY = 2f,
        eulerZ = -1f,
        smileProbability = 0.78f,
        leftEyeOpenProbability = 0.95f,
        rightEyeOpenProbability = 0.93f,
        sharpness = 120.0
    )

    private fun faceFrame(background: Int, shirt: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(320, 480, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = shirt
        canvas.drawOval(40f, 300f, 280f, 540f, paint)
        paint.color = Color.rgb(224, 177, 144)
        canvas.drawOval(86f, 65f, 234f, 260f, paint)
        paint.color = Color.rgb(55, 35, 30)
        canvas.drawOval(82f, 44f, 238f, 132f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(135f, 160f, 9f, paint)
        canvas.drawCircle(185f, 160f, 9f, paint)
        paint.color = Color.rgb(30, 30, 30)
        canvas.drawCircle(135f, 160f, 4f, paint)
        canvas.drawCircle(185f, 160f, 4f, paint)
        return bitmap
    }

    private fun collage(): Bitmap {
        val bitmap = Bitmap.createBitmap(360, 640, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(247, 244, 239))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(214, 169, 139)
        canvas.drawRoundRect(24f, 100f, 168f, 540f, 18f, 18f, paint)
        paint.color = Color.rgb(144, 181, 190)
        canvas.drawRoundRect(192f, 100f, 336f, 540f, 18f, 18f, paint)
        paint.color = Color.rgb(35, 32, 29)
        paint.textSize = 24f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("FaceGrid", 24f, 52f, paint)
        paint.color = Color.WHITE
        paint.textSize = 15f
        canvas.drawText("Person 1", 36f, 510f, paint)
        canvas.drawText("Person 2", 204f, 510f, paint)
        return bitmap
    }
}

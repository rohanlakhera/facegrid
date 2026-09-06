package com.example.facegrid

import com.example.facegrid.data.processing.cosineSimilarity
import com.example.facegrid.data.processing.normalizedAverage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessingMathTest {
    @Test
    fun cosineSimilarityRecognizesOrthogonalVectors() {
        assertEquals(0f, cosineSimilarity(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)), 0.0001f)
        assertEquals(1f, cosineSimilarity(floatArrayOf(2f, 0f), floatArrayOf(1f, 0f)), 0.0001f)
    }

    @Test
    fun normalizedAverageKeepsCentroidNormalized() {
        val average = normalizedAverage(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), 1)
        assertEquals(1f, kotlin.math.sqrt((average[0] * average[0] + average[1] * average[1]).toDouble()).toFloat(), 0.0001f)
        assertTrue(average[0] > 0f && average[1] > 0f)
    }
}

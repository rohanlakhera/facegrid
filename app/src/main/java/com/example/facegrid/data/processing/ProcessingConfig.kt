package com.example.facegrid.data.processing

object ProcessingConfig {
    const val SAMPLE_FPS = 5
    const val MIN_FACE_AREA_RATIO = 0.0125f
    const val MIN_FACE_WIDTH_RATIO = 0.06f
    // These are eligibility gates, not representative-frame scores. The
    // original values discarded entire appearances when ML Kit reported a
    // slightly angled face or when a sampled frame was mildly blurred.
    const val MIN_SHARPNESS = 2.0
    const val MAX_EULER_Y = 55f
    const val MAX_EULER_Z = 45f
    const val TRACK_MIN_IOU = 0.08f
    // At 5 sampled frames/second, a frame with no detected face marks the end
    // of a continuous appearance. This prevents a new person after a cut from
    // inheriting the previous person's track merely because boxes overlap.
    const val MAX_TRACK_MISSED_FRAMES = 0
    const val DUPLICATE_FACE_IOU = 0.35f
    const val DUPLICATE_FACE_CONTAINMENT = 0.90f
    const val MIN_SEGMENT_VISIBLE_FRAMES = 2
    const val IDENTITY_SIMILARITY_THRESHOLD = 0.50f
    // A short appearance can contain mostly blurred/oblique crops. For those
    // appearances, recover the identity from the strongest candidate crop,
    // but require a clear nearest-cluster margin to avoid arbitrary merges.
    const val SINGLETON_MERGE_SIMILARITY_THRESHOLD = 0.45f
    const val SINGLETON_MERGE_MARGIN = 0.03f
    const val SEGMENT_SIMILARITY_TOP_K = 3
    const val MAX_SEGMENT_CANDIDATE_FRAMES = 5
    const val MODEL_ASSET_NAME = "ghostfacenet_float16.tflite"
    const val MODEL_INPUT_SIZE = 112
    const val EMBEDDING_FALLBACK_CROP_SCALE = 1.25f
    const val FALLBACK_EMBEDDING_SIZE = 128
}

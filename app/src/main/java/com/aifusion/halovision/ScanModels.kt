package com.aifusion.halovision

data class ScanQuality(
    val trackingState: String,
    val depthWidth: Int,
    val depthHeight: Int,
    val sampledValidDepthPixels: Int,
    val timestampNanos: Long
)

data class SpatialPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val confidence: Int = 0
)

data class InferenceBackendStatus(
    val modelId: String,
    val requestedBackend: String,
    val activeBackend: String,
    val available: Boolean,
    val fallbackReason: String? = null
)

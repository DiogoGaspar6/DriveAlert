package com.example.drivealert.domain

class FatigueAnalyzer {

    private var closedEyesFrames = 0
    private val threshold = 20

    fun update(leftEye: Float?, rightEye: Float?): Boolean {

        if (leftEye == null || rightEye == null) return false

        val eyesClosed = leftEye < 0.4 && rightEye < 0.4

        if (eyesClosed) {
            closedEyesFrames++
        } else {
            closedEyesFrames = 0
        }

        return closedEyesFrames > threshold
    }
}
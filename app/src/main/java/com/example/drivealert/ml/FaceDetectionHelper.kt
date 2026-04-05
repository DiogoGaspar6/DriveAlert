package com.example.drivealert.ml

import android.util.Log
import com.example.drivealert.domain.FatigueAnalyzer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*

class FaceDetectorHelper {

    private val detector: FaceDetector
    private val fatigueAnalyzer = FatigueAnalyzer()

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()

        detector = FaceDetection.getClient(options)
    }

    fun processImage(image: InputImage, onResult: (Boolean) -> Unit, onComplete: () -> Unit) {
        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {

                    val face = faces[0]

                    val leftEye = face.leftEyeOpenProbability
                    val rightEye = face.rightEyeOpenProbability

                    val isTired = fatigueAnalyzer.update(leftEye, rightEye)
                    onResult(isTired)
                    if (isTired) {
                        Log.d("MLKIT", "UTILIZADOR COM SONO !!")
                    } else {
                        Log.d("MLKIT", "UTILIZADOR SEM SONO")
                    }

                } else {
                    Log.d("MLKIT", "Sem rosto")
                }
            }
            .addOnFailureListener {
                Log.e("MLKIT", "Erro na deteção", it)
            }
            .addOnCompleteListener {
                onComplete()
            }
    }
}
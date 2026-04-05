package com.example.drivealert.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.drivealert.ml.FaceDetectorHelper
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay

private val faceDetectorHelper = FaceDetectorHelper()
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScreen() {
    var isTired by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mediaPlayer = remember {
        MediaPlayer.create(
            context,
            Settings.System.DEFAULT_ALARM_ALERT_URI
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->

                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {

                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )

                                    faceDetectorHelper.processImage(
                                        image,
                                        onResult = { tired ->
                                            isTired = tired
                                        },
                                        onComplete = {
                                            imageProxy.close()
                                        }
                                    )
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        Text(
            text = if (isTired) "Cansaço detetado ⚠️" else "Atento ✅",
            modifier = Modifier.align(Alignment.BottomCenter).padding(50.dp),
            color = Color.White
        )
    }

    LaunchedEffect(isTired) {
        if (isTired) {

            while (isTired) {

                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                }

                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                val pattern = longArrayOf(
                    0,
                    500,
                    200,
                    500,
                    200,
                    800
                )

                vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )

                delay(3000)
            }
        } else {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                mediaPlayer.seekTo(0)
            }
        }
    }
}